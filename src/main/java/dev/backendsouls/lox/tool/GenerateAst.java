package dev.backendsouls.lox.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        var outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign   : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Get      : Expr object, Token name",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Logical  : Expr left, Token operator, Expr right",
                "Set      : Expr object, Token name, Expr value",
                "Super    : Token keyword, Token method",
                "This     : Token keyword",
                "Unary    : Token operator, Expr right",
                "Variable : Token name"));

        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Class      : Token name, Expr.Variable superclass, List<Stmt.Function> methods",
                "Expression : Expr expression",
                "Function   : Token name, List<Token> params, List<Stmt> body",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Print      : Expr expression",
                "Return     : Token keyword, Expr value",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body"));
    }

    private static void defineAst(final String outputDir, final String baseName, List<String> types)
            throws IOException {
        var path = outputDir + "/" + baseName + ".java";
        var writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package dev.backendsouls.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("public interface " + baseName + " {");

        defineVisitor(writer, baseName, types);

        // The AST classes.
        for (var type : types) {
            var recordName = type.split(":")[0].trim();
            var fields = type.split(":")[1].trim();
            defineType(writer, baseName, recordName, fields);
        }

        writer.println("    <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineType(
            final PrintWriter writer, final String baseName,
            final String recordName, final String fieldList) {
        var record = "    record " + recordName + "(" + fieldList + ")" + " implements " + baseName + " {";

        writer.println(record);

        writer.println("        @Override");
        writer.println("        public <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + recordName + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
        writer.println();
    }

    private static void defineVisitor(
            final PrintWriter writer, final String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (var type : types) {
            var typeName = type.split(":")[0].trim();
            writer.println(
                    "        R visit" + typeName + baseName + "("
                            + typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }");
        writer.println();
    }
}
