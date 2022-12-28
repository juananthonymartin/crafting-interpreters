package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
	private final String name;
	private final Expr.Function declaration;
	private final Environment closure;

	private final boolean isInitializer;

	LoxFunction(String name, Expr.Function declaration, Environment closure, boolean isInitializer) {
		this.name = name;
		this.closure = closure;
		this.declaration = declaration;
		this.isInitializer = isInitializer;
	}

	LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
		this.name = declaration.name.lexeme;
		this.closure = closure;
		this.declaration = declaration.function;
		this.isInitializer = isInitializer;
	}

	// Returns new LoxFunction after binding LoxInstance as 'this'.
	LoxFunction bind(LoxInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new LoxFunction(name, declaration, environment, isInitializer);
	}
	
	boolean noParameterList() {
		return declaration.parameters == null;
	}

	@Override
	public int arity() {
		if (declaration.parameters == null) {
			return -1;
		} else {
			return declaration.parameters.size();
		}
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment environment = new Environment(closure);
		
		if (!noParameterList()) {
			for (int i = 0; i < declaration.parameters.size(); i++) {
				environment.define(declaration.parameters.get(i).lexeme, arguments.get(i));
			}
		}
		
		try {
			interpreter.executeBlock(declaration.body, environment);
		} catch (Return returnValue) {
			if (isInitializer) return closure.getAt(0, "this");
			return returnValue.value;
		}
		
		if (isInitializer) return closure.getAt(0, "this");
		return null;
	}

	@Override
	public String toString() {
		if (name == null)
			return "<fn>";
		return "<fn " + name + ">";
	}
}