package src.ast;

public class VarRefNode extends Expression {
    public String name;

    public VarRefNode(String name) {
        this.name = name;
    }
}