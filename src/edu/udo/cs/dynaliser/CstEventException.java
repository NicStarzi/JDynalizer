package edu.udo.cs.dynaliser;

public class CstEventException extends CstEvent {
	
	private final IdentifiedObject excObj;
	
	protected CstEventException(CstEvent parentEvent, Exception exception, int objID) {
		super(CstEventType.EXCEPTION, parentEvent, exception.getClass().getName());
		excObj = new IdentifiedObject(exception, objID);
	}
	
	protected IdentifiedObject getSourceObject() {
		return getExceptionObject();
	}
	
	public IdentifiedObject getExceptionObject() {
		return excObj;
	}
	
	public Exception getException() {
		return (Exception) getExceptionObject().getIdentifiedObject();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getEventType().toString());
		sb.append("::");
		sb.append(getException().getClass().getName());
		sb.append("=");
		sb.append(getException().getMessage());
		return sb.toString();
	}
	
}