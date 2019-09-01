package edu.udo.cs.dynalysis.observers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.udo.cs.dynaliser.CstEvent;
import edu.udo.cs.dynaliser.CstEventConstructor;
import edu.udo.cs.dynaliser.CstEventMethod;
import edu.udo.cs.dynaliser.CstEventStaticMethod;
import edu.udo.cs.dynalysis.JDynObserver;
import edu.udo.cs.dynalysis.JDynUtil;
import edu.udo.cs.dynalysis.processors.ElapsedTimeProc.Times;

public class ElapsedTimeObs implements JDynObserver {
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<String, PerMethodData> measuredTimeMap = new HashMap<>();
	private Times times;
	private String outPath;
	private int minCalls;
	
	public void setArgs(String args) {
		times = Times.parseString(JDynUtil.extractArg(args, "unit"));
		outPath = JDynUtil.extractArg(args, "outfile");
		String minCallsStr = JDynUtil.extractArg(args, "mincalls");
		if (minCallsStr != null && !minCallsStr.isEmpty()) {
			try {
				minCalls = Integer.parseInt(minCallsStr);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			minCalls = -1;
		}
	}
	
	public void onStaticMethodEnd(CstEventStaticMethod event) {
		recordTime(event);
	}
	
	public void onConstructorEnd(CstEventConstructor event) {
		recordTime(event);
	}
	
	public void onMethodEnd(CstEventMethod event) {
		recordTime(event);
	}
	
	private void recordTime(CstEvent event) {
		String sig = event.getSignature();
		long time = event.getAfterNanoTime() - event.getBeforeNanoTime();
		
		lock.writeLock().lock();
		try {
			PerMethodData data = measuredTimeMap.get(sig);
			if (data == null) {
				data = new PerMethodData();
				measuredTimeMap.put(sig, data);
			}
			data.calls++;
			data.total += time;
			if (time > data.worst) {
				data.worst = time;
			}
			if (time < data.best) {
				data.best = time;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public void onShutDown() {
		String title = "#   Elapsed Time   #";
		StringBuilder sb = new StringBuilder(title.length());
		sb.append('#');
		for (int i = 0; i < title.length() - 2; i++) {
			sb.append('=');
		}
		sb.append('#');
		String titleCover = sb.toString();
		
		Object printKey = JDynUtil.startPrinting(outPath);
		JDynUtil.print(printKey, titleCover);
		JDynUtil.print(printKey, title);
		JDynUtil.print(printKey, titleCover);
		JDynUtil.print(printKey);
		
		lock.readLock().lock();
		try {
			for (Entry<String, PerMethodData> timeEntry : measuredTimeMap.entrySet()) {
				PerMethodData data = timeEntry.getValue();
				if (minCalls > 0 && data.calls < minCalls) {
					continue;
				}
				JDynUtil.print(printKey, "=====  ", timeEntry.getKey(), "  =====");
				
				long worst = (long) (data.worst / times.divideNanosBy);
				long best = (long) (data.best / times.divideNanosBy);
				long total = (long) (data.total / times.divideNanosBy);
				double avg = total / data.calls;
				
				String suffix = times.toString();
				
				JDynUtil.print(printKey, "calls\t= ", data.calls);
				JDynUtil.print(printKey, "best\t= ", best, " \t", suffix);
				JDynUtil.print(printKey, "worst\t= ", worst, " \t", suffix);
				JDynUtil.print(printKey, "total\t= ", total, " \t", suffix);
				JDynUtil.print(printKey, "average\t= ", avg, " \t", suffix, "\n");
			}
			JDynUtil.stopPrinting(printKey);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	protected static class PerMethodData {
		long calls;
		long best = Long.MAX_VALUE;
		long worst = Long.MIN_VALUE;
		long total;
	}
	
}