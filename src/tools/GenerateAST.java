package tools;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenerateAST {
    public static void main(String[] args) throws IOException {
//        if(args.length !=1){
//            System.err.println("Usage: generate_ast <output_directory>");
//            System.exit(64);
//        }
        String outDir = "/Users/rohinjoshi/Work/codes/Jalang/src/jasper/";
        defineAst(outDir, "Expr", Arrays.asList(
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Unary    : Token operator, Expr right",
                "Variable : Token name"
        ));
        defineAst(outDir, "Stmt", Arrays.asList(
                "Expression : Expr expression",
                "Print      : Expr expression",
                "Var        : Token name, Expr initializer"
        ));
    }
    private static void defineAst(
            String outDir, String baseName, List<String> types
    ) throws IOException{
        String path = outDir + "/" + baseName + ".java";
        PrintWriter pw = new PrintWriter(path, "UTF-8");
        pw.println("package jalang;");
        pw.println();
        pw.println("import java.util.*;");
        pw.println();
        pw.println("abstract class "+baseName + "{");
        defineVisitor(pw, baseName, types);
        for(String type : types){
            String[] temp = type.split(":");
            String className = temp[0].trim();
            String fields = temp[1].trim();
            defineType(pw,baseName,className,fields);

        }
        pw.println();
        pw.println("    abstract <R> R accept(Visitor<R> visitor);");
        pw.println("}");
        pw.close();

    }

    private static void defineVisitor(PrintWriter pw, String baseName, List<String> types) {
        pw.println(" interface Visitor<R> {");
        for(String type: types){
            String typeName = type.split(":")[0].trim();
            pw.println(" R visit" + typeName + baseName +"(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        pw.println("    }");
    }

    private static void defineType(
            PrintWriter writer, String baseName, String className, String fieldList
    ){
        writer.println(" static class "+ className + " extends "+ baseName + " {");
        writer.println("    " + className + "("+ fieldList + ") {");
        String[] fields = fieldList.split(", ");
        for(String field : fields){
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }
        writer.println("    }");
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" +
                className + baseName + "(this);");
        writer.println("    }");
        writer.println();
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }
        writer.println("  }");
    }
}
