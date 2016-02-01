package obs;

import edu.udo.cs.jcstg.CstgObserver;

public class SimpleObs implements CstgObserver {
	
	public SimpleObs() {
		System.out.println("Constructor!!!");
	}
	
	public static void main(String[] args) {
		System.out.println("main!!!");
	}
	
	public void onClassTransformed(String className) {
		System.out.println("onClassTransformed="+className);
	}
	
	public void onConstructorTransformed(String className) {
		System.out.println("onConstructorTransformed="+className);
	}
	
	public void onStaticMethodTransformed(String className, String methodName) {
		System.out.println("onStaticMethodTransformed="+methodName);
	}
	
	public void onMethodTransformed(String className, String methodName) {
		System.out.println("onMethodTransformed="+methodName);
	}
	
	public void onConstructorStart(Class<?> clazz, 
			Object[] params, 
			boolean hasParams) {
		
		System.out.println("onConstructorStart="+clazz.getSimpleName());
	}
	
	public void onConstructorEnd(Object object) {
		System.out.println("onConstructorEnd="+object.getClass().getSimpleName());
	}
	
	public void onStaticMethodStart(Class<?> clazz, 
			String methodName, 
			Object[] params, 
			boolean hasParams) {
		
		System.out.println("onStaticMethodStart="+clazz.getSimpleName()+"."+methodName);
	}
	
	public void onStaticMethodEnd(Class<?> clazz, 
			String methodName, 
			Object returnedValue, 
			boolean hasReturn) {
		
		System.out.println("onStaticMethodEnd="+clazz.getSimpleName()+"."+methodName);
	}
	
	public void onMethodStart(Object obj, 
			String methodName, 
			Object[] params, 
			boolean hasParams) {
		
		System.out.println("onMethodStart="+obj.getClass().getSimpleName()+"."+methodName);
	}
	
	public void onMethodEnd(Object obj, 
			String methodName, 
			Object returnedValue, 
			boolean hasReturn) {
		
		System.out.println("onMethodEnd="+obj.getClass().getSimpleName()+"."+methodName);
	}
	
}