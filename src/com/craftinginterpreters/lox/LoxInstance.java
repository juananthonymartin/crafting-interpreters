package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
	private String klassName;
	private LoxClass klass;
	private final Map<String, Object> fields = new HashMap<>();

	LoxInstance(LoxClass klass) {
		this.klass = klass;
	}
	
	LoxInstance(String klassName) {
		System.out.println("Calling LoxInstance default constructor for " + klassName);
		this.klassName = klassName;
	}

	Object get(Token name) {
		if (fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}

		LoxFunction method = klass.findMethod(name.lexeme);
		if (method != null)
			return method.bind(this);

		throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
	}

	void set(Token name, Object value) {
		fields.put(name.lexeme, value);
	}

	@Override
	public String toString() {
		if (klass == null) return klassName + " instance";
		return klass.name + " instance";
	}
}