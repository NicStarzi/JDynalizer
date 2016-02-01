package edu.udo.cs.dynalysis.processors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import edu.udo.cs.dynaliser.CallSequenceTree;
import edu.udo.cs.dynaliser.CstEvent;
import edu.udo.cs.dynalysis.JDynUtil;
import edu.udo.cs.dynalysis.JDynProcessor;

public class ElapsedTimeProc implements JDynProcessor {
	
	private final AtomicInteger outCount = new AtomicInteger(0);
	private Times times;
	private String outPath;
	
	public void setArgs(String args) {
		times = Times.parseString(JDynUtil.extractArg(args, "unit"));
		outPath = JDynUtil.extractArg(args, "outfile");
	}
	
	public void processSequence(CallSequenceTree sequence) {
		Map<String, List<Long>> timesMap = new HashMap<>();
		
		Deque<CstEvent> eventStack = new ArrayDeque<>();
		eventStack.push(sequence.getRootEvent());
		while (!eventStack.isEmpty()) {
			CstEvent event = eventStack.pop();
			if (event == null) {
				System.err.println("event=null, root="+sequence.getRootEvent());
				continue;
			}
			
			String key = event.getSignature();
			if (key != null) {
				long time = event.getAfterNanoTime() - event.getBeforeNanoTime();
				List<Long> times = timesMap.get(key);
				if (times == null) {
					times = new ArrayList<>();
					timesMap.put(key, times);
				}
				times.add(Long.valueOf(time));
			}
			if (event.getChildCount() > 0) {
				for (CstEvent childEvent : event.getChildren()) {
					eventStack.push(childEvent);
				}
			}
		}
		printTimes(timesMap);
	}
	
	private void printTimes(Map<String, List<Long>> timesMap) {
		String title = "#   Elapsed Time   #";
		StringBuilder sb = new StringBuilder(title.length());
		sb.append('#');
		for (int i = 0; i < title.length() - 2; i++) {
			sb.append('=');
		}
		sb.append('#');
		String titleCover = sb.toString();
		
		String out = outPath;
		if (JDynUtil.isOutputToFile(outPath)) {
			int number = outCount.getAndIncrement();
			out = outPath + number + ".txt";
		}
		Object printKey = JDynUtil.startPrinting(out);
		JDynUtil.print(printKey, titleCover);
		JDynUtil.print(printKey, title);
		JDynUtil.print(printKey, titleCover);
		JDynUtil.print(printKey);
		
		for (Entry<String, List<Long>> timeEntry : timesMap.entrySet()) {
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
	}
	
	public static enum Times {
		NANOS(1) {
			public String toString() {
				return "nano seconds";
			}
		},
		MICROS(1_000) {
			public String toString() {
				return "micro seconds";
			}
		},
		MILLIS(1_000_000) {
			public String toString() {
				return "milli seconds";
			}
		},
		SECONDS(1_000_000_000) {
			public String toString() {
				return "seconds";
			}
		},
		;
		
		public final double divideNanosBy;
		
		private Times(double divideNanosBy) {
			this.divideNanosBy = divideNanosBy;
		}
		
		public abstract String toString();
		
		public static Times parseString(String args) {
			if (args.isEmpty()) {
				return NANOS;
			}
			for (Times t : values()) {
				if (args.equalsIgnoreCase(t.name())) {
					return t;
				}
			}
			return NANOS;
		}
		
	}
	
}