package dev.backendsouls.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();

    private static boolean hadError = false;

    private static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.println("> ");
            String line = reader.readLine();

            if (line == null) {
                break;
            }

            run(line);
            Lox.hadError = false;
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (Lox.hadError) {
            System.exit(65);
        }

        if (Lox.hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void run(String source) {

        var scanner = new Scanner(source);
        var tokens = scanner.scanTokens();

        var parser = new Parser(tokens);
        var statements = parser.parse();

        if (Lox.hadError) {
            return;
        }

        var resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        
        if (Lox.hadError) {
            return;
        }

        interpreter.interpret(statements);
    }

    static void error(final int line, final String message) {
        report(line, "", message);
    }

    static void error(final Token token, final String message) {
        if (token.tokenType() == TokenType.EOF) {
            report(token.line(), " at end", message);
            return;
        }

        report(token.line(), " at '" + token.lexeme() + "'", message);
    }

    public static void runtimeError(RuntimeError error) {
        Lox.hadRuntimeError = true;
        System.err.println(error.getMessage() + "\n[line " + error.token.line() + "]");
    }

    private static void report(final int line, final String where, final String message) {
        Lox.hadError = true;
        System.err.println("[line " + line + "] Error " + where + ": " + message);
    }
}
