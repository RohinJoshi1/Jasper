package jasper;
import java.util.*;
public class Instance {
    private final HashMap<String, Object> fields= new HashMap<>();
    private Class c;
    Instance(Class c){
        this.c = c;
    }
    @Override
    public String toString(){
        return c.name + "Instance";
    }
    Object get(Token name){
        if(fields.containsKey(name.lexeme)){
            return fields.get(name.lexeme);
        }
        Function method = c.getMethod(name.lexeme);
        if(method!=null)return method.bind(this);
        throw  new RuntimeError(name, "Undefined property '"+ name +"'. ");
    }
    void set(Token name , Object value){
        fields.put(name.lexeme,value);
    }

}
