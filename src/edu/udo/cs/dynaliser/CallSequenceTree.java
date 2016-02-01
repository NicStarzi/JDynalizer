package edu.udo.cs.dynaliser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CallSequenceTree implements Iterable<edu.udo.cs.dynaliser.CstEvent> {
	
	// Lazy initialization
	private final Thread thread;
	private Deque<CstEvent> eventStack;
	private CstEvent rootEvent;
	private int size;
	
	CallSequenceTree(Thread sequenceThread) {
		thread = sequenceThread;
	}
	
	public Thread getThread() {
		return thread;
	}
	
	boolean isFinal() {
		return rootEvent != null && (eventStack == null 
				|| eventStack.isEmpty());
	}
	
	public CstEvent getRootEvent() {
		return rootEvent;
	}
	
	protected CstEventConstructor beforeConstructor(
			Class<?> clazz, 
			String signature, 
			Object[] params, 
			boolean hasParams) 
	{
		CstEventConstructor event;
		if (hasParams) {
			event = new CstEventConstructor(peekEvent(), clazz, signature, params);
		} else {
			event = new CstEventConstructor(peekEvent(), clazz, signature);
		}
		pushEvent(event);
		return event;
	}
	
	protected CstEventConstructor afterConstructor(Object object) {
		CstEventConstructor event = (CstEventConstructor) popEvent(CstEventConstructor.class);
		event.makeEndTime();
		if (event.getConstructedClass() != object.getClass()) {
			event.setSuperConstructor(object);
		} else {
			event.setRegularConstructor(object);
		}
		size++;
		return event;
	}
	
	protected CstEventStaticMethod beforeStaticMethod(
			Class<?> clazz, 
			String signature, 
			Object[] params, 
			boolean hasParams) 
	{
		CstEventStaticMethod event;
		if (hasParams) {
			event = new CstEventStaticMethod(peekEvent(), clazz, signature, params);
		} else {
			event = new CstEventStaticMethod(peekEvent(), clazz, signature);
		}
		pushEvent(event);
		return event;
	}
	
	protected CstEventStaticMethod afterStaticMethod(
			Class<?> clazz, 
			String signature, 
			Object returnedValue, 
			boolean hasReturn) 
	{
		CstEventStaticMethod event = (CstEventStaticMethod) popEvent(CstEventStaticMethod.class);
		event.makeEndTime();
		event.setReturnedValue(returnedValue, hasReturn);
		size++;
		return event;
	}
	
	protected CstEventMethod beforeMethod(
			Object object, 
			int id, 
			String signature, 
			Object[] params, 
			boolean hasParams) 
	{
		IdentifiedObject self = new IdentifiedObject(object, id);
		CstEventMethod event;
		if (hasParams) {
			event = new CstEventMethod(peekEvent(), self, signature, params);
		} else {
			event = new CstEventMethod(peekEvent(), self, signature);
		}
		pushEvent(event);
		return event;
	}
	
	protected CstEventMethod afterMethod(
			Object object, 
			String signature, 
			Object returnedValue, 
			boolean hasReturn) 
	{
		CstEventMethod event = (CstEventMethod) popEvent(CstEventMethod.class);
		event.makeEndTime();
		event.setReturnedValue(returnedValue, hasReturn);
		size++;
		return event;
	}
	
	protected CstEventException exception(Exception exception, int id) {
		CstEventException event = new CstEventException(peekEvent(), exception, id);
		event.makeEndTime();
		pushEvent(event);
		popEvent(CstEventException.class);
		size++;
		return event;
	}
	
	public int getEventCount() {
		return size;
	}
	
	public Iterator<CstEvent> iterator() {
		return new CstIterator(this);
	}
	
	protected void pushEvent(CstEvent event) {
		// A final tree can not have any events appended
		if (isFinal()) {
			throw new IllegalStateException("CallSequence.isFinal()");
		}
		// Lazy initialization
		if (eventStack == null) {
			eventStack = new ArrayDeque<>();
		}
		// The first event is the root event
		if (rootEvent == null) {
			rootEvent = event;
		} else {
			CstEvent currentEvent = eventStack.peek();
			currentEvent.addChild(event);
		}
		eventStack.push(event);
	}
	
	protected CstEvent popEvent(Class<?> expectedClass) {
		if (isFinal()) {
			throw new IllegalStateException("CallSequence.isFinal()");
		}
		if (eventStack == null || eventStack.isEmpty()) {
			return rootEvent;
		}
		CstEvent event = eventStack.pop();
		if (expectedClass != null && event.getClass() != expectedClass) {
			throw new IllegalStateException(
					event.getClass().getSimpleName()
					+" != "+
					expectedClass.getSimpleName());
		}
		// After the tree was finalized the stack is no longer needed
		if (isFinal()) {
			eventStack = null;
		}
		return event;
	}
	
	protected CstEvent peekEvent() {
		if (eventStack == null || eventStack.isEmpty()) {
			return rootEvent;
		}
		return eventStack.peek();
	}
	
	protected IdentifiedObject peekTop() {
		CstEvent event;
		if (eventStack == null || eventStack.isEmpty()) {
			event = rootEvent;
		} else {
			event = eventStack.peek();
		}
		if (event == null) {
			return null;
		}
		return event.getSourceObject();
	}
	
	public class CstIterator implements Iterator<CstEvent> {
		
		private final List<CstEvent> eventList;
		private int pos = 0;
		
		public CstIterator(CallSequenceTree seqDia) {
			if (seqDia.rootEvent == null) {
				eventList = Collections.emptyList();
			} else {
				eventList = new ArrayList<>();
				
				Deque<CstEvent> eventStack = new ArrayDeque<>();
				eventStack.push(seqDia.rootEvent);
				while (!eventStack.isEmpty()) {
					CstEvent current = eventStack.pop();
					eventList.add(current);
					
					for (CstEvent event : current.getChildren()) {
						eventStack.push(event);
					}
				}
			}
		}
		
		public boolean hasNext() {
			return eventList.size() > pos;
		}
		
		public CstEvent next() {
			if (!hasNext()) {
				throw new NoSuchElementException("pos="+pos);
			}
			return eventList.get(pos++);
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
}