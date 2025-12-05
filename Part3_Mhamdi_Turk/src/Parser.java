package src;

import java.util.List;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Parser for Yalcc.
 * * The parser implements a recursive descent mimicking the run of the pushdown
 * automaton: the call stack replacing the automaton stack.
 * * @author Mrudula Balachander, inspired from earlier versions of the project
 * (exact authors not determined).
 *
 */
public class Parser {
    /**
     * Lexer object for the parsed file.
     */
    private LexicalAnalyzer scanner;
    /**
     * Current symbol at the head of the word to be read. This corresponds to the
     * look-ahead (of length 1).
     */
    private Symbol current;

    // Code Generation fields
    private StringBuilder codeBuffer; // Buffers the instructions body
    private Set<String> variables; // Tracks declared variables for 'alloca'
    private int registerCounter; // Counter for temporary registers (%1, %2...)
    private int labelCounter; // Counter for flow labels (if, while)

    /**
     * Creates a Parser object for the provided file and initialized the look-ahead.
     * * @param source a FileReader object for the parsed file.
     * 
     * @throws IOException in case the lexing fails (syntax error).
     */
    public Parser(FileReader source) throws IOException {
        this.scanner = new LexicalAnalyzer(source);
        this.current = scanner.nextToken();

        // Init code gen tools
        this.codeBuffer = new StringBuilder();
        this.variables = new HashSet<>();
        this.registerCounter = 0;
        this.labelCounter = 0;
    }

    // ==========================================================
    // Code Generation Utils
    // ==========================================================

    /**
     * Generates a new unique temporary register name.
     * 
     * @return The register name (e.g., "%1")
     */
    private String newReg() {
        registerCounter++;
        return "%" + registerCounter;
    }

    /**
     * Generates a new unique label name.
     * 
     * @return The label name (e.g., "label_5")
     */
    private String newLabel() {
        labelCounter++;
        return "label_" + labelCounter;
    }

    /**
     * Emits a raw LLVM instruction to the buffer.
     * 
     * @param instr The instruction string.
     */
    private void emit(String instr) {
        codeBuffer.append("  ").append(instr).append("\n");
    }

    /**
     * Emits a label to the buffer.
     * 
     * @param label The label name (without colon in arg).
     */
    private void emitLabel(String label) {
        codeBuffer.append(label).append(":\n");
    }

    /* Matching of terminals */
    /**
     * Advances in the input stream, consuming one token.
     * * @throws IOException in case the lexing fails (syntax error).
     */
    private void consume() throws IOException {
        current = scanner.nextToken();
    }

    /**
     * Matches a (terminal) token from the head of the word.
     * * @param token then LexicalUnit (terminal) to be matched.
     * 
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the matching fails (syntax error): the next
     *                        tolen is not the one to be matched.
     */
    private void match(LexicalUnit token) throws IOException, ParseException {
        if (!current.getType().equals(token)) {
            // There is a parsing error
            throw new ParseException(current, Arrays.asList(token));
        } else {
            consume();
        }
    }

