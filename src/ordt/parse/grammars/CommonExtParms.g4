// ordt parameter grammar - used in external parameter files as well as parameters embedded in rdl
//
// Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.

grammar CommonExtParms;

ext_parms_root
  :  ext_parm_defs
     EOF
  ;

ext_parm_defs
  : ( global_defs
    | rdl_in_defs
    | jspec_in_defs
    | rdl_out_defs
    | jspec_out_defs
    | systemverilog_out_defs
    | uvmregs_out_defs
    | reglist_out_defs
    | bench_out_defs
    | xml_out_defs
 //   | cppmod_out_defs
    | pydrvmod_out_defs
    | model_annotation
    )*
  ;

// ------------ global_defs
 global_defs
   : 'global'
     LBRACE
     (global_parm_assign)*
     RBRACE
   ;

 global_parm_assign
   : 'min_data_size' EQ NUM
   | 'base_address' EQ NUM
   | 'use_js_address_alignment' EQ bool
   | 'suppress_alignment_warnings' EQ bool
   | 'default_base_map_name' EQ STR
   | 'allow_unordered_addresses' EQ bool
   | 'debug_mode' EQ STR
   ;

// ------------ rdl_in_defs
 rdl_in_defs
   : 'input' 'rdl'
     LBRACE
     (rdl_in_parm_assign)*
     RBRACE
   ;
   
 rdl_in_parm_assign
   : 'process_component' EQ STR
   | 'resolve_reg_category' EQ bool
   | 'restrict_defined_property_names' EQ bool
   | 'default_rw_hw_access' EQ bool
   ;
   
// ------------ jspec_in_defs
 jspec_in_defs
   : 'input' 'jspec'
     LBRACE
     (jspec_in_parm_assign)*
     RBRACE
   ;
   
 jspec_in_parm_assign
   : 'process_typedef' EQ STR
   | 'root_regset_is_addrmap' EQ bool
   | 'root_is_external_decode' EQ bool // deprecate??
   | 'external_replication_threshold' EQ NUM
   ;
   
// ------------ rdl_out_defs
 rdl_out_defs
   : 'output' 'rdl'
     LBRACE
     (rdl_out_parm_assign)*
     RBRACE
   ;
   
 rdl_out_parm_assign
   : 'root_component_is_instanced' EQ bool
   | 'output_jspec_attributes' EQ bool
   | 'no_root_enum_defs' EQ bool
   ;

// ------------ jspec_out_defs
 jspec_out_defs
   : 'output' 'jspec'
     LBRACE
     (jspec_out_parm_assign)*
     RBRACE
   ;
   
 jspec_out_parm_assign
   : 'root_regset_is_instanced' EQ bool
   | 'external_decode_is_root' EQ bool // deprecate??
   | 'add_js_include' EQ STR
   | 'root_typedef_name' EQ STR
   | 'root_instance_name' EQ STR
   | 'root_instance_repeat' EQ NUM
   | 'add_user_param_defines' EQ bool
   | 'keep_fset_hierarchy' EQ bool
   ;
   
