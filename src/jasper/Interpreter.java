package jasper;

import javax.management.RuntimeErrorException;
import java.io.*;
import java.sql.Statement;
import java.util.*;

public class Interpreter implements  Expr.Visitor<Object> , Stmt.Visitor<Void> {
    static final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter(){
        globals.define("clock", new JasperCallable() {

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis()/1000;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.define("printf", new JasperCallable() {
            @Override
            public int arity() {
                return -1; // Variable number of arguments
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (arguments.isEmpty()) {
                    System.out.println();
                    return  null;
                }
                String format = (String) arguments.getFirst();
                Object[] args = arguments.subList(1, arguments.size()).toArray();
                System.out.printf(format, args);
                return null;
            }
        });
        // Updated input function
        globals.define("input", new JasperCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    return reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeError(null, "Failed to read input: " + e.getMessage());
                }
            }
        });

        // Updated file_read function
        globals.define("file_read", new JasperCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (arguments.size() != 1 || !(arguments.get(0) instanceof String)) {
                    throw new RuntimeError(null, "file_read expects a single string argument (file path).");
                }
                String filePath = (String) arguments.get(0);
                try {
                    StringBuilder content = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new FileReader(filePath));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    reader.close();
                    return content.toString();
                } catch (IOException e) {
                    throw new RuntimeError(null, "Failed to read file: " + e.getMessage());
                }
            }
        });

        // Updated file_write function
        globals.define("file_write", new JasperCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (arguments.size() != 2 || !(arguments.get(0) instanceof String) || !(arguments.get(1) instanceof String)) {
                    throw new RuntimeError(null, "file_write expects two string arguments (file path and content).");
                }
                String filePath = (String) arguments.get(0);
                String content = (String) arguments.get(1);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
                    writer.write(content);
                    writer.close();
                    return null;
                } catch (IOException e) {
                    throw new RuntimeError(null, "Failed to write to file: " + e.getMessage());
                }
            }
        });
    }

    void resolve(Expr expr, int depth){
        locals.put(expr,depth);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                NumberCheck(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                NumberCheck(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                NumberCheck(expr.operator, left, right);
                return (double) left * (double) right;
            //+ : Integers , Strings ++ -> i +1
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case GREATER:
                NumberCheck(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                NumberCheck(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                NumberCheck(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                NumberCheck(expr.operator, left, right);
                return (double) left <= (double) right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> args = new ArrayList<>();
        for(Expr a : expr.arguments){
            args.add(evaluate(a));
        }

        if(!(callee instanceof JasperCallable)){
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }
        JasperCallable function = (JasperCallable)callee;
        int arity = function.arity();
        if(arity!=-1 && args.size() != arity){
            throw new RuntimeError(expr.paren, "Expected "+ arity +"  arguments, got "+args.size());
        }
        return  function.call(this, args);

        //I need to check if len(args) == expected_len


    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if((object instanceof Instance)){
           return ((Instance)object).get(expr.name);
        }
        throw new RuntimeError(expr.name, " Only instances have properties");
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
        if(!(object instanceof Instance)){
            throw new RuntimeError(expr.name, "Only instances have fields");
        }
        Object value = evaluate(expr.value);
        ((Instance)object).set(expr.name, value);
        return value;

    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    void NumberCheck(Token t, Object... operands) {
        for (Object o : operands) {
            if (!(o instanceof Double)) {
                throw new RuntimeError(t, "Operand must be a number");
            }
        }
        return;

    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if(expr.operator.type == TokenType.OR){
            if(isTruth(left))return left; // short circuit OR
        }else{
            if(!isTruth(left))return left; //Short circuit AND
        }
        return evaluate(expr.right);
    }


    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                NumberCheck(expr.operator, right);
                return -(double) right;
            case BANG:

                return !isTruth(right);
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {

        return lookUpVariable(expr.name, expr);
    }
    private Object lookUpVariable(Token name, Expr expr){
        Integer dist = locals.get(expr);
        if(dist!= null){
            return environment.getAt(dist, name.lexeme);
        }
        return globals.get(name);
    }


    private boolean isTruth(Object obj) {
        return switch (obj) {
            case null -> false;
            case Boolean b -> (boolean) obj;
            case Double i -> (double) obj > 0;
            default -> true;
        };
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    public void interpret(List<Stmt> tokens) {
        try {
            for (Stmt token : tokens) {
                execute(token);
            }
        } catch (RuntimeError e) {
            Jasper.runtimeError(e);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }


    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    void executeBlock(List<Stmt> stmts, Environment env){
        Environment prev = this.environment;
        try{
            this.environment = environment;
            for(Stmt s : stmts){
                execute(s);
            }}
        catch(Return returnvalue){
                throw returnvalue;
        }finally {
            this.environment = prev;
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);
        Map<String, Function> methods = new HashMap<>();
        for(Stmt.Function method : stmt.methods){
            Function fun = new Function(method, environment,method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, fun);
        }
        Class c = new Class(stmt.name.lexeme,methods);
        environment.assign(stmt.name, c);
        return  null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        Function func = new Function(stmt,environment,false);
        environment.define(stmt.name.lexeme, func);
        return null;

    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruth(evaluate(stmt.condition))){
            execute(stmt.then);
        }else if(stmt.elseBranch!=null){
            execute(stmt.elseBranch);
        }
        return  null;
    }


    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object val = null;
        if(stmt.value != null) val = evaluate(stmt.value);
        throw new Return(val);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTruth(evaluate(stmt.condition))){
            execute(stmt.body);
        }
        return  null;
    }

}
