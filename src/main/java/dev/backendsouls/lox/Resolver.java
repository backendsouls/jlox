package dev.backendsouls.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;

    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    public Resolver(final Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        this.resolve(expr.value());
        this.resolveLocal(expr, expr.name());
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return null;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        if (!this.scopes.isEmpty() && this.scopes.peek().get(expr.name().lexeme()) == Boolean.FALSE) {
            Lox.error(expr.name(), "Can't read local variable in its own initializer.");
        }

        this.resolveLocal(expr, expr.name());
        return null;
    }

    void resolveLocal(Expr expr, Token name) {
        for (int i = this.scopes.size() - 1; i >= 0; i--) {
            if (this.scopes.get(i).containsKey(name.lexeme())) {
                this.interpreter.resolve(expr, this.scopes.size() - 1 - i);
                return;
            }
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        this.beginScope();
        this.resolve(stmt.statements());
        this.endScope();
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
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        this.declare(stmt.name());
        this.define(stmt.name());

        this.resolveFunction(stmt);
        return null;
    }

    private void resolveFunction(Stmt.Function function) {
        this.beginScope();

        for (var param : function.params()) {
            this.declare(param);
            this.define(param);
        }

        this.resolve(function.body());

        this.endScope();
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
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
        scope.put(name.lexeme(), false);
    }

    private void define(Token name) {
        if (this.scopes.isEmpty()) {
            return;
        }

        this.scopes.peek().put(name.lexeme(), true);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        return null;
    }
}
