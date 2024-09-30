package jasper;
import java.util.*;

import static jasper.TokenType.*;

/*
* GRAMMAR RULES
* expression -> assignment;
* assignment    →  (call ".")?IDENTIFIER "=" assignment | OR  ;
* OR -> AND ( "or" AND)*;
* AND -> EQUALITY ("AND" EQUALITY)*;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary | call;
call           -> primary ( "(" args ? ")"  | "." IDENTIFIER )* ;
args -> expression ( "," expression)*;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" | IDENTIFIER;
program -> (declaration)* EOF;
* declaration -> varDecl | statement | funcDecl | classDecl;
* classDecl -> "class" + IDENTIFIER + "{" + function* + "}"
* funcDecl        → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
* statement -> (printStmt | ifStmt | expressionStmt | block | whileStmt | forStmt | returnStmt);
* returnStmt -> "return" expression? ";" ;
* forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")" statement ;
* whileStmt      → "while" "(" expression ")" statement ;
* ifStmt -> "if" "(" expression ")" statement
*   ("else" statement)?;
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
            if(match(CLASS)) return classDecl();
            if(match(FUNC))return function("function");
            if(match(VAR))return varDeclaration();
            return statement();
        }catch (ParseError e){
            synchronize();
            return null;
        }
    }
//    classDecl -> "class" + IDENTIFIER + "{" + function* + "}"
    private Stmt.Class classDecl(){
        Token name = consume(IDENTIFIER, "Class name expected");
        consume(LEFT_BRACE,"Opening '{' expected");
        List<Stmt.Function> methods = new ArrayList<>();
        while(!isAtEnd() && !check(RIGHT_PAREN)){
            methods.add(this.function("method"));
        }
        consume(RIGHT_PAREN , "Closing } expected after class declaration");
        return new Stmt.Class(name, methods);
    }
    private Stmt.Function function(String kind){
        Token name = consume(IDENTIFIER, "Expect " + kind + "name");
        //Get params
        consume(LEFT_PAREN, "Expect '(' after "+ kind + "name");
        List<Token> params = new ArrayList<>();
        if(!check(RIGHT_PAREN)){
            do{
                params.add(consume(IDENTIFIER, "Expect param name"));
            }while(match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters");
        consume(LEFT_BRACE, "Function body needs to be enclosed within { <body >}");
        List<Stmt> body = block();
        return new Stmt.Function(name, params, body);
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
        Expr expr = or();
//        Expr expr = equality();
        if(match(EQUAL)){
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }else if(expr instanceof Expr.Get){
                Expr.Get get  = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }
            ParseError error = error(equals, "Invalid assignment target.");
        }
        return  expr;
     }
     // AND ("OR AND")*
     private Expr or(){
        Expr expr = and();
        while(match(OR)){
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator , right);
        }
        return expr;
     }
     private Expr and(){
        Expr expr = equality();
        while(match(AND)){
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
     }
     private Stmt statement(){
        if(match(PRINT)) return printStatement();
        if(match(RETURN))return returnStmt();
        if(match(IF))return ifStatement();
        if(match(WHILE))return whileStatement();
        if(match(FOR))return forStatement();
        if(match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
     }
     private Stmt returnStmt(){
        Token keyword = previous();
        Expr val = null;
        if(!check(SEMICOLON)){
            val = expression();
        }
        consume(SEMICOLON, "Expected ; after return statement");
        return new Stmt.Return(keyword, val);
     }
//     for(int x=0;condition; x++);
     private Stmt forStatement(){
        consume(LEFT_PAREN, "Expect ( after for");
        Stmt initializer;
        if(match(SEMICOLON)){
            initializer = null;
        }else if(match(VAR)){
            initializer = varDeclaration();
        }else{
            initializer = expressionStatement();
        }
        Expr condition=null;
        if(!check(SEMICOLON)){
            condition = expression();
        }
        consume(SEMICOLON, " Expect ; after for condition");
        Expr increment = null;
        if(!check(RIGHT_PAREN)){
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ) after for clauses");
        Stmt body = statement();
        if(increment!=null){
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        if(condition == null)condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);
        if(initializer!= null){
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
     }
     private Stmt whileStatement(){
        consume(LEFT_PAREN, "Expected ( after while keyword");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ) after condition");
        Stmt body = statement();
        return  new Stmt.While(condition, body);
     }
     private Stmt ifStatement(){
        consume(LEFT_PAREN,"Expected ( after if");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ) after condition");
        Stmt then = statement();
        Stmt else_ = null;
        if(match(ELSE)){
            else_ = statement();
        }
        return new Stmt.If(condition, then, else_);
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
        return call();
    }
    //call           -> primary ( "(" args ? ")" ) *
    //args -> expression ( "," expression)*;
    private Expr call(){
        Expr expr = primary();
        while(true){
            if(match(LEFT_PAREN)){
                expr = processCall(expr);
            }else if(match(DOT)){
                Token name = consume(IDENTIFIER, "Expected property name after '.'");
                expr = new Expr.Get(expr, name);
            }
            else break;
        }
        return  expr;
    }

    private  Expr processCall(Expr callee){
        List<Expr> arguments = new ArrayList<>();
        if(!check(RIGHT_PAREN)){
            do{
                //Maybe add an argument limit like java? 255? to simplify bytecode, Skipping this cuz meh
                arguments.add(expression());
            }while(match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expected ')' after function arguments");
        return new Expr.Call(callee, paren, arguments);
    }

    //primary        → NUMBER | STRING | "true" | "false" | "nil"
    //               | "(" expression ")" ;
    private Expr primary(){
        if(match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }
        if(match(THIS)){
            return new Expr.This(previous());
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
        Jasper.error(token,message);
        return new ParseError();
    }
    private  void synchronize(){
        advance();
        while(!isAtEnd()){
            if(previous().type == SEMICOLON)return;
            switch (peek().type){
                case CLASS:
                case FUNC:
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
