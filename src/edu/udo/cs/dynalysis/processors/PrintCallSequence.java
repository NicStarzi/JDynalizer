package edu.udo.cs.dynalysis.processors;

import java.util.concurrent.atomic.AtomicInteger;

import edu.udo.cs.dynaliser.CallSequenceTree;
import edu.udo.cs.dynaliser.CstEvent;
import edu.udo.cs.dynaliser.CstEventConstructor;
import edu.udo.cs.dynaliser.CstEventException;
import edu.udo.cs.dynaliser.CstEventMethod;
import edu.udo.cs.dynaliser.CstEventStaticMethod;
import edu.udo.cs.dynaliser.IdentifiedObject;
import edu.udo.cs.dynalysis.JDynProcessor;
import edu.udo.cs.dynalysis.JDynUtil;

public class PrintCallSequence implements JDynProcessor {
	
	private final AtomicInteger outCount = new AtomicInteger(0);
	private String outPath;
	private int minSize;
	
	public void setArgs(String args) {
		outPath = JDynUtil.extractArg(args, "outfile");
		String minSizeStr = JDynUtil.extractArg(args, "minsize");
		if (minSizeStr != null && !minSizeStr.isEmpty()) {
			try {
				minSize = Integer.parseInt(minSizeStr);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			minSize = -1;
		}
	}
	
	public void processSequence(CallSequenceTree seq) {
		if (minSize > 0 && seq.getEventCount() < minSize) {
			return;
		}
		String title = "#   Call Sequence   #";
		StringBuilder sbTitleCover = new StringBuilder(title.length());
		sbTitleCover.append('#');
		for (int i = 0; i < title.length() - 2; i++) {
			sbTitleCover.append('=');
		}
		sbTitleCover.append('#');
		String titleCover = sbTitleCover.toString();
		
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
		
		StringBuilder sbSeq = new StringBuilder();
		sbSeq.append(seq.getThread().toString());
		sbSeq.append("\n");
		appendRecursive(sbSeq, seq.getRootEvent(), 0);
		JDynUtil.print(printKey, sbSeq.toString());
		
		JDynUtil.stopPrinting(printKey);
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
			sb.append(JDynUtil.getMethodNameFromSignature(mEvent.getSignature()));
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
			sb.append(JDynUtil.getMethodNameFromSignature(sEvent.getSignature()));
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
//	
//	private void appendRecursive(Object printKey, CstEvent event, int depth) {
//		inline(printKey, depth);
//		appendEvent(printKey, event);
//		JDynUtil.print(printKey, System.lineSeparator());
//		for (CstEvent child : event.getChildren()) {
//			appendRecursive(printKey, child, depth + 1);
//		}
//	}
//	
//	private void inline(Object printKey, int depth) {
//		for (int i = 0; i < depth; i++) {
//			JDynUtil.print(printKey, "\t");
//		}
//	}
//	
//	private void appendEvent(Object printKey, CstEvent event) {
//		JDynUtil.print(printKey, event.getEventType());
//		String className;
//		String methodName;
//		int id;
//		switch (event.getEventType()) {
//		case CONSTRUCTOR:
//			CstEventConstructor cEvent = (CstEventConstructor) event;
//			className = cEvent.getConstructedClass().getSimpleName();
//			id = cEvent.getConstructedObject().getUniqueID();
//			JDynUtil.print(printKey, ", class=", className, ", id=", id);
//			if (cEvent.hasParameters()) {
//				Object[] params = cEvent.getParameters();
//				for (int i = 0; i < params.length; i++) {
//					JDynUtil.print(printKey, ", param=", i, "=");
//					appendParamOrReturn(printKey, params[i]);
//				}
//			}
//			break;
//		case EXCEPTION:
//			CstEventException eEvent = (CstEventException) event;
//			className = eEvent.getException().getClass().getSimpleName();
//			String excMsg = eEvent.getException().getMessage();
//			JDynUtil.print(printKey, ", class=", className, ", msg=", excMsg);
//			break;
//		case METHOD:
//			CstEventMethod mEvent = (CstEventMethod) event;
//			className = mEvent.getInvokingObject().getIdentifiedObject().getClass().getSimpleName();
//			id = mEvent.getInvokingObject().getUniqueID();
//			methodName = JDynUtil.getMethodNameFromSignature(mEvent.getSignature());
//			JDynUtil.print(printKey, ", class=", className, ", id=", id, ", method=", methodName);
//			if (mEvent.hasReturnValue()) {
//				JDynUtil.print(printKey, ", returned=");
//				appendParamOrReturn(printKey, mEvent.getReturnedValue());
//			}
//			if (mEvent.hasParameters()) {
//				Object[] params = mEvent.getParameters();
//				for (int i = 0; i < params.length; i++) {
//					JDynUtil.print(printKey, ", param=", i, "=");
//					appendParamOrReturn(printKey, params[i]);
//				}
//			}
//			break;
//		case STATIC_METHOD:
//			CstEventStaticMethod sEvent = (CstEventStaticMethod) event;
//			className = sEvent.getMethodClass().getSimpleName();
//			methodName = JDynUtil.getMethodNameFromSignature(sEvent.getSignature());
//			JDynUtil.print(printKey, ", class=", className, ", method=", methodName);
//			if (sEvent.hasReturnValue()) {
//				JDynUtil.print(printKey, ", returned=");
//				appendParamOrReturn(printKey, sEvent.getReturnedValue());
//			}
//			if (sEvent.hasParameters()) {
//				Object[] params = sEvent.getParameters();
//				for (int i = 0; i < params.length; i++) {
//					JDynUtil.print(printKey, ", param=", i, "=");
//					appendParamOrReturn(printKey, params[i]);
//				}
//			}
//			break;
//		default:
//			break;
//		}
//	}
//	
//	private void appendParamOrReturn(Object printKey, Object obj) {
//		if (obj == null) {
//			JDynUtil.print(printKey, "null");
//		} else if (obj instanceof IdentifiedObject) {
//			appendObject(printKey, (IdentifiedObject) obj);
//		} else {
//			appendWrapper(printKey, obj);
//		}
//	}
//	
//	private void appendObject(Object printKey, IdentifiedObject object) {
//		String className = object.getIdentifiedObjectClass().getSimpleName();
//		JDynUtil.print(printKey, className, "(", object.getUniqueID(), ")");
//	}
//	
//	private void appendWrapper(Object printKey, Object maybeWrapper) {
//		String className = maybeWrapper.getClass().getSimpleName();
//		if (maybeWrapper.getClass().isEnum()
//				|| maybeWrapper instanceof String
//				|| maybeWrapper instanceof Integer
//				|| maybeWrapper instanceof Float
//				|| maybeWrapper instanceof Double
//				|| maybeWrapper instanceof Long
//				|| maybeWrapper instanceof Character
//				|| maybeWrapper instanceof Byte) 
//		{
//			JDynUtil.print(printKey, className, "(", maybeWrapper.toString(), ")");
//		} else {
//			JDynUtil.print(printKey, className);
//		}
//	}
	
}