package src;

import java.util.List;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Parser for Yalcc.
 * * This parser implements the recursive descent algorithm.
 * It has been modified to generate LLVM IR code directly during parsing.
 */
public class Parser {
    
    private LexicalAnalyzer scanner;
    private Symbol current;

    // Tools for LLVM generation
    private StringBuilder codeBuffer; // We store the code here before printing it
    private Set<String> variables;    // Keeps track of vars to 'alloca' them at the start
    private int registerCounter;      // To generate unique registers like %1, %2
    private int labelCounter;         // To generate unique labels for jumps

    /**
     * Constructor. Opens the file and inits the counters.
     */
    public Parser(FileReader source) throws IOException {
        this.scanner = new LexicalAnalyzer(source);
        this.current = scanner.nextToken();

        // Init generation tools
        this.codeBuffer = new StringBuilder();
        this.variables = new HashSet<>();
        this.registerCounter = 0;
        this.labelCounter = 0;
    }



    // Returns a new temporary register (e.g. "%5")
    private String newReg() {
        registerCounter++;
        return "%" + registerCounter;
    }

    // Returns a new label for control flow (e.g. "label_2")
    private String newLabel() {
        labelCounter++;
        return "label_" + labelCounter;
    }

    // Writes an instruction to our buffer with indentation
    private void emit(String instr) {
        codeBuffer.append("  ").append(instr).append("\n");
    }

    // Writes a label (no indentation)
    private void emitLabel(String label) {
        codeBuffer.append(label).append(":\n");
    }

    // Get next token
    private void consume() throws IOException {
        current = scanner.nextToken();
    }

    // Check token type and consume, or throw error
    private void match(LexicalUnit token) throws IOException, ParseException {
        if (!current.getType().equals(token)) {
            throw new ParseException(current, Arrays.asList(token));
        } else {
            consume();
        }
    }


    // Grammar Rules
  
    // Entry point
    public void parse() throws IOException, ParseException {
        program();
    }

    /**
     * <Program> -> Prog ...
     * This is where we setup the LLVM file structure.
     */
    private void program() throws IOException, ParseException {
        match(LexicalUnit.PROG);
        match(LexicalUnit.PROGNAME);
        match(LexicalUnit.IS);

        // Parse the code first to find all variables
        code();

        match(LexicalUnit.END);

        // --- Output everything to stdout ---

        // Standard headers for printf/scanf
        System.out.println("; Target: LLVM IR");
        System.out.println("declare i32 @printf(i8*, ...)");
        System.out.println("declare i32 @scanf(i8*, ...)");
        
        // Constants (strings for IO)
        System.out.println("@.str_out = private unnamed_addr constant [4 x i8] c\"%d\\0A\\00\"");
        System.out.println("@.str_in = private unnamed_addr constant [3 x i8] c\"%d\\00\"");
        System.out.println("@.str_prompt = private unnamed_addr constant [9 x i8] c\"input : \\00\""); 
        
        System.out.println();

        // Start of main
        System.out.println("define i32 @main() {");
        System.out.println("entry:");

        // 4. Allocate memory for all variables seen in the code
        if (!variables.isEmpty()) {
            System.out.println("  ; Variable allocations");
            for (String var : variables) {
                System.out.println("  %" + var + " = alloca i32");
            }
            System.out.println("  ; End allocations\n");
        }

        // Print the code we generated
        System.out.print(codeBuffer.toString());

        // Exit
        System.out.println("  ret i32 0");
        System.out.println("}");
    }

    // <Code> : loops through instructions until End or Else
    private void code() throws IOException, ParseException {
        switch (current.getType()) {
            case IF:
            case WHILE:
            case PRINT:
            case INPUT:
            case VARNAME:
                instruction();
                match(LexicalUnit.SEMI);
                code();
                break;
            case END:
            case ELSE:
                // We stop parsing here
                break;
            default:
                throw new ParseException(current, NonTerminal.Code, Arrays.asList(
                        LexicalUnit.IF, LexicalUnit.ELSE, LexicalUnit.WHILE,
                        LexicalUnit.PRINT, LexicalUnit.INPUT, LexicalUnit.VARNAME, LexicalUnit.END));
        }
    }

    // Dispatcher for instructions
    private void instruction() throws IOException, ParseException {
        switch (current.getType()) {
            case VARNAME:
                assignExpr();
                break;
            case IF:
                ifExpr();
                break;
            case WHILE:
                whileExpr();
                break;
            case PRINT:
                outputExpr();
                break;
            case INPUT:
                inputExpr();
                break;
            default:
                throw new ParseException(current, NonTerminal.Instruction, Arrays.asList(
                        LexicalUnit.VARNAME, LexicalUnit.IF, LexicalUnit.WHILE,
                        LexicalUnit.PRINT, LexicalUnit.INPUT));
        }
    }

