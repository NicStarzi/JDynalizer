package edu.udo.cs.dynalysis.processors;

import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import edu.udo.cs.dynaliser.CallSequenceTree;
import edu.udo.cs.dynaliser.CstEvent;
import edu.udo.cs.dynaliser.CstEventConstructor;
import edu.udo.cs.dynaliser.CstEventException;
import edu.udo.cs.dynaliser.CstEventMethod;
import edu.udo.cs.dynaliser.CstEventStaticMethod;
import edu.udo.cs.dynaliser.CstEventType;
import edu.udo.cs.dynaliser.IdentifiedObject;
import edu.udo.cs.dynalysis.JDynProcessor;

public class ToTikZUml implements JDynProcessor {
	
	private final Set<String> preExisting = new HashSet<>();
	private final Set<String> constructed = null;
	private int depth = 0;
	
	public void setArgs(String args) {
	}
	
	public void processSequence(CallSequenceTree seq) {
		calculateObjects(seq);
		
		StringBuilder sb = new StringBuilder();
		sb.append("\\documentclass[a4paper,11pt, svgnames]{article}\n"
				+ "\\usepackage[T1]{fontenc}\n"
				+ "\\usepackage[utf8]{inputenc}\n"
				+ "\\usepackage[french]{babel}\n"
				+ "\\usepackage{listings}\n"
				+ "\\usepackage{../tikz-uml}\n");
		
		sb.append("\\date{}\n"
				+ "\\title{}\n"
				+ "\\author{}\n"
				+ "\\begin{document}\n"
				+ "\\begin{center}\n"
				+ "\\begin{tikzpicture}\n"
				+ "\\begin{umlseqdiag}\n");
		appendRecursive(sb, seq.getRootEvent());
		sb.append("\\end{umlseqdiag}\n"
				+ "\\end{tikzpicture}\n"
				+ "\\end{center}\n"
				+ "\\end{document}\n");
		try (FileWriter writer = new FileWriter("outTikZ.txt")) {
			writer.write(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void registerObject(Set<String> set, IdentifiedObject obj) {
		StringBuilder sb = new StringBuilder();
		appendObject(sb, obj);
		set.add(sb.toString());
	}
	
	private void calculateObjects(CallSequenceTree seq) {
		Deque<CstEvent> eventStack = new ArrayDeque<>();
		eventStack.push(seq.getRootEvent());
		while (!eventStack.isEmpty()) {
			CstEvent current = eventStack.pop();
			registerObject(preExisting, current.getCallingObject());
			if (current.getEventType() == CstEventType.CONSTRUCTOR) {
				CstEventConstructor eventC = (CstEventConstructor) current;
				registerObject(constructed, eventC.getConstructedObject());
			}
			for (CstEvent childEvent : current.getChildren()) {
				eventStack.push(childEvent);
			}
		}
		preExisting.removeAll(constructed);
	}
	
	private void appendRecursive(StringBuilder sb, CstEvent event) {
//		System.out.println(event.toString());
		for (int i = 0; i < depth; i++) {
			sb.append("\t");
		}
		switch (event.getEventType()) {
		case CONSTRUCTOR:
			CstEventConstructor cEvent = (CstEventConstructor) event;
			appendConstructor(sb, cEvent);
			break;
		case EXCEPTION:
			CstEventException eEvent = (CstEventException) event;
			appendException(sb, eEvent);
			break;
		case METHOD:
			CstEventMethod mEvent = (CstEventMethod) event;
			appendMethod(sb, mEvent);
			break;
		case STATIC_METHOD:
			CstEventStaticMethod sEvent = (CstEventStaticMethod) event;
			appendStaticMethod(sb, sEvent);
			break;
		default:
			break;
		}
		sb.append("\n");
		for (CstEvent child : event.getChildren()) {
			appendRecursive(sb, child);
		}
		switch (event.getEventType()) {
		case STATIC_METHOD:
		case METHOD:
			depth--;
			for (int i = 0; i < depth; i++) {
				sb.append("\t");
			}
			sb.append("\\end{umlcall}\n\n");
			break;
		case CONSTRUCTOR:
		case EXCEPTION:
		default:
			break;
		}
	}
	
//	private void appendPreExisting(StringBuilder sb, IdentifiedObject obj) {
//		sb.append("\\umlobject[class=");
//		sb.append(obj.getIdentifiedObjectClass().getSimpleName());
//		sb.append("] {");
//		appendObject(sb, obj);
//		sb.append("}");
//	}
	
	private void appendConstructor(StringBuilder sb, CstEventConstructor event) {
		sb.append("\\umlcreatecall[class=");
		sb.append(event.getConstructedClass().getSimpleName());
		sb.append("] {");
		appendObject(sb, event.getCallingObject());
		sb.append("}{");
		appendObject(sb, event.getConstructedObject());
		sb.append("}");
	}
	
	private void appendException(StringBuilder sb, CstEventException event) {
		sb.append("\\umlcreatecall[class=");
		sb.append(event.getException().getClass().getSimpleName());
		sb.append("] {");
		appendObject(sb, event.getCallingObject());
		sb.append("}{");
		appendObject(sb, new IdentifiedObject(event.getException(), -1));
		sb.append("}");
	}
	
	private void appendMethod(StringBuilder sb, CstEventMethod event) {
		sb.append("\\begin{umlcall}[op={");
		sb.append(event.getSignature());
		sb.append("({");
		if (event.hasParameters()) {
			Object[] params = event.getParameters();
			for (int i = 0; i < params.length - 1; i++) {
				appendParamOrReturn(sb, params[i]);
				sb.append(", ");	
			}
			if (params.length > 0) {
				appendParamOrReturn(sb, params[params.length - 1]);
			}
		}
		sb.append("})}");
		if (event.hasReturnValue()) {
			sb.append(", return=");
			appendParamOrReturn(sb, event.getReturnedValue());
		}
		sb.append("] {");
		appendObject(sb, event.getCallingObject());
		sb.append("}{");
		appendObject(sb, event.getInvokingObject());
		sb.append("}");
		depth++;
	}
	
	private void appendStaticMethod(StringBuilder sb, CstEventStaticMethod event) {
		sb.append("\\begin{umlcall}[op={");
		sb.append(event.getSignature());
		sb.append("({");
		if (event.hasParameters()) {
			Object[] params = event.getParameters();
			for (int i = 0; i < params.length - 1; i++) {
				appendParamOrReturn(sb, params[i]);
				sb.append(", ");	
			}
			if (params.length > 0) {
				appendParamOrReturn(sb, params[params.length - 1]);
			}
		}
		sb.append("})}");
		if (event.hasReturnValue()) {
			sb.append(", return=");
			appendParamOrReturn(sb, event.getReturnedValue());
		}
		sb.append("] {");
		appendObject(sb, event.getCallingObject());
		sb.append("}{");
		appendObject(sb, event.getMethodClassObject());
		sb.append("}");
		depth++;
	}
	
	private void appendObject(StringBuilder sb, IdentifiedObject obj) {
		if (obj == null) {
			sb.append("null -1");
//			sb.append(" (");
//			sb.append(-1);
//			sb.append(")");
		} else {
			Object object = obj.getIdentifiedObject();
			if (object instanceof Class<?>) {
				Class<?> clazz = (Class<?>) object;
				sb.append(clazz.getSimpleName());
			} else {
				sb.append(obj.getIdentifiedObjectClass().getSimpleName());
			}
			sb.append(" ");
//			sb.append(" (");
			sb.append(obj.getUniqueID());
//			sb.append(")");
		}
	}
	//\newcommand{name}[num]{definition}
	// \newcommand{\wbal}{The Wikibook about \LaTeX}
	// This is ��\wbal'' \ldots{} ��\wbal''
	private void appendParamOrReturn(StringBuilder sb, Object obj) {
		if (obj instanceof Integer
				|| obj instanceof Double
				|| obj instanceof Float
				|| obj instanceof Long
				|| obj instanceof Byte
				|| obj instanceof Boolean
				|| obj instanceof Character
				|| obj.getClass().isEnum())
		{
			sb.append(obj.toString());
		} else if (obj instanceof String) {
			sb.append("\"" + obj + "\"");
		} else if (obj.getClass().isArray()) {
			sb.append(obj.getClass().getSimpleName());
		} else if (obj instanceof IdentifiedObject) {
			IdentifiedObject idObj = (IdentifiedObject) obj;
			appendObject(sb, idObj);
		} else {
			sb.append("some");
			sb.append(obj.getClass().getSimpleName());
		}
	}
	
}