package edu.udo.cs.dynaliser;

public class IdentifiedObject {
	
	private final Object obj;
	private final int id;
	
	/**
	 * Stupid constructor. Does not check parameters for correctness.<br>
	 * @param obj							the identified non-null object
	 * @param id							the unique id for the object
	 * @throws IllegalArgumentException		if obj is null
	 */
	public IdentifiedObject(Object obj, int id) {
		if (obj == null) {
			throw new IllegalArgumentException("obj == null");
		}
		this.obj = obj;
		this.id = id;
	}
	
	/**
	 * The object that is identified by the unique id returned 
	 * by {@link #getUniqueID()}.<br>
	 * @return			a non-null object of any class
	 */
	public Object getIdentifiedObject() {
		return obj;
	}
	
	/**
	 * Returns the unique id given to the identified object.<br>
	 * This id is unique for all objects of the same class. 
	 * There may be other identified objects with the same id 
	 * iff they belong to a different class.<br>
	 * Subclasses are treated as different from their super classes.<br>
	 * @return			the unique id of the identified object
	 */
	public int getUniqueID() {
		return id;
	}
	
	/**
	 * Returns the class of the identified object.<br>
	 * @return		{@link #getIdentifiedObject()}.{@link #getClass()}
	 */
	public Class<?> getIdentifiedObjectClass() {
		return getIdentifiedObject().getClass();
	}
	
}