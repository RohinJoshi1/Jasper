package jasper;
import java.io.*;
import java.util.*;

public class Interpreter implements  Expr.Visitor<Object> , Stmt.Visitor<Void> {
    final Environment globals = new Environment();
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

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Jasper.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if(stmt.superclass!=null){
            superclass = evaluate(stmt.superclass);
            if(!(superclass instanceof JasperClass)){
                throw  new RuntimeError(stmt.superclass.name,"Superclass must be a class");
            }

        }
        environment.define(stmt.name.lexeme, null);
        if(stmt.superclass!=null){
            environment = new Environment(environment);
            environment.define("super",superclass);
        }
        Map<String, Function> methods = new HashMap<>();

        for (Stmt.Function method : stmt.methods) {
            Function function = new Function(method, environment, method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        JasperClass c = new JasperClass(stmt.name.lexeme,(JasperClass)superclass,  methods);
        if(superclass!=null){
            environment = environment.enclosing;
        }
        environment.assign(stmt.name, c);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        Function function = new Function(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.then);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
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
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
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
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof JasperCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        JasperCallable function = (JasperCallable)callee;
        if (function.arity()!=-1 && arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof Instance) {
            return ((Instance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof Instance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((Instance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        JasperClass supercls = (JasperClass)environment.getAt(distance, "super");
        Instance obj = (Instance) environment.getAt(distance-1, "this");
        Function method = supercls.getMethod(obj, expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method,
                    "Undefined property '" + expr.method.lexeme + "'.");
        }
        return method.bind(obj);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        // nil is only equal to nil.
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}
