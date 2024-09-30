package jasper;
import java.util.*;
public class Instance {
    private final Map<String, Object> fields= new HashMap<>();
    private JasperClass c;
    Instance(JasperClass c){
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
        Function method = c.getMethod(this,name.lexeme);
        if(method!=null)return method;
        throw  new RuntimeError(name, "Undefined property '"+ name +"'. ");
    }
    void set(Token name , Object value){
        fields.put(name.lexeme,value);
    }

}
