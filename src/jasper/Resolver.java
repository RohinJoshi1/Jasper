package jasper;

import java.util.*;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<HashMap<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private enum FunctionType{
        NONE,
        FUNCTION,
    }
    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }
    private void resolveFunction(Stmt.Function function, FunctionType type){
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for(Token param : function.params){
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.then);
        if(stmt.elseBranch!=null)resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if(currentFunction == FunctionType.NONE){
            Jasper.error(stmt.keyword, "Cannot return on top level code");
        }
        if(stmt.value != null){
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    void resolve(List<Stmt> statements) {
        for (Stmt s : statements) {
            resolve(s);
        }
    }

    private void resolve(Stmt s) {
        s.accept(this);
    }

    void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        this.scopes.push(new HashMap<String, Boolean>());
        return;
    }

    private void endScope() {
        this.scopes.pop();
    }


    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, Boolean> curr_scope = scopes.peek();
        if(curr_scope.containsKey(name.lexeme)){
            Jasper.error(name, "̌Variable with this name already defined");
        }
        curr_scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr,expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for(Expr arg : expr.arguments){
            resolve(arg);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE){
            Jasper.error(expr.name, "Can't read local variable in it's own initializer");
        }
        resolveLocal(expr, expr.name);
        return null;
    }
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

}
