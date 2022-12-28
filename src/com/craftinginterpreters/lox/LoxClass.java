package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
	final String name;
	private final Map<String, LoxFunction> methods;

	LoxClass(LoxClass metaclass, String name, Map<String, LoxFunction> methods) {
		super(metaclass);
		this.name = name;
		this.methods = methods;
	}

	LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}

		return null;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int arity() {
		LoxFunction initializer = findMethod("init");
		if (initializer == null)
			return 0;
		return initializer.arity();
	}

	// Triggered After after parsing Expr.Call
	// Method creates new LoxInstance
	// If there's an initiliazers binds it to the instance and calls it.
	// Return new lox instance
	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		LoxInstance instance = new LoxInstance(this);
		LoxFunction initializer = findMethod("init");

		if (initializer != null) {
			initializer.bind(instance).call(interpreter, arguments);
		}

		return instance;
	}
}