// Generated from /Users/snellenbach/git/open-register-design-tool/src/ordt/parse/grammars/SystemRDL.g4 by ANTLR 4.5.1
package ordt.parse.systemrdl;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SystemRDLParser}.
 */
public interface SystemRDLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#root}.
	 * @param ctx the parse tree
	 */
	void enterRoot(SystemRDLParser.RootContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#root}.
	 * @param ctx the parse tree
	 */
	void exitRoot(SystemRDLParser.RootContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#parameter_block}.
	 * @param ctx the parse tree
	 */
	void enterParameter_block(SystemRDLParser.Parameter_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#parameter_block}.
	 * @param ctx the parse tree
	 */
	void exitParameter_block(SystemRDLParser.Parameter_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_definition}.
	 * @param ctx the parse tree
	 */
	void enterProperty_definition(SystemRDLParser.Property_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_definition}.
	 * @param ctx the parse tree
	 */
	void exitProperty_definition(SystemRDLParser.Property_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_body}.
	 * @param ctx the parse tree
	 */
	void enterProperty_body(SystemRDLParser.Property_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_body}.
	 * @param ctx the parse tree
	 */
	void exitProperty_body(SystemRDLParser.Property_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_type}.
	 * @param ctx the parse tree
	 */
	void enterProperty_type(SystemRDLParser.Property_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_type}.
	 * @param ctx the parse tree
	 */
	void exitProperty_type(SystemRDLParser.Property_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_default}.
	 * @param ctx the parse tree
	 */
	void enterProperty_default(SystemRDLParser.Property_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_default}.
	 * @param ctx the parse tree
	 */
	void exitProperty_default(SystemRDLParser.Property_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_usage}.
	 * @param ctx the parse tree
	 */
	void enterProperty_usage(SystemRDLParser.Property_usageContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_usage}.
	 * @param ctx the parse tree
	 */
	void exitProperty_usage(SystemRDLParser.Property_usageContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_component}.
	 * @param ctx the parse tree
	 */
	void enterProperty_component(SystemRDLParser.Property_componentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_component}.
	 * @param ctx the parse tree
	 */
	void exitProperty_component(SystemRDLParser.Property_componentContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_boolean_type}.
	 * @param ctx the parse tree
	 */
	void enterProperty_boolean_type(SystemRDLParser.Property_boolean_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_boolean_type}.
	 * @param ctx the parse tree
	 */
	void exitProperty_boolean_type(SystemRDLParser.Property_boolean_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_string_type}.
	 * @param ctx the parse tree
	 */
	void enterProperty_string_type(SystemRDLParser.Property_string_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_string_type}.
	 * @param ctx the parse tree
	 */
	void exitProperty_string_type(SystemRDLParser.Property_string_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_number_type}.
	 * @param ctx the parse tree
	 */
	void enterProperty_number_type(SystemRDLParser.Property_number_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_number_type}.
	 * @param ctx the parse tree
	 */
	void exitProperty_number_type(SystemRDLParser.Property_number_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_ref_type}.
	 * @param ctx the parse tree
	 */
	void enterProperty_ref_type(SystemRDLParser.Property_ref_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_ref_type}.
	 * @param ctx the parse tree
	 */
	void exitProperty_ref_type(SystemRDLParser.Property_ref_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#component_def}.
	 * @param ctx the parse tree
	 */
	void enterComponent_def(SystemRDLParser.Component_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#component_def}.
	 * @param ctx the parse tree
	 */
	void exitComponent_def(SystemRDLParser.Component_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#explicit_component_inst}.
	 * @param ctx the parse tree
	 */
	void enterExplicit_component_inst(SystemRDLParser.Explicit_component_instContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#explicit_component_inst}.
	 * @param ctx the parse tree
	 */
	void exitExplicit_component_inst(SystemRDLParser.Explicit_component_instContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#anonymous_component_inst_elems}.
	 * @param ctx the parse tree
	 */
	void enterAnonymous_component_inst_elems(SystemRDLParser.Anonymous_component_inst_elemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#anonymous_component_inst_elems}.
	 * @param ctx the parse tree
	 */
	void exitAnonymous_component_inst_elems(SystemRDLParser.Anonymous_component_inst_elemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#external_clause}.
	 * @param ctx the parse tree
	 */
	void enterExternal_clause(SystemRDLParser.External_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#external_clause}.
	 * @param ctx the parse tree
	 */
	void exitExternal_clause(SystemRDLParser.External_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#component_inst_elem}.
	 * @param ctx the parse tree
	 */
	void enterComponent_inst_elem(SystemRDLParser.Component_inst_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#component_inst_elem}.
	 * @param ctx the parse tree
	 */
	void exitComponent_inst_elem(SystemRDLParser.Component_inst_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#array}.
	 * @param ctx the parse tree
	 */
	void enterArray(SystemRDLParser.ArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#array}.
	 * @param ctx the parse tree
	 */
	void exitArray(SystemRDLParser.ArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#instance_ref}.
	 * @param ctx the parse tree
	 */
	void enterInstance_ref(SystemRDLParser.Instance_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#instance_ref}.
	 * @param ctx the parse tree
	 */
	void exitInstance_ref(SystemRDLParser.Instance_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#simple_instance_ref}.
	 * @param ctx the parse tree
	 */
	void enterSimple_instance_ref(SystemRDLParser.Simple_instance_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#simple_instance_ref}.
	 * @param ctx the parse tree
	 */
	void exitSimple_instance_ref(SystemRDLParser.Simple_instance_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#verilog_expression}.
	 * @param ctx the parse tree
	 */
	void enterVerilog_expression(SystemRDLParser.Verilog_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#verilog_expression}.
	 * @param ctx the parse tree
	 */
	void exitVerilog_expression(SystemRDLParser.Verilog_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#instance_ref_elem}.
	 * @param ctx the parse tree
	 */
	void enterInstance_ref_elem(SystemRDLParser.Instance_ref_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#instance_ref_elem}.
	 * @param ctx the parse tree
	 */
	void exitInstance_ref_elem(SystemRDLParser.Instance_ref_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_assign}.
	 * @param ctx the parse tree
	 */
	void enterProperty_assign(SystemRDLParser.Property_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_assign}.
	 * @param ctx the parse tree
	 */
	void exitProperty_assign(SystemRDLParser.Property_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#default_property_assign}.
	 * @param ctx the parse tree
	 */
	void enterDefault_property_assign(SystemRDLParser.Default_property_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#default_property_assign}.
	 * @param ctx the parse tree
	 */
	void exitDefault_property_assign(SystemRDLParser.Default_property_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#explicit_property_assign}.
	 * @param ctx the parse tree
	 */
	void enterExplicit_property_assign(SystemRDLParser.Explicit_property_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#explicit_property_assign}.
	 * @param ctx the parse tree
	 */
	void exitExplicit_property_assign(SystemRDLParser.Explicit_property_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#post_property_assign}.
	 * @param ctx the parse tree
	 */
	void enterPost_property_assign(SystemRDLParser.Post_property_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#post_property_assign}.
	 * @param ctx the parse tree
	 */
	void exitPost_property_assign(SystemRDLParser.Post_property_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_assign_rhs}.
	 * @param ctx the parse tree
	 */
	void enterProperty_assign_rhs(SystemRDLParser.Property_assign_rhsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_assign_rhs}.
	 * @param ctx the parse tree
	 */
	void exitProperty_assign_rhs(SystemRDLParser.Property_assign_rhsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#concat}.
	 * @param ctx the parse tree
	 */
	void enterConcat(SystemRDLParser.ConcatContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#concat}.
	 * @param ctx the parse tree
	 */
	void exitConcat(SystemRDLParser.ConcatContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#concat_elem}.
	 * @param ctx the parse tree
	 */
	void enterConcat_elem(SystemRDLParser.Concat_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#concat_elem}.
	 * @param ctx the parse tree
	 */
	void exitConcat_elem(SystemRDLParser.Concat_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property}.
	 * @param ctx the parse tree
	 */
	void enterProperty(SystemRDLParser.PropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property}.
	 * @param ctx the parse tree
	 */
	void exitProperty(SystemRDLParser.PropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#unimplemented_property}.
	 * @param ctx the parse tree
	 */
	void enterUnimplemented_property(SystemRDLParser.Unimplemented_propertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#unimplemented_property}.
	 * @param ctx the parse tree
	 */
	void exitUnimplemented_property(SystemRDLParser.Unimplemented_propertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_rvalue_constant}.
	 * @param ctx the parse tree
	 */
	void enterProperty_rvalue_constant(SystemRDLParser.Property_rvalue_constantContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_rvalue_constant}.
	 * @param ctx the parse tree
	 */
	void exitProperty_rvalue_constant(SystemRDLParser.Property_rvalue_constantContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#property_modifier}.
	 * @param ctx the parse tree
	 */
	void enterProperty_modifier(SystemRDLParser.Property_modifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#property_modifier}.
	 * @param ctx the parse tree
	 */
	void exitProperty_modifier(SystemRDLParser.Property_modifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#id}.
	 * @param ctx the parse tree
	 */
	void enterId(SystemRDLParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#id}.
	 * @param ctx the parse tree
	 */
	void exitId(SystemRDLParser.IdContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#num}.
	 * @param ctx the parse tree
	 */
	void enterNum(SystemRDLParser.NumContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#num}.
	 * @param ctx the parse tree
	 */
	void exitNum(SystemRDLParser.NumContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#str}.
	 * @param ctx the parse tree
	 */
	void enterStr(SystemRDLParser.StrContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#str}.
	 * @param ctx the parse tree
	 */
	void exitStr(SystemRDLParser.StrContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#enum_def}.
	 * @param ctx the parse tree
	 */
	void enterEnum_def(SystemRDLParser.Enum_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#enum_def}.
	 * @param ctx the parse tree
	 */
	void exitEnum_def(SystemRDLParser.Enum_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#enum_body}.
	 * @param ctx the parse tree
	 */
	void enterEnum_body(SystemRDLParser.Enum_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#enum_body}.
	 * @param ctx the parse tree
	 */
	void exitEnum_body(SystemRDLParser.Enum_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#enum_entry}.
	 * @param ctx the parse tree
	 */
	void enterEnum_entry(SystemRDLParser.Enum_entryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#enum_entry}.
	 * @param ctx the parse tree
	 */
	void exitEnum_entry(SystemRDLParser.Enum_entryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#enum_property_assign}.
	 * @param ctx the parse tree
	 */
	void enterEnum_property_assign(SystemRDLParser.Enum_property_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#enum_property_assign}.
	 * @param ctx the parse tree
	 */
	void exitEnum_property_assign(SystemRDLParser.Enum_property_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#ext_parms_root}.
	 * @param ctx the parse tree
	 */
	void enterExt_parms_root(SystemRDLParser.Ext_parms_rootContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#ext_parms_root}.
	 * @param ctx the parse tree
	 */
	void exitExt_parms_root(SystemRDLParser.Ext_parms_rootContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#ext_parm_defs}.
	 * @param ctx the parse tree
	 */
	void enterExt_parm_defs(SystemRDLParser.Ext_parm_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#ext_parm_defs}.
	 * @param ctx the parse tree
	 */
	void exitExt_parm_defs(SystemRDLParser.Ext_parm_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#global_defs}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_defs(SystemRDLParser.Global_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#global_defs}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_defs(SystemRDLParser.Global_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#global_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_parm_assign(SystemRDLParser.Global_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#global_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_parm_assign(SystemRDLParser.Global_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#rdl_in_defs}.
	 * @param ctx the parse tree
	 */
	void enterRdl_in_defs(SystemRDLParser.Rdl_in_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#rdl_in_defs}.
	 * @param ctx the parse tree
	 */
	void exitRdl_in_defs(SystemRDLParser.Rdl_in_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#rdl_in_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterRdl_in_parm_assign(SystemRDLParser.Rdl_in_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#rdl_in_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitRdl_in_parm_assign(SystemRDLParser.Rdl_in_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#jspec_in_defs}.
	 * @param ctx the parse tree
	 */
	void enterJspec_in_defs(SystemRDLParser.Jspec_in_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#jspec_in_defs}.
	 * @param ctx the parse tree
	 */
	void exitJspec_in_defs(SystemRDLParser.Jspec_in_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#jspec_in_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterJspec_in_parm_assign(SystemRDLParser.Jspec_in_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#jspec_in_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitJspec_in_parm_assign(SystemRDLParser.Jspec_in_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#rdl_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterRdl_out_defs(SystemRDLParser.Rdl_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#rdl_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitRdl_out_defs(SystemRDLParser.Rdl_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#rdl_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterRdl_out_parm_assign(SystemRDLParser.Rdl_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#rdl_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitRdl_out_parm_assign(SystemRDLParser.Rdl_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#jspec_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterJspec_out_defs(SystemRDLParser.Jspec_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#jspec_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitJspec_out_defs(SystemRDLParser.Jspec_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#jspec_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterJspec_out_parm_assign(SystemRDLParser.Jspec_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#jspec_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitJspec_out_parm_assign(SystemRDLParser.Jspec_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#systemverilog_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterSystemverilog_out_defs(SystemRDLParser.Systemverilog_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#systemverilog_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitSystemverilog_out_defs(SystemRDLParser.Systemverilog_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#systemverilog_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterSystemverilog_out_parm_assign(SystemRDLParser.Systemverilog_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#systemverilog_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitSystemverilog_out_parm_assign(SystemRDLParser.Systemverilog_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#uvmregs_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterUvmregs_out_defs(SystemRDLParser.Uvmregs_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#uvmregs_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitUvmregs_out_defs(SystemRDLParser.Uvmregs_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#uvmregs_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterUvmregs_out_parm_assign(SystemRDLParser.Uvmregs_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#uvmregs_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitUvmregs_out_parm_assign(SystemRDLParser.Uvmregs_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#reglist_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterReglist_out_defs(SystemRDLParser.Reglist_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#reglist_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitReglist_out_defs(SystemRDLParser.Reglist_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#reglist_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterReglist_out_parm_assign(SystemRDLParser.Reglist_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#reglist_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitReglist_out_parm_assign(SystemRDLParser.Reglist_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#bench_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterBench_out_defs(SystemRDLParser.Bench_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#bench_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitBench_out_defs(SystemRDLParser.Bench_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#bench_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterBench_out_parm_assign(SystemRDLParser.Bench_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#bench_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitBench_out_parm_assign(SystemRDLParser.Bench_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#model_annotation}.
	 * @param ctx the parse tree
	 */
	void enterModel_annotation(SystemRDLParser.Model_annotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#model_annotation}.
	 * @param ctx the parse tree
	 */
	void exitModel_annotation(SystemRDLParser.Model_annotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#annotation_command}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation_command(SystemRDLParser.Annotation_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#annotation_command}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation_command(SystemRDLParser.Annotation_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#implemented_rdl_property}.
	 * @param ctx the parse tree
	 */
	void enterImplemented_rdl_property(SystemRDLParser.Implemented_rdl_propertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#implemented_rdl_property}.
	 * @param ctx the parse tree
	 */
	void exitImplemented_rdl_property(SystemRDLParser.Implemented_rdl_propertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SystemRDLParser#bool}.
	 * @param ctx the parse tree
	 */
	void enterBool(SystemRDLParser.BoolContext ctx);
	/**
	 * Exit a parse tree produced by {@link SystemRDLParser#bool}.
	 * @param ctx the parse tree
	 */
	void exitBool(SystemRDLParser.BoolContext ctx);
}