    /* Applying grammar rules */
    /**
     * Parses the file.
     * * @return a ParseTree containing the parsed file structured by the grammar
     * rules.
     * 
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
    public ParseTree parse() throws IOException, ParseException {
        // Program is the initial symbol of the grammar
        program();
        return new ParseTree(NonTerminal.Program); // Dummy return to satisfy Main.java signature
    }

    /**
     * Treats a &lt;Program&gt; at the top of the stack.
     * * Tries to apply rule
     * [1]&nbsp;&lt;Prog&gt;&nbsp;&rarr;&nbsp;<code>Prog [ProgName] Is</code>
     * &lt;Code&gt; <code>End</code>
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private void program() throws IOException, ParseException {
        // [1] <Program> -> begin <Code> end
        match(LexicalUnit.PROG);
        match(LexicalUnit.PROGNAME);
        match(LexicalUnit.IS);

        // Parse body first to gather all variables and instructions
        code();

        match(LexicalUnit.END);

        // --- FINAL OUTPUT GENERATION ---

        // 1. Headers
        System.out.println("; Target: LLVM IR");
        System.out.println("declare i32 @printf(i8*, ...)");
        System.out.println("declare i32 @scanf(i8*, ...)");
        System.out.println("@.str_out = private unnamed_addr constant [4 x i8] c\"%d\\0A\\00\""); // For Print
        System.out.println("@.str_in = private unnamed_addr constant [3 x i8] c\"%d\\00\""); // For Input
        System.out.println();

        // 2. Main function start
        System.out.println("define i32 @main() {");
        System.out.println("entry:");

        // 3. Allocations (Generated at the top of main)
        if (!variables.isEmpty()) {
            System.out.println("  ; Variable allocations");
            for (String var : variables) {
                System.out.println("  %" + var + " = alloca i32");
            }
            System.out.println("  ; End allocations\n");
        }

        // 4. Body Code
        System.out.print(codeBuffer.toString());

        // 5. Main function end
        System.out.println("  ret i32 0");
        System.out.println("}");
    }

    /**
     * Treats a &lt;Code&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[2]&nbsp;&lt;Code&gt;&nbsp;&rarr;&nbsp;&lt;Instruction&gt;&nbsp;<code>;</code>&nbsp;&lt;Code&gt;</li>
     * <li>[3]&nbsp;&lt;Code&gt;&nbsp;&rarr;&nbsp;&epsilon;</li>
     * </ul>
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private void code() throws IOException, ParseException {
        switch (current.getType()) {
            // [2] <Code> -> <Instruction>;<Code>
            case IF:
            case WHILE:
            case PRINT:
            case INPUT:
            case VARNAME:
                instruction();
                match(LexicalUnit.SEMI);
                code();
                break;
            // [3] <Code> -> EPSILON
            case END:
            case ELSE:
                break;
            default:
                throw new ParseException(current, NonTerminal.Code, Arrays.asList(
                        LexicalUnit.IF,
                        LexicalUnit.ELSE,
                        LexicalUnit.WHILE,
                        LexicalUnit.PRINT,
                        LexicalUnit.INPUT,
                        LexicalUnit.VARNAME,
                        LexicalUnit.END));
        }
    }

    /**
     * Treats a &lt;Instruction&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[4]&nbsp;&lt;Instruction&gt;&nbsp;&rarr;&nbsp;&lt;Assign&gt;</li>
     * <li>[5]&nbsp;&lt;Instruction&gt;&nbsp;&rarr;&nbsp;&lt;If&gt;</li>
     * <li>[6]&nbsp;&lt;Instruction&gt;&nbsp;&rarr;&nbsp;&lt;While&gt;</li>
     * <li>[7]&nbsp;&lt;Instruction&gt;&nbsp;&rarr;&nbsp;&lt;Output&gt;</li>
     * <li>[8]&nbsp;&lt;Instruction&gt;&nbsp;&rarr;&nbsp;&lt;Input&gt;</li>
     * </ul>
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private void instruction() throws IOException, ParseException {
        switch (current.getType()) {
            // [4] <Instruction> -> <Assign>
            case VARNAME:
                assignExpr();
                break;
            // [5] <Instruction> -> <If>
            case IF:
                ifExpr();
                break;
            // [6] <Instruction> -> <While>
            case WHILE:
                whileExpr();
                break;
            // [7] <Instruction> -> <Output>
            case PRINT:
                outputExpr();
                break;
            // [8] <Instruction> -> <Input>
            case INPUT:
                inputExpr();
                break;
            default:
                throw new ParseException(current, NonTerminal.Instruction, Arrays.asList(
                        LexicalUnit.VARNAME,
                        LexicalUnit.IF,
                        LexicalUnit.WHILE,
                        LexicalUnit.PRINT,
                        LexicalUnit.INPUT));
        }
    }

    /**
     * Treats a &lt;Assign&gt; at the top of the stack.
     * * Tries to apply rule
     * [9]&nbsp;&lt;Assign&gt;&nbsp;&rarr;&nbsp;[Varname]<code>:=</code>&lt;ExprArith&gt;
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private void assignExpr() throws IOException, ParseException {
        String varName = (String) current.getValue();
        variables.add(varName); // Register variable for alloca
        match(LexicalUnit.VARNAME);

        match(LexicalUnit.ASSIGN);

        String resultReg = exprArith();

        emit("store i32 " + resultReg + ", i32* %" + varName);
    }

    /**
     * Treats a &lt;ExprArith&gt; at the top of the stack.
     * * Tries to apply rule
     * [10]&nbsp;&lt;ExprArith&gt;&nbsp;&rarr;&nbsp;&lt;Prod&gt;&lt;ExprArith'&gt;
     * * @return the register containing the result of the expression.
     * 
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private String exprArith() throws IOException, ParseException {
        String leftReg = prod();
        return exprArithPrime(leftReg);
    }

    /**
     * Treats a &lt;ExprArith'&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[11]&nbsp;&lt;ExprArith'&gt;&nbsp;&rarr;&nbsp;<code>+</code>&lt;Prod&gt;&lt;ExprArith'&gt;</li>
     * <li>[12]&nbsp;&lt;ExprArith'&gt;&nbsp;&rarr;&nbsp;<code>-</code>&lt;Prod&gt;&lt;ExprArith'&gt;</li>
     * <li>[13]&nbsp;&lt;ExprArith'&gt;&nbsp;&rarr;&nbsp;&epsilon;</li>
     * </ul>
     * * @param leftOp the register holding the left operand.
     * 
     * @return the register containing the accumulated result.
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private String exprArithPrime(String leftOp) throws IOException, ParseException {
        switch (current.getType()) {
            // [11] <ExprArith'> -> + <Prod> <ExprArith'>
            case PLUS:
                match(LexicalUnit.PLUS);
                String rightOpPlus = prod();
                String newRegPlus = newReg();
                emit(newRegPlus + " = add i32 " + leftOp + ", " + rightOpPlus);
                return exprArithPrime(newRegPlus);

            // [12] <ExprArith'> -> - <Prod> <ExprArith'>
            case MINUS:
                match(LexicalUnit.MINUS);
                String rightOpMinus = prod();
                String newRegMinus = newReg();
                emit(newRegMinus + " = sub i32 " + leftOp + ", " + rightOpMinus);
                return exprArithPrime(newRegMinus);

            // [13] <ExprArith'> -> EPSILON
            case SEMI:
            case RPAREN:
            case RBRACK:
            case EQUAL:
            case SMALEQ:
            case SMALLER:
            case IMPLIES:
            case PIPE:
                return leftOp;

            default:
                throw new ParseException(current, NonTerminal.ExprArithPrime, Arrays.asList(
                        LexicalUnit.PLUS,
                        LexicalUnit.MINUS,
                        LexicalUnit.SEMI,
                        LexicalUnit.RPAREN,
                        LexicalUnit.RBRACK,
                        LexicalUnit.EQUAL,
                        LexicalUnit.SMALEQ,
                        LexicalUnit.SMALLER,
                        LexicalUnit.IMPLIES,
                        LexicalUnit.PIPE));
        }
    }

    /**
     * Treats a &lt;Prod&gt; at the top of the stack.
     * * Tries to apply rule
     * [14]&nbsp;&lt;Prod&gt;&nbsp;&rarr;&nbsp;&lt;Atom&gt;&lt;Prod'&gt;
     * * @return the register containing the product result.
     * 
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private String prod() throws IOException, ParseException {
        String leftReg = atom();
        return prodPrime(leftReg);
    }

    /**
     * Treats a &lt;Prod'&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[19]&nbsp;&lt;Prod'&gt;&nbsp;&rarr;&nbsp;<code>*</code>&lt;Atom&gt;&lt;Prod'&gt;</li>
     * <li>[20]&nbsp;&lt;Prod'&gt;&nbsp;&rarr;&nbsp;<code>/</code>&lt;Atom&gt;&lt;Prod'&gt;</li>
     * <li>[21]&nbsp;&lt;Prod'&gt;&nbsp;&rarr;&nbsp;&epsilon;</li>
     * </ul>
     * * @param leftOp the register holding the left operand.
     * 
     * @return the register containing the accumulated product.
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private String prodPrime(String leftOp) throws IOException, ParseException {
        switch (current.getType()) {
            // [15] <Prod'> -> * <Atom> <Prod'>
            case TIMES:
                match(LexicalUnit.TIMES);
                String rightOpTimes = atom();
                String newRegTimes = newReg();
                emit(newRegTimes + " = mul i32 " + leftOp + ", " + rightOpTimes);
                return prodPrime(newRegTimes);

            // [16] <Prod'> -> / <Atom> <Prod'>
            case DIVIDE:
                match(LexicalUnit.DIVIDE);
                String rightOpDiv = atom();
                String newRegDiv = newReg();
                emit(newRegDiv + " = sdiv i32 " + leftOp + ", " + rightOpDiv);
                return prodPrime(newRegDiv);

            // [17] <Prod'> -> EPSILON
            case SEMI:
            case PLUS:
            case MINUS:
            case RPAREN:
            case RBRACK:
            case EQUAL:
            case SMALLER:
            case SMALEQ:
            case IMPLIES:
            case PIPE:
                return leftOp;

            default:
                throw new ParseException(current, NonTerminal.ProdPrime, Arrays.asList(
                        LexicalUnit.PLUS,
                        LexicalUnit.MINUS,
                        LexicalUnit.TIMES,
                        LexicalUnit.DIVIDE,
                        LexicalUnit.SEMI,
                        LexicalUnit.RPAREN,
                        LexicalUnit.RBRACK,
                        LexicalUnit.SMALEQ,
                        LexicalUnit.IMPLIES,
                        LexicalUnit.PIPE,
                        LexicalUnit.EQUAL,
                        LexicalUnit.SMALLER));
        }
    }

    /**
     * Treats a &lt;Atom&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[18]&nbsp;&lt;Atom&gt;&nbsp;&rarr;&nbsp;[VarName]</li>
     * <li>[19]&nbsp;&lt;Atom&gt;&nbsp;&rarr;&nbsp;[Number]</li>
     * <li>[20]&nbsp;&lt;Atom&gt;&nbsp;&rarr;&nbsp;<code>(</code>&lt;ExprArith&gt;<code>)</code></li>
     * <li>[21]&nbsp;&lt;Atom&gt;&nbsp;&rarr;&nbsp;<code>-</code>&lt;Atom&gt;</li>
     * </ul>
     * * @return the register containing the atom's value.
     * 
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private String atom() throws IOException, ParseException {
        String reg;
        switch (current.getType()) {
            // [21] <Atom> -> - <Atom>
            case MINUS:
                match(LexicalUnit.MINUS);
                String atomReg = atom();
                reg = newReg();
                emit(reg + " = sub i32 0, " + atomReg);
                return reg;

            // [20] <Atom> -> (<ExprArith>)
            case LPAREN:
                match(LexicalUnit.LPAREN);
                reg = exprArith();
                match(LexicalUnit.RPAREN);
                return reg;

            // [18] <Atom> -> [VarName]
            case VARNAME:
                String varName = (String) current.getValue();
                variables.add(varName); // Ensure tracked
                match(LexicalUnit.VARNAME);
                reg = newReg();
                emit(reg + " = load i32, i32* %" + varName);
                return reg;

            // [19] <Atom> -> [Number]
            case NUMBER:
                Integer val = (Integer) current.getValue();
                match(LexicalUnit.NUMBER);
                return val.toString();

            default:
                throw new ParseException(current, NonTerminal.Atom, Arrays.asList(
                        LexicalUnit.MINUS,
                        LexicalUnit.LPAREN,
                        LexicalUnit.VARNAME,
                        LexicalUnit.NUMBER));
        }
    }

    /**
     * Treats a &lt;If&gt; at the top of the stack.
     * * Tries to apply rule
     * [22]&nbsp;&lt;If&gt;&nbsp;&rarr;&nbsp;<code>IF</code>&nbsp;<code>{</code>&lt;Cond&gt;<code>}</code>&nbsp;<code>THEN</code>&nbsp;&lt;Code&gt;&nbsp;<code>ELSE</code>&nbsp;&lt;IfTail&gt;
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
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

        // --- TRUE BLOCK ---
        emitLabel(labelTrue);
        code();
        emit("br label %" + labelEnd);

        // --- FALSE/ELSE BLOCK ---
        emitLabel(labelFalse);
        ifTail();
        emit("br label %" + labelEnd);

        // --- END BLOCK ---
        emitLabel(labelEnd);
    }

    /**
     * Treats a &lt;IfTail&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[23]&nbsp;&lt;IfTail&gt;&nbsp;&rarr;&nbsp;<code>End</code></li>
     * <li>[24]&nbsp;&lt;IfTail&gt;&nbsp;&rarr;&nbsp;<code>Else</code>&nbsp;&lt;Instruction&gt;&nbsp;<code>End</code></li>
     * </ul>
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private void ifTail() throws IOException, ParseException {
        if (current.getType() == LexicalUnit.ELSE) {
            match(LexicalUnit.ELSE);
            code();
            match(LexicalUnit.END);
        } else {
            match(LexicalUnit.END);
        }
    }

    /**
     * Treats a &lt;Cond&gt; at the top of the stack.
     * * Tries to apply rule
     * [25]&nbsp;&lt;Cond&gt;&nbsp;&rarr;&nbsp;&lt;SimpleCond&gt;&lt;Cond'&gt;
     * * @return the register containing the boolean result (i1).
     * 
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private String cond() throws IOException, ParseException {
        String leftReg = simpleCond();
        return condPrime(leftReg);
    }

    /**
     * Treats a &lt;Cond'&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[26]&nbsp;&lt;Cond'&gt;&nbsp;&rarr;&nbsp;<code>-></code>
     * &lt;Cond'&gt;</li>
     * <li>[27]&nbsp;&lt;Cond'&gt;&nbsp;&rarr;&nbsp;&epsilon;</li>
     * </ul>
     * * @param leftOp the register holding the left operand.
     * 
     * @return the register containing the boolean result.
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private String condPrime(String leftOp) throws IOException, ParseException {
        if (current.getType() == LexicalUnit.IMPLIES) {
            match(LexicalUnit.IMPLIES);

            // Recurse to <Cond> to handle right associativity
            String rightOp = cond();

            // Implementation of implication: !left | right
            String notLeft = newReg();
            emit(notLeft + " = xor i1 " + leftOp + ", 1");

            String res = newReg();
            emit(res + " = or i1 " + notLeft + ", " + rightOp);

            return res;
        }
        return leftOp;
    }

    /**
     * Treats a &lt;SimpleCond&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[28]&nbsp;&lt;SimpleCond&gt;&nbsp;&rarr;&nbsp;<code>|</code>&lt;Cond&gt;<code>|</code></li>
     * <li>[29]&nbsp;&lt;SimpleCond&gt;&nbsp;&rarr;&nbsp;&lt;ExprArith&gt;&lt;Comp&gt;&lt;ExprArith&gt;</li>
     * </ul>
     * * @return the register containing the boolean result (i1).
     * 
     * @throws IOException    in case the lexing fails (syntax error).
     * @throws ParseException in case the parsing fails (syntax error).
     */
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
                case EQUAL:
                    codeOp = "eq";
                    break;
                case SMALEQ:
                    codeOp = "sle";
                    break;
                case SMALLER:
                    codeOp = "slt";
                    break;
                default:
                    break;
            }

            emit(res + " = icmp " + codeOp + " i32 " + leftReg + ", " + rightReg);
            return res;
        }
    }

    /**
     * Treats a &lt;Comp&gt; at the top of the stack.
     * * Tries to apply one of the rules
     * <ul>
     * <li>[30]&nbsp;&lt;Comp&gt;&nbsp;&rarr;&nbsp;<code>==</code></li>
     * <li>[31]&nbsp;&lt;Comp&gt;&nbsp;&rarr;&nbsp;<code>&lt;=</code></li>
     * <li>[32]&nbsp;&lt;Comp&gt;&nbsp;&rarr;&nbsp;<code>&lt;</code></li>
     * </ul>
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private void compOp() throws IOException, ParseException {
        switch (current.getType()) {
            case EQUAL:
                match(LexicalUnit.EQUAL);
                break;
            case SMALEQ:
                match(LexicalUnit.SMALEQ);
                break;
            case SMALLER:
                match(LexicalUnit.SMALLER);
                break;
            default:
                throw new ParseException(current, NonTerminal.Comp,
                        Arrays.asList(LexicalUnit.EQUAL, LexicalUnit.SMALEQ, LexicalUnit.SMALLER));
        }
    }

    /**
     * Treats a &lt;While&gt; at the top of the stack.
     * * Tries to apply rule
     * [33]&nbsp;&lt;While&gt;&nbsp;&rarr;&nbsp;<code>While</code>&nbsp;<code>{</code>&lt;Cond&gt;<code>}</code>&nbsp;<code>Do</code>&nbsp;&lt;Code&gt;&nbsp;<code>End</code>
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private void whileExpr() throws IOException, ParseException {
        match(LexicalUnit.WHILE);
        match(LexicalUnit.LBRACK);

        String labelCond = newLabel();
        String labelBody = newLabel();
        String labelEnd = newLabel();

        emit("br label %" + labelCond);

        // --- CONDITION BLOCK ---
        emitLabel(labelCond);
        String condReg = cond();
        emit("br i1 " + condReg + ", label %" + labelBody + ", label %" + labelEnd);

        match(LexicalUnit.RBRACK);
        match(LexicalUnit.DO);

        // --- BODY BLOCK ---
        emitLabel(labelBody);
        code();
        emit("br label %" + labelCond);

        match(LexicalUnit.END);

        // --- END BLOCK ---
        emitLabel(labelEnd);
    }

    /**
     * Treats a &lt;Output&gt; at the top of the stack.
     * * Tries to apply rule
     * [34]&nbsp;&lt;Output&gt;&nbsp;&rarr;&nbsp;<code>Print(</code>[Varname]<code>)</code>
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
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
        emit(callReg
                + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str_out, i32 0, i32 0), i32 "
                + valReg + ")");
    }

    /**
     * Treats a &lt;Input&gt; at the top of the stack.
     * * Tries to apply rule
     * [34]&nbsp;&lt;Input&gt;&nbsp;&rarr;&nbsp;<code>Input(</code>[Varname]<code>)</code>
     * * @throws IOException in case the lexing fails (syntax error).
     * 
     * @throws ParseException in case the parsing fails (syntax error).
     */
    private void inputExpr() throws IOException, ParseException {
        match(LexicalUnit.INPUT);
        match(LexicalUnit.LPAREN);

        String varName = (String) current.getValue();
        variables.add(varName);
        match(LexicalUnit.VARNAME);

        match(LexicalUnit.RPAREN);

        String callReg = newReg();
        emit(callReg
                + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str_in, i32 0, i32 0), i32* %"
                + varName + ")");
    }
}