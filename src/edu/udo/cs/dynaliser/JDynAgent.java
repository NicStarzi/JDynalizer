package edu.udo.cs.dynaliser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.udo.cs.dynalysis.JDynObserver;
import edu.udo.cs.dynalysis.JDynProcessor;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;

public class JDynAgent implements ClassFileTransformer {
	
	/**
	 * This file will be loaded as settings file if the no arguments are
	 * provided to the {@link #premain(String, Instrumentation)} method.<br>
	 */
	private static final String DEFAULT_SETTINGS_FILE_NAME = "jdynSettings.xml";
	private static Instrumentation instr;
	
	/**
	 * This special method is being called before the main method of the user
	 * application; it is part of the Instrumentation-API and used to
	 * transform the byte code of the user class files.<br>
	 * @param agentArguments	jcstg takes one optional argument for the settings file
	 * @param instrumentation	passed by the system to attach the {@link ClassFileTransformer}
	 */
	public static void premain(String agentArguments, Instrumentation instrumentation) {
		instr = instrumentation;
		// Assume default values for settings
		String settingsFileName = DEFAULT_SETTINGS_FILE_NAME;
		boolean debug = false;
		if (agentArguments != null && !agentArguments.isEmpty()) {
			String[] args = agentArguments.split(",");
			// Iterate over arguments and check if they are legal
			for (String arg : args) {
				String lowerArg = arg.toLowerCase();
				// Enable debug output ?
				if ("-d".equals(lowerArg)
						|| "-debug".equals(lowerArg))
				{
					debug = true;
				// Settings file path
				} else if (lowerArg.startsWith("-f=")) {
					settingsFileName = arg.substring("-f=".length());
				// Settings file path written out
				} else if (lowerArg.startsWith("-file=")) {
					settingsFileName = arg.substring("-file=".length());
				// Any other arguments are undefined right now
				} else {
					System.err.println("JDyn Error: Illegal Argument '"+arg+"'");
				}
			}
		}
		JDynAgent agent = new JDynAgent(settingsFileName, debug);
		// In case the settings do not contain any observers or processors there is nothing to do
		if (!agent.isNeeded()) {
			return;
		}
		instrumentation.addTransformer(agent);
	}
	
	public static long getObjectSize(Object obj) {
		return instr.getObjectSize(obj);
	}
	
	/**
	 * This key is used for the preferences used to store the user settings.<br>
	 */
	protected static final String JDYN_PREFERENCES = JDynAgent.class.getName().replace('.', '/')+"/Preferences";
	
	/**
	 * This settings object is parsed from a file for easier access to
	 * user settings.<br>
	 */
	private final JDynSettings settings;
	/**
	 * {@link JDynClassTransformation} contains utility methods to inject byte code
	 * to a class to make it usable by the JCSTG.<br>
	 */
	private final JDynClassTransformation transformation;
	
