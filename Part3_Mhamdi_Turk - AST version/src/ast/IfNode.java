package src.ast;

public class IfNode extends Instruction {
    public Expression condition;
    public BlockNode thenBlock;
    public BlockNode elseBlock; // Peut être null si pas de 'Else'

    public IfNode(Expression condition, BlockNode thenBlock, BlockNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }
}