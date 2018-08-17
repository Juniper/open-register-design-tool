/*
The following code is an extension of the SystemRDL 1.0 grammar provided for use by Accellera under Apache 2.0 license.  
See http://accellera.org/downloads/standards/systemrdl for additional info.

----------------------


Changes:
- converted format from antlr v2 to antlr v4
- changed to skip comments since antlr4 allows
- removed bogus c++ method calls embedded in antlr2 code
- fixed issue with explicit_property_assign to allow assignment w/o rhs
- made external, internal, and alias mutually exclusive in explicit_component_inst
- added wildcard field option in instance_ref
- added incrsaturate and incrthreshold as valid aliases for saturate and threshold
- added property_modifier to instance_ref so interrupt modifiers can be assigned
- added inline ordt control parameter grammar support (import ExtParms)
- added unimplemented_property rule under property
- added category, subcategory, js_attributes properties for jspec translation
- added uvmreg_is_mem property for uvm model gen
- added rshift address operator to create address gaps 
- added use_interface, use_new_interface properties to enable systemverilog IO interface encaps
- added external_decode option to create holes in map due to external decoder use
- added external type clause to allow selection of alternate external interfaces
- added rtl_coverage property
- added nextposedge, nextnegedge edge properties for use in rhs assigns
- added satoutput property to explicitly control counter sat output gen (no sat output by default)
- added verilog_expression to allow signal assigns using simple logic expressions
- added predicates/lexer methods to correctly identify user-defined property ID tokens
- added uvmreg_prune register property
- added fieldstruct component type
- added use_struct, use_new_struct properties for systemverilog IO interface encaps
- added js_macro_name, js_macro_mode, js_namespace parms
- implemented_rdl_property define is moved to ExtParams
- allowed repeat define of same user property in parser - flagged as warn later
- made component instance address order more flexible to allow increment after modulus
- added field_data and rep_level external options for PARALLEL and SRAM external types
*/

grammar SystemRDL;

import CommonExtParms;

@header {
  package ordt.parse.systemrdl;
}

@lexer::members {
  private static java.util.Set<String> userDefinedProperties = new java.util.HashSet<String>();

  public static void addUserProperty(String prop) {
    userDefinedProperties.add(prop);
    //System.out.println("adding user property " + prop + " to set");
  }

  public static boolean isUserProperty(String prop) {
    //System.out.println("user property " + prop + " is found=" + userDefinedProperties.contains(prop));
    return userDefinedProperties.contains(prop);
  }

}

root
  : ( parameter_block
    | component_def
    | enum_def
    | explicit_component_inst
    | property_assign
    | property_definition
    )* EOF
  ;

 parameter_block
   : '<PARMS>' ext_parm_defs '</PARMS>'
   ;
    
 property_definition
   : 'property'
     (id | PROPERTY)  { SystemRDLLexer.addUserProperty($id.text); }  // System.out.println("user property=" + $id.text); 
     LBRACE
     property_body
     RBRACE
     SEMI 
   ;

property_body
  :  property_type ( property_usage (property_default)?
                   | property_default property_usage
                   )
  |  property_usage ( property_type (property_default)?
                    | property_default property_type
                    )
  |  property_default ( property_type  property_usage
                      | property_usage property_type
                      )
  ;

property_type
  :
    'type' EQ
    ( property_string_type
    | property_number_type
    | property_boolean_type
    | property_ref_type
    ) SEMI
  ;

property_default
  :
    (
    'default' EQ (str | num | 'true' | 'false')
    SEMI
    )
  ;

property_usage
  :
    'component' EQ property_component (OR property_component)* SEMI
  ;

property_component
  : ('signal' | 'addrmap' | 'reg' | 'regfile' | 'field' | 'fieldstruct' | 'all')
  ;

property_boolean_type
  : 'boolean'
  ;

property_string_type
  : 'string'
  ;

property_number_type
  : 'number'
  ;

property_ref_type
  : ('addrmap' | 'reg' | 'regfile'  | 'field' | 'fieldstruct' | 'ref')
  ;

component_def
  : ('addrmap' | 'reg' | 'regfile' | 'field' | 'fieldstruct' | 'signal' )
    ( id
    |
    )
    LBRACE
      ( component_def
      | explicit_component_inst
      | property_assign
      | enum_def
      )*
    RBRACE
    ( anonymous_component_inst_elems )?
    SEMI
  ;

explicit_component_inst
  : ( external_clause 
    | 'internal' 
    | 'alias' id )?
    id
    component_inst_elem
    (COMMA component_inst_elem)*
    SEMI
  ;

anonymous_component_inst_elems
  : (external_clause)?
    component_inst_elem
    (COMMA component_inst_elem)*
  ;

external_clause
  : ( 'external_decode' 
    | 'external' ( LPAREN ( 'DEFAULT' | external_parallel_clause | 'BBV5_8' | 'BBV5_16' | 
                          external_sram_clause | external_serial8_clause | external_ring_clause ) RPAREN )?
    )
  ;

external_parallel_clause
  : 'PARALLEL' (external_opt_option_clause | external_field_data_option_clause | external_rep_level_option_clause)*
  ;

external_sram_clause
  : 'SRAM' (external_field_data_option_clause | external_rep_level_option_clause)*
  ;

external_serial8_clause
  : SERIAL8 (external_dly_option_clause)?
  ;
  