// ------------ systemverilog_out_defs
 systemverilog_out_defs
   : 'output' 'systemverilog'
     LBRACE
     ( systemverilog_out_parm_assign
     | systemverilog_wrapper_info )*
     RBRACE
   ;
   
 systemverilog_out_parm_assign
   : 'leaf_address_size' EQ NUM
   | 'root_has_leaf_interface' EQ bool 
   | 'root_decoder_interface' EQ ('leaf' | 'parallel' | 'parallel_pulsed' | 'serial8' | 'ring8' | 'ring16' | 'ring32' | 'spi_PIO') 
   | 'secondary_decoder_interface' EQ ('none' | 'leaf' | 'parallel' | 'parallel_pulsed' | 'serial8' | 'ring8' | 'ring16' | 'ring32' | 'engine1' | 'spi_PIO') 
   | 'secondary_base_address' EQ NUM 
   | 'secondary_low_address' EQ NUM 
   | 'secondary_high_address' EQ NUM 
   | 'secondary_on_child_addrmaps' EQ bool
   | 'base_addr_is_parameter' EQ bool 
   | 'module_tag' EQ STR 
   | 'use_gated_logic_clock' EQ bool 
   | 'gated_logic_access_delay' EQ NUM 
   | 'use_external_select' EQ bool 
   | 'block_select_mode' EQ ('internal' | 'external' | 'always') 
   | 'export_start_end' EQ bool 
   | 'always_generate_iwrap' EQ bool 
   | 'suppress_no_reset_warnings' EQ bool
   | 'generate_child_addrmaps' EQ bool
   | 'ring_inter_node_delay' EQ NUM
   | 'bbv5_timeout_input' EQ bool
   | 'include_default_coverage' EQ bool
   | 'generate_external_regs' EQ bool   // also allowed in bench output
   | 'child_info_mode' EQ ('perl' | 'module') 
   | 'pulse_intr_on_clear' EQ bool
   | 'reuse_iwrap_structures' EQ bool
   | 'optimize_parallel_externals' EQ bool
   | 'use_async_resets' EQ bool
   | 'nack_partial_writes' EQ bool
   | 'write_enable_size' EQ NUM
   | 'max_internal_reg_reps' EQ NUM
   | 'separate_iwrap_encap_files' EQ bool 
   | 'generate_dv_bind_modules' EQ bool 
   | 'use_global_dv_bind_controls' EQ bool 
   | 'include_addr_monitor' EQ bool 
   | 'generate_iwrap_xform_modules' EQ bool 
   | 'include_sequential_assign_delays' EQ bool
   | 'reset_all_outputs' EQ bool
   ;
   
 systemverilog_wrapper_info
   : 'wrapper_info'
     LBRACE
     ( systemverilog_wrapper_remap_command )+
     RBRACE
   ;
   
 systemverilog_wrapper_remap_command
   : 'set_passthru' STR
   | 'set_invert' STR
   | 'add_sync_stages' STR NUM ID? ID?  // sig_match num_stages clock module_override
//   | 'set_async_data' STR STR NUM ID 
   ;
      
// ------------ uvmregs_out_defs
 uvmregs_out_defs
   : 'output' 'uvmregs'
     LBRACE
     (uvmregs_out_parm_assign)*
     RBRACE
   ;
   
 uvmregs_out_parm_assign
   : 'is_mem_threshold' EQ NUM
   | 'suppress_no_category_warnings' EQ bool
   | 'include_address_coverage' EQ bool
   | 'max_reg_coverage_bins' EQ NUM
   | 'reuse_uvm_classes' EQ bool
   | 'skip_no_reset_db_update' EQ bool
   | 'uvm_model_mode' EQ ('heavy' | 'lite1' | 'native') 
   | 'regs_use_factory' EQ bool
   | 'use_numeric_uvm_class_names' EQ bool
   | 'uvm_mem_strategy' EQ ('basic' | 'block_wrapped' | 'mimic_reg_api') 
   | 'base_address_override' EQ NUM
   | 'use_module_path_defines' EQ bool
   ;   
   
// ------------ reglist_out_defs
 reglist_out_defs
   : 'output' 'reglist'
     LBRACE
     (reglist_out_parm_assign)*
     RBRACE
   ;  
   
 reglist_out_parm_assign
   : 'display_external_regs' EQ bool
   | 'show_reg_type' EQ bool 
   | 'match_instance' EQ STR 
   | 'show_fields' EQ bool 
   ;
   
// ------------ bench_out_defs
 bench_out_defs
   : 'output' 'bench'
     LBRACE
     (bench_out_parm_assign)*
     RBRACE
   ;  
   
 bench_out_parm_assign
   : 'add_test_command' EQ STR
   | 'generate_external_regs' EQ bool 
   | 'only_output_dut_instances' EQ bool 
   | 'total_test_time' EQ NUM 
   ;
   
// ------------ xml_out_defs
 xml_out_defs
   : 'output' 'xml'
     LBRACE
     (xml_out_parm_assign)*
     RBRACE
   ;  
   
 xml_out_parm_assign
   : 'include_field_hw_info' EQ bool 
   | 'include_component_info' EQ bool 
   ;
   
