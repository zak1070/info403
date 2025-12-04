package src;

import src.ast.*;
import java.util.HashMap;
import java.util.Map;

public class Compiler {
    private int regCounter = 1;
    private int labelCounter = 1;
    private Map<String, String> symbolTable = new HashMap<>();

    public void compile(AstNode root) {
        // 1. En-tête LLVM
        printLLVMHeader();

        // 2. Allocation des variables (Passe préliminaire)
        preScanVariables(root);

        System.out.println("define i32 @main() {");
        System.out.println("entry:");

        for (String var : symbolTable.keySet()) {
            System.out.println("  %" + var + " = alloca i32");
            System.out.println("  store i32 0, i32* %" + var);
        }
        System.out.println("");

        // 3. Compilation
        if (root instanceof BlockNode) {
            compileBlock((BlockNode) root);
        } else if (root instanceof Instruction) {
            compileInstruction((Instruction) root);
        }

        // 4. Fin
        System.out.println("  ret i32 0");
        System.out.println("}");
    }

    // --- Passe 1 : Recherche des variables ---
    private void preScanVariables(AstNode node) {
        if (node instanceof BlockNode) {
            for (Instruction i : ((BlockNode) node).instructions)
                preScanVariables(i);
        } else if (node instanceof IfNode) {
            preScanVariables(((IfNode) node).thenBlock);
            if (((IfNode) node).elseBlock != null)
                preScanVariables(((IfNode) node).elseBlock);
        } else if (node instanceof WhileNode) {
            preScanVariables(((WhileNode) node).body);
        } else if (node instanceof AssignNode) {
            String name = ((AssignNode) node).varName;
            symbolTable.putIfAbsent(name, name);
        } else if (node instanceof InputNode) {
            String name = ((InputNode) node).varName;
            symbolTable.putIfAbsent(name, name);
        }
    }

    // --- Passe 2 : Compilation ---
    private void compileInstruction(Instruction node) {
        if (node instanceof BlockNode)
            compileBlock((BlockNode) node);
        else if (node instanceof AssignNode)
            compileAssign((AssignNode) node);
        else if (node instanceof PrintNode)
            compilePrint((PrintNode) node);
        else if (node instanceof InputNode)
            compileInput((InputNode) node);
        else if (node instanceof IfNode)
            compileIf((IfNode) node);
        else if (node instanceof WhileNode)
            compileWhile((WhileNode) node);
    }

    private void compileBlock(BlockNode node) {
        for (Instruction i : node.instructions) {
            compileInstruction(i);
        }
    }

    private void compileAssign(AssignNode node) {
        String val = compileExpression(node.value);
        System.out.println("  store i32 " + val + ", i32* %" + node.varName);
    }

    private void compilePrint(PrintNode node) {
        String val = newReg();
        System.out.println("  " + val + " = load i32, i32* %" + node.varName);
        System.out.println(
                "  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.strP, i32 0, i32 0), i32 " + val
                        + ")");
    }

    private void compileInput(InputNode node) {
        System.out.println(
                "  call i32 (i8*, ...) @scanf(i8* getelementptr ([3 x i8], [3 x i8]* @.strS, i32 0, i32 0), i32* %"
                        + node.varName + ")");
    }

    private void compileIf(IfNode node) {
        String lThen = newLabel("if_then");
        String lElse = newLabel("if_else");
        String lEnd = newLabel("if_end");

        String cond = compileExpression(node.condition);

        boolean hasElse = (node.elseBlock != null);
        System.out.println("  br i1 " + cond + ", label %" + lThen + ", label %" + (hasElse ? lElse : lEnd));

        System.out.println(lThen + ":");
        compileBlock(node.thenBlock);
        System.out.println("  br label %" + lEnd);

        if (hasElse) {
            System.out.println(lElse + ":");
            compileBlock(node.elseBlock);
            System.out.println("  br label %" + lEnd);
        }
        System.out.println(lEnd + ":");
    }

    private void compileWhile(WhileNode node) {
        String lCond = newLabel("while_cond");
        String lBody = newLabel("while_body");
        String lEnd = newLabel("while_end");

        System.out.println("  br label %" + lCond);
        System.out.println(lCond + ":");

        String cond = compileExpression(node.condition);
        System.out.println("  br i1 " + cond + ", label %" + lBody + ", label %" + lEnd);

        System.out.println(lBody + ":");
        compileBlock(node.body);
        System.out.println("  br label %" + lCond);

        System.out.println(lEnd + ":");
    }

    private String compileExpression(Expression node) {
        if (node instanceof NumberNode) {
            return String.valueOf(((NumberNode) node).value);
        } else if (node instanceof VarRefNode) {
            String r = newReg();
            System.out.println("  " + r + " = load i32, i32* %" + ((VarRefNode) node).name);
            return r;
        } else if (node instanceof BinOpNode) {
            return compileBinOp((BinOpNode) node);
        }
        throw new RuntimeException("Expression inconnue");
    }

