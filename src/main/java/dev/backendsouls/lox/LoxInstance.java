package dev.backendsouls.lox;

import java.util.HashMap;
import java.util.Map;

public final class LoxInstance {

    private LoxClass loxClass;
    private final Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass loxClass) {
        this.loxClass = loxClass;
    }

    public Object get(Token name) {

        if (this.fields.containsKey(name.lexeme())) {
            return this.fields.get(name.lexeme());
        }

        var method = (LoxFunction) this.loxClass.findMethod(name.lexeme());
        if (method != null) {
            return method.bind(this);
        }

        throw new RuntimeError(name, "Undefined property '" + name.lexeme() + "'.");
    }

    public void set(Token name, Object value) {

        this.fields.put(name.lexeme(), value);
    }

    @Override
    public String toString() {
        return this.loxClass.name() + " instance";
    }

}
