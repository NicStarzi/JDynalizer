package edu.udo.cs.dynaliser;

import java.util.ArrayList;
import java.util.List;

import edu.udo.cs.dynalysis.JDynObserver;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.Modifier;
import javassist.bytecode.AccessFlag;

public class JDynClassTransformation {
	
	/**
	 * This is used for the class file transformation.<br>
	 */
	private static final String JCSTG_EVENT_DISPATCHER = JDynEventDispatcher.class.getName();
	/**
	 * The name of the private member variable used to determine whether a class 
	 * was initialized yet or not.<br>
	 * A new member variable of this name will be injected in each modified class.<br>
	 */
	private static final String TRANSFORM_CLASS_INIT_FIELD_NAME = "cstg_class_transform_init";
	
	/**
	 * Cached for performance
	 */
	private final StringBuilder callbackBuilder = new StringBuilder(256);
	/**
	 * All behaviors that belong to the class that is currently being 
	 * modified. This is used for the initialization.<br>
	 */
	private final List<CtBehavior> behaviorList = new ArrayList<>();
	private CtClass excClass;
	private boolean isObsCB = false;
	private boolean isProcCB = false;
	
	private String methodListString;
	private String classRef;
	private CtClass transClass;
	
	public JDynClassTransformation() {
	}
	
	public void setExceptionClass(CtClass exceptionClass) {
		excClass = exceptionClass;
	}
	
	public CtClass getExceptionClass() {
		return excClass;
	}
	
	public void enableObserverCallback() {
		isObsCB = true;
	}
	
	public void enableProcessorCallback() {
		isProcCB = true;
	}
	
	public void setClass(CtClass classObj, String className) throws Exception {
		transClass = classObj;
		classRef = className + ".class";
		
		if (isObsCB) {
			CtClass initFieldType = CtPrimitiveType.booleanType;
			CtField initField = new CtField(initFieldType, 
					TRANSFORM_CLASS_INIT_FIELD_NAME, transClass);
			
			// Volatile for multi-threaded applications
			initField.setModifiers(initField.getModifiers() 
					| Modifier.STATIC | Modifier.VOLATILE);
			
			// Synthetic to not show up during reflection
			initField.getFieldInfo().setAccessFlags(
					initField.getFieldInfo().getAccessFlags() | AccessFlag.SYNTHETIC);
			
			// Initially true. Set to false after the first instrumented method was called
			transClass.addField(initField, CtField.Initializer.constant(true));
		}
	}
	
	public void addBehavior(CtBehavior method) {
		behaviorList.add(method);
	}
	
	public void instrumentAndClear() throws Exception {
		callbackBuilder.delete(0, callbackBuilder.length());
		callbackBuilder.append("\"");
		for (int i = 0; i < behaviorList.size() - 1; i++) {
			CtBehavior behavior = behaviorList.get(i);
			callbackBuilder.append(behavior.getLongName());
			callbackBuilder.append(";");
		}
		if (behaviorList.size() > 0) {
			CtBehavior behavior = behaviorList.get(behaviorList.size() - 1);
			callbackBuilder.append(behavior.getLongName());
		}
		callbackBuilder.append("\"");
		methodListString = callbackBuilder.toString();
		callbackBuilder.delete(0, callbackBuilder.length());
		
		for (int i = 0; i < behaviorList.size(); i++) {
			CtBehavior behavior = behaviorList.get(i);
			addCallback(behavior);
		}
		behaviorList.clear();
	}
	
	/**
	 * Manipulates the byte code of the given behavior to add callbacks at the 
	 * beginning and the end of the user code, as well as a try-catch-block 
	 * enclosing the original user code and the initialization callback used by 
	 * {@link JDynObserver CstgObservers} if needed.<br>
	 * The callbacks are used to signal the start and end of an invocation of 
	 * the given behavior.<br>
	 * 
	 * @param behavior		either a CtMethod or CtConstructor that is to be changed
	 * @throws Exception	several exceptions from javassist, but we can't deal with them anyways
	 */
	private void addCallback(CtBehavior behavior) throws Exception {
		if (!isObsCB && !isProcCB) {
			return;
		}
		String behaviorName = buildBehaviorName(behavior);
		boolean hasParams = behavior.getParameterTypes().length > 0;
		boolean hasReturnVal = false;
		if (behavior instanceof CtMethod) {
			if (((CtMethod) behavior).getReturnType() != CtPrimitiveType.voidType) {
				hasReturnVal = true;
			}
		}
		String beforeCB;
		if (hasParams) {
			beforeCB = buildBeforeCallback_Params(behavior, behaviorName);
		} else {
			beforeCB = buildBeforeCallback_NoParams(behavior, behaviorName);
		}
		String afterCB;
		if (hasReturnVal) {
			afterCB = buildAfterCallback_Return(behavior, behaviorName);
		} else {
			afterCB = buildAfterCallback_NoReturn(behavior, behaviorName);
		}
		String catchCB = buildCatchCallback();
		behavior.insertBefore(beforeCB);
		behavior.insertAfter(afterCB, true);
		if (isObsCB) {
			String obsCB = buildObserverCallback(behavior);
			behavior.insertBefore(obsCB);
		}
		behavior.addCatch(catchCB, getExceptionClass(), "e");
	}
	
