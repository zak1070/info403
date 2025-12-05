package src;

import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Main entry point for the Part 3 Compiler.
 * Reads a YALCC source file and outputs LLVM IR to stdout.
 *
 * Errors should go to stderr.
 */
public class Main {
    public static void main(String[] args) {
        // Check arguments
        if (args.length != 1) {
            // Error messages strictly on stderr
            System.err.println("Usage: java -jar part3.jar [FILE]");
            System.exit(1);
        }

        FileReader source = null;
        try {
            // Open file
            source = new FileReader(args[0]);

            // The Parser constructor initializes the LexicalAnalyzer
            Parser parser = new Parser(source);

            // The parse() method starts the parsing process
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
                    System.err.println("Error closing file: " + e.getMessage());
                }
            }
        }
    }
}
