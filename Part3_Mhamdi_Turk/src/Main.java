package src;

import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar part3.jar [FILE]");
            System.exit(1);
        }

        FileReader codeSource = null;
        try {
            codeSource = new FileReader(args[0]);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + args[0]);
            System.exit(1);
        }

        try {
            // 1. Initialisation du parser
            Parser parser = new Parser(codeSource);

            // 2. Désactiver l'affichage des règles (IMPORTANT pour ne pas polluer le LLVM)
            parser.displayRuleNumbers(); // Ou une méthode pour tout couper, voir étape suivante

            // 3. En-tête LLVM (Standard)
            System.out.println("; Target: LLVM IR");
            System.out.println("; Authors: [Ton Nom], [Nom Binôme]");
            System.out.println("");
            System.out.println("@.str_read = private unnamed_addr constant [3 x i8] c\"%d\\00\", align 1");
            System.out.println("@.str_print = private unnamed_addr constant [4 x i8] c\"%d\\0A\\00\", align 1");
            System.out.println("declare i32 @scanf(i8*, ...)");
            System.out.println("declare i32 @printf(i8*, ...)");
            System.out.println("");
            System.out.println("define i32 @main() {");
            System.out.println("entry:");
            
            // TODO: Ici, nous devrons allouer les variables (alloca)
            // Pour l'instant, on laisse vide ou on le fera dans le parser.

            // 4. Lancement de la compilation
            // Note: On ne récupère plus de ParseTree, on lance juste l'analyse qui imprimera le code
            parser.parse();

            // 5. Pied de page LLVM
            System.out.println("    ret i32 0");
            System.out.println("}");

        } catch (Exception e) {
            // En cas d'erreur, on écrit sur STDERR pour ne pas créer un fichier .ll valide mais buggé
            System.err.println("Error:> " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}