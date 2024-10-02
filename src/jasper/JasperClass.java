package jasper;
import java.util.*;
public class JasperClass implements JasperCallable{
    final String name;
    final JasperClass superclass;
    private final Map<String, Function> methods;
    JasperClass(String name, JasperClass superclass, Map<String, Function> methods){
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }
    @Override
    public String toString(){
        return this.name;
    }

    @Override
    public int arity() {
        Function init = methods.get("init");
        if(init == null)return 0;
        return init.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Instance instance = new Instance(this);
        Function init = methods.get("init");
        if(init != null){
            init.bind(instance).call(interpreter,arguments);
        }
        return instance;
    }
    Function getMethod(Instance instance, String name){
        if(methods.containsKey(name)){
            return methods.get(name).bind(instance);
        }
        if(superclass != null){
            return superclass.getMethod(instance, name);
        }
        return  null;
    }
}