	/**
	 * Parses the settings file (if possible) and saves the settings to the
	 * {@link Preferences}.<br>
	 */
	private JDynAgent(String settingsFileName, boolean debug) {
		if (debug) {
			JDynAgent.debugMsg("Parse settings file", settingsFileName);
		}
		JDynSettingsParser settingsParser = new JDynSettingsParser();
		settings = settingsParser.parseSettings(new File(settingsFileName));
		if (debug) {
			settings.setDebug();
		}
		
		// In case the settings do not contain any observers or processors there is nothing to do
		if (!isNeeded()) {
			if (settings.isDebug()) {
				JDynAgent.debugMsg("No", JDynObserver.class.getSimpleName(), "or",
						JDynProcessor.class.getSimpleName(),
						"registered. Analysis aborted.");
			}
			transformation = null;
			return;
		}
		
		// We only enable the injection of these callbacks if needed to save performance
		transformation = new JDynClassTransformation();
		if (settings.hasObservers()) {
			if (settings.isDebug()) {
				JDynAgent.debugMsg("Enable", JDynObserver.class.getSimpleName(),
						"callbacks.");
			}
			transformation.enableObserverCallback();
		}
		if (settings.hasProcessors()) {
			if (settings.isDebug()) {
				JDynAgent.debugMsg("Enable", JDynProcessor.class.getSimpleName(),
						"callbacks.");
			}
			transformation.enableProcessorCallback();
		}
		
		// We write the settings we read from file to the preferences to share
		// them with other ClassLoader contexts.
		Preferences prefs = Preferences.userRoot().node(JDYN_PREFERENCES);
		if (settings.isDebug()) {
			JDynAgent.debugMsg("Write settings to preferences:", settings);
		}
		settings.writeToPreferences(prefs);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			if (settings.isDebug()) {
				JDynAgent.debugMsg("Witing settings failed. Reason: ");
				e.printStackTrace();
			}
			// Dont care about the exception. There is nothing we can do here.
		}
	}
	
	/**
	 * Returns true if any callbacks would be added to classes passed to the
	 * {@link ClassFileTransformer}. Returns false if the instrumentation is
	 * not needed at all.<br>
	 * @return		true if instrumentation should happen, false if it is not needed
	 */
	public boolean isNeeded() {
		return settings.hasClassesIncluded()
				&& (settings.hasObservers() || settings.hasProcessors());
	}
	
	/**
	 * This method is called by the JVM for each class that is to be transformed.<br>
	 * @param loader				dont care about this
	 * @param className				might be null in which case we do nothing
	 * @param classBeingRedefined	dont care about this
	 * @param protectionDomain		dont care about this
	 * @param classfileBuffer		might be null in which case we do nothing
	 */
	@Override
	public byte[] transform(
			ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException
	{
		// Could be null for some reason
		if (className == null) {
			return classfileBuffer;
		}
		boolean debug = settings.isDebug();
		
		// Java class names use '.' instead of '/' which is used in file path names
		className = className.replace('/', '.');
		if (debug) {
			JDynAgent.debugMsg("Check class", className, "for analysis.");
		}
		// Check if class is included
		if (!settings.isClassIncluded(className)) {
			return classfileBuffer;
		}
		if (debug) {
			JDynAgent.debugMsg("Instrument class", className);
		}
		// If the class object is loaded but an exception is thrown we must detach it in the finally block
		CtClass classObj = null;
		// If we do not make any modifications we return the original byte code
		byte[] result = classfileBuffer;
		
		try {
			// Use of javassist API
			ClassPool pool = ClassPool.getDefault();
			// Exception class is needed to add try-catch block to code transformation
			// This is cached for performance
			if (transformation.getExceptionClass() == null) {
				transformation.setExceptionClass(pool.get("java.lang.Exception"));
			}
			// Load class file
			classObj = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
			
			// Don't manipulate interfaces
			if (!classObj.isInterface()) {
				transformation.setClass(classObj, className);
				
				// CtBehaviors might be methods or constructors
				CtBehavior[] behaviors = classObj.getDeclaredBehaviors();
				for (int i = 0; i < behaviors.length; i++) {
					CtBehavior behav = behaviors[i];
					if (debug) {
						JDynAgent.debugMsg("Check method", behav.getLongName(),
								"for analysis.");
					}
					// Don't manipulate abstract or native methods
					if (!isAbstract(behav)
							&& !isNative(behav)
						// TODO: should default constructors be excluded?
//							&& !isDefaultConstructor(methods[i])
							)
					{
						// If behavior is a method check settings if method is included
						if (behav instanceof CtMethod) {
							CtMethod method = (CtMethod) behav;
							if (settings.isMethodIncluded(method.getLongName())) {
								if (debug) {
									JDynAgent.debugMsg("Instrument method", method.getLongName());
								}
								transformation.addBehavior(method);
							}
						} else {
							// If it is not a method, the behavior is a constructor
							transformation.addBehavior(behav);
						}
					} else {
						if (debug) {
							JDynAgent.debugMsg("Method", behav.getLongName(),
									"is abstract or native. No instrumentation.");
						}
					}
				}
				transformation.instrumentAndClear();
				// If class was modified we return the new byte code
				result = classObj.toBytecode();
			} else {
				if (debug) {
					JDynAgent.debugMsg(className, "is interface. No instrumentation.");
				}
			}
		} catch (Exception e) {
			JDynAgent.debugMsg("Error during instrumentation. ClassName=",className);
			e.printStackTrace();
		} finally {
			// We might not have modified the class at all
			if (classObj != null) {
				classObj.detach();
			}
		}
		// Return either original byte code or modified byte code
		return result;
	}
	
	/**
	 * Returns true if the given behavior is abstract.<br>
	 * @param behavior a non-null {@link CtBehavior}
	 * @return true if the ABSTRACT modifier is set for behavior
	 */
	private boolean isAbstract(CtBehavior behavior) {
		return (behavior.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT;
	}
	
	/**
	 * Returns true if the given behavior is a native method or constructor.<br>
	 * @param behavior a non-null {@link CtBehavior}
	 * @return true if the NATIVE modifier is set for behavior
	 */
	private boolean isNative(CtBehavior behavior) {
		return (behavior.getModifiers() & Modifier.NATIVE) == Modifier.NATIVE;
	}
	
	/**
	 * Returns true if the given behavior is a static method.<br>
	 * @param behavior a non-null {@link CtBehavior}
	 * @return true if the STATIC modifier is set for behavior
	 */
	@SuppressWarnings("unused") // may be used in the future
	private boolean isStatic(CtBehavior behavior) {
		return (behavior.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
	}
	
	/**
	 * Returns true if the given behavior is the default constructor.<br>
	 * @param behavior a non-null {@link CtBehavior}
	 * @return true if the behavior is an empty constructor
	 */
	@SuppressWarnings("unused") // may be used in the future
	private boolean isDefaultConstructor(CtBehavior behavior) {
		return behavior instanceof CtConstructor && ((CtConstructor) behavior).isEmpty();
	}
	
	public static void debugMsg(Object ... args) {
		StringBuilder sb = new StringBuilder();
		sb.append("JDyn: ");
		for (Object o : args) {
			sb.append(o);
			sb.append(" ");
		}
		System.out.println(sb.toString());
	}
	
}