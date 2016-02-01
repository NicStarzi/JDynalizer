package edu.udo.cs.dynalysis.processors;

import edu.udo.cs.dynaliser.CallSequenceTree;
import edu.udo.cs.dynaliser.CstEvent;
import edu.udo.cs.dynalysis.JDynProcessor;

public class ToConsole implements JDynProcessor {
	
	public void setArgs(String args) {
	}
	
	public void processSequence(CallSequenceTree seq) {
		appendRecursive(seq.getRootEvent());
	}
	
	private void appendRecursive(CstEvent event) {
		System.out.println(event.toString());
		for (CstEvent child : event.getChildren()) {
			appendRecursive(child);
		}
	}
	
}