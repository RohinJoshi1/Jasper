package jasper;

import java.util.List;

public class Function implements JasperCallable{

    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInit;

    public Function(Stmt.Function declaration, Environment closure, boolean isInit) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInit = isInit;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }
    Function bind(Instance instance){
        Environment env = new Environment(closure);
        env.define("this",instance);
        return new Function(declaration,env,isInit);
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
            if (isInit) return closure.getAt(0, "this");
            return  returnvalue.value;
        }
        if(isInit) return closure.getAt(0, "this");
        return null; //TODO add return value
    }
    public String toString(){
        return "<fn " + declaration.name.lexeme + ">";
    }
}
