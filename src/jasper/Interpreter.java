package jasper;

import java.sql.Statement;
import java.util.*;

public class Interpreter implements  Expr.Visitor<Object> , Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
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
        return environment.get(expr.name);
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
            Jalang.runtimeError(e);
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
            }
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
