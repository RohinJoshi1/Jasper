package jasper;

import java.util.*;

public class Interpreter implements  Expr.Visitor<Object> , Stmt.Visitor<Void> {
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
        return null;
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
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        return null;
    }

}
