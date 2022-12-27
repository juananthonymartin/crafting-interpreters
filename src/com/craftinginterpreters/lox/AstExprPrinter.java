package com.craftinginterpreters.lox;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.craftinginterpreters.lox.Expr.Assign;
import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Call;
import com.craftinginterpreters.lox.Expr.Get;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Logical;
import com.craftinginterpreters.lox.Expr.Set;
import com.craftinginterpreters.lox.Expr.This;
import com.craftinginterpreters.lox.Expr.Unary;
import com.craftinginterpreters.lox.Expr.Variable;
import com.craftinginterpreters.lox.Stmt.Block;
import com.craftinginterpreters.lox.Stmt.Break;
import com.craftinginterpreters.lox.Stmt.Class;
import com.craftinginterpreters.lox.Stmt.Expression;
import com.craftinginterpreters.lox.Stmt.Function;
import com.craftinginterpreters.lox.Stmt.If;
import com.craftinginterpreters.lox.Stmt.Print;
import com.craftinginterpreters.lox.Stmt.Return;
import com.craftinginterpreters.lox.Stmt.Var;
import com.craftinginterpreters.lox.Stmt.While;

class AstExprPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

	public static void main(String[] args) throws IOException {

		// -123 * (45.67)
		Expr expr = new Expr.Binary(new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1), new Expr.Literal(123)),
				new Token(TokenType.STAR, "*", null, 1), new Expr.Grouping(new Expr.Literal(45.67)));

		var stmtexpr = new Stmt.Expression(expr);

		// var a = 1;
		// while (a < 5)
		// {
		// print a;
		// a = a + 1;
		// }

		var identifier = new Token(TokenType.IDENTIFIER, "a", null, 1);
		var initializer = new Expr.Literal(1);
		var stmtvar = new Stmt.Var(identifier, initializer);

		System.out.println(new AstExprPrinter().print(stmtvar));

		var exprvar = new Expr.Variable(identifier);
		Expr cond = new Expr.Binary(exprvar, new Token(TokenType.LESS_EQUAL, "<=", null, 1), new Expr.Literal(5));

		// print a;
		var stmtprint = new Stmt.Print(stmtvar.initializer);

		// a = a + 1;
		// a + 1;
		var variable = new Expr.Variable(identifier);
		var literal = new Expr.Literal(1);
		var plusToken = new Token(TokenType.PLUS, "+", null, 1);
		var binaryAddition = new Expr.Binary(variable, plusToken, literal);

		// a = a + 1;
		var exprassign = new Expr.Assign(identifier, binaryAddition);
		var stmtexprLoop = new Stmt.Expression(exprassign);

		// if a == 3 break;
		Expr conditonal = new Expr.Binary(exprvar, new Token(TokenType.EQUAL_EQUAL, "==", null, 1),
				new Expr.Literal(3));

		var stmtBreak = new Stmt.Break();
		var stmtIf = new Stmt.If(cond, stmtBreak, null);

		var stmts = Arrays.asList(stmtprint, stmtexprLoop, stmtIf);
		var stmtblock = new Stmt.Block(stmts);

		var stmtWhile = new Stmt.While(cond, stmtblock);

		System.out.println(new AstExprPrinter().print(stmtWhile));
	}

	// < Statements and State omit
	String print(Expr expr) {
		return expr.accept(this);
	}
	// > Statements and State omit

	String print(Stmt stmt) {
		return stmt.accept(this);
	}

	@Override
	public String visitAssignExpr(Expr.Assign expr) {
		return parenthesize2("AssignExpr", expr.name.lexeme, expr.value);
	}

	@Override
	public String visitBinaryExpr(Expr.Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Expr.Grouping expr) {
		return parenthesize("group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Expr.Literal expr) {
		if (expr.value == null)
			return "nil";
		return expr.value.toString();
	}

	@Override
	public String visitLogicalExpr(Expr.Logical expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitUnaryExpr(Expr.Unary expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
	}

	@Override
	public String visitVariableExpr(Expr.Variable expr) {
		return expr.name.lexeme;
	}

	/*
	 * private String parenthesize3(String name, Arg... exprs) { if }
	 */

	private String parenthesize(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();

		builder.append("(").append(name);
		for (Expr expr : exprs) {
			builder.append(" ");
			builder.append(expr.accept(this));
		}
		builder.append(")");

		return builder.toString();
	}

	// < print-utilities
	// > omit
	// Note: AstPrinting other types of syntax trees is not shown in the
	// book, but this is provided here as a reference for those reading
	// the full code.
	private String parenthesize2(String name, Object... parts) {
		StringBuilder builder = new StringBuilder();

		builder.append("(").append(name);
		transform(builder, parts);
		builder.append(")");

		return builder.toString();
	}

	private void transform(StringBuilder builder, Object... parts) {
		for (Object part : parts) {
			builder.append(" ");
			if (part instanceof Expr) {
				builder.append(((Expr) part).accept(this));
				// > Statements and State omit
			} else if (part instanceof Stmt) {
				builder.append(((Stmt) part).accept(this));
			} else if (part instanceof Token) {
				builder.append(((Token) part).lexeme);
			} else if (part instanceof List) {
				transform(builder, ((List) part).toArray());
			} else {
				builder.append(part);
			}
		}
	}

	@Override
	public String visitBreakStmt(Break stmt) {
		StringBuilder builder = new StringBuilder();
		builder.append("(Stmt.break)");
		return builder.toString();
	}

	@Override
	public String visitBlockStmt(Block stmt) {
		StringBuilder builder = new StringBuilder();
		builder.append("(Stmt.block ");

		for (Stmt statement : stmt.statements) {
			builder.append(statement.accept(this));
		}

		builder.append(")");
		return builder.toString();
	}

	@Override
	public String visitExpressionStmt(Expression stmt) {
		return parenthesize(";", stmt.expression);
	}

	@Override
	public String visitIfStmt(If stmt) {
		if (stmt.elseBranch == null) {
			return parenthesize2("Stmt.if", stmt.condition, stmt.thenBranch);
		}

		return parenthesize2("Stmt.if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
	}

	@Override
	public String visitPrintStmt(Print stmt) {
		return parenthesize("Stmt.print", stmt.expression);
	}

	@Override
	public String visitVarStmt(Var stmt) {
		if (stmt.initializer == null) {
			return parenthesize2("Stmt.var", stmt.name);
		}

		return parenthesize2("Stmt.var", stmt.name, "=", stmt.initializer);
	}

	@Override
	public String visitWhileStmt(While stmt) {
		return parenthesize2("Stmt.while", stmt.condition, stmt.body);
	}

	@Override
	public String visitCallExpr(Call expr) {
		return parenthesize2("Expr.call", expr.callee, expr.arguments);
	}

	@Override
	public String visitReturnStmt(Return stmt) {
		return parenthesize("Stmt.return", stmt.value);
	}

	@Override
	public String visitFunctionStmt(Stmt.Function stmt) {
		StringBuilder builder = new StringBuilder();
		
		String firstLine = "(Function.Stmt" + stmt.name.lexeme + "(";;
		
		builder.append(firstLine);

		for (Token param : stmt.function.parameters) {
			if (param != stmt.function.parameters.get(0))
				builder.append(" ");
			builder.append(param.lexeme);
		}

		builder.append(") ");

		for (Stmt body : stmt.function.body) {
			builder.append(body.accept(this));
		}

		builder.append(")");
		return builder.toString();
	}
	

	@Override
	public String visitFunctionExpr(com.craftinginterpreters.lox.Expr.Function expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitClassStmt(Stmt.Class stmt) {
		StringBuilder builder = new StringBuilder();
		builder.append("(class " + stmt.name.lexeme);

		for (Stmt.Function method : stmt.methods) {
			builder.append(" " + print(method));
		}

		builder.append(")");
		return builder.toString();
	}

	@Override
	public String visitGetExpr(Expr.Get expr) {
		return parenthesize2("Get.Expr", expr.object, expr.name.lexeme);
	}

	@Override
	public String visitSetExpr(Expr.Set expr) {
		return parenthesize2("Set.Expr", expr.object, expr.name.lexeme, expr.value);
	}

	@Override
	public String visitThisExpr(Expr.This expr) {
		return "this";
	}}