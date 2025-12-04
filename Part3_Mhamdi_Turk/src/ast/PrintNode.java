package src.ast;

public class PrintNode extends Instruction {
    public String varName;

    public PrintNode(String varName) {
        this.varName = varName;
    }
}