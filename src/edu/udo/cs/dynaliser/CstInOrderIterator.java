package edu.udo.cs.dynaliser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;

public class CstInOrderIterator {
	
	private final Deque<CstEvent> stack = new ArrayDeque<>();
	private final CallSequenceTree seq;
	
	CstInOrderIterator(CallSequenceTree seqTree) {
		seq = seqTree;
		if (seq.getRootEvent() != null) {
			stack.push(seq.getRootEvent());
		}
	}
	
	public boolean hasNext() {
		return stack.size() > 0;
	}
	
	public CstEvent next() {
		CstEvent current = stack.pollFirst();
		
		List<CstEvent> children = current.getChildren();
		ListIterator<CstEvent> iter = children.listIterator(children.size());
		while (iter.hasPrevious()) {
			stack.addFirst(iter.previous());
		}
		return current;
	}
	
}