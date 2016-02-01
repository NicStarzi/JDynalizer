package edu.udo.cs.dynalysis.observers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;

import edu.udo.cs.dynaliser.CstEventConstructor;
import edu.udo.cs.dynaliser.CstEventMethod;
import edu.udo.cs.dynaliser.CstEventStaticMethod;
import edu.udo.cs.dynalysis.JDynUtil;
import edu.udo.cs.dynalysis.JDynObserver;

public class MethodCoverage implements JDynObserver {
	
	private final AtomicInteger outCount = new AtomicInteger(0);
	private final Map<Class<?>, Set<String>> methodsKnownPerClass = new HashMap<>();
	private final Map<Class<?>, Set<String>> methodsCoveredPerClass = new HashMap<>();
	private String outPath;
	
	public void setArgs(String args) {
		outPath = JDynUtil.extractArg(args, "outfile");
	}
	
	public void onConstructorTransformed(Class<?> clazz, String signature) {
		Set<String> methodsKnown = methodsKnownPerClass.get(clazz);
		if (methodsKnown == null) {
			methodsKnown = new HashSet<>();
			methodsKnownPerClass.put(clazz, methodsKnown);
		}
		methodsKnown.add(signature);
	}
	
	public void onStaticMethodTransformed(Class<?> clazz, String signature) {
		Set<String> methodsKnown = methodsKnownPerClass.get(clazz);
		if (methodsKnown == null) {
			methodsKnown = new HashSet<>();
			methodsKnownPerClass.put(clazz, methodsKnown);
		}
		methodsKnown.add(signature);
	}
	
	public void onMethodTransformed(Class<?> clazz, String signature) {
		Set<String> methodsKnown = methodsKnownPerClass.get(clazz);
		if (methodsKnown == null) {
			methodsKnown = new HashSet<>();
			methodsKnownPerClass.put(clazz, methodsKnown);
		}
		methodsKnown.add(signature);
	}
	
	public void onStaticMethodStart(CstEventStaticMethod event) {
		Class<?> clazz = event.getMethodClass();
		Set<String> methodsCovered = methodsCoveredPerClass.get(clazz);
		if (methodsCovered == null) {
			methodsCovered = new HashSet<>();
			methodsCoveredPerClass.put(clazz, methodsCovered);
		}
		methodsCovered.add(event.getSignature());
	}
	
	public void onConstructorStart(CstEventConstructor event) {
		Class<?> clazz = event.getConstructedClass();
		Set<String> methodsCovered = methodsCoveredPerClass.get(clazz);
		if (methodsCovered == null) {
			methodsCovered = new HashSet<>();
			methodsCoveredPerClass.put(clazz, methodsCovered);
		}
		methodsCovered.add(clazz.getSimpleName());
	}
	
	public void onMethodStart(CstEventMethod event) {
		Class<?> clazz = event.getInvokingObject().getIdentifiedObjectClass();
		Set<String> methodsCovered = methodsCoveredPerClass.get(clazz);
		if (methodsCovered == null) {
			methodsCovered = new HashSet<>();
			methodsCoveredPerClass.put(clazz, methodsCovered);
		}
		methodsCovered.add(event.getSignature());
	}
	
	public void onShutDown() {
		String title = "#   Method Coverage   #";
		StringBuilder sb = new StringBuilder(title.length());
		sb.append('#');
		for (int i = 0; i < title.length() - 2; i++) {
			sb.append('=');
		}
		sb.append('#');
		String titleCover = sb.toString();
		
		String out = outPath;
		if (JDynUtil.isOutputToFile(outPath)) {
			int number = outCount.getAndIncrement();
			out = outPath + number + ".txt";
		}
		Object printKey = JDynUtil.startPrinting(out);
		JDynUtil.print(printKey, titleCover);
		JDynUtil.print(printKey, title);
		JDynUtil.print(printKey, titleCover);
		JDynUtil.print(printKey);
		
		for (Entry<Class<?>, Set<String>> entry : methodsKnownPerClass.entrySet()) {
			Set<String> methodsKnown = entry.getValue();
			Set<String> methodsCovered = methodsCoveredPerClass.get(entry.getKey());
			
			JDynUtil.print(printKey, "=======  ", entry.getKey().getName(), "  =======");
			int covered = methodsCovered == null ? 0 : methodsCovered.size();
			int known = methodsKnown == null ? 0 : methodsKnown.size();
			JDynUtil.print(printKey, "methods known: (", known, ")=", methodsKnown);
			JDynUtil.print(printKey, "methods covered: (", covered, ")=", methodsCovered);
			JDynUtil.print(printKey, "Coverage percent: ", (covered / (double) known) * 100, "%");
			JDynUtil.print(printKey);
		}
		JDynUtil.stopPrinting(printKey);
	}
	
}