    private String compileBinOp(BinOpNode node) {
        if (node.operator.equals("->")) {
            String left = compileExpression(node.left);
            String notA = newReg();
            System.out.println("  " + notA + " = xor i1 " + left + ", 1");
            String right = compileExpression(node.right);
            String res = newReg();
            System.out.println("  " + res + " = or i1 " + notA + ", " + right);
            return res;
        }

        String l = compileExpression(node.left);
        String r = compileExpression(node.right);
        String res = newReg();

        switch (node.operator) {
            case "+":
                System.out.println("  " + res + " = add i32 " + l + ", " + r);
                break;
            case "-":
                System.out.println("  " + res + " = sub i32 " + l + ", " + r);
                break;
            case "*":
                System.out.println("  " + res + " = mul i32 " + l + ", " + r);
                break;
            case "/":
                System.out.println("  " + res + " = sdiv i32 " + l + ", " + r);
                break;
            case "==":
                System.out.println("  " + res + " = icmp eq i32 " + l + ", " + r);
                break;
            case "<":
                System.out.println("  " + res + " = icmp slt i32 " + l + ", " + r);
                break;
            case "<=":
                System.out.println("  " + res + " = icmp sle i32 " + l + ", " + r);
                break;
            default:
                throw new RuntimeException("Opérateur inconnu: " + node.operator);
        }
        return res;
    }

    private String newReg() {
        return "%r" + (regCounter++);
    }

    private String newLabel(String s) {
        return s + "_" + (labelCounter++);
    }

    private void printLLVMHeader() {
        System.out.println("; Target: LLVM IR generated by YALCC");
        System.out.println("declare i32 @printf(i8*, ...)");
        System.out.println("declare i32 @scanf(i8*, ...)");
        System.out.println("@.strP = private unnamed_addr constant [4 x i8] c\"%d\\0A\\00\", align 1");
        System.out.println("@.strS = private unnamed_addr constant [3 x i8] c\"%d\\00\", align 1");
        System.out.println("");
    }
}
// package src;

// import src.ast.*;
// import java.util.HashMap;
// import java.util.Map;

// public class Compiler {
// private int regCounter = 1;
// private int labelCounter = 1;
// private Map<String, String> symbolTable = new HashMap<>();

// public void compile(AstNode root) {
// // 1. En-tête LLVM
// printLLVMHeader();

// // 2. Allocation des variables
// // On parcourt l'AST pour trouver les variables et les déclarer au début
// preScanVariables(root);

// System.out.println("define i32 @main() {");
// System.out.println("entry:");

// // Création des 'alloca'
// for (String var : symbolTable.keySet()) {
// System.out.println(" %" + var + " = alloca i32");
// System.out.println(" store i32 0, i32* %" + var);
// }
// System.out.println("");

// // 3. Compilation des instructions
// if (root instanceof BlockNode) {
// compileBlock((BlockNode) root);
// } else {
// // Sécurité si la racine n'est pas un bloc
// compileInstruction((Instruction) root);
// }

// // 4. Fin du programme
// System.out.println(" ret i32 0");
// System.out.println("}");
// }

// // --- Passe 1 : Recherche des variables ---
// private void preScanVariables(AstNode node) {
// if (node instanceof BlockNode) {
// for (Instruction i : ((BlockNode) node).instructions)
// preScanVariables(i);
// } else if (node instanceof IfNode) {
// preScanVariables(((IfNode) node).condition);
// preScanVariables(((IfNode) node).thenBlock);
// if (((IfNode) node).elseBlock != null)
// preScanVariables(((IfNode) node).elseBlock);
// } else if (node instanceof WhileNode) {
// preScanVariables(((WhileNode) node).condition);
// preScanVariables(((WhileNode) node).body);
// } else if (node instanceof AssignNode) {
// String name = ((AssignNode) node).varName;
// symbolTable.putIfAbsent(name, name);
// preScanVariables(((AssignNode) node).value); // Attention: champ 'value'
// } else if (node instanceof InputNode) {
// String name = ((InputNode) node).varName;
// symbolTable.putIfAbsent(name, name);
// } else if (node instanceof BinOpNode) {
// preScanVariables(((BinOpNode) node).left);
// preScanVariables(((BinOpNode) node).right);
// }
// }

// // --- Passe 2 : Génération de code ---

// private void compileInstruction(Instruction node) {
// if (node instanceof BlockNode)
// compileBlock((BlockNode) node);
// else if (node instanceof AssignNode)
// compileAssign((AssignNode) node);
// else if (node instanceof PrintNode)
// compilePrint((PrintNode) node);
// else if (node instanceof InputNode)
// compileInput((InputNode) node);
// else if (node instanceof IfNode)
// compileIf((IfNode) node);
// else if (node instanceof WhileNode)
// compileWhile((WhileNode) node);
// }

