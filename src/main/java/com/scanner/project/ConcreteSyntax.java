package com.scanner.project;
// ConcreteSyntax.java

// Implementation of the Recursive Descent Parser algorithm
// This parser is adapted to work with the TokenStream implementation
// that produces token types: "Keyword", "Identifier", "Literal", "Operator", "Separator", "Other".

public class ConcreteSyntax {

	// Instance variables
	public Token token; // current token that is considered from the input stream
	public TokenStream input; // stream of tokens generated in by the lexical analysis

	// Constructor
	public ConcreteSyntax(TokenStream ts) {
		input = ts;
		token = input.nextToken(); // retrieve its first Token
	}

	// Method that prints a syntax error message
	private String SyntaxError(String tok) {
		String s = "Syntax error - Expecting: " + tok + " But saw: "
				+ token.getType() + " = " + token.getValue();
		System.out.println(s);
		return s;
	}

	// Match a string with the value of a token. If no problem, go to the next
	// token otherwise generate an error message
	private void match(String s) {
		if (token.getValue().equals(s))
			token = input.nextToken();
		else
			throw new RuntimeException(SyntaxError(s));
	}

	// Implementation of the Recursive Descent Parser

	public Program program() {
		// Program --> main '{' Declarations Block '}'
		Program p = new Program();

		// Expect keyword main
		if (token.getType().equals("Keyword") && token.getValue().equals("main"))
			match("main");
		else
			throw new RuntimeException(SyntaxError("main"));

		// Expect '{' separator
		if (token.getType().equals("Separator") && token.getValue().equals("{"))
			match("{");
		else
			throw new RuntimeException(SyntaxError("{"));

		// parse declarations and the body block, then match closing "}"
		p.decpart = declarations();
		p.body = statements(); // statements() parses until it sees "}"
		match("}");
		return p;
	}

	private Declarations declarations() {
		// Declarations --> { Declaration }*
		Declarations ds = new Declarations();
		while (token.getType().equals("Keyword")
				&& (token.getValue().equals("integer") || token.getValue().equals("bool"))) {
			declaration(ds);
		}
		return ds;
	}

	private void declaration(Declarations ds) {
		// Declaration --> Type Identifiers ;
		Type t = type();
		identifiers(ds, t);
		// Expect ';' separator
		if (token.getType().equals("Separator") && token.getValue().equals(";"))
			match(";");
		else
			throw new RuntimeException(SyntaxError(";"));
	}

	private Type type() {
		// Type --> integer | bool
		Type t = null;
		if (token.getType().equals("Keyword") && token.getValue().equals("integer"))
			t = new Type(token.getValue());
		else if (token.getType().equals("Keyword") && token.getValue().equals("bool"))
			t = new Type(token.getValue());
		else
			throw new RuntimeException(SyntaxError("integer | bool"));
		token = input.nextToken(); // pass over the type
		return t;
	}

	private void identifiers(Declarations ds, Type t) {
		// Identifiers --> Identifier { , Identifier }*
		Declaration d = new Declaration(); // first declaration
		d.t = t; // its type
		if (token.getType().equals("Identifier")) {
			d.v = new Variable();
			d.v.id = token.getValue(); // its value
			ds.addElement(d);
			token = input.nextToken();
			while (token.getType().equals("Separator") && token.getValue().equals(",")) {
				// consume ','
				match(",");
				d = new Declaration(); // next declaration
				d.t = t; // its type
				if (token.getType().equals("Identifier")) {
					d.v = new Variable(); // its value
					d.v.id = token.getValue();
					ds.addElement(d);
					token = input.nextToken(); // get "," or ";"
				} else
					throw new RuntimeException(SyntaxError("Identifier"));
			}
		} else
			throw new RuntimeException(SyntaxError("Identifier"));
	}

	private Statement statement() {
		// Statement --> ; | Block | Assignment | IfStatement | WhileStatement
		Statement s = new Skip();
		// Skip statement ';'
		if (token.getType().equals("Separator") && token.getValue().equals(";")) {
			token = input.nextToken();
			return s;
		} else if (token.getType().equals("Separator") && token.getValue().equals("{")) { // Block
			token = input.nextToken();
			s = statements();
			match("}");
		} else if (token.getType().equals("Keyword") && token.getValue().equals("if")) // IfStatement
			s = ifStatement();
		else if (token.getType().equals("Keyword") && token.getValue().equals("while")) { // WhileStatement
			s = whileStatement();
		} else if (token.getType().equals("Identifier")) { // Assignment
			s = assignment();
		} else
			throw new RuntimeException(SyntaxError("Statement"));
		return s;
	}

	private Block statements() {
		// Block --> '{' Statements '}'   (but caller already consumed '{')
		Block b = new Block();
		while (!(token.getType().equals("Separator") && token.getValue().equals("}"))) {
			b.blockmembers.addElement(statement());
		}
		return b;
	}

	private Assignment assignment() {
		// Assignment --> Identifier := Expression ;
		Assignment a = new Assignment();
		if (token.getType().equals("Identifier")) {
			// set target
			a.target = new Variable();
			a.target.id = token.getValue();
			token = input.nextToken();
			// Expect operator ":="
			if (token.getType().equals("Operator") && token.getValue().equals(":="))
				match(":=");
			else
				throw new RuntimeException(SyntaxError(":="));
			a.source = expression();
			// Expect ';'
			if (token.getType().equals("Separator") && token.getValue().equals(";"))
				match(";");
			else
				throw new RuntimeException(SyntaxError(";"));
		} else
			throw new RuntimeException(SyntaxError("Identifier"));
		return a;
	}

