package jasper;

import java.util.List;

public class Function implements JasperCallable{

    private final Stmt.Function declaration;
    private final Environment closure;

    public Function(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for(int i=0; i<arguments.size();i++){
            environment.define(declaration.params.get(i).lexeme,arguments.get(i));
        }
        try{
            interpreter.executeBlock(declaration.body, environment);
        }catch (Return returnvalue){
            return  returnvalue.value;
        }
        return null; //TODO add return value
    }
    public String toString(){
        return "<fn " + declaration.name.lexeme + ">";
    }
}
