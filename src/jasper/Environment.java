package jasper;
import java.util.*;
public class Environment {
    private final Map<String , Object> map = new HashMap<>();
    final Environment enclosing;
    Environment (){
        enclosing = null;
    }
    Environment (Environment enclosing){
        this.enclosing = enclosing;
    }
    void define(String name, Object value){
        map.put(name,value);
    }
    void assign(Token name, Object value){
        if(map.containsKey(name.lexeme)){
            map.put(name.lexeme,value);
            return;
        }
        if(enclosing!=null){
            enclosing.assign(name,value);
            return;
        }
        throw new RuntimeError(name, "Undefined variable '"+name.lexeme+"'.");
    }
    Object get(Token name){
        if(map.containsKey(name.lexeme)){
            return map.get(name.lexeme);
        }
        if(enclosing != null) return enclosing.get(name);
        throw new RuntimeError(name, "Undefined variable '"+name.lexeme+"'.");
    }
}
