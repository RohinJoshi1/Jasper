package jasper;
import java.util.*;
public class Environment {
    private final Map<String , Object> map = new HashMap<>();
    void define(String name, Object value){
        map.put(name,value);
    }
    Object get(Token name){
        if(map.containsKey(name.lexeme)){
            return map.get(name.lexeme);
        }
        throw new RuntimeError(name, "Undefined variable '"+name.lexeme+"'.");
    }
}
