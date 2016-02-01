package edu.udo.cs.dynaliser;

public class CstEventStaticMethod extends CstEvent {
	
	private final IdentifiedObject clazzObj;
	private final Class<?> clazz;
	private final Object[] params;
	private Object retVal;
	private boolean hasRetVal;
	
	protected CstEventStaticMethod(
			CstEvent parentEvent, 
			Class<?> methodClass, 
			String signature)
	{
		this(parentEvent, methodClass, signature, EMPTY_ARRAY);
	}
	
	protected CstEventStaticMethod(
			CstEvent parentEvent, 
			Class<?> methodClass, 
			String signature, 
			Object[] parameters) 
	{
		super(CstEventType.STATIC_METHOD, parentEvent, signature);
		clazzObj = new IdentifiedObject(methodClass, -1);
		clazz = methodClass;
		params = parameters;
	}
	
	protected IdentifiedObject getSourceObject() {
		return getMethodClassObject();
	}
	
	protected void setReturnedValue(Object returnedValue, boolean hasReturnValue) {
		retVal = returnedValue;
		hasRetVal = hasReturnValue;
	}
	
	public IdentifiedObject getMethodClassObject() {
		return clazzObj;
	}
	
	public Class<?> getMethodClass() {
		return clazz;
	}
	
	public Object[] getParameters() {
		return params;
	}
	
	public boolean hasParameters() {
		return params != null && params.length > 0;
	}
	
	public Object getReturnedValue() {
		return retVal;
	}
	
	public boolean hasReturnValue() {
		return hasRetVal;
	}
	
}