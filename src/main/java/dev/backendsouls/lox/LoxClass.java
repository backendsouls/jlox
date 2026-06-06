package dev.backendsouls.lox;

import java.util.List;
import java.util.Map;

public record LoxClass(String name, Map<String, LoxFunction> methods) implements LoxCallable {

    @Override
    public int arity() {

        var initializer = (LoxFunction) this.findMethod("init");
        if (initializer == null) {
            return 0;
        }

        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {

        var instance = new LoxInstance(this);

        var initializer = (LoxFunction) this.findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public String toString() {
        return name;
    }

    public Object findMethod(String name) {
        if (this.methods().containsKey(name)) {
            return this.methods().get(name);
        }

        return null;
    }
}
