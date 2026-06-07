package dev.backendsouls.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import dev.backendsouls.lox.Expr.Get;
import dev.backendsouls.lox.Expr.Set;
import dev.backendsouls.lox.Expr.Super;
import dev.backendsouls.lox.Expr.This;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;

    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    private FunctionType currentFunction = FunctionType.NONE;

    private ClassType currentClass = ClassType.CLASS;

    public Resolver(final Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {

        this.resolve(expr.value());
        this.resolveLocal(expr, expr.name());

        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expression) {

        this.resolve(expression.left());
        this.resolve(expression.right());

        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expression) {

        this.resolve(expression.callee());

        for (Expr argument : expression.arguments()) {
            this.resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGetExpr(Get expression) {

        this.resolve(expression.object());

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expression) {

        this.resolve(expression.expression());

        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expression) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expression) {

        this.resolve(expression.left());
        this.resolve(expression.right());

        return null;
    }

    @Override
    public Void visitSetExpr(Set expression) {

        this.resolve(expression.value());
        this.resolve(expression.object());

        return null;
    }

    @Override
    public Void visitSuperExpr(Super expression) {

        if (this.currentClass == ClassType.NONE) {
            Lox.error(expression.keyword(), "Can't use 'super' outside of a class.");
        }

        if (this.currentClass != ClassType.SUBCLASS) {
            Lox.error(expression.keyword(), "Can't use 'super' outside of a class.");
        }

        this.resolveLocal(expression, expression.keyword());

        return null;
    }

    @Override
    public Void visitThisExpr(This expression) {

        if (this.currentClass == ClassType.NONE) {
            Lox.error(expression.keyword(), "Can't use 'this' outside of a class.");

            return null;
        }

        this.resolveLocal(expression, expression.keyword());

        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expression) {

        this.resolve(expression.right());

        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!this.scopes.isEmpty() && this.scopes.peek().get(expr.name().lexeme()) == Boolean.FALSE) {
            Lox.error(expr.name(), "Can't read local variable in its own initializer.");
        }

        this.resolveLocal(expr, expr.name());

        return null;
    }

    void resolveLocal(Expr expression, Token name) {
        for (int i = this.scopes.size() - 1; i >= 0; i--) {
            if (this.scopes.get(i).containsKey(name.lexeme())) {
                this.interpreter.resolve(expression, this.scopes.size() - 1 - i);
                return;
            }
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block statement) {
        this.beginScope();
        this.resolve(statement.statements());
        this.endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class statement) {

        var enclosingClass = this.currentClass;
        this.currentClass = ClassType.CLASS;

        this.declare(statement.name());
        this.define(statement.name());

        if (statement.superclass() != null) {
            if (statement.name().lexeme().equals(statement.superclass().name().lexeme())) {
                Lox.error(statement.superclass().name(), "A class can't inherit from itself.");
            }

            this.currentClass = ClassType.SUBCLASS;
            this.resolve(statement.superclass());

            this.beginScope();
            this.scopes.peek().put("super", true);
        }

        this.beginScope();
        this.scopes.peek().put("this", true);

        for (Stmt.Function method : statement.methods()) {
            var declaration = FunctionType.METHOD;

            if (method.name().lexeme().equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }

            this.resolveFunction(method, declaration);
        }

        this.endScope();

        if (statement.superclass() != null) {
            this.endScope();
        }

        this.currentClass = enclosingClass;

        return null;
    }

    private void beginScope() {
        this.scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        this.scopes.pop();
    }

    void resolve(List<Stmt> statements) {
        for (var statement : statements) {
            this.resolve(statement);
        }
    }

    void resolve(Stmt statement) {
        statement.accept(this);
    }

    void resolve(Expr expression) {
        expression.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression statement) {

        this.resolve(statement.expression());

        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {

        this.declare(stmt.name());
        this.define(stmt.name());

        this.resolveFunction(stmt, FunctionType.FUNCTION);

        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType functionType) {

        var enclosingFunction = currentFunction;
        this.currentFunction = functionType;

        this.beginScope();

        for (var param : function.params()) {
            this.declare(param);
            this.define(param);
        }

        this.resolve(function.body());

        this.endScope();

        this.currentFunction = enclosingFunction;
    }

    @Override
    public Void visitIfStmt(Stmt.If statement) {

        this.resolve(statement.condition());
        this.resolve(statement.thenBranch());

        if (statement.elseBranch() != null) {
            this.resolve(statement.elseBranch());
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print statement) {

        this.resolve(statement.expression());

        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return statement) {

        if (this.currentFunction == FunctionType.NONE) {
            Lox.error(statement.keyword(), "Can't return from top-level code.");
        }

        if (statement.value() != null) {

            if (this.currentFunction == FunctionType.INITIALIZER) {
                Lox.error(statement.keyword(), "Can't return a value from an constructor.");
            }

            this.resolve(statement.value());
        }

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        this.declare(stmt.name());

        if (stmt.initializer() != null) {
            this.resolve(stmt.initializer());
        }

        this.define(stmt.name());

        return null;
    }

    private void declare(Token name) {
        if (this.scopes.isEmpty()) {
            return;
        }

        var scope = this.scopes.peek();
        if (scope.containsKey(name.lexeme())) {
            Lox.error(name, "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme(), false);
    }

    private void define(Token name) {
        if (this.scopes.isEmpty()) {
            return;
        }

        this.scopes.peek().put(name.lexeme(), true);
    }

    @Override
    public Void visitWhileStmt(Stmt.While statement) {

        this.resolve(statement.condition());
        this.resolve(statement.body());

        return null;
    }
}
