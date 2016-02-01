package edu.udo.cs.dynaliser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import edu.udo.cs.dynalysis.JDynObserver;

public class JDynEventDispatcher {
	
	private static final boolean NO_IDENTIFY;
	private static final boolean DEBUG;
	/**
	 * Used for locking the object identification method.
	 */
	private static final Object identifyLock;
	/**
	 * Used to count how many instances of a particular class have been created
	 */
	private static final Map<Class<?>, Integer> classInstanceCountMap;
	/**
	 * Maps objects to a unique integer id to distinguish between them.<br>
	 * An {@link IdentityHashMap} is used in case the user has overwritten 
	 * the {@link Object#hashCode()} and/or {@link Object#equals(Object)} 
	 * methods.<br>
	 */
	private static final Map<Object, Integer> objectToIdMap;
	/**
	 * Maps threads to sequences so that each thread has its own
	 */
	private static final Map<Thread, CallSequenceTree> threadToSequenceMap = new HashMap<>();
	private static final JDynProcessorThread procThread;
	/**
	 * A Collection of all observers that are to be notified of class or 
	 * method transformations.<br>
	 */
	private static final Iterable<JDynObserver> observers;
	private static final JDynSettings settings;
	
	static {
		Preferences prefs = Preferences.userRoot().node(JDynAgent.JDYN_PREFERENCES);
		settings = new JDynSettings(prefs);
		procThread = new JDynProcessorThread(settings);
		// The debug status of settings, although mutable, does never change
		DEBUG = settings.isDebug();
		if (DEBUG) {
			JDynAgent.debugMsg(JDynEventDispatcher.class.getSimpleName(), "initialized");
		}
		if (settings.hasObservers()) {
			if (DEBUG) {
				JDynAgent.debugMsg("Create", JDynObserver.class.getSimpleName(), "instances");
			}
			observers = settings.createObservers();
		} else {
			observers = Collections.emptyList();
		}
		
		// If the NO_IDENTIFY option is set we won't identify objects uniquely.
		NO_IDENTIFY = settings.isNoIdentify();
		if (NO_IDENTIFY) {
			if (DEBUG) {
				JDynAgent.debugMsg("Disable object identification");
			}
			// With NO_IDENTIFY we don't need these
			identifyLock = null;
			classInstanceCountMap = null;
			objectToIdMap = null;
		} else {
			if (DEBUG) {
				JDynAgent.debugMsg("Enable object identification");
			}
			identifyLock = new Object();
			classInstanceCountMap = new HashMap<>();
			objectToIdMap = new IdentityHashMap<>();
		}
		
		registerShutdownHook();
	}
	
