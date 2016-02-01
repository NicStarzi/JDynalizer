package edu.udo.cs.dynaliser;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

import edu.udo.cs.dynalysis.JDynProcessor;

public class JDynProcessorThread {
	
	private final ReentrantLock seqsLock = new ReentrantLock();
	private final ReentrantLock threadLock = new ReentrantLock();
	private final JDynSettings settings;
	private volatile Thread thread;
	private Deque<CallSequenceTree> seqsToProcess;
	private Collection<JDynProcessor> procs;
	
	public JDynProcessorThread(JDynSettings settings) {
		if (settings == null) {
			throw new IllegalArgumentException("settings == null || !settings.hasProcessors()");
		}
		this.settings = settings;
	}
	
	public void process(CallSequenceTree seq) {
		if (!settings.hasProcessors()) {
			return;
		}
		seqsLock.lock();
		try {
			lazyInitializeStack();
			seqsToProcess.push(seq);
		} finally {
			seqsLock.unlock();
		}
		threadLock.lock();
		try {
			if (thread == null) {
				// This thread should NOT be a demon thread,
				// it may have to run after the instrumented program has finished
				thread = new Thread(() -> threadRun());
				thread.start();
			}
		} finally {
			threadLock.unlock();
		}
	}
	
	private void lazyInitializeStack() {
		if (seqsToProcess == null) {
			seqsToProcess = new ArrayDeque<>();
		}
	}
	
	private void lazyInitializeProcessors() {
		if (procs == null) {
			procs = settings.createProcessors();
		}
	}
	
	private void threadRun() {
		while (true) {
			CallSequenceTree seq;
			seqsLock.lock();
			try {
				seq = seqsToProcess.poll();
			} finally {
				seqsLock.unlock();
			}
			if (seq == null) {
				break;
			}
			lazyInitializeProcessors();
			for (JDynProcessor proc : procs) {
				proc.processSequence(seq);
			}
		}
		threadLock.lock();
		try {
			thread = null;
		} finally {
			threadLock.unlock();
		}
	}
	
}