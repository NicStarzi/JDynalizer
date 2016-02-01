package proc;

import edu.udo.cs.jcstg.CallSequence;
import edu.udo.cs.jcstg.CallSequenceProcessor;
import edu.udo.cs.jcstg.CallSequence.Event;

public class TestProc implements CallSequenceProcessor {
	
	public void processSequence(CallSequence seq) {
		appendRecursive(seq.getRootEvent());
	}
	
	private void appendRecursive(Event event) {
		System.out.println("EVENT: "+event.toString());
		for (Event child : event.getChildren()) {
			appendRecursive(child);
		}
	}
	
}