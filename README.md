# Crafting Interpreters

## A Tree-Walker Interpreter

## Some differences

- Java 25 implementation of the book
- Use of records with interfaces instead of abstract class with inheritance

## Lexical Grammar

```
NUMBER      → DIGIT+ ( "." DIGIT+ )? ;
STRING      → "\"" <any char except "\"">* "\"" ;
IDENTIFIER  → ALPHA ( ALPHA | DIGIT )* ;
ALPHA       → "a" ... "z" | "A" ... "Z" | "_" ;
DIGIT       → "0" ... "9" ;
```

## Syntax Grammar

### Statements

```
program     → declaration* EOF ;

declaration → classDecl
            | funDecl
            | varDecl
            | statement ;

clasDecl      "class" IDENTIFIER "{" function* "}"

funDecl     → "fun" function ;

varDecl     → "var" IDENTIFIER ( "=" expression )? ";" ;

statement   → exprStmt
            | forStmt
            | isStmt
            | printStmt
            | returnStmt
            | whileStmt
            | block ;

returnStmt  → "return" expression? ";" ;

forStmt     → "for" "(" ( varDecl | exprStmt | ";") expression? ";" expression? ")"
               statement ;

whileStmt   → "while" "(" expression ")" statement ;

ifStmt      → "if" "(" expression ")" statement
              ("else" statement)? ;

block       → "{" declaration* "}" ;

exprStmt    → expression ";" ;

printStmt   → "print" expression ";" ;
```

### Expressions

```
expression  → assignment ;

assignment  → (call ".")? IDENTIFIER "=" assignment
            | logic_or ;

logic_or    → logic_and ( "or" logic_and )* ;
logic_and   → equality ( "and" equality )* ;
equality    → comparison ( ("!=" | "==") comparison )* ;
comparison  → term ( ( ">" | ">=" | "<" | "<=") term )* ;
term        → factor ( ( "-" | "+" ) factor)* ;
factor      → unary ( ( "/" | "*" ) unary)* ;

unary       → ( "!" | "-") unary | call ;
call        → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
primary     → "true" | "false" | "nil"
            | NUMBER | STRING
            | "(" expression ")"
            | IDENTIFIER ;
```

### Utility Rules

```
parameters  → IDENTIFIER ( "," IDENTIFIER )* ;
arguments   → expression ( "," expression )* ;
function    → IDENTIFIER "(" parameters? ")" block ;
```
