package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
	final String name;
	private final Map<String, LoxFunction> methods;
	private ArrayList<LoxClass>  superclasses;
	
	LoxClass(LoxClass metaclass, ArrayList<LoxClass> superclasses, String name, Map<String, LoxFunction> methods) {
		super(metaclass);
		this.superclasses = superclasses;
		this.name = name;
		this.methods = methods;
	}

	
	LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}
		
		if (superclasses != null && !superclasses.isEmpty()) {
			for (LoxClass loxClass : superclasses) {
				return loxClass.findMethod(name);
			}
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