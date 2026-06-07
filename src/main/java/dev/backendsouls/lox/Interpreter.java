package dev.backendsouls.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import dev.backendsouls.lox.Expr.Get;
import dev.backendsouls.lox.Expr.Set;
import dev.backendsouls.lox.Expr.Super;
import dev.backendsouls.lox.Expr.This;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Environment globals = new Environment();

    // Records use value-based equals/hashCode, so distinct AST nodes with identical
    // fields (e.g. two `Variable(Token("i", line 6))` on the same line) would
    // collide as map keys. IdentityHashMap keys by reference, keeping each node's
    // resolution separate
    private final Map<Expr, Integer> locals = new IdentityHashMap<>();

    private Environment environment = this.globals;

    public Interpreter() {
        this.globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    public Environment globals() {
        return this.globals;
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (var statement : statements) {
                this.execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    public void resolve(Expr expr, int depth) {
        this.locals.put(expr, depth);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {

        var value = this.evaluate(expr.value());
        // this.environment.assign(expr.name(), value);

        var distance = this.locals.get(expr);
        if (distance != null) {
            this.environment.assignAt(distance, expr.name(), value);

            return value;
        }

        this.globals.assign(expr.name(), value);

        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        var right = this.evaluate(expr.right());
        var left = this.evaluate(expr.left());

        return switch (expr.operator().tokenType()) {
            case TokenType.BANG_EQUAL -> !this.isEqual(left, right);
            case TokenType.EQUAL_EQUAL -> this.isEqual(left, right);
            case TokenType.GREATER -> {
                this.checkNumberOperands(expr.operator(), left, right);
                yield (double) left > (double) right;
            }
            case TokenType.GREATER_EQUAL -> {
                this.checkNumberOperands(expr.operator(), left, right);
                yield (double) left >= (double) right;
            }
            case TokenType.LESS -> {
                this.checkNumberOperands(expr.operator(), left, right);
                yield (double) left < (double) right;
            }
            case TokenType.LESS_EQUAL -> {
                this.checkNumberOperands(expr.operator(), left, right);
                yield (double) left <= (double) right;
            }
            case TokenType.MINUS -> {
                this.checkNumberOperands(expr.operator(), left, right);
                yield (double) left - (double) right;
            }
            case TokenType.SLASH -> {
                this.checkNumberOperands(expr.operator(), left, right);
                yield (double) left / (double) right;
            }
            case TokenType.STAR -> {
                this.checkNumberOperands(expr.operator(), left, right);
                yield (double) left * (double) right;
            }
            case TokenType.PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    yield (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    yield left + (String) right;
                }

                throw new RuntimeError(expr.operator(), "Operands must be two numbers or two strings.");
            }
            default -> null;
        };
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        var callee = this.evaluate(expr.callee());

        var arguments = new ArrayList<Object>();
        for (var argument : expr.arguments()) {
            arguments.add(this.evaluate(argument));
        }

        if (!(callee instanceof LoxCallable function)) {
            throw new RuntimeError(expr.paren(), "Can only call functions and classes.");
        }

        if (arguments.size() != function.arity()) {
            var message = "Expected " + function.arity() + " arguments but got " + arguments.size() + ".";
            throw new RuntimeError(expr.paren(), message);
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Get expression) {

        var object = this.evaluate(expression.object());
        if (object instanceof LoxInstance loxInstance) {
            return loxInstance.get(expression.name());
        }

        throw new RuntimeError(expression.name(), "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return this.evaluate(expr.expression());
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value();
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        var left = this.evaluate(expr.left());

        if (expr.operator().tokenType() == TokenType.OR) {
            if (this.isTruthy(left)) {
                return left;
            }
        } else {
            if (!this.isTruthy(left)) {
                return left;
            }
        }

        return this.evaluate(expr.right());
    }

    @Override
    public Object visitSetExpr(Set expression) {

        var object = this.evaluate(expression.object());

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expression.name(), "Only instances have fields.");
        }

        var value = this.evaluate(expression.value());
        ((LoxInstance) object).set(expression.name(), value);

        return value;
    }

    @Override
    public Object visitSuperExpr(Super expression) {

        var distance = this.locals.get(expression);
        var superclass = (LoxClass) this.environment.getAt(distance, "super");
        var object = (LoxInstance) this.environment.getAt(distance - 1, "this");
        var method = (LoxFunction) superclass.findMethod(expression.method().lexeme());

        if (method == null) {
            throw new RuntimeError(expression.method(), "Undefined property '" + expression.method().lexeme() + "'.");
        }

        return method.bind(object);
    }

    @Override
    public Object visitThisExpr(This expression) {
        return this.lookUpVariable(expression.keyword(), expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        var right = this.evaluate(expr.right());

        return switch (expr.operator().tokenType()) {
            case TokenType.BANG -> !this.isTruthy(right);
            case TokenType.MINUS -> {
                this.checkNumberOperand(expr.operator(), right);
                yield -(double) right;
            }
            default -> null;
        };

    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        // return this.environment.get(expr.name());
        return this.lookUpVariable(expr.name(), expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {

        final Integer distance = this.locals.get(expr);

        if (distance != null) {
            return this.environment.getAt(distance, name.lexeme());
        }

        return this.globals.get(name);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }

        if (object instanceof Boolean) {
            return (boolean) object;
        }

        return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }

        if (left == null) {
            return false;
        }

        return left.equals(right);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }

        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }

        System.out.println(left);
        System.out.println(right);

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            var text = object.toString();

            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        }

        return object.toString();
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        this.executeBlock(stmt.statements(), new Environment(this.environment));
        return null;
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previousEnvironment = this.environment;

        try {
            this.environment = environment;

            for (var statement : statements) {
                this.execute(statement);
            }
        } finally {
            this.environment = previousEnvironment;
        }
    }

    @Override
    public Void visitClassStmt(Stmt.Class statement) {

        Object superclass = null;
        if (statement.superclass() != null) {
            superclass = this.evaluate(statement.superclass());
            if (!(superclass instanceof LoxClass)) {
                Lox.error(statement.superclass().name(), "Superclass must be a class.");
            }
        }

        this.environment.define(statement.name().lexeme(), null);

        if (statement.superclass() != null) {
            this.environment = new Environment(this.environment);
            this.environment.define("super", superclass);
        }

        var methods = new HashMap<String, LoxFunction>();
        for (var method : statement.methods()) {
            var isInitializer = method.name().lexeme().equals("init");
            var function = new LoxFunction(method, environment, isInitializer);
            methods.put(method.name().lexeme(), function);
        }

        LoxClass loxClass = new LoxClass(statement.name().lexeme(), (LoxClass) superclass, methods);

        if (superclass != null) {
            this.environment = this.environment.getEnclosing();
        }

        this.environment.assign(statement.name(), loxClass);

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        this.evaluate(stmt.expression());
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        var function = new LoxFunction(stmt, this.environment, false);
        this.environment.define(stmt.name().lexeme(), function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (this.isTruthy(this.evaluate(stmt.condition()))) {
            this.execute(stmt.thenBranch());
        } else if (stmt.elseBranch() != null) {
            this.execute(stmt.elseBranch());
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        var value = this.evaluate(stmt.expression());
        System.out.println(this.stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;

        if (stmt.value() != null) {
            value = this.evaluate(stmt.value());
        }

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;

        if (stmt.initializer() != null) {
            value = this.evaluate(stmt.initializer());
        }

        this.environment.define(stmt.name().lexeme(), value);

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (this.isTruthy(this.evaluate(stmt.condition()))) {
            this.execute(stmt.body());
        }

        return null;
    }
}
