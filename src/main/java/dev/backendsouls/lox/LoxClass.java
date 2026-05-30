package dev.backendsouls.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LoxClass(String name) implements LoxCallable {

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new LoxInstance(this);
    }

    @Override
    public String toString() {
        return name;
    }
}
