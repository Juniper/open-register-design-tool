// Generated from /Users/snellenbach/git/open-register-design-tool/src/ordt/parse/grammars/ExtParms.g4 by ANTLR 4.5.1
package ordt.parse.parameters;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ExtParmsParser}.
 */
public interface ExtParmsListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#ext_parms_root}.
	 * @param ctx the parse tree
	 */
	void enterExt_parms_root(ExtParmsParser.Ext_parms_rootContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#ext_parms_root}.
	 * @param ctx the parse tree
	 */
	void exitExt_parms_root(ExtParmsParser.Ext_parms_rootContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#ext_parm_defs}.
	 * @param ctx the parse tree
	 */
	void enterExt_parm_defs(ExtParmsParser.Ext_parm_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#ext_parm_defs}.
	 * @param ctx the parse tree
	 */
	void exitExt_parm_defs(ExtParmsParser.Ext_parm_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#global_defs}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_defs(ExtParmsParser.Global_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#global_defs}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_defs(ExtParmsParser.Global_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#global_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_parm_assign(ExtParmsParser.Global_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#global_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_parm_assign(ExtParmsParser.Global_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#rdl_in_defs}.
	 * @param ctx the parse tree
	 */
	void enterRdl_in_defs(ExtParmsParser.Rdl_in_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#rdl_in_defs}.
	 * @param ctx the parse tree
	 */
	void exitRdl_in_defs(ExtParmsParser.Rdl_in_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#rdl_in_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterRdl_in_parm_assign(ExtParmsParser.Rdl_in_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#rdl_in_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitRdl_in_parm_assign(ExtParmsParser.Rdl_in_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#jspec_in_defs}.
	 * @param ctx the parse tree
	 */
	void enterJspec_in_defs(ExtParmsParser.Jspec_in_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#jspec_in_defs}.
	 * @param ctx the parse tree
	 */
	void exitJspec_in_defs(ExtParmsParser.Jspec_in_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#jspec_in_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterJspec_in_parm_assign(ExtParmsParser.Jspec_in_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#jspec_in_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitJspec_in_parm_assign(ExtParmsParser.Jspec_in_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#rdl_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterRdl_out_defs(ExtParmsParser.Rdl_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#rdl_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitRdl_out_defs(ExtParmsParser.Rdl_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#rdl_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterRdl_out_parm_assign(ExtParmsParser.Rdl_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#rdl_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitRdl_out_parm_assign(ExtParmsParser.Rdl_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#jspec_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterJspec_out_defs(ExtParmsParser.Jspec_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#jspec_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitJspec_out_defs(ExtParmsParser.Jspec_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#jspec_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterJspec_out_parm_assign(ExtParmsParser.Jspec_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#jspec_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitJspec_out_parm_assign(ExtParmsParser.Jspec_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#systemverilog_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterSystemverilog_out_defs(ExtParmsParser.Systemverilog_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#systemverilog_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitSystemverilog_out_defs(ExtParmsParser.Systemverilog_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#systemverilog_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterSystemverilog_out_parm_assign(ExtParmsParser.Systemverilog_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#systemverilog_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitSystemverilog_out_parm_assign(ExtParmsParser.Systemverilog_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#systemverilog_wrapper_info}.
	 * @param ctx the parse tree
	 */
	void enterSystemverilog_wrapper_info(ExtParmsParser.Systemverilog_wrapper_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#systemverilog_wrapper_info}.
	 * @param ctx the parse tree
	 */
	void exitSystemverilog_wrapper_info(ExtParmsParser.Systemverilog_wrapper_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#systemverilog_wrapper_remap_command}.
	 * @param ctx the parse tree
	 */
	void enterSystemverilog_wrapper_remap_command(ExtParmsParser.Systemverilog_wrapper_remap_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#systemverilog_wrapper_remap_command}.
	 * @param ctx the parse tree
	 */
	void exitSystemverilog_wrapper_remap_command(ExtParmsParser.Systemverilog_wrapper_remap_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#uvmregs_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterUvmregs_out_defs(ExtParmsParser.Uvmregs_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#uvmregs_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitUvmregs_out_defs(ExtParmsParser.Uvmregs_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#uvmregs_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterUvmregs_out_parm_assign(ExtParmsParser.Uvmregs_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#uvmregs_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitUvmregs_out_parm_assign(ExtParmsParser.Uvmregs_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#reglist_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterReglist_out_defs(ExtParmsParser.Reglist_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#reglist_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitReglist_out_defs(ExtParmsParser.Reglist_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#reglist_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterReglist_out_parm_assign(ExtParmsParser.Reglist_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#reglist_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitReglist_out_parm_assign(ExtParmsParser.Reglist_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#bench_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterBench_out_defs(ExtParmsParser.Bench_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#bench_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitBench_out_defs(ExtParmsParser.Bench_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#bench_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterBench_out_parm_assign(ExtParmsParser.Bench_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#bench_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitBench_out_parm_assign(ExtParmsParser.Bench_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#xml_out_defs}.
	 * @param ctx the parse tree
	 */
	void enterXml_out_defs(ExtParmsParser.Xml_out_defsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#xml_out_defs}.
	 * @param ctx the parse tree
	 */
	void exitXml_out_defs(ExtParmsParser.Xml_out_defsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#xml_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void enterXml_out_parm_assign(ExtParmsParser.Xml_out_parm_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#xml_out_parm_assign}.
	 * @param ctx the parse tree
	 */
	void exitXml_out_parm_assign(ExtParmsParser.Xml_out_parm_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#model_annotation}.
	 * @param ctx the parse tree
	 */
	void enterModel_annotation(ExtParmsParser.Model_annotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#model_annotation}.
	 * @param ctx the parse tree
	 */
	void exitModel_annotation(ExtParmsParser.Model_annotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#annotation_command}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation_command(ExtParmsParser.Annotation_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#annotation_command}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation_command(ExtParmsParser.Annotation_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#implemented_rdl_property}.
	 * @param ctx the parse tree
	 */
	void enterImplemented_rdl_property(ExtParmsParser.Implemented_rdl_propertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#implemented_rdl_property}.
	 * @param ctx the parse tree
	 */
	void exitImplemented_rdl_property(ExtParmsParser.Implemented_rdl_propertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExtParmsParser#bool}.
	 * @param ctx the parse tree
	 */
	void enterBool(ExtParmsParser.BoolContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExtParmsParser#bool}.
	 * @param ctx the parse tree
	 */
	void exitBool(ExtParmsParser.BoolContext ctx);
}