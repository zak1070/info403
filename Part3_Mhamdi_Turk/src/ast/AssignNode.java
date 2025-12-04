package src.ast;

public class AssignNode extends Instruction {
    public String varName;
    public Expression value;

    public AssignNode(String varName, Expression value) {
        this.varName = varName;
        this.value = value;
    }
}