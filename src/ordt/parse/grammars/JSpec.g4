/*
JSpec antlr4 grammar - snellenbach 1/7/15

Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
*/

grammar JSpec;

@header {
  package ordt.parse.jspec;
}

@lexer::members {
  private static java.util.Set<String> userDefinedParameters = new java.util.HashSet<String>();

  public static void addUserParameter(String parm) {
    userDefinedParameters.add(parm);
    //System.out.println("adding user parameter " + parm + " to set");
  }

  public static boolean isUserParameter(String parm) {
    //System.out.println("user parameter " + parm + " is found=" + userDefinedParameters.contains(parm));
    return userDefinedParameters.contains(parm);
  }
}

root
  : ( num_constant_def
    | string_constant_def
    | type_definition
    | param_type_definition
    | typedef_instance
    | register_set_def
    )* EOF
  ;
  
/* constant EA_FO_DATAPATH_WIDTH "Datapath width" {integer[10] = 512;}; */  
num_constant_def
   : 'constant' id str         
     LBRACE
     (  integer_constant_assign   //  int constant value assign
      | width_constant_assign+  // enum or field constant used for WIDTH()
     )   
     RBRACE
     SEMI 
   ;
   
/* integer[10] = 512; */
integer_constant_assign
   : 'integer' array EQ num_expression
     SEMI 
   ;
   
/* enum [4] { assigns }; -or- integer el_bit[1]  "Excess low Bit"; -or- field_fieldset_id */
width_constant_assign
   : ( 'enum' array LBRACE ~(LBRACE|RBRACE)* RBRACE SEMI 
     | 'integer' id array  str SEMI
     |  id id? str? SEMI    // field or fieldset id
     )
   ;
      
/* constant EA_FO_DATAPATH_WIDTH "Datapath width" {...}; (unsupported constants to be ignored) */  
string_constant_def
   : 'constant' id str         
     LBRACE
     bracket_pair+
     RBRACE
     SEMI 
   ;
   
bracket_pair
   : ~(LBRACE|RBRACE)+ (LBRACE (~(LBRACE|RBRACE) | bracket_pair)? RBRACE)*? ~(LBRACE|RBRACE)*   //  allow nested pairs 
   ;

   
/* typedef enum...  */
type_definition
   : 'typedef'
     ( register_set_def 
     | register_def 
     | field_def 
     | transaction_def
     )
   ;

 param_type_definition
   : 'typedef' 'param'
     id  { JSpecLexer.addUserParameter($id.text); }  // System.out.println("user parameter=" + $id.text); 
     EQ ('string' | 'boolean' | 'integer')
     SEMI 
   ;

/*    enum bla[EA_FO_LOG2_NUM_PORTS] "desc" {
         HSL0 = 0 "HSL channel 0";
         HSL1 = 1 "HSL channel 1";
         };
*/
enum_field_def
   : 'enum' id? array (str | jstr)?
     LBRACE
     (enum_value_assign)+
     RBRACE         
     param_block?
     SEMI 
   ;

/* HSL0 = 0 "HSL channel 0"; */
enum_value_assign
   : id EQ num_expression (str | jstr)
     SEMI 
   ;  
   
/* transaction xtxn_trans "Top level XTXN struct" {...}; NOT SUPPORTED*/
transaction_def
   : 'transaction' id (str | jstr)?
     LBRACE
     bracket_pair+
     RBRACE
     SEMI 
   ;
   
/* val = "bla 0"; */
value_assign
   : assign_parameter EQ
     ( defined_attribute
     | num_expression 
     | str 
     | jstr
     | defined_attribute_set
     )         
     SEMI 
   ; 
   
 /* */   
typedef_instance
   : id id str 
     param_block?
     SEMI
  ;
     
/*    param { reset = TEST; } */
param_block
   : 'param'
     LBRACE
     (value_assign)*
     RBRACE         
   ;
    
assign_parameter
   : 'access_mode'
//   | 'address'   // remove since common comp name
   | 'address_alignment' 
   | 'addr_decode_allow_case' 
   | 'addr_decode_allow_x' 
   | 'addr_decode_instances' 
   | 'addr_decode_fixed_width' 
   | 'addr_decode_lsb' 
   | 'addr_decode_msb' 
   | 'anonymous' 
   | 'attributes' 
   | 'autogen' 
   | 'category' 
   | 'description' 
   | 'macro_name' 
   | 'macro_mode' 
   | 'operational_access_mode' 
   | 'overlay' 
   | 'overlay_tag' 
   | 'overlay_tag_width' 
   | 'pack_msb' 
   | 'pad_msb' 
   | 'register_width' 
   | 'register_set_size' 
//   | 'repeat'   // remove these since they are common comp names 
//   | 'reset' 
//   | 'root' 
   | 'sub_category' 
   | 'superset_check' 
//   | USER_PARAMETER
   | ID // still allow unchecked ID in parse for now, since want some IDs to just pass thru 
   ;  
    