/*   
// ------------ cppmod_out_defs
 cppmod_out_defs
   : 'output' 'cppmod'
     LBRACE
     (cppmod_out_parm_assign)*
     RBRACE
   ;  
   
 cppmod_out_parm_assign
   : 'set_no_model' EQ STR
   | 'set_lite_model' EQ STR
   | 'set_full_model' EQ STR
   ;
*/

// ------------ pydrvmod_out_defs
 pydrvmod_out_defs
   : 'output' 'pydrvmod'
     LBRACE
     (pydrvmod_out_parm_assign)*
     RBRACE
   ;  
   
 pydrvmod_out_parm_assign
   : 'default_tag_name' EQ STR
   ;

// ------------ model_annotation
 model_annotation
   : 'annotate'
     LBRACE
     (annotation_command)*
     RBRACE
   ;

// ------------ annotation_command
 annotation_command
   : ('set_reg_property' | 'set_field_property' | 'set_fieldset_property' | 'set_regset_property')
     'default'? (ID | implemented_rdl_property) EQ STR  // external is a parms keyword, so special case ID
     ('instances' | 'components')
     STR
   ;
 
 // ------------ define all implemented rdl propertied here since they're needed for inline annotate
 implemented_rdl_property
  : 'name'
  | 'desc'
//| 'arbiter'
  | 'rset'
  | 'rclr'
  | 'woclr'
  | 'woset'

  | 'we'
  | 'wel'

  | 'swwe'
  | 'swwel'

  | 'hwset'
  | 'hwclr'

  | 'swmod'
  | 'swacc'

  | 'sticky'
  | 'stickybit'
  | 'intr'

  | 'anded'
  | 'ored'
  | 'xored'

  | 'counter'
  | 'overflow'

//| 'sharedextbus'
//| 'errextbus'

  | 'reset'

//| 'littleendian'
//| 'bigendian'
//| 'rsvdset'
//| 'rsvdsetX'
//| 'bridge'
//| 'shared'
//| 'msb0'
//| 'lsb0'
//| 'sync'
//| 'async'
  | 'cpuif_reset'
  | 'field_reset'
  | 'activehigh'
  | 'activelow'
  | 'singlepulse'
  | 'underflow'

  | 'incr'
  | 'decr'

  | 'incrwidth'
  | 'decrwidth'

  | 'incrvalue'
  | 'decrvalue'

  | 'saturate'
  | 'incrsaturate'
  | 'decrsaturate'

  | 'threshold'
  | 'incrthreshold'
  | 'decrthreshold'

  | 'dontcompare'
  | 'donttest'
//| 'internal'

//| 'alignment'
  | 'regwidth'
  | 'fieldwidth'
  | 'signalwidth'
//| 'accesswidth'

  | 'sw'
  | 'hw'
//| 'addressing'
  | 'precedence'

  | 'encode'
  | 'resetsignal'
//| 'clock'

  | 'mask'
  | 'enable'

//| 'hwenable'
//| 'hwmask'

  | 'haltmask'
  | 'haltenable'


  | 'halt'

  | 'next'

  | 'nextposedge'   // added
  | 'nextnegedge'   // added
  | 'maskintrbits'   // added  
  | 'satoutput'   // added

  | 'category'   // added
  | 'sub_category'   // added
  
  | 'js_attributes'   // added
  | 'js_superset_check'   // added
  | 'js_macro_name'   // added
  | 'js_macro_mode'   // added
  | 'js_namespace'   // added
  | 'js_repeat_max'   // added
  | 'js_typedef_name'   // added   deprecate?
  | 'js_instance_name'   // added   deprecate?
  | 'js_instance_repeat'   // added   deprecate?
  
  | 'fieldstructwidth'  // added

  | 'rtl_coverage'   // added

  | 'uvmreg_is_mem'   // added
  | 'uvmreg_prune'   // added
  
  | 'use_new_interface'   // added
  | 'use_interface'   // added
  | 'use_new_struct'   // added
  | 'use_struct'   // added

  | 'cppmod_prune'   // added

  | hwload_property   // added
  ;  
  
// ------------

hwload_property
  : 'hwload' ( LPAREN NUM RPAREN )?
  ;
   
bool
  : ('true' | 'false')
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


ID
  : ('\\')?
    (LETTER | '_')(LETTER | '_' | '0'..'9')*
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

LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
EQ     : '=' ;
