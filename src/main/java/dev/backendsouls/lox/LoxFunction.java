package dev.backendsouls.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    private final Environment closure;

    private final boolean isInitializer;

    public LoxFunction(final Stmt.Function declaration, final Environment closure, final boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    public LoxFunction bind(LoxInstance instance) {
        var environment = new Environment(this.closure);
        environment.define("this", instance);

        return new LoxFunction(this.declaration, environment, this.isInitializer);
    }

    @Override
    public int arity() {
        return this.declaration.params().size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var names = this.declaration.params().iterator();
        var values = arguments.iterator();
        var environment = new Environment(this.closure);

        while (names.hasNext() && values.hasNext()) {
            environment.define(names.next().lexeme(), values.next());
        }

        try {
            interpreter.executeBlock(this.declaration.body(), environment);
        } catch (Return returnValue) {

            if (this.isInitializer) {
                return this.closure.getAt(0, "this");
            }

            return returnValue.value;
        }

        if (this.isInitializer) {
            return this.closure.getAt(0, "this");
        }

        return null;
    }

    @Override
    public String toString() {
        return "<fn " + this.declaration.name().lexeme() + ">";
    }
}
