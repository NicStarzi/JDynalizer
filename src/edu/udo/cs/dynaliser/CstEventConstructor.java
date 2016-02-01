package edu.udo.cs.dynaliser;

public class CstEventConstructor extends CstEvent {
		
		private final Class<?> clazz;
		private final Object[] params;
		private IdentifiedObject self;
		private Object costructedObj;
		private boolean superConstructor;
		
		protected CstEventConstructor(CstEvent parentEvent, 
				Class<?> constructedClass, 
				String signature) 
		{
			this(parentEvent, constructedClass, signature, EMPTY_ARRAY);
		}
		
		protected CstEventConstructor(CstEvent parentEvent, 
				Class<?> constructedClass, 
				String signature, 
				Object[] parameters) 
		{
			super(CstEventType.CONSTRUCTOR, parentEvent, signature);
			clazz = constructedClass;
			params = parameters;
		}
		
		protected IdentifiedObject getSourceObject() {
			return getConstructedObject();
		}
		
		protected void setSuperConstructor(Object object) {
			if (costructedObj != null) {
				throw new IllegalStateException("costructedObj != null");
			}
			superConstructor = true;
			costructedObj = object;
		}
		
		protected void setRegularConstructor(Object object) {
			if (costructedObj != null) {
				throw new IllegalStateException("costructedObj != null");
			}
			superConstructor = false;
			costructedObj = object;
		}
		
		protected void setID(int id) {
			if (costructedObj == null) {
				throw new IllegalStateException("costructedObj == null");
			}
			self = new IdentifiedObject(costructedObj, id);
			for (CstEvent childEvent : getChildren()) {
				if (childEvent.getEventType() == CstEventType.CONSTRUCTOR) {
					CstEventConstructor cEvent = (CstEventConstructor) childEvent;
					if (cEvent.isSuperConstructor()) {
						cEvent.setID(id);
					}
				}
			}
		}
		
		public boolean isSuperConstructor() {
			return superConstructor;
		}
		
		public Class<?> getConstructedClass() {
			return clazz;
		}
		
		public Object[] getParameters() {
			return params;
		}
		
		public boolean hasParameters() {
			return params != null && params.length > 0;
		}
		
		public IdentifiedObject getConstructedObject() {
			return self;
		}
		
	}