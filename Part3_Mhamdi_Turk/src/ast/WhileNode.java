package src.ast;

public class WhileNode extends Instruction {
    public Expression condition;
    public BlockNode body;

    public WhileNode(Expression condition, BlockNode body) {
        this.condition = condition;
        this.body = body;
    }
}