    // Handles assignment: x = ...
    private void assignExpr() throws IOException, ParseException {
        String varName = (String) current.getValue();
        variables.add(varName); // Add to set for allocation
        match(LexicalUnit.VARNAME);
        
        match(LexicalUnit.ASSIGN);
        
        String resultReg = exprArith(); // Calculate right side
        emit("store i32 " + resultReg + ", i32* %" + varName);
    }

    // Arithmetic expressions (handles precedence)
    private String exprArith() throws IOException, ParseException {
        String leftReg = prod();
        return exprArithPrime(leftReg);
    }

    // Handles + and -
    private String exprArithPrime(String leftOp) throws IOException, ParseException {
        switch (current.getType()) {
            case PLUS:
                match(LexicalUnit.PLUS);
                String rightOpPlus = prod();
                String newRegPlus = newReg();
                emit(newRegPlus + " = add i32 " + leftOp + ", " + rightOpPlus);
                return exprArithPrime(newRegPlus);
            case MINUS:
                match(LexicalUnit.MINUS);
                String rightOpMinus = prod();
                String newRegMinus = newReg();
                emit(newRegMinus + " = sub i32 " + leftOp + ", " + rightOpMinus);
                return exprArithPrime(newRegMinus);
            // Follow set for ExprArith'
            case SEMI: case RPAREN: case RBRACK: case EQUAL: 
            case SMALEQ: case SMALLER: case IMPLIES: case PIPE:
                return leftOp;
            default:
                throw new ParseException(current, NonTerminal.ExprArithPrime, Arrays.asList(
                        LexicalUnit.PLUS, LexicalUnit.MINUS, LexicalUnit.SEMI));
        }
    }

    // Handles products
    private String prod() throws IOException, ParseException {
        String leftReg = atom();
        return prodPrime(leftReg);
    }

    // Handles * and /
    private String prodPrime(String leftOp) throws IOException, ParseException {
        switch (current.getType()) {
            case TIMES:
                match(LexicalUnit.TIMES);
                String rightOpTimes = atom();
                String newRegTimes = newReg();
                emit(newRegTimes + " = mul i32 " + leftOp + ", " + rightOpTimes);
                return prodPrime(newRegTimes);
            case DIVIDE:
                match(LexicalUnit.DIVIDE);
                String rightOpDiv = atom();
                String newRegDiv = newReg();
                emit(newRegDiv + " = sdiv i32 " + leftOp + ", " + rightOpDiv);
                return prodPrime(newRegDiv);
            // Follow set
            case SEMI: case PLUS: case MINUS: case RPAREN: case RBRACK: 
            case EQUAL: case SMALLER: case SMALEQ: case IMPLIES: case PIPE:
                return leftOp;
            default:
                throw new ParseException(current, NonTerminal.ProdPrime, Arrays.asList(LexicalUnit.TIMES));
        }
    }

    // Basic units: numbers, vars, parens
    private String atom() throws IOException, ParseException {
        String reg;
        switch (current.getType()) {
            case MINUS: // Unary minus
                match(LexicalUnit.MINUS);
                String atomReg = atom();
                reg = newReg();
                emit(reg + " = sub i32 0, " + atomReg);
                return reg;
            case LPAREN:
                match(LexicalUnit.LPAREN);
                reg = exprArith();
                match(LexicalUnit.RPAREN);
                return reg;
            case VARNAME:
                String varName = (String) current.getValue();
                variables.add(varName);
                match(LexicalUnit.VARNAME);
                reg = newReg();
                emit(reg + " = load i32, i32* %" + varName);
                return reg;
            case NUMBER:
                Integer val = (Integer) current.getValue();
                match(LexicalUnit.NUMBER);
                return val.toString();
            default:
                throw new ParseException(current, NonTerminal.Atom, Arrays.asList(
                        LexicalUnit.MINUS, LexicalUnit.LPAREN, LexicalUnit.VARNAME, LexicalUnit.NUMBER));
        }
    }

    // Handles If { cond } Then ... Else ...
    private void ifExpr() throws IOException, ParseException {
        match(LexicalUnit.IF);
        match(LexicalUnit.LBRACK);
        String condReg = cond(); 
        match(LexicalUnit.RBRACK);
        match(LexicalUnit.THEN);

        String labelTrue = newLabel();
        String labelFalse = newLabel();
        String labelEnd = newLabel();

        emit("br i1 " + condReg + ", label %" + labelTrue + ", label %" + labelFalse);

        // True branch
        emitLabel(labelTrue);
        code();
        emit("br label %" + labelEnd);

        // False branch
        emitLabel(labelFalse);
        ifTail();
        emit("br label %" + labelEnd);

        // End of if
        emitLabel(labelEnd);
    }