	private Expression expression() {
		// Expression --> Conjunction { || Conjunction }*
		Binary b;
		Expression e;
		e = conjunction();
		while (token.getType().equals("Operator") && token.getValue().equals("||")) {
			b = new Binary();
			b.term1 = e;
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term2 = conjunction();
			e = b;
		}
		return e;
	}

	private Expression conjunction() {
		// Conjunction --> Relation { && Relation }*
		Binary b;
		Expression e;
		e = relation();
		while (token.getType().equals("Operator") && token.getValue().equals("&&")) {
			b = new Binary();
			b.term1 = e;
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term2 = relation();
			e = b;
		}
		return e;
	}

	private Expression relation() {
		// Relation --> Addition { (< | <= | > | >= | == | != | <>) Addition }*
		Binary b;
		Expression e;
		e = addition();
		while (token.getType().equals("Operator") &&
		       (token.getValue().equals("<") || token.getValue().equals("<=")
				|| token.getValue().equals(">") || token.getValue().equals(">=")
				|| token.getValue().equals("==") || token.getValue().equals("!=")
				|| token.getValue().equals("<>"))) {
			b = new Binary();
			b.term1 = e;
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term2 = addition();
			e = b;
		}
		return e;
	}

	private Expression addition() {
		// Addition --> Term { [ + | - ] Term }*
		Binary b;
		Expression e;
		e = term();
		while (token.getType().equals("Operator") &&
		       (token.getValue().equals("+") || token.getValue().equals("-"))) {
			b = new Binary();
			b.term1 = e;
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term2 = term();
			e = b;
		}
		return e;
	}

	private Expression term() {
		// Term --> Negation { [ '*' | / ] Negation }*
		Binary b;
		Expression e;
		e = negation();
		while (token.getType().equals("Operator") &&
		       (token.getValue().equals("*") || token.getValue().equals("/"))) {
			b = new Binary();
			b.term1 = e;
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term2 = negation();
			e = b;
		}
		return e;
	}

	private Expression negation() {
		// Negation --> { ! }opt Factor
		Unary u;
		if (token.getType().equals("Operator") && token.getValue().equals("!")) {
			u = new Unary();
			u.op = new Operator(token.getValue());
			token = input.nextToken();
			u.term = factor();
			return u;
		} else
			return factor();
	}

	private Expression factor() {
		// Factor --> Identifier | Literal | ( Expression )
		Expression e = null;
		if (token.getType().equals("Identifier")) {
			Variable v = new Variable();
			v.id = token.getValue();
			e = v;
			token = input.nextToken();
		} else if (token.getType().equals("Literal")) {
			Value v = null;
			if (isInteger(token.getValue()))
				v = new Value((new Integer(token.getValue())).intValue());
			else if (token.getValue().equals("True") || token.getValue().equals("true"))
				v = new Value(true);
			else if (token.getValue().equals("False") || token.getValue().equals("false"))
				v = new Value(false);
			else
				throw new RuntimeException(SyntaxError("Literal"));
			e = v;
			token = input.nextToken();
		} else if (token.getType().equals("Separator") && token.getValue().equals("(")) {
			token = input.nextToken();
			e = expression();
			match(")");
		} else
			throw new RuntimeException(SyntaxError("Identifier | Literal | ("));
		return e;
	}

	private Conditional ifStatement() {
		// IfStatement --> if ( Expression ) Statement { else Statement }opt
		Conditional c = new Conditional();
		// Expect keyword if
		if (token.getType().equals("Keyword") && token.getValue().equals("if"))
			match("if");
		else
			throw new RuntimeException(SyntaxError("if"));

		// Expect '('
		if (token.getType().equals("Separator") && token.getValue().equals("("))
			match("(");
		else
			throw new RuntimeException(SyntaxError("("));

		c.test = expression();

		// Expect ')'
		if (token.getType().equals("Separator") && token.getValue().equals(")"))
			match(")");
		else
			throw new RuntimeException(SyntaxError(")"));

		c.thenbranch = statement();

		if (token.getType().equals("Keyword") && token.getValue().equals("else")) {
			// consume else
			token = input.nextToken();
			c.elsebranch = statement();
		} else {
			c.elsebranch = null;
		}
		return c;
	}

	private Loop whileStatement() {
		// WhileStatement --> while ( Expression ) Statement
		Loop l = new Loop();
		// Expect keyword while
		if (token.getType().equals("Keyword") && token.getValue().equals("while"))
			match("while");
		else
			throw new RuntimeException(SyntaxError("while"));

		// Expect '('
		if (token.getType().equals("Separator") && token.getValue().equals("("))
			match("(");
		else
			throw new RuntimeException(SyntaxError("("));

		l.test = expression();

		// Expect ')'
		if (token.getType().equals("Separator") && token.getValue().equals(")"))
			match(")");
		else
			throw new RuntimeException(SyntaxError(")"));

		l.body = statement();
		return l;
	}

	private boolean isInteger(String s) {
		if (s == null || s.length() == 0) return false;
		for (int i = 0; i < s.length(); i++)
			if ('0' > s.charAt(i) || '9' < s.charAt(i))
				return false;
		return true;
	}
}
