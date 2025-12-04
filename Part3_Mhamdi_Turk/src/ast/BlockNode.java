package src.ast;

import java.util.List;
import java.util.ArrayList;

public class BlockNode extends Instruction {
    public List<Instruction> instructions;

    public BlockNode(List<Instruction> instructions) {
        this.instructions = instructions;
    }
    
    // Constructeur vide pratique
    public BlockNode() {
        this.instructions = new ArrayList<>();
    }
}