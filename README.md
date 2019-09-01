# JDynaliser
A tool for dynamic program analysis of Java programs. When this library is used as a java agent, it will inject java bytecode into the user program to record method invocations and object initialization. The data is stored as a tree of CstEvent instances.

Users can write implementations of either edu.udo.cs.dynalysis.JDynObserver or edu.udo.cs.dynalysis.JDynProcessor to access the CstEvent data and to perform their own analysis. A JDynObserver gets online access to the data as it is gathered. A JDynProcessor performs an offline analysis on the CstEvent data after the complete termination of a call stack.

See the class edu.udo.cs.dynalysis.observers.ElapsedTimeObs for an example of a JDynObserver. This observer records the running time of every executed method and outputs the results to a file after shutdown.

See the class edu.udo.cs.dynalysis.processors.PrintCallSequence for an example of a JDynProcessor. This processor prints the entire sequence of calls within a stack trace to an output file in a human readable format.