defined_attribute
   : 'unknown'
   | 'true'
   | 'false'
   | 'True'
   | 'False'
   | 'TRUE'
   | 'FALSE'
   | 'READ_WRITE' 
   | 'READ_ONLY' 
   | 'READ_TO_CLEAR' 
   | 'CLEAR_ON_READ' 
   | 'WRITE_ONLY' 
   | 'WRITE_ONE_TO_CLEAR'
   | 'WRITE_ONE_TO_SET' 
   | 'DIAGNOSTIC' 
   | 'ERROR_COUNTER' 
   | 'STAT_COUNTER' 
   | 'CONSTRAINED_CONFIG' 
   | 'DYNAMIC_CONFIG' 
   | 'STATIC_CONFIG' 
   | 'INTERRUPT' 
   | 'DEBUG' 
   | 'INFO' 
   | 'CGATE_UNSAFE' 
   | 'FATAL' 
   | 'MAJOR' 
   | 'STATE' 
   | 'MINOR_RECOVERABLE' 
   | 'MINOR_TRANSIENT' 
   | 'NOEXPORT' 
   | 'JS_ATTRIB_INT_STATUS'
   | 'JS_ATTRIB_DO_NOT_TEST'
   | 'JS_ATTRIB_TEST_ACCESS_ONLY'
   | 'JS_ATTRIB_VOLATILE'
   | 'JS_ATTRIB_EXTERNAL_MEMORY'
   | 'SKIP_CHILDREN'
   | 'SKIP_ADDRESS'
   | 'SKIP_ADDRESS_AND_CHILDREN'
   | 'SKIP_REGISTER_SET_SIZE'
   | 'MANUAL'
   | 'STANDARD'
//   | jspec_attribute
   ;  

//jspec_attribute
//   : 'JS_ATTRIB_' ID
//  ;
     
defined_attribute_set
   : ( LBRACE defined_attribute (COMMA defined_attribute)* RBRACE
     | defined_attribute (OR defined_attribute)*
     )
   ;
         
/*    register_set ea_fo_str "EA_FO per-stream registers" { ... */
register_set_def  // allow constants to be defined in regsets
   : 'register_set' id (str | jstr)
     LBRACE
     (value_assign | register_set_def | register_def 
     | typedef_instance | type_definition | param_type_definition | test_group_def | num_constant_def
     | string_constant_def)+
     RBRACE         
     SEMI 
   ;
   
/*    register ea_fo_str_reg "bla bla register" { ... */
register_def
   : 'register' id (str | jstr)
     LBRACE
     (value_assign | field_def | typedef_instance | type_definition)+
     RBRACE         
     SEMI 
   ;    
   
/*    field deinitions */
field_def
   : field_set_def
   | int_field_def
   | enum_field_def
   | nop_field_def
   ; 
      
/*  field_set ea_fo_mem_protect_fset "Template for EA_FO memory protection registers" {.. */
field_set_def
   : 'field_set' id (str | jstr)
     LBRACE
     (value_assign | int_field_def | enum_field_def | nop_field_def | field_set_def | typedef_instance)*
     RBRACE         
     SEMI 
   ;    
  
/*  integer fake_int[1] "Not a real interrupt" param {.. */
int_field_def
   : 'integer' id? array (str | jstr)?    
     param_block?
     SEMI 
   ; 
      
/*  nop[5]; */
nop_field_def
   : 'nop' array
     param_block?
     SEMI 
   ;    

/* */   
test_group_def
   : 'test_group'
     LBRACE 
     id (COMMA id)* SEMI 
     RBRACE
     SEMI
  ;
   
      
array
  : LSQ num_expression
    RSQ
  ;
  
num_expression
  :   num_expression op=(PLUS | MINUS | STAR | DIV | EXP | LSHIFT |RSHIFT | OR | AND) num_expression
    | LPAREN num_expression RPAREN 
    | 'WIDTH' LPAREN id RPAREN
    | NUM  
    | ID
  ;
  
id
  : ID
  ;
  
jstr
  : JSTR
  ;

str
  : STR
  ;
  

fragment LETTER : ('a'..'z'|'A'..'Z') ;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines

SL_COMMENT
  : ( '//' ~[\r\n]* '\r'? '\n'
    | '#' ~[\r\n]* '\r'? '\n'
    ) -> skip
  ;

ML_COMMENT
    :   ( '/*' .*? '*/'
        ) -> skip
    ;

ID
  : ('\\')?
    (LETTER | '_')(LETTER | '_' | '0'..'9')* /* { if(isUserParameter(getText())) setType(USER_PARAMETER); } */
  ;

USER_PARAMETER
  : 'XUSER_PARAMETERX'  // this rule will never match, but ID can be converted to USER_PARAMETER by predicate
  ;

NUM
  : ('0'..'9')+
  | '0b' ('0' | '1') ('0' | '1' | '_')*
  | '0x' ('0'..'9' | 'a'..'f' | 'A'..'F') ('0'..'9' | 'a'..'f' | 'A'..'F' | '_')*
  ;

fragment ESC_DQUOTE
  : '\\\"'
  ;
  
JSTR
  : LJQUOTE
      .*?
    RJQUOTE
        {
           String s = getText();
           if (s != null) setText(s.replaceAll("\\n#[^\\n]*", "")); /* SL_COMMENTs can be embedded in JSTR - remove these */
           /* if (s.contains("#")) System.out.println("jspec str parse...\n" + s);*/
        }
  ;
  
STR
  : '"'
      ( ~('"' | '\n' | '\\')
      | ESC_DQUOTE
      | '\n'
      )*
    '"'
  ;

LJQUOTE : '"{' ;
RJQUOTE : '}"' ;

LSHIFT : '<<' ;
RSHIFT : '>>' ;
  
LBRACE : '{' ;
RBRACE : '}' ;
LSQ    : '[' ;
RSQ    : ']' ;

LPAREN : '(' ;
RPAREN : ')' ;

AT     : '@' ;
OR     : '|' ;
AND    : '&' ;
SEMI   : ';' ;
COLON  : ':' ;
COMMA  : ',' ;
DOT    : '.' ;
STAR   : '*' ;

EQ     : '=' ;
PLUS   : '+' ;
MINUS  : '-' ;
DIV    : '/' ;
EXP    : '^' ;
DOLLAR : '$' ;