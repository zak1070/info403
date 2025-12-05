package src.ast;

public class InputNode extends Instruction {
    public String varName;

    public InputNode(String varName) {
        this.varName = varName;
    }
}