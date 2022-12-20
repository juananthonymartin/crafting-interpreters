package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Environment {
	final public int id;
	final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();

	Environment() {
		enclosing = null;
		this.id = 1;
	}

	Environment(Environment enclosing) {
		this.enclosing = enclosing;
		this.id = this.enclosing.id + 1;
	}

	Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			var assignment = values.get(name.lexeme);
			if (assignment == null) {
				throw new RuntimeError(name, "Accessing un-assigned variable '" + name.lexeme + "'.");
			}
			return assignment;
		}

		if (enclosing != null)
			return enclosing.get(name);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	Object get(String tokenName) {
		if (values.containsKey(tokenName)) {
			var assignment = values.get(tokenName);
			if (assignment == null) {
				throw new RuntimeError(null, "Accessing un-assigned variable '" + tokenName + "'.");
			}
			return assignment;
		}

		if (enclosing != null)
			return enclosing.get(tokenName);

		throw new RuntimeError(null, "Undefined variable '" + tokenName + "'.");
	}

	void define(String name, Object value) {
		values.put(name, value);
	}

	Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
	}

	void assignAt(int distance, Token name, Object value) {
		ancestor(distance).values.put(name.lexeme, value);
	}

	Environment ancestor(int distance) {
		Environment environment = this;
		for (int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}

		return environment;
	}

	String getKeys() {
		return values.keySet().toString();
	}

	void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}

		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
}