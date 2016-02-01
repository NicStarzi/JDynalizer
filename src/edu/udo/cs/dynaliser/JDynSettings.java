package edu.udo.cs.dynaliser;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import edu.udo.cs.dynalysis.JDynObserver;
import edu.udo.cs.dynalysis.JDynProcessor;

public class JDynSettings {
	
	/*
	 * These are used for reading and writing of preferences.
	 */
	private static final String PREFERENCE_KEYWORD_COUNT_INCLUDED = "countInc";
	private static final String PREFERENCE_KEYWORD_INCLUDED = "inc";
	private static final String PREFERENCE_KEYWORD_COUNT_EXCLUDED = "countExc";
	private static final String PREFERENCE_KEYWORD_EXCLUDED = "exc";
	private static final String PREFERENCE_KEYWORD_COUNT_METHOD = "countMtd";
	private static final String PREFERENCE_KEYWORD_METHOD = "mtd";
	private static final String PREFERENCE_KEYWORD_COUNT_PROCESSOR = "countProc";
	private static final String PREFERENCE_KEYWORD_PROCESSOR = "proc";
	private static final String PREFERENCE_KEYWORD_PROCESSOR_ARGS = "procArgs";
	private static final String PREFERENCE_KEYWORD_COUNT_OBSERVER = "countObs";
	private static final String PREFERENCE_KEYWORD_OBSERVER = "obs";
	private static final String PREFERENCE_KEYWORD_OBSERVER_ARGS = "obsArgs";
	private static final String PREFERENCE_KEYWORD_NO_IDENTIFY = "noIdentify";
	private static final String PREFERENCE_KEYWORD_DEBUG = "debug";
	
	/**
	 * Prefixes of classes that should be included in the analysis.
	 */
	private final String[] incPrefixes;
	/**
	 * Prefixes of classes that should be excluded from the analysis.
	 */
	private final String[] excPrefixes;
	/**
	 * Prefixes of methods that should be excluded from the analysis.
	 */
	private final String[] excMethods;
	/**
	 * Full class name and args String for all {@link JDynProcessor processors}.
	 */
	private final ClassAndArgs[] procClasses;
	/**
	 * Full class name and args String for all {@link JDynObserver observers}.
	 */
	private final ClassAndArgs[] obsClasses;
	private final boolean noIdentify;
	private boolean debug;
	
	/**
	 * Attempts to construct a {@link JDynSettings} object from preferences.
	 * @param prefs		must be non-null
	 */
	public JDynSettings(Preferences prefs) {
		int countInc = prefs.getInt(PREFERENCE_KEYWORD_COUNT_INCLUDED, 0);
		incPrefixes = new String[countInc];
		for (int i = 0; i < countInc; i++) {
			String incClsName = prefs.get(PREFERENCE_KEYWORD_INCLUDED + i, null);
			incPrefixes[i] = incClsName;
		}
		
		int countExc = prefs.getInt(PREFERENCE_KEYWORD_COUNT_EXCLUDED, 0);
		excPrefixes = new String[countExc];
		for (int i = 0; i < countExc; i++) {
			String excClsName = prefs.get(PREFERENCE_KEYWORD_EXCLUDED + i, null);
			excPrefixes[i] = excClsName;
		}
		
		int countMtd = prefs.getInt(PREFERENCE_KEYWORD_COUNT_METHOD, 0);
		excMethods = new String[countMtd];
		for (int i = 0; i < countMtd; i++) {
			String excMtdName = prefs.get(PREFERENCE_KEYWORD_METHOD + i, null);
			excMethods[i] = excMtdName;
		}
		
		int countProc = prefs.getInt(PREFERENCE_KEYWORD_COUNT_PROCESSOR, 0);
		procClasses = new ClassAndArgs[countProc];
		for (int i = 0; i < countProc; i++) {
			String procClsName = prefs.get(PREFERENCE_KEYWORD_PROCESSOR + i, null);
			String procArgs = prefs.get(PREFERENCE_KEYWORD_PROCESSOR_ARGS + i, null);
			procClasses[i] = new ClassAndArgs(procClsName, procArgs);
		}
		extractClasses(procClasses);
		
		int countObs = prefs.getInt(PREFERENCE_KEYWORD_COUNT_OBSERVER, 0);
		obsClasses = new ClassAndArgs[countObs];
		for (int i = 0; i < countObs; i++) {
			String obsClsName = prefs.get(PREFERENCE_KEYWORD_OBSERVER + i, null);
			String obsArgs = prefs.get(PREFERENCE_KEYWORD_OBSERVER_ARGS + i, null);
			obsClasses[i] = new ClassAndArgs(obsClsName, obsArgs);
		}
		extractClasses(obsClasses);
		
		noIdentify = prefs.getBoolean(PREFERENCE_KEYWORD_NO_IDENTIFY, false);
		debug = prefs.getBoolean(PREFERENCE_KEYWORD_DEBUG, false);
	}
	
