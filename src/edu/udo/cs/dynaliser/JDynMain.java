package edu.udo.cs.dynaliser;


public class JDynMain {
	
	public static void main(String[] args) {
		newLine();
		print("#===================================#");
		print("# Java Call Sequence Tree Generator #");
		print("#===================================#");
		newLine();
		print("This program is not supposed to be used directly.");
		print("Instead, you wan't to use it as an Agent.");
		print("To use an Agent you have to add the following command", 
				"line argument when starting your JVM: ");
		print("  -javaagent:[PATH-TO-JCSTG].jar");
		newLine();
		print("An example usage could look like this: ");
		print("  java -javaagent:Tools/jcstg.jar -jar MyApp.jar");
		newLine();
		print("For more detailed information please read through the", 
				"readme file or have a look at the homepage at");
		print("  www.???.com");
	}
	
	public void test(String c) {
		
	}
	
	public void test(Integer a, Boolean b, String c) {
		
	}
	
	private static void newLine() {
		System.out.println();
	}
	
	private static void print(Object ... args) {
		StringBuilder sb = new StringBuilder();
		for (Object o : args) {
			sb.append(o);
			sb.append(" ");
		}
		sb.delete(sb.length() - 1, sb.length());
		System.out.println(sb.toString());
	}
	
}