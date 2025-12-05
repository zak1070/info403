package src;

import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Main entry point for the Part 3 Compiler.
 * Reads a YALCC source file and outputs LLVM IR to stdout.
 *
 * Strict requirement: ONLY LLVM code must be printed to stdout.
 * Errors should go to stderr.
 */
public class Main {
    public static void main(String[] args) {
        // 1. Check arguments
        if (args.length != 1) {
            // Error messages strictly on stderr to avoid polluting LLVM output if piped
            System.err.println("Usage: java -jar part3.jar [FILE]");
            System.exit(1);
        }

        FileReader source = null;
        try {
            // 2. Open file
            source = new FileReader(args[0]);

            // 3. Parse and Generate
            // The Parser constructor initializes the LexicalAnalyzer
            Parser parser = new Parser(source);

            // The parse() method in our updated Parser prints LLVM IR directly to
            // System.out
            parser.parse();

        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found - " + args[0]);
            System.exit(1);
        } catch (ParseException e) {
            System.err.println("Syntax Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
}
// package src;

// import java.io.FileReader;
// import java.io.FileNotFoundException;
// import java.io.FileInputStream;
// import java.io.InputStream;
// import java.io.BufferedWriter;
// import java.io.FileWriter;
// import java.io.IOException;

// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.TreeMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;

// /**
// *
// * Project Part 2: Parser
// *
// * @author Marie Van Den Bogaard, Léo Exibard, Gilles Geeraerts, Sarah Winter,
// * edited by Mrudula Balachander
// *
// */

// public class Main {
// /**
// *
// * The parser
// *
// * @param args The argument(s) given to the program
// * @throws IOException java.io.IOException if an I/O-Error occurs
// * @throws FileNotFoundException java.io.FileNotFoundException if the
// specified
// * file does not exist
// *
// */
// public static void main(String[] args) throws FileNotFoundException,
// IOException, SecurityException, Exception {
// // Display the usage when no arguments are given
// if (args.length == 0) {
// System.out.println("Usage: java -jar part2.jar [OPTION] [FILE]\n"
// + "\tOPTION:\n"
// + "\t -wt (write-tree) filename.tex: writes latex tree to filename.tex\n"
// + "\t -dr (display-rules): writes each rule in full\n"
// + "\tFILE:\n"
// + "\tA .ycc file containing a YALCC program\n");
// System.exit(0);
// } else {
// boolean writeTree = false;
// boolean fullOutput = false;
// BufferedWriter bwTree = null;
// FileWriter fwTree = null;
// FileReader codeSource = null;
// try {
// codeSource = new FileReader(args[args.length - 1]);
// } catch (IOException e) {
// e.printStackTrace();
// }
// ParseTree parseTree = null;
// String tex = "\\documentclass{standalone}\\begin{document}Parsing error, no
// tree produced.\\end{document}";

// for (int i = 0; i < args.length; i++) {
// if (args[i].equals("-wt") || args[i].equals("--write-tree")) {
// writeTree = true;
// try {
// fwTree = new FileWriter(args[i + 1]);
// bwTree = new BufferedWriter(fwTree);
// // System.out.println("Opened file " + args[i+1]) ;
// } catch (IOException e) {
// e.printStackTrace();
// }
// }
// if (args[i].equals("-dr") || args[i].equals("--display-rules")) {
// fullOutput = true;
// }
// }
// Parser parser = new Parser(codeSource);
// if (fullOutput) {
// parser.displayFullRules();
// }
// try {
// parseTree = parser.parse();
// if (writeTree) {
// tex = parseTree.toLaTeX();
// }
// ;
// } catch (ParseException e) {
// System.out.println("Error:> " + e.getMessage());
// } catch (Exception e) {
// System.out.println("Error:> " + e);
// }
// if (writeTree) {
// try {
// bwTree.write(tex);
// } catch (IOException e) {
// e.printStackTrace();
// } finally {
// try {
// if (bwTree != null)
// bwTree.close();
// if (fwTree != null)
// fwTree.close();
// } catch (IOException ex) {
// ex.printStackTrace();
// }
// }
// }
// }
// }

// /** Default constructor (should not be used) */
// private Main() {
// };
// }
