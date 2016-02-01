package edu.udo.cs.dynaliser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.udo.cs.dynaliser.JDynSettings.ClassAndArgs;

public class JDynSettingsParser {
	
	public JDynSettingsParser() {
	}
	
	public JDynSettings parseSettings(File settingsFile) {
		try {
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = fac.newDocumentBuilder();
			Document doc = builder.parse(settingsFile);
			
			List<String> incClassList = readAttributeList(doc, "include", "prefix");
			List<String> excClassList = readAttributeList(doc, "exclude", "prefix");
			List<String> excMethodList = readAttributeList(doc, "excludeMethod", "signature");
			List<ClassAndArgs> procClassList = readClassAndArgsList(doc, "processor");
			List<ClassAndArgs> obsClassList = readClassAndArgsList(doc, "observer");
			
			boolean identifyObjects = readBool(doc, "identifyObjects", true);
			boolean debugMode = readBool(doc, "debugMode", false);
			
			String[] includedClasses = incClassList.toArray(new String[incClassList.size()]);
			String[] excludedClasses = excClassList.toArray(new String[excClassList.size()]);
			String[] excludedMethods = excMethodList.toArray(new String[excMethodList.size()]);
			ClassAndArgs[] processorClasses = procClassList.toArray(new ClassAndArgs[procClassList.size()]);
			ClassAndArgs[] observerClasses = obsClassList.toArray(new ClassAndArgs[obsClassList.size()]);
			return new JDynSettings(includedClasses, 
					excludedClasses, 
					excludedMethods, 
					processorClasses, 
					observerClasses,
					identifyObjects, 
					debugMode);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new JDynSettings(new String[0], new String[0], 
				new String[0], new ClassAndArgs[0], new ClassAndArgs[0], 
				false, false);
	}
	
	private static List<ClassAndArgs> readClassAndArgsList(Document doc, String tag) {
		List<ClassAndArgs> result = new ArrayList<>();
		forAllElems(doc, tag, 
				(elem) -> {
					String className = elem.getAttribute("class");
					String args = elem.getAttribute("args");
					result.add(new ClassAndArgs(className, args));
				}
			);
		return result;
	}
	
	private static List<String> readAttributeList(Document doc, String tag, String attribute) {
		List<String> result = new ArrayList<>();
		forAllElems(doc, tag, 
			(elem) -> result.add(elem.getAttribute(attribute))
		);
		return result;
	}
	
	private static boolean readBool(Document doc, String tag, boolean defaultValue) {
		boolean[] result = {defaultValue};
		forAllElems(doc, tag, 
			(elem) -> result[0] = "true".equalsIgnoreCase(elem.getAttribute("value"))
		);
		return result[0];
	}
	
	private static void forAllElems(Document doc, String tag, ForElem act) {
		NodeList nodes = doc.getElementsByTagName(tag);
		for (int i = 0; i < nodes.getLength(); i++) {
			try {
				Node incNode = nodes.item(i);
				Element elem = (Element) incNode;
				act.next(elem);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static interface ForElem {
		void next(Element elem);
	}
	
}