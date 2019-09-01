package edu.udo.cs.dynalysis.observers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.udo.cs.dynaliser.CstEventConstructor;
import edu.udo.cs.dynaliser.JDynAgent;
import edu.udo.cs.dynalysis.JDynObserver;
import edu.udo.cs.dynalysis.JDynUtil;

public class ObjSizeObs implements JDynObserver {
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<Class<?>, Long> measuredSizeMap = new HashMap<>();
	private String outPath;
	
	public void setArgs(String args) {
		outPath = JDynUtil.extractArg(args, "outfile");
	}
	
	public void onConstructorEnd(CstEventConstructor event) {
		System.out.println("ObjSizeObs.onConstructorEnd="+event);
		Class<?> objClass = event.getConstructedClass();
		lock.writeLock().lock();
		try {
			Long size = measuredSizeMap.get(objClass);
			if (size == null) {
				Object obj = event.getConstructedObject();
				System.out.println("class="+objClass.getSimpleName()+", obj="+obj);
				size = Long.valueOf(JDynAgent.getObjectSize(obj));
				System.out.println("size="+size);
				measuredSizeMap.put(objClass, size);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public void onShutDown() {
		String title = "#   Object Sizes   #";
		StringBuilder sb = new StringBuilder(title.length());
		sb.append('#');
		for (int i = 0; i < title.length() - 2; i++) {
			sb.append('=');
		}
		sb.append('#');
		String titleCover = sb.toString();
		
		Object printKey = JDynUtil.startPrinting(outPath);
		JDynUtil.print(printKey, titleCover);
		JDynUtil.print(printKey, title);
		JDynUtil.print(printKey, titleCover);
		JDynUtil.print(printKey);
		
		lock.readLock().lock();
		try {
			for (Entry<Class<?>, Long> entry : measuredSizeMap.entrySet()) {
				JDynUtil.print(printKey, entry.getKey().getName(), 
						"\t\t=\t", entry.getValue().toString());
			}
			JDynUtil.stopPrinting(printKey);
		} finally {
			lock.readLock().unlock();
		}
	}
	
}