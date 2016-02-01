package edu.udo.cs.dynalysis;

import edu.udo.cs.dynaliser.CstEventConstructor;
import edu.udo.cs.dynaliser.CstEventException;
import edu.udo.cs.dynaliser.CstEventMethod;
import edu.udo.cs.dynaliser.CstEventStaticMethod;

public interface JDynObserver {
	
	/**
	 * Called by the {@link JDynEventDispatcher} when the observer is created.<br>
	 * The argument is read from the configuration file on start up. It is never 
	 * null but may be empty. The observer is free to use it for in any way it 
	 * likes.<br>
	 * This method is not supposed to throw any exceptions. In case of malformed 
	 * input the observer should choose an appropriate way of informing the user 
	 * of the error.<br>
	 * @param args		String taken from config file, may be empty but never null
	 */
	default void setArgs(String args) {}
	
	/**
	 * Called by the {@link JDynEventDispatcher} the first time a method from the 
	 * given Class is being called.<br>
	 * This method should be called only once per Class unless the class was loaded 
	 * by multiple ClassLoaders in which case it is possible for this method to be 
	 * called again with a Class with an identical name.<br>
	 * @param clazz		never null
	 */
	default void onClassTransformed(Class<?> clazz) {}
	
	default void onConstructorTransformed(Class<?> clazz, String signature) {}
	
	default void onStaticMethodTransformed(Class<?> clazz, String signature) {}
	
	default void onMethodTransformed(Class<?> clazz, String signature) {}
	
	default void onConstructorStart(CstEventConstructor event) {}
	
	default void onConstructorEnd(CstEventConstructor event) {}
	
	default void onStaticMethodStart(CstEventStaticMethod event) {}
	
	default void onStaticMethodEnd(CstEventStaticMethod event) {}
	
	default void onMethodStart(CstEventMethod event) {}
	
	default void onMethodEnd(CstEventMethod event) {}
	
	default void onException(CstEventException event) {}
	
	default void onShutDown() {}
	
}