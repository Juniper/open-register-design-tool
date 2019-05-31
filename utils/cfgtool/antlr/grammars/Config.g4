/*
config grammar
*/

grammar Config;

root
  : (  ml_comment | class_def )*
    EOF
  ;

class_def
  : 'configClass' id_str parameter_list_def
    LBRACE
      version_check_statement?
      ( ml_comment | method_def )*
    RBRACE
  ;
    

method_def
  : 'method' id_str parameter_list_def
    LBRACE 
      version_check_statement?
      block_statement*
    RBRACE
  ;

version_check_statement
  : 'allowVersions' LBRACE id_str (COMMA id_str)* RBRACE
  ;
  
block_statement
  :  config_call
   | for_loop
   | while_loop
   | if_block
   | case_statement
   | method_call
   | class_assign
   | simple_assign
  ; 

class_assign
  : path_elem EQ 'new' id_str LPAREN call_parms RPAREN
  ;
  
simple_assign
  : id_str EQ value
  ;
   
parameter_list_def
  : LPAREN
     (parameter_def (COMMA parameter_def)*)?
    RPAREN
  ;
  
parameter_def
  : ( path_def
    | value_def
    | enum_def
    | bool_def )
  ;
  
path_def
  : 'path' id_str
  ;
  
value_def
  : 'val' id_str
  ;
  
enum_def
  : 'enum' id_str
  ;
  
bool_def
  : 'bool' id_str
  ;
  
config_call
  :  write_config_call
   | read_config_call
   | poll_config_call
  ;
  
write_config_call
  : ('writeReg' | 'writeField' | 'rmwField') LPAREN path COMMA value RPAREN
  ;
  
read_config_call
  : id_str EQ ('readReg' | 'readField') LPAREN path RPAREN
  ;
  
poll_config_call
  : ('pollRegWhile' | 'pollFieldWhile') LPAREN path compare_op value (COMMA value (COMMA value)? )? RPAREN
  ;
  
method_call
  : (path DOT)* path_elem LPAREN call_parms RPAREN
  ;

call_parms
  : (call_parm (COMMA call_parm)* )*
  ;
  
call_parm
  : path | num
  ;
  
for_loop
  : 'for' LPAREN id_str COLON (num_range | path) RPAREN
    LBRACE block_statement* RBRACE
  ;
  
while_loop
  : 'while' LPAREN value compare_op value RPAREN
    LBRACE block_statement* RBRACE
  ;
  
if_block
  : 'if' LPAREN value compare_op value RPAREN
    LBRACE block_statement* RBRACE
    else_block?
  ;

else_block
  : 'else'
    LBRACE block_statement* RBRACE
  ;
  
case_statement
  : 'case' LPAREN id_str RPAREN
    LBRACE 
      labeled_case_block+ 
      default_case_block
    RBRACE
  ;
  
labeled_case_block
  : id_str COLON block_statement*
  ;
  
default_case_block
  : 'default' COLON block_statement*
  ;
  
num_range
  : value DBLDOT value
  ;
  
path
  : path_elem (DOT path_elem)* 
  ;

path_elem
  : ('path' | 'val' | 'int' | 'new' | id_str) path_elem_range*  // allow unescaped keywords in path
  ;

path_elem_range
  : LSQ (value | STAR) (COLON (value | STAR))* RSQ
  ;

value
  : num | id_str
  ;
  
compare_op
  : DBLEQ | NE | LT | GT | LE | GE
  ;

id_str
  : ID
  ;

num
  : NUM
  ;
  
ml_comment
  : ML_COMMENT
  ;

fragment LETTER : ('a'..'z'|'A'..'Z') ;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines


SL_COMMENT
  : ( '//' ~[\r\n]* '\r'? '\n'
    ) -> skip
  ;


ML_COMMENT
    :   ( '/*' .*? '*/'
        )
    ;

ID
  : ('\\')?
    (LETTER | '_')(LETTER | '_' | '0'..'9')*
  ;

NUM
  : ('0'..'9')* ('0'..'9')
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
    '"'
  ;

LBRACE : '{' ;
RBRACE : '}' ;
LSQ    : '[' ;
RSQ    : ']' ;

LPAREN : '(' ;
RPAREN : ')' ;

SEMI   : ';' ;
COLON  : ':' ;
COMMA  : ',' ;
DOT    : '.' ;
STAR   : '*' ;

EQ     : '=' ;
GT     : '>';
LT     : '<';
DBLEQ  : '==' ;
NE    : '!=';
GE    : '>=';
LE    : '<=';
DBLDOT : '..' ;
