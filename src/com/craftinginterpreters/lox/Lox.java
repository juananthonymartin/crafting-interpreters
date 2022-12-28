package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

public class Lox {
	private static Interpreter interpreter;
	static boolean hadError = false;
	static boolean hadRuntimeError = false;

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64);
		} else if (args.length == 1) {
			interpreter = new Interpreter(RunningMode.INPUT_FILE);
			runFile(args[0]);
		} else {
			interpreter = new Interpreter(RunningMode.REPL);
			runPrompt();
		}
	}

	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));

		// Indicate an error in the exit code.
		if (hadError)
			System.exit(65);
		if (hadRuntimeError)
			System.exit(70);
	}

	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		for (;;) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null)
				break;
			run(line);
			hadError = false;
		}
	}

	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();

// PRINT STATEMENTS AFTER SCANNERS
//		for (var token : tokens) {
//			System.out.println(token.lexeme + ", ");
//		}

		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();

// PRINT STATEMENTS AFTER PARSER		

		for (Iterator iterator = statements.iterator(); iterator.hasNext();) {
			Stmt stmt = (Stmt) iterator.next();
			System.out.println(new AstExprPrinter().print(stmt));
		}
		System.out.println("---------------PARSER DONE---------------");

		// Stop if there was a syntax error.
		if (hadError)
			return;

		Resolver resolver = new Resolver(interpreter);
		resolver.resolve(statements);

		// Stop if there was a resolution error.
		if (hadError)
			return;

		// System.out.println(new AstPrinter().print(expression));
		interpreter.interpret(statements);
	}

	static void error(int line, String message) {
		report(line, "", message);
	}

	static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
		hadRuntimeError = true;
	}

	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}

	static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}
}