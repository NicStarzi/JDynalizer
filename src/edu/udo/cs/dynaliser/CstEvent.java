package edu.udo.cs.dynaliser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CstEvent {
	
	// Used by child classes for empty parameter lists or other purposes
	protected static final Object[] EMPTY_ARRAY = new Object[0];
	// Parent is given by CallSequenceTree when this event is created
	private final CstEvent parent;
	// Type should be unique per class and constant
	private final CstEventType type;
	// The signature of the method / constructor / exception
	private final String sig;
	// The nano time of the system when this event was created
	private final long beforeTime;
	// The nano time of the system when this event was finished
	// This is set in the method makeEndTime()
	private long afterTime;
	// Lazy initialized
	private List<CstEvent> children;
	
	CstEvent(CstEventType eventType, CstEvent parentEvent, String signature) {
		type = eventType;
		parent = parentEvent;
		sig = signature;
		beforeTime = System.nanoTime();
	}
	
	/**
	 * Returns the object that resulted in this event being generated. 
	 * More closely, this is the object behind the parent event.<br>
	 * If this event doesn't have a parent or the parent event is of 
	 * unknown type null is returned.<br>
	 * @return		either an identified object or null
	 */
	public IdentifiedObject getCallingObject() {
		if (parent == null) {
			return null;
		}
		return parent.getSourceObject();
	}
	
	/**
	 * Returns the object that is associated with the event. The source 
	 * of an event is the calling object of all child events.<br>
	 * @return			an {@link IdentifiedObject} or null if this 
	 * 					is the root event of a call sequence tree
	 */
	protected abstract IdentifiedObject getSourceObject();
	
	public CstEventType getEventType() {
		return type;
	}
	
	public String getSignature() {
		return sig;
	}
	
	/**
	 * Returns the system time in nano seconds when this event was started.<br>
	 * @return time in nano seconds
	 */
	public long getBeforeNanoTime() {
		return beforeTime;
	}
	
	/**
	 * Returns the system time in nano seconds after this event has ended.<br>
	 * @return time in nano seconds
	 */
	public long getAfterNanoTime() {
		return afterTime;
	}
	
	/**
	 * Sets the after time of this event as returned by the 
	 * {@link #getAfterNanoTime()}. This method is supposed to be called by 
	 * the {@link CallSequenceTree} only.<br>
	 */
	void makeEndTime() {
		afterTime = System.nanoTime();
	}
	
	public CstEvent getParent() {
		return parent;
	}
	
	void addChild(CstEvent child) {
		lazyCreateChildList();
		children.add(child);
	}
	
	public int getChildCount() {
		if (children == null) {
			return 0;
		}
		return children.size();
	}
	
	public List<CstEvent> getChildren() {
		if (children == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(children);
	}
	
	private void lazyCreateChildList() {
		if (children == null) {
			children = new ArrayList<>(2);
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getEventType().toString());
		sb.append("::");
		sb.append(getSignature());
		return sb.toString();
	}
	
}