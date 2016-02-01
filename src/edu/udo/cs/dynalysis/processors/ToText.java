package edu.udo.cs.dynalysis.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.locks.ReentrantLock;

import edu.udo.cs.dynaliser.CallSequenceTree;
import edu.udo.cs.dynaliser.CstEvent;
import edu.udo.cs.dynaliser.CstEventConstructor;
import edu.udo.cs.dynaliser.CstEventException;
import edu.udo.cs.dynaliser.CstEventMethod;
import edu.udo.cs.dynaliser.CstEventStaticMethod;
import edu.udo.cs.dynaliser.IdentifiedObject;
import edu.udo.cs.dynalysis.JDynProcessor;

public class ToText implements JDynProcessor {
	
	private static final ReentrantLock writerLock = new ReentrantLock();
	private static volatile Writer outWriter;
	
	private static void openWriter() {
		writerLock.lock();
		try {
			if (outWriter != null) {
				return;
			}
			try {
				PrintWriter fileWriter = new PrintWriter(new File("Test.txt"));
				outWriter = new BufferedWriter(fileWriter);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			writerLock.unlock();
		}
	}
	
	public void setArgs(String args) {
	}
	
	public void processSequence(CallSequenceTree seq) {
		openWriter();
		StringBuilder sb = new StringBuilder();
		appendRecursive(sb, seq.getRootEvent(), 0);
		try {
			writerLock.lock();
			try {
				outWriter.write(sb.toString());
			} finally {
				writerLock.unlock();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void appendRecursive(StringBuilder sb, CstEvent event, int depth) {
		inline(sb, depth);
		appendEvent(sb, event);
		sb.append(System.lineSeparator());
		for (CstEvent child : event.getChildren()) {
			appendRecursive(sb, child, depth + 1);
		}
	}
	
	private void inline(StringBuilder sb, int depth) {
		for (int i = 0; i < depth; i++) {
			sb.append('\t');
		}
	}
	
	private void appendEvent(StringBuilder sb, CstEvent event) {
		System.out.println(event);
		sb.append(event.getEventType());
		switch (event.getEventType()) {
		case CONSTRUCTOR:
			CstEventConstructor cEvent = (CstEventConstructor) event;
			sb.append(", class=");
			sb.append(cEvent.getConstructedClass().getSimpleName());
			sb.append(", id=");
			sb.append(cEvent.getConstructedObject().getUniqueID());
			if (cEvent.hasParameters()) {
				Object[] params = cEvent.getParameters();
				for (int i = 0; i < params.length; i++) {
					sb.append(", param");
					sb.append(i);
					sb.append("=");
					appendParamOrReturn(sb, params[i]);
				}
			}
			break;
		case EXCEPTION:
			CstEventException eEvent = (CstEventException) event;
			sb.append(", class=");
			sb.append(eEvent.getException().getClass().getSimpleName());
			sb.append(", msg=");
			sb.append(eEvent.getException().getMessage());
			break;
		case METHOD:
			CstEventMethod mEvent = (CstEventMethod) event;
			sb.append(", class=");
			sb.append(mEvent.getInvokingObject().getIdentifiedObject().getClass().getSimpleName());
			sb.append(", id=");
			sb.append(mEvent.getInvokingObject().getUniqueID());
			sb.append(", method=");
			sb.append(mEvent.getSignature());
			if (mEvent.hasReturnValue()) {
				sb.append(", returned=");
				appendParamOrReturn(sb, mEvent.getReturnedValue());
			}
			if (mEvent.hasParameters()) {
				Object[] params = mEvent.getParameters();
				for (int i = 0; i < params.length; i++) {
					sb.append(", param");
					sb.append(i);
					sb.append("=");
					appendParamOrReturn(sb, params[i]);
				}
			}
			break;
		case STATIC_METHOD:
			CstEventStaticMethod sEvent = (CstEventStaticMethod) event;
			sb.append(", class=");
			sb.append(sEvent.getMethodClass().getSimpleName());
			sb.append(", method=");
			sb.append(sEvent.getSignature());
			if (sEvent.hasReturnValue()) {
				sb.append(", returned=");
				appendParamOrReturn(sb, sEvent.getReturnedValue());
			}
			if (sEvent.hasParameters()) {
				Object[] params = sEvent.getParameters();
				for (int i = 0; i < params.length; i++) {
					sb.append(", param");
					sb.append(i);
					sb.append("=");
					appendParamOrReturn(sb, params[i]);
				}
			}
			break;
		default:
			break;
		}
	}
	
	private void appendParamOrReturn(StringBuilder sb, Object obj) {
		if (obj == null) {
			sb.append("null");
		} else if (obj instanceof IdentifiedObject) {
			appendObject(sb, (IdentifiedObject) obj);
		} else {
			sb.append(obj.getClass().getSimpleName());
			appendWrapper(sb, obj);
		}
	}
	
	private void appendObject(StringBuilder sb, IdentifiedObject object) {
		sb.append(object.getIdentifiedObjectClass().getSimpleName());
		sb.append('(');
		sb.append(object.getUniqueID());
		sb.append(')');
	}
	
	private void appendWrapper(StringBuilder sb, Object maybeWrapper) {
		if (maybeWrapper.getClass().isEnum()
				|| maybeWrapper instanceof String
				|| maybeWrapper instanceof Integer
				|| maybeWrapper instanceof Float
				|| maybeWrapper instanceof Double
				|| maybeWrapper instanceof Long
				|| maybeWrapper instanceof Character
				|| maybeWrapper instanceof Byte) 
		{
			sb.append('(');
			sb.append(maybeWrapper.toString());
			sb.append(')');
		}
	}
	
}