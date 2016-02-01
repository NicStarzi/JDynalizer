package edu.udo.cs.dynalysis.observers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private final Map<String, List<Long>> measuredTimeMap = new HashMap<>();
	private Times times;
	private String outPath;
	
	public void setArgs(String args) {
		times = Times.parseString(JDynUtil.extractArg(args, "unit"));
		outPath = JDynUtil.extractArg(args, "outfile");
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
			List<Long> timesList = measuredTimeMap.get(sig);
			if (timesList == null) {
				timesList = new ArrayList<>();
				measuredTimeMap.put(sig, timesList);
			}
			timesList.add(Long.valueOf(time));
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
			for (Entry<String, List<Long>> timeEntry : measuredTimeMap.entrySet()) {
				JDynUtil.print(printKey, "=====  ", timeEntry.getKey(), "  =====");
				
				long worst = Long.MIN_VALUE;
				long best = Long.MAX_VALUE;
				long total = 0;
				double avg = 0;
				
				for (Long value : timeEntry.getValue()) {
					if (worst < value) {
						worst = value;
					}
					if (best > value) {
						best = value;
					}
					total += value;
				}
				worst /= times.divideNanosBy;
				best /= times.divideNanosBy;
				total /= times.divideNanosBy;
				avg = total / (double) timeEntry.getValue().size();
				
				String suffix = times.toString();
				
				JDynUtil.print(printKey, "calls\t= ", timeEntry.getValue().size());
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
	
}