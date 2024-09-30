package jasper;
import java.util.*;
public class Class implements JasperCallable{
    final String name;
    private final Map<String, Function> methods;
    Class(String name, Map<String, Function> methods){
        this.name = name;
        this.methods = methods;
    }
    @Override
    public String toString(){
        return this.name;
    }

    @Override
    public int arity() {
        Function init = getMethod("init");
        if(init == null)return 0;
        return init.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Instance instance = new Instance(this);
        Function init = getMethod("init");
        if(init != null){
            init.bind(instance).call(interpreter,arguments);
        }
        return instance;
    }
    Function getMethod(String name){
        if(methods.containsKey(name)){
            return methods.get(name);
        }
        return  null;
    }
}