	private String buildObserverCallback(CtBehavior method) {
		String callBack = buildCallback("classInit", classRef, methodListString);
		callbackBuilder.delete(0, callbackBuilder.length());
		callbackBuilder.append("if (");
		callbackBuilder.append(TRANSFORM_CLASS_INIT_FIELD_NAME);
		callbackBuilder.append(") {");
		callbackBuilder.append(TRANSFORM_CLASS_INIT_FIELD_NAME);
		callbackBuilder.append(" = false;");
		callbackBuilder.append(callBack);
		callbackBuilder.append("}");
		return callbackBuilder.toString();
	}
	
	private String buildBeforeCallback_NoParams(CtBehavior method, String methodName) {
		if (method.getMethodInfo().isConstructor()) {
			return buildCallback("beforeConstructor", classRef, methodName, "null", "false");
		} else if (isStatic(method)) {
			return buildCallback("beforeStaticMethod", classRef, methodName, "null", "false");
		}
		return buildCallback("beforeMethod", "this", methodName, "null", "false");
	}
	
	private String buildBeforeCallback_Params(CtBehavior method, String methodName) {
		if (method.getMethodInfo().isConstructor()) {
			return buildCallback("beforeConstructor", classRef, methodName, "$args", "true");
		} else if (isStatic(method)) {
			return buildCallback("beforeStaticMethod", classRef, methodName, "$args", "true");
		}
		return buildCallback("beforeMethod", "this", methodName, "$args", "true");
	}
	
	private String buildAfterCallback_NoReturn(CtBehavior method, String methodName) {
		if (method.getMethodInfo().isConstructor()) {
			return buildCallback("afterConstructor", "this");
		} else if (isStatic(method)) {
			return buildCallback("afterStaticMethod", classRef, methodName, "null", "false");
		}
		return buildCallback("afterMethod", "this", methodName, "null", "false");
	}
	
	/**
	 * Builds a callback to the {@link JDynEventDispatcher} calling either 
	 * {@link JDynEventDispatcher#afterConstructor(Object)}, 
	 * {@link JDynEventDispatcher#afterStaticMethod(Class, String, Object, boolean)} or 
	 * {@link JDynEventDispatcher#afterMethod(Object, String, Object, boolean)} with the 
	 * respective arguments.<br>
	 * @param method		the method to which the callback belongs
	 * @param methodName	the name of the method
	 * @return				compilable java code that represents a callback to the {@link JDynEventDispatcher}
	 */
	private String buildAfterCallback_Return(CtBehavior method, String methodName) {
		if (method.getMethodInfo().isConstructor()) {
			return buildCallback("afterConstructor", "this");
		} else if (isStatic(method)) {
			return buildCallback("afterStaticMethod", classRef, methodName, "($w) $_", "true");
		}
		return buildCallback("afterMethod", "this", methodName, "($w) $_", "true");
	}
	
	/**
	 * Returns the name of the behavior enclosed by quotation marks (").<br>
	 * @param behavior	a non null behavior
	 * @return			the name of the behavior in between quotation marks
	 */
	private String buildBehaviorName(CtBehavior behavior) {
		callbackBuilder.delete(0, callbackBuilder.length());
		callbackBuilder.append("\"");
		callbackBuilder.append(behavior.getLongName());
		callbackBuilder.append("\"");
		return callbackBuilder.toString();
	}
	
	/**
	 * Builds a callback to the {@link JDynEventDispatcher#exception(Exception)} 
	 * method. The callback must be inserted inside the catch block of a try-catch 
	 * statement.<br>
	 * The exception variable is assumed to be called <code>e</code>.<br>
	 * @return			compilable java code that represents a callback to 
	 * 					{@link JDynEventDispatcher#exception(Exception)} followed 
	 * 					by a <code>throw</code> statement.<br>
	 */
	private String buildCatchCallback() {
		String exceptionCB = buildCallback("exception", "e");
		callbackBuilder.delete(0, callbackBuilder.length());
		callbackBuilder.append("{");
		callbackBuilder.append(exceptionCB);
		callbackBuilder.append(" throw e;}");
		return callbackBuilder.toString();
	}
	
	/**
	 * Builds a callback to the {@link JDynEventDispatcher} invoking the method 
	 * with name <code>methodName</code> with the given parameters.
	 * @param methodName					the name of the method that is to be called
	 * @param params						an array containing the parameters to be passed
	 * @throws NullPointerException			if methodName or params is null
	 * @throws IndexOutOfBoundsException	if params has no elements
	 * @return								compilable java code that represents a callback 
	 * 										to a method of the {@link JDynEventDispatcher}
	 */
	private String buildCallback(String methodName, String ... params) {
		int size = JCSTG_EVENT_DISPATCHER.length() + 4 + methodName.length();
		for (int i = 0; i < params.length; i++) {
			size += params.length;
		}
		size += (params.length - 1) * 2;
		
		callbackBuilder.delete(0, callbackBuilder.length());
		callbackBuilder.ensureCapacity(size);
		callbackBuilder.append(JCSTG_EVENT_DISPATCHER);
		callbackBuilder.append('.');
		callbackBuilder.append(methodName);
		callbackBuilder.append('(');
		for (int i = 0; i < params.length - 1; i++) {
			callbackBuilder.append(params[i]);
			callbackBuilder.append(", ");
		}
		callbackBuilder.append(params[params.length - 1]);
		callbackBuilder.append(");");
		return callbackBuilder.toString();
	}
	
	/**
	 * Returns true if the given behavior is a static method.<br>
	 * @param behavior					a non-null {@link CtBehavior}
	 * @throws NullPointerException		if behavior is null
	 * @return true						if the STATIC modifier is set for behavior
	 */
	private boolean isStatic(CtBehavior behavior) {
		return (behavior.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
	}
	
}