	private static void registerShutdownHook() {
		if (DEBUG) {
			JDynAgent.debugMsg("Register shutdown hooks");
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			onShutDown();
		}));
	}
	
	private static void onShutDown() {
		List<CallSequenceTree> allSeqs = getAllCurrentSequences();
		for (CallSequenceTree seq : allSeqs) {
			if (!seq.isFinal()) {
				processSequence(seq);
			}
		}
		for (JDynObserver obs : observers) {
			try {
				obs.onShutDown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static List<CallSequenceTree> getAllCurrentSequences() {
		synchronized (threadToSequenceMap) {
			return new ArrayList<>(threadToSequenceMap.values());
		}
	}
	
	public static void classInit(Class<?> clazz, String methods) {
		if (DEBUG) {
			JDynAgent.debugMsg("Class", clazz, 
					"was initialized with instrumented methods", methods);
		}
		String[] methodNames = methods.split(";");
		for (JDynObserver obs : observers) {
			try {
				obs.onClassTransformed(clazz);
				for (int i = 0; i < methodNames.length; i++) {
					try {
						obs.onMethodTransformed(clazz, methodNames[i]);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void beforeConstructor(
			Class<?> clazz, 
			String sig, 
			Object[] params, 
			boolean hasParams) 
	{
		if (DEBUG) {
			JDynAgent.debugMsg("Constructor of class", clazz, "started");
		}
		transformParams(params, hasParams);
		CallSequenceTree seq = getCurrentThreadSequence();
		CstEventConstructor event = seq.beforeConstructor(
				clazz, sig, params, hasParams);
		
		fireConstructorStart(observers, event);
	}
	
	public static void afterConstructor(Object object) {
		if (DEBUG) {
			JDynAgent.debugMsg("Constructor of class", object.getClass(), "ended");
		}
		CallSequenceTree seq = getCurrentThreadSequence();
		CstEventConstructor event = seq.afterConstructor(object);
		if (!event.isSuperConstructor()) {
			int id = identifyObject(object);
			event.setID(id);
		}
		processSequenceIfNecessary(seq);
		
		fireConstructorEnd(observers, event);
	}
	
	public static void beforeStaticMethod(
			Class<?> clazz, 
			String sig, 
			Object[] params, 
			boolean hasParams) 
	{
		if (DEBUG) {
			JDynAgent.debugMsg("Static method", sig, 
					"of class", clazz, "started");
		}
		transformParams(params, hasParams);
		CallSequenceTree seq = getCurrentThreadSequence();
		CstEventStaticMethod event = seq.beforeStaticMethod(
				clazz, sig, params, hasParams);
		
		fireStaticMethodStart(observers, event);
	}
	
	public static void afterStaticMethod(
			Class<?> clazz, 
			String sig, 
			Object returnedValue, 
			boolean hasReturn) 
	{
		if (DEBUG) {
			JDynAgent.debugMsg("Static method", sig, 
					"of class", clazz, "ended");
		}
		returnedValue = transformReturnValue(returnedValue, hasReturn);
		CallSequenceTree seq = getCurrentThreadSequence();
		CstEventStaticMethod event = seq.afterStaticMethod(
				clazz, sig, returnedValue, hasReturn);
		
		processSequenceIfNecessary(seq);
		fireStaticMethodEnd(observers, event);
	}
	
	public static void beforeMethod(
			Object obj, 
			String sig, 
			Object[] params, 
			boolean hasParams) 
	{
		if (DEBUG) {
			JDynAgent.debugMsg("Method", sig, 
					"of class", obj.getClass(), "started");
		}
		transformParams(params, hasParams);
		CallSequenceTree seq = getCurrentThreadSequence();
		CstEventMethod event = seq.beforeMethod(obj, 
				identifyObject(obj), sig, params, hasParams);
		
		fireMethodStart(observers, event);
	}
	
	public static void afterMethod(
			Object obj, 
			String sig, 
			Object returnedValue, 
			boolean hasReturn) 
	{
		if (DEBUG) {
			JDynAgent.debugMsg("Method", sig, 
					"of class", obj.getClass(), "ended");
		}
		returnedValue = transformReturnValue(returnedValue, hasReturn);
		CallSequenceTree seq = getCurrentThreadSequence();
		CstEventMethod event = seq.afterMethod(obj, sig, 
				returnedValue, hasReturn);
		
		processSequenceIfNecessary(seq);
		fireMethodEnd(observers, event);
	}
	
	public static void exception(Exception exception) {
		if (DEBUG) {
			JDynAgent.debugMsg("Exception", exception.getClass(), 
					"was thrown");
		}
		CallSequenceTree seq = getCurrentThreadSequence();
		CstEventException event = seq.exception(exception, 
				identifyObject(exception));
		
		processSequenceIfNecessary(seq);
		fireException(observers, event);
	}
	
	private static void transformParams(Object[] params, boolean hasParams) {
		if (hasParams) {
			for (int i = 0; i < params.length; i++) {
				Integer id = probeObject(params[i]);
				if (id != null) {
					params[i] = new IdentifiedObject(params[i], id);
				}
			}
		}
	}
	
	private static Object transformReturnValue(Object returnedValue, boolean hasReturn) {
		if (hasReturn && !NO_IDENTIFY) {
			Integer id = probeObject(returnedValue);
			if (id != null) {
				returnedValue = new IdentifiedObject(returnedValue, id);
			}
		}
		return returnedValue;
	}
	
	private static void processSequenceIfNecessary(final CallSequenceTree seq) {
		if (seq.isFinal()) {
			processSequence(seq);
		}
	}
	
	protected static void processSequence(final CallSequenceTree seq) {
		if (DEBUG) {
			JDynAgent.debugMsg("Process call sequence with", seq.getEventCount(), "events");
		}
		procThread.process(seq);
	}
	
	private static CallSequenceTree getCurrentThreadSequence() {
		synchronized (threadToSequenceMap) {
			Thread thread = Thread.currentThread();
			CallSequenceTree seq = threadToSequenceMap.get(thread);
			if (seq == null || seq.isFinal()) {
				seq = new CallSequenceTree(thread);
				threadToSequenceMap.put(thread, seq);
			}
			return seq;
		}
	}
	
	private static Integer probeObject(Object obj) {
		if (NO_IDENTIFY) {
			return null;
		}
		synchronized (identifyLock) {
			return objectToIdMap.get(obj);
		}
	}
	
	private static Integer identifyObject(Object obj) {
		if (NO_IDENTIFY) {
			return Integer.valueOf(-1);
		}
		synchronized (identifyLock) {
			Integer id = objectToIdMap.get(obj);
			if (id == null) {
				Integer classInstanceCount = classInstanceCountMap.get(obj.getClass());
				if (classInstanceCount == null) {
					classInstanceCount = Integer.valueOf(0);
				}
				int oldValue = classInstanceCount.intValue();
				id = classInstanceCount;
				classInstanceCount = Integer.valueOf(oldValue + 1);
				classInstanceCountMap.put(obj.getClass(), classInstanceCount);
				objectToIdMap.put(obj, id);
			}
			return id;
		}
	}
	
	private static void fireConstructorStart(
			Iterable<JDynObserver> obses, CstEventConstructor e) 
	{
		fireObsEvent(obses, (obs) -> obs.onConstructorStart(e));
	}
	
	private static void fireConstructorEnd(
			Iterable<JDynObserver> obses, CstEventConstructor e) 
	{
		fireObsEvent(obses, (obs) -> obs.onConstructorEnd(e));
	}
	
	private static void fireStaticMethodStart(
			Iterable<JDynObserver> obses, CstEventStaticMethod e) 
	{
		fireObsEvent(obses, (obs) -> obs.onStaticMethodStart(e));
	}
	
	private static void fireStaticMethodEnd(
			Iterable<JDynObserver> obses, CstEventStaticMethod e) 
	{
		fireObsEvent(obses, (obs) -> obs.onStaticMethodEnd(e));
	}
	
	private static void fireMethodStart(
			Iterable<JDynObserver> obses, CstEventMethod e) 
	{
		fireObsEvent(obses, (obs) -> obs.onMethodStart(e));
	}
	
	private static void fireMethodEnd(
			Iterable<JDynObserver> obses, CstEventMethod e) 
	{
		fireObsEvent(obses, (obs) -> obs.onMethodEnd(e));
	}
	
	private static void fireException(
			Iterable<JDynObserver> obses, CstEvent event) 
	{
		CstEventException e = (CstEventException) event;
		fireObsEvent(obses, (obs) -> obs.onException(e));
	}
	
	private static void fireObsEvent(Iterable<JDynObserver> obses, 
			ObsEventAct act) 
	{
		for (JDynObserver obs : obses) {
			try {
				act.forObs(obs);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static interface ObsEventAct {
		void forObs(JDynObserver obs);
	}
	
}