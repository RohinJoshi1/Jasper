package jasper;
import java.io.IOException;
import java.io.*;
import java.util.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static jasper.TokenType.EOF;

public class Jasper {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    public static void main(String[] args) throws IOException {
        if(args.length > 1){
            System.out.println("Usage: Jasper [script]");
            System.exit(64);
        }else if(args.length == 1){
            run(args[0]);
        }
        else{
            runPrompt();
        }
    }
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if(hadError)System.exit(64);
        if (hadRuntimeError) System.exit(70);
    }
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        while(true){
            System.out.println("> ");
            String line = reader.readLine();
            if(line == null)break;
            run(line);
            hadError= false;
        }
    }

    private static void run(String source) {
        Scanner sc = new Scanner(source);
        List<Token> tokens = sc.scanTokens();
//        for(Token t : tokens){
//            System.out.println(t.type);
//        }
        Parser parser = new Parser(tokens);
        List<Stmt> expression = parser.parse();
        if(hadError)return;
        interpreter.interpret(expression);
//        System.out.println(new AstPrinter().print(expression));
    }
    static void error(int line, String message){
        report(line, " ",message);
    }
    static void error(Token token, String message){
        if(token.type == EOF){
            report(token.line, "at end",message);
        }else{
            report(token.line, "at '"+ token.lexeme +"'",message);
        }
    }

    private static void report(int line, String where, String message){
        System.out.println("[line "+ line + "] Error "+ where + ": "+message);
        hadError = true;
    }
    static void runtimeError(RuntimeError e){
        System.err.println(e.getMessage() + "\n[line" + e.token.line + "]");
        hadRuntimeError= true;
    }
}
