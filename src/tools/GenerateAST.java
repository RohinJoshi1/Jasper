package tools;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenerateAST {
    public static void main(String[] args) throws IOException {
        if(args.length !=1){
            System.err.println("Usage: generate_ast <output_directory>");
            System.exit(64);
        }
        String outDir = args[0];
        defineAst(outDir, "Expr", Arrays.asList(
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Unary    : Token operator, Expr right"
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
        for(String type : types){
            String[] temp = type.split(":");
            String className = temp[0].trim();
            String fields = temp[1].trim();
            defineType(pw,baseName,className,fields);
        }
        pw.println("}");
        pw.close();

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
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }
        writer.println("  }");
    }
}