	/**
	 * Constructs {@link JDynSettings} with the given arguments.
	 * @param includedClasses
	 * @param excludedClasses
	 * @param excludedMethods
	 * @param processorClasses
	 * @param observerClasses
	 */
	protected JDynSettings(
			String[] includedClasses, 
			String[] excludedClasses, 
			String[] excludedMethods, 
			ClassAndArgs[] processorClasses, 
			ClassAndArgs[] observerClasses,
			boolean identifyObjects, 
			boolean debugMode) 
	{
		incPrefixes = includedClasses;
		excPrefixes = excludedClasses;
		excMethods = excludedMethods;
		procClasses = processorClasses;
		obsClasses = observerClasses;
		noIdentify = !identifyObjects;
		debug = debugMode;
		extractClasses(procClasses);
		extractClasses(obsClasses);
	}
	
	public void setDebug() {
		debug = true;
	}
	
	public boolean isDebug() {
		return debug;
	}
	
	public boolean isNoIdentify() {
		return noIdentify;
	}
	
	/**
	 * Returns true if the list of included class prefixes is not empty.<br>
	 * @return		true if there are any classes that are included
	 */
	public boolean hasClassesIncluded() {
		return incPrefixes.length > 0;
	}
	
	/**
	 * Returns an array of Class objects from the given array of class names.<br>
	 * This method can find both classes from the class path and from other 
	 * locations on disc by use of a {@link ClassLoader}.<br>
	 * If a class can not be found it will not be included in the result without 
	 * any exceptions being thrown.<br>
	 * @param classNames		must be non-null
	 */
	private void extractClasses(ClassAndArgs[] classNames) {
//		List<Class<?>> classList = new ArrayList<>();
		
		// Lazily initialized
		ProxyClassLoader proxyClassLoader = null;
		for (int i = 0; i < classNames.length; i++) {
			String clsName = classNames[i].getFullClassName();
			
			try {
				Class<?> clazz = Class.forName(clsName);
				classNames[i].asClass = clazz;
//				classList.add(clazz);
			} catch (Exception e1) {
				String clsFileName = clsName.replace('.', '/') + ".class";
				File clsFile = new File(clsFileName);
				if (!clsFile.exists() || clsFile.isDirectory()) {
					e1.printStackTrace();
					continue;
				}
				try (FileInputStream fis = new FileInputStream(clsFile)) {
					byte[] classBytes = new byte[fis.available()];
					fis.read(classBytes);
					
					if (proxyClassLoader == null) {
						proxyClassLoader = new ProxyClassLoader();
					}
					Class<?> clazz = proxyClassLoader.loadFromBytes(clsName, classBytes);
					classNames[i].asClass = clazz;
//					classList.add(clazz);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			
		}
//		Class<?>[] arr = new Class[classList.size()];
//		return classList.toArray(arr);
	}
	
	/**
	 * Returns true if the class with the given class name should be included 
	 * in the analysis.<br>
	 * @param className			the full name of the class including packages
	 * @return					true if the class is supposed to be instrumented
	 */
	public boolean isClassIncluded(String className) {
		for (int i = 0; i < excPrefixes.length; i++) {
			if (className.startsWith(excPrefixes[i])) {
				return false;
			}
		}
		for (int i = 0; i < incPrefixes.length; i++) {
			if (className.startsWith(incPrefixes[i])) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the method with the given signature should be included 
	 * in the analysis.<br>
	 * @param methodSignature	the full signature of the method including the 
	 * 							full name of the enclosing class
	 * @return					true if the method is supposed to be instrumented
	 */
	public boolean isMethodIncluded(String methodSignature) {
		for (int i = 0; i < excMethods.length; i++) {
			if (methodSignature.equals(excMethods[i])) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns true if there is at least one {@link JDynProcessor} 
	 * registered in the settings file.<br>
	 * @return					true if the Collection of {@link JDynProcessor CallSequenceProcessors} 
	 * 							returned by {@link #createProcessors()} is not empty. 
	 */
	public boolean hasProcessors() {
		return procClasses.length > 0;
	}
	
	/**
	 * Creates and returns a Collection of instances of all 
	 * {@link JDynProcessor CallSequenceProcessors} that are registered.
	 * @return					a non-null Collection of {@link JDynProcessor CallSequenceProcessors}
	 */
	public Collection<JDynProcessor> createProcessors() {
		int count = procClasses.length;
		List<JDynProcessor> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			@SuppressWarnings("unchecked")
			Class<JDynProcessor> procClass = (Class<JDynProcessor>) procClasses[i].asClass;
			try {
				JDynProcessor proc = procClass.newInstance();
				proc.setArgs(procClasses[i].getArgsString());
				list.add(proc);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}
	
	/**
	 * Returns true if there is at least one {@link JDynObserver} 
	 * registered in the settings file.<br>
	 * @return					true if the Collection of {@link JDynObserver CstgObservers} 
	 * 							returned by {@link #createObservers()} is not empty. 
	 */
	public boolean hasObservers() {
		return obsClasses.length > 0;
	}
	
	/**
	 * Creates and returns a Collection of instances of all 
	 * {@link JDynObserver CstgObservers} that are registered.
	 * @return					a non-null Collection of {@link JDynObserver CstgObservers}
	 */
	public Collection<JDynObserver> createObservers() {
		int count = obsClasses.length;
		if (count == 0) {
			return Collections.emptyList();
		}
		List<JDynObserver> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			@SuppressWarnings("unchecked")
			Class<JDynObserver> obsClass = (Class<JDynObserver>) obsClasses[i].asClass;
			try {
				JDynObserver obs = obsClass.newInstance();
				obs.setArgs(obsClasses[i].getArgsString());
				list.add(obs);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}
	
	/**
	 * Writes the contents of this {@link JDynSettings} object to the given preferences.<br>
	 * The contents are written in a way that a {@link JDynSettings} object with identical 
	 * contents can be recreated from the preferences by using the {@link #JDynSettings(Preferences)} 
	 * constructor.<br>
	 * @param prefs			a non-null {@link Preferences} object
	 */
	public void writeToPreferences(Preferences prefs) {
		int countInc = incPrefixes.length;
		prefs.putInt(PREFERENCE_KEYWORD_COUNT_INCLUDED, countInc);
		for (int i = 0; i < countInc; i++) {
			prefs.put(PREFERENCE_KEYWORD_INCLUDED+i, incPrefixes[i]);
		}
		
		int countExc = excPrefixes.length;
		prefs.putInt(PREFERENCE_KEYWORD_COUNT_EXCLUDED, countExc);
		for (int i = 0; i < countExc; i++) {
			prefs.put(PREFERENCE_KEYWORD_EXCLUDED+i, excPrefixes[i]);
		}
		
		int countProc = procClasses.length;
		prefs.putInt(PREFERENCE_KEYWORD_COUNT_PROCESSOR, countProc);
		for (int i = 0; i < countProc; i++) {
			prefs.put(PREFERENCE_KEYWORD_PROCESSOR+i, procClasses[i].getFullClassName());
			prefs.put(PREFERENCE_KEYWORD_PROCESSOR_ARGS+i, procClasses[i].getArgsString());
		}
		
		int countObs = obsClasses.length;
		prefs.putInt(PREFERENCE_KEYWORD_COUNT_OBSERVER, countObs);
		for (int i = 0; i < countObs; i++) {
			prefs.put(PREFERENCE_KEYWORD_OBSERVER+i, obsClasses[i].getFullClassName());
			prefs.put(PREFERENCE_KEYWORD_OBSERVER_ARGS+i, obsClasses[i].getArgsString());
		}
		
		prefs.putBoolean(PREFERENCE_KEYWORD_NO_IDENTIFY, noIdentify);
		prefs.putBoolean(PREFERENCE_KEYWORD_DEBUG, debug);
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Settings [incClasses=");
		builder.append(Arrays.toString(incPrefixes));
		builder.append(", excClasses=");
		builder.append(Arrays.toString(excPrefixes));
		builder.append(", excMethods=");
		builder.append(Arrays.toString(excMethods));
		builder.append(", procClasses=");
		builder.append(Arrays.toString(procClasses));
		builder.append(", noIdentify=");
		builder.append(noIdentify);
		builder.append(", debug=");
		builder.append(debug);
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * This inner classes is needed to expose the protected method 
	 * {@link #defineClass(String, byte[], int, int)} from {@link ClassLoader} 
	 * to the public.<br>
	 */
	private static class ProxyClassLoader extends ClassLoader {
		
		/**
		 * Calls super{@link #defineClass(String, byte[], int, int)} with 0 
		 * and bytes.length as 3rd and 4th argument respectively.<br>
		 * @param className		the name of the class
		 * @param bytes			the byte code for the class
		 * @return				a Class object
		 * @see #defineClass(String, byte[], int, int)
		 */
		public Class<?> loadFromBytes(String className, byte[] bytes) {
			System.out.println("ProxyClassLoader.loadFromBytes()="+className);
			return super.defineClass(className, bytes, 0, bytes.length);
		}
		
	}
	
	public static class ClassAndArgs {
		
		private final String fullClassName;
		private final String argsString;
		private Class<?> asClass;
		
		public ClassAndArgs(String className) {
			this(className, "");
		}
		
		public ClassAndArgs(String className, String args) {
			if (className == null) {
				throw new NullPointerException("className == null");
			}
			if (args == null) {
				throw new NullPointerException("args == null");
			}
			fullClassName = className;
			argsString = args;
		}
		
		public String getFullClassName() {
			return fullClassName;
		}
		
		public String getArgsString() {
			return argsString;
		}
		
	}
	
}