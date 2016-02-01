package edu.udo.cs.dynalysis;

import edu.udo.cs.dynaliser.CallSequenceTree;

public interface JDynProcessor {
	
	default void setArgs(String args) {}
	
	public void processSequence(CallSequenceTree sequence);
	
}