// private void compileBlock(BlockNode node) {
// for (Instruction i : node.instructions) {
// compileInstruction(i);
// }
// }

// private void compileAssign(AssignNode node) {
// String valReg = compileExpression(node.value); // Utilisation de .value
// System.out.println(" store i32 " + valReg + ", i32* %" + node.varName);
// }

// private void compilePrint(PrintNode node) {
// String val = newReg();
// System.out.println(" " + val + " = load i32, i32* %" + node.varName);
// System.out.println(
// " call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.strP,
// i32 0, i32 0), i32 " + val
// + ")");
// }

// private void compileInput(InputNode node) {
// System.out.println(
// " call i32 (i8*, ...) @scanf(i8* getelementptr ([3 x i8], [3 x i8]* @.strS,
// i32 0, i32 0), i32* %"
// + node.varName + ")");
// }

// private void compileIf(IfNode node) {
// String lThen = newLabel("if_then");
// String lElse = newLabel("if_else");
// String lEnd = newLabel("if_end");

// String cond = compileExpression(node.condition);

// boolean hasElse = (node.elseBlock != null);
// System.out.println(" br i1 " + cond + ", label %" + lThen + ", label %" +
// (hasElse ? lElse : lEnd));

// System.out.println(lThen + ":");
// compileBlock(node.thenBlock);
// System.out.println(" br label %" + lEnd);

// if (hasElse) {
// System.out.println(lElse + ":");
// compileBlock(node.elseBlock);
// System.out.println(" br label %" + lEnd);
// }

// System.out.println(lEnd + ":");
// }

// private void compileWhile(WhileNode node) {
// String lCond = newLabel("while_cond");
// String lBody = newLabel("while_body");
// String lEnd = newLabel("while_end");

// System.out.println(" br label %" + lCond);

// System.out.println(lCond + ":");
// String cond = compileExpression(node.condition);
// System.out.println(" br i1 " + cond + ", label %" + lBody + ", label %" +
// lEnd);

// System.out.println(lBody + ":");
// compileBlock(node.body);
// System.out.println(" br label %" + lCond);

// System.out.println(lEnd + ":");
// }

// private String compileExpression(Expression node) {
// if (node instanceof NumberNode) {
// return String.valueOf(((NumberNode) node).value);
// } else if (node instanceof VarRefNode) {
// String r = newReg();
// System.out.println(" " + r + " = load i32, i32* %" + ((VarRefNode)
// node).name);
// return r;
// } else if (node instanceof BinOpNode) {
// return compileBinOp((BinOpNode) node);
// }
// throw new RuntimeException("Expression inconnue");
// }

// private String compileBinOp(BinOpNode node) {
// // Implication (->) : A -> B équivaut à (!A) | B
// if (node.operator.equals("->")) {
// String left = compileExpression(node.left);
// String notA = newReg();
// System.out.println(" " + notA + " = xor i1 " + left + ", 1");

// String right = compileExpression(node.right);
// String res = newReg();
// System.out.println(" " + res + " = or i1 " + notA + ", " + right);
// return res;
// }

// String l = compileExpression(node.left);
// String r = compileExpression(node.right);
// String res = newReg();

// switch (node.operator) {
// case "+":
// System.out.println(" " + res + " = add i32 " + l + ", " + r);
// break;
// case "-":
// System.out.println(" " + res + " = sub i32 " + l + ", " + r);
// break;
// case "*":
// System.out.println(" " + res + " = mul i32 " + l + ", " + r);
// break;
// case "/":
// System.out.println(" " + res + " = sdiv i32 " + l + ", " + r);
// break;

// case "==":
// System.out.println(" " + res + " = icmp eq i32 " + l + ", " + r);
// break;
// case "<":
// System.out.println(" " + res + " = icmp slt i32 " + l + ", " + r);
// break;
// case "<=":
// System.out.println(" " + res + " = icmp sle i32 " + l + ", " + r);
// break;

// default:
// throw new RuntimeException("Opérateur inconnu: " + node.operator);
// }
// return res;
// }

// private String newReg() {
// return "%r" + (regCounter++);
// }

// private String newLabel(String s) {
// return s + "_" + (labelCounter++);
// }

// private void printLLVMHeader() {
// System.out.println("; Target: LLVM IR");
// System.out.println("declare i32 @printf(i8*, ...)");
// System.out.println("declare i32 @scanf(i8*, ...)");
// System.out.println("@.strP = private unnamed_addr constant [4 x i8]
// c\"%d\\0A\\00\", align 1");
// System.out.println("@.strS = private unnamed_addr constant [3 x i8]
// c\"%d\\00\", align 1");
// System.out.println("");
// }
// }