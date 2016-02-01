package edu.udo.cs.dynalysis.umlseq;

import java.util.HashSet;

import edu.udo.cs.dynaliser.CallSequenceTree;
import edu.udo.cs.dynaliser.CstEvent;
import edu.udo.cs.dynalysis.JDynProcessor;

public class SequenceDiagramBuilder implements JDynProcessor {
	
	public void processSequence(CallSequenceTree sequence) {
		HashSet<Object> knownObj = new HashSet<>();
		for (CstEvent event : sequence) {
			
		}
	}
	
}