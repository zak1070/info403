package src.ast;

public class BinOpNode extends Expression {
    public Expression left;
    public String operator;
    public Expression right;

    public BinOpNode(Expression left, String operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}