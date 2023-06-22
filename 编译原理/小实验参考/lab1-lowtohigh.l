%{
#include <ctype.h>
#include <stdio.h>
%}

%option noyywrap

%%

"/*""/"*([^*/]|[^*]"/"|"*"[^/])*"*"*"*/" {
	printf("%s",yytext);
}

. {
    for(size_t i = 0; i < yyleng; i++) {
        if(islower(yytext[i])) yytext[i] = toupper(yytext[i]);
    }
    printf("%s", yytext);
}

%%

int main() {
    yylex();
    return 0;
}