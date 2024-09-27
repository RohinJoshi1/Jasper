package jasper;
import java.util.*;
interface JasperCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
