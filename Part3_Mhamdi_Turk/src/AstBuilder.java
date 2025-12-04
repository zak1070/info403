package src;

import src.ast.*;
import java.util.List;
import java.util.ArrayList;

public class AstBuilder {

    public static AstNode build(ParseTree tree) {
        Symbol label = tree.getLabel();

        if (label.isNonTerminal()) {
            NonTerminal type = (NonTerminal) label.getValue();
            switch (type) {
                case Program:
                    return buildProgram(tree);
                case Code:
                    return buildCode(tree);
                case Instruction:
                    return buildInstruction(tree);
                case Assign:
                    return buildAssign(tree);
                case If:
                    return buildIf(tree);
                case While:
                    return buildWhile(tree);
                case Output:
                    return buildPrint(tree);
                case Input:
                    return buildInput(tree);
                case ExprArith:
                    return buildExprArith(tree);
                case Atom:
                    return buildAtom(tree);
                case Cond:
                    return buildCond(tree);
                default:
                    throw new RuntimeException("Node non géré à la racine : " + type);
            }
        } else {
            throw new RuntimeException("Tentative de build sur un terminal : " + label.getType());
        }
    }

    private static BlockNode buildProgram(ParseTree tree) {
        return (BlockNode) buildCode(tree.getChildren().get(3));
    }

    private static BlockNode buildCode(ParseTree tree) {
        List<Instruction> instrs = new ArrayList<>();
        ParseTree current = tree;

        while (current.getChildren().size() > 1) { 
            ParseTree instrTree = current.getChildren().get(0);
            instrs.add((Instruction) buildInstruction(instrTree));
            current = current.getChildren().get(2);
        }
        return new BlockNode(instrs);
    }

    private static Instruction buildInstruction(ParseTree tree) {
        ParseTree child = tree.getChildren().get(0);
        Symbol label = child.getLabel();
        
        if (label.isNonTerminal()) {
            NonTerminal type = (NonTerminal) label.getValue();
            switch(type) {
                case Assign: return buildAssign(child);
                case If: return buildIf(child);
                case While: return buildWhile(child);
                case Output: return buildPrint(child);
                case Input: return buildInput(child);
                default: throw new RuntimeException("Instruction inconnue: " + type);
            }
        }
        throw new RuntimeException("Erreur structure Instruction");
    }

    private static AssignNode buildAssign(ParseTree tree) {
        String varName = (String) tree.getChildren().get(0).getLabel().getValue();
        Expression expr = buildExprArith(tree.getChildren().get(2));
        return new AssignNode(varName, expr);
    }

    private static PrintNode buildPrint(ParseTree tree) {
        String varName = (String) tree.getChildren().get(2).getLabel().getValue();
        return new PrintNode(varName);
    }

    private static InputNode buildInput(ParseTree tree) {
        String varName = (String) tree.getChildren().get(2).getLabel().getValue();
        return new InputNode(varName);
    }

    private static IfNode buildIf(ParseTree tree) {
        Expression condition = buildCond(tree.getChildren().get(2));
        BlockNode thenBlock = buildCode(tree.getChildren().get(5));
        BlockNode elseBlock = buildIfTail(tree.getChildren().get(6));
        return new IfNode(condition, thenBlock, elseBlock);
    }

    private static BlockNode buildIfTail(ParseTree tree) {
        ParseTree firstChild = tree.getChildren().get(0);
        if (firstChild.getLabel().getType() == LexicalUnit.ELSE) {
            return buildCode(tree.getChildren().get(1));
        }
        return null;
    }

    private static WhileNode buildWhile(ParseTree tree) {
        Expression condition = buildCond(tree.getChildren().get(2));
        BlockNode body = buildCode(tree.getChildren().get(5));
        return new WhileNode(condition, body);
    }

    private static Expression buildExprArith(ParseTree tree) {
        Expression left = buildProd(tree.getChildren().get(0));
        return buildExprArithPrime(tree.getChildren().get(1), left);
    }

    private static Expression buildExprArithPrime(ParseTree tree, Expression left) {
        if (tree.getChildren().get(0).getLabel().getType() == LexicalUnit.EPSILON) {
            return left;
        }
        String op = tree.getChildren().get(0).getLabel().getType() == LexicalUnit.PLUS ? "+" : "-";
        Expression right = buildProd(tree.getChildren().get(1));
        BinOpNode newNode = new BinOpNode(left, op, right);
        return buildExprArithPrime(tree.getChildren().get(2), newNode);
    }

    private static Expression buildProd(ParseTree tree) {
        Expression left = buildAtom(tree.getChildren().get(0));
        return buildProdPrime(tree.getChildren().get(1), left);
    }

    private static Expression buildProdPrime(ParseTree tree, Expression left) {
        if (tree.getChildren().get(0).getLabel().getType() == LexicalUnit.EPSILON) {
            return left;
        }
        LexicalUnit type = tree.getChildren().get(0).getLabel().getType();
        String op = (type == LexicalUnit.TIMES) ? "*" : "/";
        Expression right = buildAtom(tree.getChildren().get(1));
        BinOpNode newNode = new BinOpNode(left, op, right);
        return buildProdPrime(tree.getChildren().get(2), newNode);
    }

    private static Expression buildAtom(ParseTree tree) {
        ParseTree child = tree.getChildren().get(0);
        LexicalUnit type = child.getLabel().getType();

        if (type == LexicalUnit.VARNAME) {
            return new VarRefNode((String) child.getLabel().getValue());
        } else if (type == LexicalUnit.NUMBER) {
            return new NumberNode((Integer) child.getLabel().getValue());
        } else if (type == LexicalUnit.MINUS) {
            Expression atom = buildAtom(tree.getChildren().get(1));
            return new BinOpNode(new NumberNode(0), "-", atom);
        } else if (type == LexicalUnit.LPAREN) {
            return buildExprArith(tree.getChildren().get(1));
        }
        throw new RuntimeException("Atom inconnu");
    }

    private static Expression buildCond(ParseTree tree) {
        Expression left = buildSimpleCond(tree.getChildren().get(0));
        return buildCondPrime(tree.getChildren().get(1), left);
    }

    private static Expression buildCondPrime(ParseTree tree, Expression left) {
        if (tree.getChildren().get(0).getLabel().getType() == LexicalUnit.EPSILON) {
            return left;
        }
        Expression right = buildCond(tree.getChildren().get(1));
        return new BinOpNode(left, "->", right);
    }

    private static Expression buildSimpleCond(ParseTree tree) {
        ParseTree firstChild = tree.getChildren().get(0);
        
        if (firstChild.getLabel().getType() == LexicalUnit.PIPE) {
            return buildCond(tree.getChildren().get(1));
        } else {
            Expression left = buildExprArith(tree.getChildren().get(0));
            ParseTree compTree = tree.getChildren().get(1);
            LexicalUnit compType = compTree.getChildren().get(0).getLabel().getType();
            String op = "";
            switch(compType) {
                case EQUAL: op = "=="; break;
                case SMALEQ: op = "<="; break;
                case SMALLER: op = "<"; break;
                default: throw new RuntimeException("Comparateur inconnu");
            }
            Expression right = buildExprArith(tree.getChildren().get(2));
            return new BinOpNode(left, op, right);
        }
    }
}