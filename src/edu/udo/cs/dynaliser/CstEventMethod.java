package edu.udo.cs.dynaliser;

public class CstEventMethod extends CstEvent {
	
	private final IdentifiedObject obj;
	private final Object[] params;
	private Object retVal;
	private boolean hasRetVal;
	
	protected CstEventMethod(
			CstEvent parentEvent, 
			IdentifiedObject object, 
			String signature) 
	{
		this(parentEvent, object, signature, EMPTY_ARRAY);
	}
	
	protected CstEventMethod(
			CstEvent parentEvent, 
			IdentifiedObject object, 
			String signature, 
			Object[] parameters) 
	{
		super(CstEventType.METHOD, parentEvent, signature);
		obj = object;
		params = parameters;
	}
	
	protected IdentifiedObject getSourceObject() {
		return getInvokingObject();
	}
	
	protected void setReturnedValue(Object returnedValue, boolean hasReturnValue) {
		retVal = returnedValue;
		hasRetVal = hasReturnValue;
	}
	
	public IdentifiedObject getInvokingObject() {
		return obj;
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