external_ring_clause
  : RING (external_dly_option_clause)?
  ;
  
external_dly_option_clause
  : 'dly' EQ num
  ;
  
external_opt_option_clause
  : 'opt' EQ ('YES' | 'NO' | 'KEEP_NACK')
  ;
  
external_field_data_option_clause
  : 'field_data' EQ ('YES' | 'NO')
  ;
  
external_rep_level_option_clause
  : 'rep_level' EQ num
  ;

component_inst_elem
  : id
    (array)?
    (EQ  num)?   // reset
    ( ( ((AT | RSHIFT | MOD)  num)? (INC num)? )   // base address select followed by increment
    | ( (INC num)? ((AT | RSHIFT | MOD)  num)? ) )   // or increment followed by base address select
  ;

array
  : LSQ num
    (COLON num)?
    RSQ
  ;

instance_ref
  : instance_ref_elem
    (DOT instance_ref_elem)*
    (DOT STAR)?                // added STAR here to allow field wildcards
    ( DREF (property | property_modifier) )?   // added property_modifier
  ;
  
simple_instance_ref  // added for differentiating signal assign
  : instance_ref_elem
    (DOT instance_ref_elem)*
  ;
  
verilog_expression  // added - allow simple vlog expression
  : verilog_expression (AND | OR | CARET | LSHIFT | RSHIFT) verilog_expression
  | LPAREN verilog_expression RPAREN 
  | LBRACE (verilog_expression COMMA)* verilog_expression RBRACE 
  | ( ((TILDE | AND | OR)? instance_ref array?) | NUM)
  ;

instance_ref_elem
  : id
    (LSQ num RSQ)?
  ;

property_assign
  : default_property_assign SEMI
  | explicit_property_assign SEMI
  | post_property_assign SEMI
  ;

default_property_assign
  : 'default'
    explicit_property_assign
  ;

explicit_property_assign
  : property_modifier
    property

  | property
    ( EQ property_assign_rhs )?   // added ? here to allow properties w/o an EQ (=true)
  ;

post_property_assign
  : instance_ref ( EQ property_assign_rhs )
  | simple_instance_ref ( EQ verilog_expression )  // allow vlog expression use in signal assigns 
  ;

property_assign_rhs
  : property_rvalue_constant
  | 'enum' enum_body
  | instance_ref
//  | concat
  ;

concat
  : LBRACE
    concat_elem
    (COMMA concat_elem)*
    RBRACE
  ;

concat_elem
  : instance_ref
  | num
  ;

property
  : implemented_rdl_property   // this is defined in ExtParms since annotation command needs it also
  | unimplemented_property  // added
  | PROPERTY  // {System.out.println("user defined property found!");}
  ;

unimplemented_property
  : 'arbiter'

  | 'sharedextbus'
  | 'errextbus'

  | 'littleendian'
  | 'bigendian'
  | 'rsvdset'
  | 'rsvdsetX'
  | 'bridge'
  | 'shared'
  | 'msb0'
  | 'lsb0'
  | 'sync'
  | 'async'

  | 'internal'

  | 'alignment'
  | 'accesswidth'

  | 'addressing'

  | 'clock'

  | 'hwenable'
  | 'hwmask'

  ;

property_rvalue_constant
  : 'true'
  | 'false'

  | 'rw'
  | 'wr'
  | 'r'
  | 'w'
  | 'na'

  | 'compact'
  | 'regalign'
  | 'fullalign'

  | 'hw'
  | 'sw'

  | num
  | str
  ;

property_modifier
  : 'posedge'
  | 'negedge'
  | 'bothedge'
  | 'level'
  | 'nonsticky'
  ;

id
  : ID
  ;

num
  : NUM
  ;

str
  : STR
  ;

enum_def
  : 'enum' id
    enum_body
    SEMI
  ;

enum_body
  : LBRACE (enum_entry)* RBRACE
  ;

enum_entry
  : id
    EQ num
    ( LBRACE (enum_property_assign)* RBRACE )?
    SEMI
  ;

enum_property_assign
  : ( 'name'
    | 'desc'
    )
    EQ str
    SEMI
  ;

fragment LETTER : ('a'..'z'|'A'..'Z') ;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines


SL_COMMENT
  : ( '//' ~[\r\n]* '\r'? '\n'
    ) -> skip
  ;


ML_COMMENT
    :   ( '/*' .*? '*/'
        ) -> skip
    ;

// handle serial and ring external mode options as tokens so no rdl keywords are added
SERIAL8
  : 'SERIAL8' ('_D' '0'..'9')?
  ;
  
RING
  : 'RING' ('8' | '16' | '32') ('_D' '0'..'9')?
  ;

ID
  : ('\\')?
    (LETTER | '_')(LETTER | '_' | '0'..'9')* { if(isUserProperty(getText())) setType(PROPERTY); }
  ;

PROPERTY
  : 'XPROPERTYX'  // this rule will never match, but ID can be converted to PROPERTY by predicate
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
OR     : '|' ;
SEMI   : ';' ;
COLON  : ':' ;
COMMA  : ',' ;
DOT    : '.' ;
STAR   : '*' ;

DREF   : '->';

EQ     : '=' ;
INC    : '+=';
MOD    : '%=';
LSHIFT : '<<';
RSHIFT : '>>';
CARET  : '^';
TILDE  : '~';
AND    : '&';
