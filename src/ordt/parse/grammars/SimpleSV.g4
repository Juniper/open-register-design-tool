/*
SimpleSV grammar - parse of basic systemverilog module header info
*/

grammar SimpleSV;

@header {
  package ordt.output.systemverilog.common.parse.simplesv;
}

root
 : (module_declaration | not_module)* EOF
 ;

module_declaration
 : 'module' module_identifier (parameter_port_list)?
   (list_of_ports (port_declaration | parameter_declaration)* | list_of_port_declarations)
   not_module 
   end_module
 ;
 
end_module
 : 'endmodule' (COLON ID)?
 ;

list_of_ports
 : LPAREN
   port (COMMA port)*
   RPAREN
   SEMI
 ;
 
list_of_port_declarations
 : LPAREN
   port_def (COMMA port_def)*
   RPAREN
   SEMI
 ;

port_def
 : ID (array)? ID
 ;
 
port_declaration
 : port_def SEMI
 ;
   
port
 : port_identifier (LSQ ~(LSQ | RSQ)+ RSQ)?
 ;
 
port_identifier
 : ID
 ;
 
parameter_port_list
 : HASH LPAREN
   parameter_def_list?
   RPAREN
 ;
 
parameter_def_list
 : 'parameter' parameter_def (COMMA parameter_def)*
 ;
 
parameter_def
 :  parameter_identifier (array)? (EQ NUM)?
 ;
 
parameter_declaration
 : 'parameter' parameter_def SEMI
 ;
 
parameter_identifier
 : ID
 ;
 
array
  : LSQ array_num
    (COLON array_num)?
    RSQ
  ;
  
array_num  // allow simple parameter equations
  : LPAREN array_num RPAREN
   | array_num (PLUS | MINUS | STAR | LSHIFT | RSHIFT) array_num
   | ID
   | NUM
  ;
 
module_identifier
 : ID
 ;
 
not_module
 : ~('module')+
 ;
 
WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines

SL_COMMENT
  : ( '//' ~[\r\n]* '\r'? '\n'
    ) -> skip
  ;

ML_COMMENT
    :   ( '/*' .*? '*/'
        ) -> skip
    ;

fragment LETTER : ('a'..'z'|'A'..'Z') ;

ID
  : (LETTER | '_')(LETTER | '_' | '0'..'9')* // { System.out.println("SimpleSV ID: " + getText()); }
  ;
 
fragment VNUM
  : '\'' ( 'b' ('0' | '1' | '_')+
         | 'd' ('0'..'9' | '_')+
         | 'o' ('0'..'7' | '_')+
         | 'h' ('0'..'9' | 'a'..'f' | 'A'..'F' | '_')+
         )
  ;

NUM
  : ('0'..'9')* (VNUM | ('0'..'9'))
  | '0x' ('0'..'9' | 'a'..'f' | 'A'..'F')+
  ;

fragment ESC_DQUOTE
  : '\\\"'
  ;

STR
  : '"'
      ( ~('"' | '\n' | '\\')
      | ESC_DQUOTE
      | '\n'
      )*
    '"' // "
  ;

LBRACE : '{' ;
RBRACE : '}' ;
LSQ    : '[' ;
RSQ    : ']' ;

LPAREN : '(' ;
RPAREN : ')' ;

AT     : '@' ;
AND    : '&';
OR     : '|' ;
SEMI   : ';' ;
COLON  : ':' ;
COMMA  : ',' ;
DOT    : '.' ;
STAR   : '*' ;
HASH   : '#' ;
QMARK  : '?' ;
PLUS   : '+' ;
MINUS  : '-' ;
DOLLAR : '$' ;
EXCL   : '!' ;

NB_EQ  : '<=' ;
EQ     : '=' ;
LSHIFT : '<<';
RSHIFT : '>>';
LT     : '<';
GT     : '>';
CARET  : '^';
TILDE  : '~';