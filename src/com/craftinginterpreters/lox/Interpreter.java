package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.craftinginterpreters.lox.Expr.Call;
import com.craftinginterpreters.lox.Stmt.Break;
import com.craftinginterpreters.lox.Stmt.Expression;
import com.craftinginterpreters.lox.Stmt.Print;
import com.craftinginterpreters.tool.Pair;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	final Environment globals = new Environment();
	private Environment environment = globals;
	private final Map<Expr, Integer> locals = new HashMap<>();

	Interpreter() {
		globals.define("clock", new LoxCallable() {
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

	private RunningMode runningMode;

	public Interpreter(RunningMode mode) {
		this.runningMode = mode;
	}

	void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value);

		Integer distance = locals.get(expr);

		if (distance != null) {
			environment.assignAt(distance, expr.name, value);
		} else {
			globals.assign(expr.name, value);
		}
		return value;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}

		environment.define(stmt.name.lexeme, value);

		if (this.runningMode == RunningMode.REPL)
			System.out.println(value);

		return null;
	}

	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return lookUpVariable(expr.name, expr);
	}

	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance, name.lexeme);
		} else {
			return globals.get(name);
		}
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = evaluate(expr.left);

		if (expr.operator.type == TokenType.OR) {
			if (isTruthy(left))
				return left;
		} else {
			if (!isTruthy(left))
				return left;
		}

		return evaluate(expr.right);
	}

	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);

		if (!(object instanceof LoxInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}

		Object value = evaluate(expr.value);
		((LoxInstance) object).set(expr.name, value);
		return value;
	}

	@Override
	public Object visitThisExpr(Expr.This expr) {
		return lookUpVariable(expr.keyword, expr);
	}

	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
		case GREATER:
			checkNumberOperands(expr.operator, left, right);
			return (double) left > (double) right;
		case GREATER_EQUAL:
			checkNumberOperands(expr.operator, left, right);
			return (double) left >= (double) right;
		case LESS:
			checkNumberOperands(expr.operator, left, right);
			return (double) left < (double) right;
		case LESS_EQUAL:
			checkNumberOperands(expr.operator, left, right);
			return (double) left <= (double) right;
		case BANG_EQUAL:
			return !isEqual(left, right);
		case EQUAL_EQUAL:
			return isEqual(left, right);
		case MINUS:
			checkNumberOperands(expr.operator, left, right);
			return (double) left - (double) right;
		case PLUS:
			if (left instanceof Double && right instanceof Double) {
				return (double) left + (double) right;
			}

			if (left instanceof String && right instanceof String) {
				return (String) left + (String) right;
			}

			if (left instanceof String leftString) {
				return leftString + right;
			}

			if (right instanceof String rightString) {
				return "" + stringify(left) + rightString;
			}

			throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
		case SLASH:
			checkNumberOperands(expr.operator, left, right);
			if (!checkValidDivision(right)) {
				throw new RuntimeError(expr.operator, "invalid division by 0");
			}
			return (double) left / (double) right;
		case STAR:
			checkNumberOperands(expr.operator, left, right);
			return (double) left * (double) right;
		}

		// Unreachable.
		return null;
	}

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = evaluate(expr.callee);

		List<Object> arguments = new ArrayList<>();
		for (Expr argument : expr.arguments) {
			arguments.add(evaluate(argument));
		}

		if (!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");
		}

		LoxCallable function = (LoxCallable) callee;
		if (arguments.size() != function.arity()) {
			throw new RuntimeError(expr.paren,
					"Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
		}
		return function.call(this, arguments);
	}

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if (object instanceof LoxInstance) {
			
			var result = ((LoxInstance) object).get(expr.name);
			
			if (result instanceof LoxFunction) {
				System.out.println("here1");
				if (((LoxFunction)result).noParameterList()) {
					System.out.println("here2");
					var value = ((LoxFunction)result).call(this, null);
					System.out.println("value" + value);
					((LoxInstance) object).set(expr.name, value);
					return value;
				}
				
			}
			
			return ((LoxInstance) object).get(expr.name);
		}

		throw new RuntimeError(expr.name, "Only instances have properties.");
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
		case MINUS:
			checkNumberOperand(expr.operator, right);
			return -(double) right;
		case BANG:
			return !isTruthy(right);
		}

		// Unreachable.
		return null;
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double)
			return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private boolean isTruthy(Object object) {
		if (object == null)
			return false;
		if (object instanceof Boolean)
			return (boolean) object;
		return true;
	}

	private boolean checkValidDivision(Object right) {
		return (double) right != 0;
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double)
			return;

		throw new RuntimeError(operator, "Operands must be numbers.");
	}

	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}

	void resolveFast(Expr expr, int depth, int index) {
		locals.put(expr, depth);
	}

	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null)
			return false;

		return a.equals(b);
	}

	@Override
	public Void visitExpressionStmt(Expression stmt) {
		var expr = evaluate(stmt.expression);
		if (this.runningMode == RunningMode.REPL)
			System.out.println(expr);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		String fnName = stmt.name.lexeme;
		environment.define(fnName, new LoxFunction(stmt, environment, false));
		return null;
	}

	@Override
	public Object visitFunctionExpr(Expr.Function expr) {
		return new LoxFunction(null, expr, environment, false);
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null)
			value = evaluate(stmt.value);

		throw new Return(value);
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		while (isTruthy(evaluate(stmt.condition))) {
			try {
				execute(stmt.body);
			} catch (RuntimeError e) {
				if (e.token.type == TokenType.BREAK) {
					break;
				}
				throw e;
			}

		}
		return null;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		environment.define(stmt.name.lexeme, null);

		Map<String, LoxFunction> classMethods = new HashMap<>();
		for (Stmt.Function method : stmt.classMethods) {
			LoxFunction function = new LoxFunction(method, environment, false);
			classMethods.put(method.name.lexeme, function);
		}

		LoxClass metaclass = new LoxClass(null, stmt.name.lexeme + " metaclass", classMethods);

		Map<String, LoxFunction> methods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
			methods.put(method.name.lexeme, function);
		}

		LoxClass klass = new LoxClass(metaclass, stmt.name.lexeme, methods);
		environment.assign(stmt.name, klass);
		return null;
	}

	void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;

		try {
			this.environment = environment;

			for (Stmt statement : statements) {
				execute(statement);
			}
		} finally {
			this.environment = previous;
		}
	}

	private String stringify(Object object) {
		if (object == null)
			return "nil";

		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}

		return object.toString();
	}

	@Override
	public Void visitBreakStmt(Break stmt) {
		throw new RuntimeError(new Token(TokenType.BREAK, "break", null, 1), "Operands must be numbers.");
	}

}