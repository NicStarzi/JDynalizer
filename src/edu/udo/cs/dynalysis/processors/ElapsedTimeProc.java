package edu.udo.cs.dynalysis.processors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import edu.udo.cs.dynaliser.CallSequenceTree;
import edu.udo.cs.dynaliser.CstEvent;
import edu.udo.cs.dynalysis.JDynProcessor;
import edu.udo.cs.dynalysis.JDynUtil;

public class ElapsedTimeProc implements JDynProcessor {
	
	private final AtomicInteger outCount = new AtomicInteger(0);
	private Times times;
	private SortBy sortBy;
	private String outPath;
	private int minCallCount;
	
	@Override
	public void setArgs(String args) {
		times = Times.parseString(JDynUtil.extractArg(args, "unit"));
		sortBy = SortBy.parseString(JDynUtil.extractArg(args, "sortBy"));
		outPath = JDynUtil.extractArg(args, "outfile");
		minCallCount = JDynUtil.toInt(JDynUtil.extractArg(args, "minCallCount"), -1);
	}
	
	private static class Record {
		final String sig;
		long best;
		long worst;
		long total;
		int count;
		public Record(String signature, long firstTime) {
			sig = signature;
			best = worst = total = firstTime;
			count = 1;
		}
		public void add(long time) {
			if (best > time) {
				best = time;
			}
			if (worst < time) {
				worst = time;
			}
			total += time;
			count++;
		}
	}
	
	@Override
	public void processSequence(CallSequenceTree sequence) {
		Map<String, Record> timesMap = new HashMap<>();
		
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
				Record rec = timesMap.get(key);
				if (rec == null) {
					rec = new Record(key, time);
					timesMap.put(key, rec);
				} else {
					rec.add(time);
				}
//				List<Long> times = timesMap.get(key);
//				if (times == null) {
//					times = new ArrayList<>();
//					timesMap.put(key, times);
//				}
//				times.add(Long.valueOf(time));
			}
			if (event.getChildCount() > 0) {
				for (CstEvent childEvent : event.getChildren()) {
					eventStack.push(childEvent);
				}
			}
		}
		filterTimes(timesMap);
		if (timesMap.size() > 0) {
			printTimes(timesMap);
		}
	}
	
	private void filterTimes(Map<String, Record> timesMap) {
		Iterator<Entry<String, Record>> iter = timesMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Record> entry = iter.next();
			Record rec = entry.getValue();
			if (rec.count < minCallCount) {
				iter.remove();
			}
		}
	}
	
	private void printTimes(Map<String, Record> timesMap) {
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
		
		Collection<Record> records;
		if (sortBy == null) {
			records = timesMap.values();
		} else {
			List<Record> recordsList = new ArrayList<>(timesMap.values());
			sortBy.sort(recordsList);
			records = recordsList;
		}
		for (Record rec : records) {
			JDynUtil.print(printKey, "=====  ", rec.sig, "  =====");
			
			long worst = (long) (rec.worst / times.divideNanosBy);
			long best = (long) (rec.best / times.divideNanosBy);
			long total = (long) (rec.total / times.divideNanosBy);
			double avg = total / (double) rec.count;
			
			String suffix = times.toString();
			
			JDynUtil.print(printKey, "calls\t= ", rec.count);
			JDynUtil.print(printKey, "best\t= ", best, " \t", suffix);
			JDynUtil.print(printKey, "worst\t= ", worst, " \t", suffix);
			JDynUtil.print(printKey, "total\t= ", total, " \t", suffix);
			JDynUtil.print(printKey, "average\t= ", avg, " \t", suffix, "\n");
		}
		JDynUtil.stopPrinting(printKey);
	}
	
	public static enum SortBy {
		CALLS	((r1, r2) -> Integer.compare(r2.count, r1.count)),
		BEST	((r1, r2) -> Long.compare(r2.best, r1.best)),
		WORST	((r1, r2) -> Long.compare(r2.worst, r1.worst)),
		TOTAL	((r1, r2) -> Long.compare(r2.total, r1.total)),
		AVERAGE	((r1, r2) -> Double.compare(r2.total / (double) r2.count, r1.total / (double) r1.count)),
		;
		
		final Comparator<Record> comparator;
		
		private SortBy(Comparator<Record> comparator) {
			this.comparator = comparator;
		}
		
		public void sort(List<Record> records) {
			records.sort(comparator);
		}
		
		public static SortBy parseString(String args) {
			if (args.isEmpty()) {
				return null;
			}
			for (SortBy t : SortBy.values()) {
				if (args.equalsIgnoreCase(t.name())) {
					return t;
				}
			}
			return null;
		}
	}
	
	public static enum Times {
		NANOS(1) {
			@Override
			public String toString() {
				return "nano seconds";
			}
		},
		MICROS(1_000) {
			@Override
			public String toString() {
				return "micro seconds";
			}
		},
		MILLIS(1_000_000) {
			@Override
			public String toString() {
				return "milli seconds";
			}
		},
		SECONDS(1_000_000_000) {
			@Override
			public String toString() {
				return "seconds";
			}
		},
		;
		
		public final double divideNanosBy;
		
		private Times(double divideNanosBy) {
			this.divideNanosBy = divideNanosBy;
		}
		
		@Override
		public abstract String toString();
		
		public static Times parseString(String args) {
			if (args.isEmpty()) {
				return NANOS;
			}
			for (Times t : Times.values()) {
				if (args.equalsIgnoreCase(t.name())) {
					return t;
				}
			}
			return NANOS;
		}
		
	}
	
}