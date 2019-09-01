package edu.udo.cs.dynalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class JDynUtil {
	
	private static final StringBuilder sb = new StringBuilder(1000);
	private static final Map<Object, Writer> outMap = new HashMap<>();
	
	private JDynUtil() {}
	
	public static String getSignatureWithoutPackage(String signature) {
		int paramListIndex = signature.indexOf('(');
		if (paramListIndex == -1) {
			paramListIndex = signature.length();
		}
		int methodNameIndex = signature.lastIndexOf('.', paramListIndex);
		if (methodNameIndex == -1) {
			return signature;
		}
		int classNameIndex = signature.lastIndexOf('.', methodNameIndex - 1);
		if (classNameIndex == -1) {
			return signature;
		}
		return signature.substring(classNameIndex + 1);
	}
	
	public static String getSignatureWithoutParameters(String signature) {
		int paramListIndex = signature.indexOf('(');
		if (paramListIndex == -1) {
			return signature;
		}
		return signature.substring(0, paramListIndex);
	}
	
	public static String getParametersFromSignature(String signature) {
		int paramListIndex = signature.indexOf('(');
		if (paramListIndex == -1) {
			return signature;
		}
		return signature.substring(paramListIndex);
	}
	
	public static String getClassNameFromSignature(String signature) {
		int paramListIndex = signature.indexOf('(');
		if (paramListIndex == -1) {
			paramListIndex = signature.length();
		}
		int methodNameIndex = signature.lastIndexOf('.', paramListIndex);
		if (methodNameIndex == -1) {
			return signature;
		}
		return signature.substring(0, methodNameIndex);
	}
	
	public static String getClassSimpleNameFromSignature(String signature) {
		int paramListIndex = signature.indexOf('(');
		if (paramListIndex == -1) {
			paramListIndex = signature.length();
		}
		int methodNameIndex = signature.lastIndexOf('.', paramListIndex);
		if (methodNameIndex == -1) {
			return signature;
		}
		int classNameIndex = signature.lastIndexOf('.', methodNameIndex - 1);
		if (classNameIndex == -1) {
			return signature.substring(0, methodNameIndex);
		}
		return signature.substring(classNameIndex + 1, methodNameIndex);
	}
	
	public static String getMethodNameFromSignature(String signature) {
		int paramListIndex = signature.indexOf('(');
		if (paramListIndex == -1) {
			paramListIndex = signature.length();
		}
		int methodNameIndex = signature.lastIndexOf('.', paramListIndex);
		if (methodNameIndex == -1) {
			return signature;
		}
		return signature.substring(methodNameIndex + 1, paramListIndex);
	}
	
	public static int toInt(String str, int defaultValue) {
		if (str == null || str.isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public static double toDouble(String str, int defaultValue) {
		if (str == null || str.isEmpty()) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(str);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public static String extractArgCaseSensitive(String args, String identifier) {
		return JDynUtil.extractArgInternal(args, args, identifier);
	}
	
	public static String extractArg(String args, String identifier) {
		String checkArgs = args.toLowerCase();
		identifier = identifier.toLowerCase();
		return JDynUtil.extractArgInternal(args, checkArgs, identifier);
	}
	
	private static String extractArgInternal(
			String returnArgs, String checkArgs, String identifier) 
	{
		String prefix = identifier + "=";
		
		int indexStart = checkArgs.indexOf(prefix);
		if (indexStart == -1) {
			return "";
		}
		indexStart += prefix.length();
		int indexStop = checkArgs.indexOf(";", indexStart);
		if (indexStop == -1) {
			return returnArgs.substring(indexStart);
		}
		return returnArgs.substring(indexStart, indexStop);
	}
	
	public static void print(Object key, Object ... args) {
		Writer writer = JDynUtil.getWriter(key);
		String txt = JDynUtil.buildText(args);
		try {
			writer.write(txt);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Writer getWriter(Object key) {
		synchronized (outMap) {
			return outMap.get(key);
		}
	}
	
	private static String buildText(Object ... args) {
		synchronized (sb) {
			sb.delete(0, sb.length());
			for (int i = 0; i < args.length; i++) {
				sb.append(args[i]);
				sb.append(" ");
			}
			sb.append("\r\n");
			return sb.toString();
		}
	}
	
	public static boolean isOutputToFile(String outFile) {
		return !(outFile == null || outFile.trim().isEmpty());
	}
	
	public static Object startPrinting(String outFile) {
		return JDynUtil.startPrinting(outFile, false);
	}
	
	public static Object startPrinting(String outFile, boolean append) {
		Writer writer;
		if (!JDynUtil.isOutputToFile(outFile)) {
			writer = new PrintWriter(System.out) {
				@Override
				public void close() {
					flush();
				}
			};
		} else {
			File file = new File(outFile);
			File parent = file.getParentFile();
			if (parent != null) {
				file.getParentFile().mkdirs();
			}
			try {
				writer = new BufferedWriter(
						new FileWriter(file, append));
			} catch (Exception e) {
				e.printStackTrace();
				writer = new PrintWriter(System.out);
			}
		}
		Object key = new Object();
		synchronized (outMap) {
			outMap.put(key, writer);
		}
		return key;
	}
	
	@SuppressWarnings("resource")
	public static void stopPrinting(Object key) {
		Writer writer;
		synchronized (outMap) {
			writer = outMap.remove(key);
		}
		try {
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}