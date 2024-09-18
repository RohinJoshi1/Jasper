package jalang;
import java.io.IOException;
import java.io.*;
import java.util.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static jalang.TokenType.EOF;

public class Jalang {
    static boolean hadError = false;
    public static void main(String[] args) throws IOException {
        if(args.length > 1){
            System.out.println("Usage: Jalang [script]");
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
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();
        if(hadError)return;
        System.out.println(new AstPrinter().print(expression));
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
}
