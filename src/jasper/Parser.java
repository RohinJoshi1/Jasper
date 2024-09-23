package jasper;
import java.util.*;

import static jasper.TokenType.*;

/*
* GRAMMAR RULES
* expression -> assignment;
* assignment    →  IDENTIFIER "=" assignment |equality ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" | IDENTIFIER;
program -> (declaration)* EOF;
* declaration -> varDecl | statement;
* statement -> (printStmt | expressionStmt | block);
* block -> "{" declaration* "}";
* exprStmt -> expression ";" ;
* printStmt -> "print" expression ";" ;
* varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
*/
public class Parser {
    static class ParseError extends RuntimeException{}
    private final List<Token> tokens;
    private int current = 0;
    Parser(List<Token> tokens){
        this.tokens = tokens;
    }
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }
    private Stmt declaration(){
        try{
            if(match(VAR))return varDeclaration();
            return statement();
        }catch (ParseError e){
            synchronize();
            return null;
        }
    }
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }


    private Expr expression(){
        return assignment();
     }
     private Expr assignment(){
        Expr expr = equality();
        if(match(EQUAL)){
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            ParseError error = error(equals, "Invalid assignment target.");
        }
        return  expr;
     }

     private Stmt statement(){
        if(match(PRINT)) return printStatement();
        if(match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
     }
     private List<Stmt> block(){
        List<Stmt> stmts = new ArrayList<>();
        while(!check(RIGHT_BRACE) && !isAtEnd()){
            stmts.add(declaration());
        }
        consume(RIGHT_BRACE,"Expect '}' after block");
        return stmts;
     }
     private Stmt printStatement(){
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
     }
     private Stmt expressionStatement(){
        Expr expr = expression();
        System.out.println(expr);
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(expr);
     }

    //comparison( ("!=" | "==")) comp)*
    private Expr equality(){
        Expr expr = comparison();
        while(match(BANG_EQUAL, EQUAL_EQUAL)){
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return  expr;
    }
    //comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private Expr comparison(){
        Expr expr = term();
        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    //factor ( ( "-" | "+" ) factor )* ;
    private Expr term(){
        Expr expr = factor();
        while(match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return  expr;
    }
    //factor → unary ( ( "/" | "*" ) unary )* ;
    private Expr factor(){
        Expr expr = unary();
        while(match(SLASH, STAR)){
            Token operation = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operation, right);
        }
        return  expr;
    }
    //unary          → ( "!" | "-" ) unary   | primary ;
    private Expr unary(){
        if(match(BANG, MINUS)){
            Token operation = previous();
            return new Expr.Unary(operation, unary());
        }
        return primary();
    }
    //primary        → NUMBER | STRING | "true" | "false" | "nil"
    //               | "(" expression ")" ;
    private Expr primary(){
        if(match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }
        if(match(FALSE)) return  new Expr.Literal(false);
        if(match(TRUE)) return  new Expr.Literal(true);
        if(match(NIL)) return  new Expr.Literal(null);
        if(match(IDENTIFIER))return new Expr.Variable(previous());
        if(match(LEFT_PAREN)){
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect Expression: ");
    }

    private Token consume(TokenType tokenType, String message) {
        if (check(tokenType)) {
            return advance();
        }
        else throw error(peek(), message);
//        return null;
    }
    private ParseError error(Token token, String message){
        Jalang.error(token,message);
        return new ParseError();
    }
    private  void synchronize(){
        advance();
        while(!isAtEnd()){
            if(previous().type == SEMICOLON)return;
            switch (peek().type){
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
    private boolean match(TokenType ...types){
        for(TokenType type : types){
            if(check(type)){
                advance();
                return  true;
            }
        }
        return false;
    }
    private boolean check(TokenType type){
        if(isAtEnd())return false;
        return peek().type == type;
    }
    private Token advance(){
        if(!isAtEnd())current++;
        return previous();
    }
    private boolean isAtEnd(){
         return peek().type==EOF;
    }
    private Token previous(){
        return  tokens.get(current-1);
    }
    private Token peek(){
        return tokens.get(current);
    }
}
