package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
	private int loopDepth = 0;

	private static class ParseError extends RuntimeException {
	}

	private final List<Token> tokens;
	private int current = 0;
	private boolean binaryError = true;

	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		while (!isAtEnd()) {
			statements.add(declaration());
		}
		return statements;
	}

	private Stmt declaration() {
		try {
			if (match(CLASS))
				return classDeclaration();
			if (check(FUN) && checkNext(IDENTIFIER)) {
				consume(FUN, null);
				return function("function");
			}
			if (match(VAR))
				return varDeclaration();

			return statement();
		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}

	private Stmt classDeclaration() {
		Token name = consume(IDENTIFIER, "Expect class name.");
		
		Expr.Variable superclass = null;
		if (match(LESS)) {
			consume(IDENTIFIER, "Expect superclass name.");
			superclass = new Expr.Variable(previous());
		}
		
		
		consume(LEFT_BRACE, "Expect '{' before class body.");

		List<Stmt.Function> classMethods = new ArrayList<>();
		List<Stmt.Function> methods = new ArrayList<>();
		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			boolean isClassMethod = match(CLASS);
			(isClassMethod ? classMethods : methods).add(function("method"));
		}

		consume(RIGHT_BRACE, "Expect '}' after class body.");

		return new Stmt.Class(name, superclass, methods, classMethods);
	}

	private boolean checkNext(TokenType tokenType) {
		if (isAtEnd())
			return false;
		if (tokens.get(current + 1).type == EOF)
			return false;
		return tokens.get(current + 1).type == tokenType;
	}

	private Expr expression() {
		return assignment();
	}

	private Stmt statement() {
		if (match(FOR)) {
			return forStatement();
		}
		if (match(IF)) {
			return ifStatement();
		}
		if (match(PRINT)) {
			return printStatement();
		}
		if (match(RETURN))
			return returnStatement();
		if (match(WHILE)) {
			return whileStatement();
		}
		if (match(LEFT_BRACE)) {
			return new Stmt.Block(block());
		}
		if (match(BREAK)) {
			return breakStatement();
		}

		return expressionStatement();
	}

	private Stmt breakStatement() {
		if (loopDepth == 0) {
			error(previous(), "Must be inside a loop to use 'Break'.");
		}

		consume(SEMICOLON, "Expect ';' after break statement");
		return new Stmt.Break();
	}

	private Stmt forStatement() {
		try {
			loopDepth++;
			consume(LEFT_PAREN, "Expect '(' after 'for'.");

			Stmt initializer;
			if (match(SEMICOLON)) {
				initializer = null;
			} else if (match(VAR)) {
				initializer = varDeclaration();
			} else {
				initializer = expressionStatement();
			}

			Expr condition = null;
			if (!check(SEMICOLON)) {
				condition = expression();
			}
			consume(SEMICOLON, "Expect ';' after loop condition.");

			Expr increment = null;
			if (!check(RIGHT_PAREN)) {
				increment = expression();
			}
			consume(RIGHT_PAREN, "Expect ')' after for clauses.");

			Stmt body = statement();

			if (increment != null) {
				body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
			}

			if (condition == null)
				condition = new Expr.Literal(true);

			body = new Stmt.While(condition, body);

			if (initializer != null) {
				body = new Stmt.Block(Arrays.asList(initializer, body));
			}

			return body;
		} finally {
			loopDepth--;
		}
	}

	private Stmt ifStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'if'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after if condition.");

		Stmt thenBranch = statement();

		Stmt elseBranch = null;
		if (match(ELSE)) {
			elseBranch = statement();
		}

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	private Stmt printStatement() {
		Expr value = expression();
		consume(SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}

	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(SEMICOLON)) {
			value = expression();
		}

		consume(SEMICOLON, "Expect ';' after return value.");
		return new Stmt.Return(keyword, value);
	}

	private Stmt varDeclaration() {
		Token name = consume(IDENTIFIER, "Expect variable name.");

		Expr initializer = null;
		if (match(EQUAL)) {
			initializer = expression();
		}

		consume(SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	private Stmt whileStatement() {
		try {
			loopDepth++;
			consume(LEFT_PAREN, "Expect '(' after 'while'.");
			Expr condition = expression();
			consume(RIGHT_PAREN, "Expect ')' after condition.");
			Stmt body = statement();
			return new Stmt.While(condition, body);
		} finally {
			loopDepth--;
		}
	}

	private Stmt expressionStatement() {
		Expr expr = expression();

		consume(SEMICOLON, "Expect ';' after expression.");
		return new Stmt.Expression(expr);
	}

	private Stmt.Function function(String kind) {
		Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
		return new Stmt.Function(name, functionBody(kind));
	}

	private Expr.Function functionBody(String kind) {
		List<Token> parameters = null;

		if (check(LEFT_PAREN)) {
			consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
			parameters = new ArrayList<>();
			if (!check(RIGHT_PAREN)) {
				do {
					if (parameters.size() >= 8) {
						error(peek(), "Can't move more than 8 parameters");
					}

					parameters.add(consume(IDENTIFIER, "Expect parameter name"));
				} while (match(COMMA));
			}

			consume(RIGHT_PAREN, "Expect ')' after paramters.");
		}

		consume(LEFT_BRACE, "Expect '{' " + kind + "body.");
		List<Stmt> body = block();
		return new Expr.Function(parameters, body);
	}

	/*
	 * private Stmt.Function function(String kind) { Token name = null; if
	 * (!kind.contentEquals("lambda")) { name = consume(IDENTIFIER, "Expect " + kind
	 * + " name."); }
	 * 
	 * consume(LEFT_PAREN, "Expect '(' after " + kind + " name."); List<Token>
	 * parameters = new ArrayList<>(); if (!check(RIGHT_PAREN)) { do { if
	 * (parameters.size() >= 255) { error(peek(),
	 * "Can't have more than 255 parameters."); }
	 * 
	 * if (match(FUN)) { System.out.println("FOUND LAMBDA AS A PARAMETER");
	 * function("lambda"); }
	 * 
	 * 
	 * parameters.add(consume(IDENTIFIER, "Expect parameter name.")); } while
	 * (match(COMMA)); } consume(RIGHT_PAREN, "Expect ')' after parameters.");
	 * 
	 * consume(LEFT_BRACE, "Expect '{' before " + kind + " body."); List<Stmt> body
	 * = block();
	 * 
	 * if (kind.contentEquals("lambda")) { name = new Token(TokenType.ANON,
	 * generateAnonFunctionSignature(parameters), null, 1); }
	 * 
	 * return new Stmt.Function(name, parameters, body); }
	 */

	// helper return ANON function name using parameters

	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();

		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}

		consume(RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}

	private Expr assignment() {
		Expr expr = or();

		if (match(EQUAL)) {
			Token equals = previous();
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value);
			} else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get) expr;
				return new Expr.Set(get.object, get.name, value);
			}

			error(equals, "Invalid assignment target.");
		}

		return expr;
	}

	private Expr or() {
		Expr expr = and();

		while (match(OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	private Expr and() {
		Expr expr = equality();

		while (match(AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	private Expr equality() {
		Expr expr = comparison();

		if (expr == null)
			return null;

		// remove
		// System.out.println("equality");

		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			// remove
			// System.out.println("equality - exp:binary(comp,op,comp)");

			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr comparison() {
		Expr expr = term();

		if (expr == null)
			return null;

		// remove
		// System.out.println("comparison");

		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();

			// remove
			// System.out.println("comparison - exp:binary(term,op,term)");

			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr term() {
		Expr expr = factor();

		if (expr == null)
			return null;

		// remove
		// System.out.println("term");

		while (match(MINUS, PLUS)) {
			Token operator = previous();

			// remove
			// System.out.println("term - exp:binary(factor,op,factor)");

			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr factor() {
		Expr expr = unary();

		if (expr == null)
			return null;

		// remove
		// System.out.println("factor");

		while (match(SLASH, STAR)) {
			Token operator = previous();

			// remove
			// System.out.println("factor - exp:binary(unary,op,unary)");

			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr unary() {

		// System.out.println("----unary----");
		if (match(BANG, MINUS)) {
			Token operator = previous();
			// System.out.println("unary - Unary(op,right)");
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return call();
	}

	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();
		if (!check(RIGHT_PAREN)) {
			do {
				if (arguments.size() >= 255) {
					error(peek(), "Can't have more than 255 arguments.");
				}

				arguments.add(expression());

			} while (match(COMMA));
		}

		Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

		return new Expr.Call(callee, paren, arguments);
	}

	private Expr call() {
		Expr expr = primary();

		while (true) {
			if (match(LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if (match(DOT)) {
				Token name = consume(IDENTIFIER, "Expect property name after '.'.");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}

		return expr;
	}

	private Expr primary() {
		// System.out.println("primary");
		if (match(FALSE))
			return new Expr.Literal(false);
		if (match(TRUE))
			return new Expr.Literal(true);
		if (match(NIL))
			return new Expr.Literal(null);

		if (match(NUMBER, STRING)) {
			// System.out.println("literal: " + previous().literal);
			return new Expr.Literal(previous().literal);
		}

		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}

		if (match(SUPER)) {
			Token keyword = previous();
			consume(DOT, "Expect '.' after 'super'.");
			Token method = consume(IDENTIFIER, "Expect superclass method name");
			return new Expr.Super(keyword, method);
		}
		
		if (match(THIS))
			return new Expr.This(previous());

		if (match(IDENTIFIER)) {
			return new Expr.Variable(previous());
		}

		if (match(FUN)) {
			return functionBody("function");
		}

		if (isBinaryOperator()) {
			return null;
		}

		// System.out.println("isBinaryOperator " + isBinaryOperator());

		throw error(peek(), "Expected expression.");
	}

	private boolean isBinaryOperator() {
		Token token = peek();

		ArrayList<TokenType> bTokens = new ArrayList<TokenType>() {
			{
				add(SLASH);
				add(STAR);
				add(PLUS);
				add(MINUS);
				add(GREATER);
				add(GREATER_EQUAL);
				add(LESS);
				add(LESS_EQUAL);
				add(BANG_EQUAL);
				add(EQUAL_EQUAL);
			}
		};

		return bTokens.contains(token.type);
	}

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}

		return false;
	}

	private Token consume(TokenType type, String message) {
		if (check(type))
			return advance();

		throw error(peek(), message);
	}

	private void synchronize() {
		advance();

		while (!isAtEnd()) {
			if (previous().type == SEMICOLON)
				return;

			switch (peek().type) {
			case CLASS:
			case FUN:
			case VAR:
			case FOR:
			case IF:
			case WHILE:
			case PRINT:
			case RETURN:
				return;
			}

			advance();
		}
	}

	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}

	private boolean check(TokenType type) {
		if (isAtEnd())
			return false;
		return peek().type == type;
	}

	private Token advance() {
		if (!isAtEnd())
			current++;
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == EOF;
	}

	private Token peek() {
		return tokens.get(current);
	}

	private Token previous() {
		return tokens.get(current - 1);
	}

	/*
	 * private Expr expression2() { Expr expr = equality();
	 * 
	 * // Expression series while (match(COMMA)) { Token operator = previous(); Expr
	 * right = expression(); expr = new Expr.Binary(expr, operator, right); }
	 * 
	 * if (expr == null) throw error(peek(), "Wrong binary expression.");
	 * 
	 * // Ternary while (match(TERNARY_BEGIN)) { // remove
	 * System.out.println("Ternary if true");
	 * 
	 * Expr ifTrue = expression(); while (match(TERNARY_END)) { // remove
	 * System.out.println("Ternary if false");
	 * 
	 * Expr ifFalse = expression(); expr = new Expr.Ternary(expr, ifTrue, ifFalse);
	 * } }
	 * 
	 * return expr; };
	 */

}