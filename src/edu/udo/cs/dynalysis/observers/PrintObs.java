package edu.udo.cs.dynalysis.observers;

import edu.udo.cs.dynaliser.CstEventConstructor;
import edu.udo.cs.dynaliser.CstEventException;
import edu.udo.cs.dynaliser.CstEventMethod;
import edu.udo.cs.dynaliser.CstEventStaticMethod;
import edu.udo.cs.dynalysis.JDynObserver;

public class PrintObs implements JDynObserver {
	
	public void onConstructorStart(CstEventConstructor event) {
		System.out.println("onConstructorStart()="+event.getSignature());
	}
	
	public void onStaticMethodStart(CstEventStaticMethod event) {
		System.out.println("onStaticMethodStart()="+event.getSignature());
	}
	
	public void onMethodStart(CstEventMethod event) {
		System.out.println("onMethodStart()="+event.getSignature());
	}
	
	public void onException(CstEventException event) {
		System.out.println("onException()="+event.getSignature());
	}
	
}