    // Handles Else block
    private void ifTail() throws IOException, ParseException {
        if (current.getType() == LexicalUnit.ELSE) {
            match(LexicalUnit.ELSE);
            code();
            match(LexicalUnit.END);
        } else {
            match(LexicalUnit.END);
        }
    }

    // Boolean expressions entry point
    private String cond() throws IOException, ParseException {
        String leftReg = simpleCond();
        return condPrime(leftReg);
    }

    // Handles Implication -> (Right associative)
    private String condPrime(String leftOp) throws IOException, ParseException {
        if (current.getType() == LexicalUnit.IMPLIES) {
            match(LexicalUnit.IMPLIES);
            String rightOp = cond();
            
            // Logic for A -> B is equivalent to (!A) OR B
            String notLeft = newReg();
            emit(notLeft + " = xor i1 " + leftOp + ", 1");
            String res = newReg();
            emit(res + " = or i1 " + notLeft + ", " + rightOp);
            return res;
        }
        return leftOp;
    }

    // Handles comparisons or parentheses in conditions
    private String simpleCond() throws IOException, ParseException {
        if (current.getType() == LexicalUnit.PIPE) {
            match(LexicalUnit.PIPE);
            String res = cond();
            match(LexicalUnit.PIPE);
            return res;
        } else {
            String leftReg = exprArith();
            LexicalUnit op = current.getType();
            compOp(); 
            String rightReg = exprArith();
            
            String res = newReg();
            String codeOp = "";
            switch (op) {
                case EQUAL:   codeOp = "eq"; break;
                case SMALEQ:  codeOp = "sle"; break;
                case SMALLER: codeOp = "slt"; break;
                default: break;
            }
            emit(res + " = icmp " + codeOp + " i32 " + leftReg + ", " + rightReg);
            return res;
        }
    }

    private void compOp() throws IOException, ParseException {
        switch (current.getType()) {
            case EQUAL: match(LexicalUnit.EQUAL); break;
            case SMALEQ: match(LexicalUnit.SMALEQ); break;
            case SMALLER: match(LexicalUnit.SMALLER); break;
            default: throw new ParseException(current, NonTerminal.Comp,
                    Arrays.asList(LexicalUnit.EQUAL, LexicalUnit.SMALEQ, LexicalUnit.SMALLER));
        }
    }

    // Handles While loops
    private void whileExpr() throws IOException, ParseException {
        match(LexicalUnit.WHILE);
        match(LexicalUnit.LBRACK);
        
        String labelCond = newLabel();
        String labelBody = newLabel();
        String labelEnd = newLabel();

        // Jump to condition first
        emit("br label %" + labelCond);

        emitLabel(labelCond);
        String condReg = cond();
        emit("br i1 " + condReg + ", label %" + labelBody + ", label %" + labelEnd);

        match(LexicalUnit.RBRACK);
        match(LexicalUnit.DO);

        emitLabel(labelBody);
        code();
        emit("br label %" + labelCond); // Loop back

        match(LexicalUnit.END);
        
        emitLabel(labelEnd);
    }

    // Handles Print(var)
    private void outputExpr() throws IOException, ParseException {
        match(LexicalUnit.PRINT);
        match(LexicalUnit.LPAREN);
        String varName = (String) current.getValue();
        variables.add(varName);
        match(LexicalUnit.VARNAME);
        match(LexicalUnit.RPAREN);

        String valReg = newReg();
        emit(valReg + " = load i32, i32* %" + varName);
        String callReg = newReg();
        emit(callReg + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str_out, i32 0, i32 0), i32 " + valReg + ")");
    }

    // Handles Input(var)
    private void inputExpr() throws IOException, ParseException {
        match(LexicalUnit.INPUT);
        match(LexicalUnit.LPAREN);
        String varName = (String) current.getValue();
        variables.add(varName);
        match(LexicalUnit.VARNAME);
        match(LexicalUnit.RPAREN);

        // Little UX improvement: print "input : " before waiting
        String promptCall = newReg();
        emit(promptCall + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str_prompt, i32 0, i32 0))");

        // Actual scanf
        String callReg = newReg();
        emit(callReg + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str_in, i32 0, i32 0), i32* %" + varName + ")");
    }
}