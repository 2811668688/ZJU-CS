/* 中缀符号计算器 */

%{
	#define YYSTYPE double
	#include <math.h>
	#include <stdio.h>
	#include <ctype.h>
	int yylex(void);
	void yyerror(char const *);
%}

%token NUM /* 定义token */
%left '-' '+'
%left '*' '/'
%left NEG       /* 定义加减乘除反运算左结合 */
%right '^'        /* 定义乘方运算右结合 */

%%                /* 以下是语法 */
input:             /* empty */
	| input line
;

line: 	'\n'
	| exp '\n'  { printf ("\t%.10g\n", $1); }
;

exp:	NUM		 { $$ = $1; }
	| exp '+' exp { $$ = $1 + $3; }
	| exp '-' exp  { $$ = $1 - $3;  }
	| exp '*' exp  { $$ = $1 * $3;  }
	| exp '/' exp  { $$ = $1 / $3;  }
	| '-' exp  %prec NEG { $$ = -$2; }
	| exp '^' exp { $$ = pow ($1, $3);  }
	| '(' exp ')'      { $$ = $2;  }
;
%%

int yylex(void) {
	int c;
	while((c = getchar()) == ' ' || c == '\t');
	if(c == '.' || isdigit(c)) {
		ungetc(c, stdin);
		scanf("%lf", &yylval);
		return NUM;
	}
	if (c == EOF) return 0;
	return c;
}

void yyerror(char const *s) {
	fprintf (stderr, ": %s", s);
}

int main(void) {
	return yyparse();
}