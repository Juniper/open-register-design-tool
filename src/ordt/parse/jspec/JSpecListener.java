// Generated from /Users/snellenbach/git/open-register-design-tool/src/ordt/parse/grammars/JSpec.g4 by ANTLR 4.5.1
package ordt.parse.jspec;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link JSpecParser}.
 */
public interface JSpecListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JSpecParser#root}.
	 * @param ctx the parse tree
	 */
	void enterRoot(JSpecParser.RootContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#root}.
	 * @param ctx the parse tree
	 */
	void exitRoot(JSpecParser.RootContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#num_constant_def}.
	 * @param ctx the parse tree
	 */
	void enterNum_constant_def(JSpecParser.Num_constant_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#num_constant_def}.
	 * @param ctx the parse tree
	 */
	void exitNum_constant_def(JSpecParser.Num_constant_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#integer_constant_assign}.
	 * @param ctx the parse tree
	 */
	void enterInteger_constant_assign(JSpecParser.Integer_constant_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#integer_constant_assign}.
	 * @param ctx the parse tree
	 */
	void exitInteger_constant_assign(JSpecParser.Integer_constant_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#width_constant_assign}.
	 * @param ctx the parse tree
	 */
	void enterWidth_constant_assign(JSpecParser.Width_constant_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#width_constant_assign}.
	 * @param ctx the parse tree
	 */
	void exitWidth_constant_assign(JSpecParser.Width_constant_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#string_constant_def}.
	 * @param ctx the parse tree
	 */
	void enterString_constant_def(JSpecParser.String_constant_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#string_constant_def}.
	 * @param ctx the parse tree
	 */
	void exitString_constant_def(JSpecParser.String_constant_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#bracket_pair}.
	 * @param ctx the parse tree
	 */
	void enterBracket_pair(JSpecParser.Bracket_pairContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#bracket_pair}.
	 * @param ctx the parse tree
	 */
	void exitBracket_pair(JSpecParser.Bracket_pairContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#type_definition}.
	 * @param ctx the parse tree
	 */
	void enterType_definition(JSpecParser.Type_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#type_definition}.
	 * @param ctx the parse tree
	 */
	void exitType_definition(JSpecParser.Type_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#enum_field_def}.
	 * @param ctx the parse tree
	 */
	void enterEnum_field_def(JSpecParser.Enum_field_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#enum_field_def}.
	 * @param ctx the parse tree
	 */
	void exitEnum_field_def(JSpecParser.Enum_field_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#enum_value_assign}.
	 * @param ctx the parse tree
	 */
	void enterEnum_value_assign(JSpecParser.Enum_value_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#enum_value_assign}.
	 * @param ctx the parse tree
	 */
	void exitEnum_value_assign(JSpecParser.Enum_value_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#transaction_def}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_def(JSpecParser.Transaction_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#transaction_def}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_def(JSpecParser.Transaction_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#value_assign}.
	 * @param ctx the parse tree
	 */
	void enterValue_assign(JSpecParser.Value_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#value_assign}.
	 * @param ctx the parse tree
	 */
	void exitValue_assign(JSpecParser.Value_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#typedef_instance}.
	 * @param ctx the parse tree
	 */
	void enterTypedef_instance(JSpecParser.Typedef_instanceContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#typedef_instance}.
	 * @param ctx the parse tree
	 */
	void exitTypedef_instance(JSpecParser.Typedef_instanceContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#param_block}.
	 * @param ctx the parse tree
	 */
	void enterParam_block(JSpecParser.Param_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#param_block}.
	 * @param ctx the parse tree
	 */
	void exitParam_block(JSpecParser.Param_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#assign_parameter}.
	 * @param ctx the parse tree
	 */
	void enterAssign_parameter(JSpecParser.Assign_parameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#assign_parameter}.
	 * @param ctx the parse tree
	 */
	void exitAssign_parameter(JSpecParser.Assign_parameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#defined_attribute}.
	 * @param ctx the parse tree
	 */
	void enterDefined_attribute(JSpecParser.Defined_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#defined_attribute}.
	 * @param ctx the parse tree
	 */
	void exitDefined_attribute(JSpecParser.Defined_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#defined_attribute_set}.
	 * @param ctx the parse tree
	 */
	void enterDefined_attribute_set(JSpecParser.Defined_attribute_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#defined_attribute_set}.
	 * @param ctx the parse tree
	 */
	void exitDefined_attribute_set(JSpecParser.Defined_attribute_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#register_set_def}.
	 * @param ctx the parse tree
	 */
	void enterRegister_set_def(JSpecParser.Register_set_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#register_set_def}.
	 * @param ctx the parse tree
	 */
	void exitRegister_set_def(JSpecParser.Register_set_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#register_def}.
	 * @param ctx the parse tree
	 */
	void enterRegister_def(JSpecParser.Register_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#register_def}.
	 * @param ctx the parse tree
	 */
	void exitRegister_def(JSpecParser.Register_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#field_def}.
	 * @param ctx the parse tree
	 */
	void enterField_def(JSpecParser.Field_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#field_def}.
	 * @param ctx the parse tree
	 */
	void exitField_def(JSpecParser.Field_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#field_set_def}.
	 * @param ctx the parse tree
	 */
	void enterField_set_def(JSpecParser.Field_set_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#field_set_def}.
	 * @param ctx the parse tree
	 */
	void exitField_set_def(JSpecParser.Field_set_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#int_field_def}.
	 * @param ctx the parse tree
	 */
	void enterInt_field_def(JSpecParser.Int_field_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#int_field_def}.
	 * @param ctx the parse tree
	 */
	void exitInt_field_def(JSpecParser.Int_field_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#nop_field_def}.
	 * @param ctx the parse tree
	 */
	void enterNop_field_def(JSpecParser.Nop_field_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#nop_field_def}.
	 * @param ctx the parse tree
	 */
	void exitNop_field_def(JSpecParser.Nop_field_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#test_group_def}.
	 * @param ctx the parse tree
	 */
	void enterTest_group_def(JSpecParser.Test_group_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#test_group_def}.
	 * @param ctx the parse tree
	 */
	void exitTest_group_def(JSpecParser.Test_group_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#array}.
	 * @param ctx the parse tree
	 */
	void enterArray(JSpecParser.ArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#array}.
	 * @param ctx the parse tree
	 */
	void exitArray(JSpecParser.ArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#num_expression}.
	 * @param ctx the parse tree
	 */
	void enterNum_expression(JSpecParser.Num_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#num_expression}.
	 * @param ctx the parse tree
	 */
	void exitNum_expression(JSpecParser.Num_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#id}.
	 * @param ctx the parse tree
	 */
	void enterId(JSpecParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#id}.
	 * @param ctx the parse tree
	 */
	void exitId(JSpecParser.IdContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#jstr}.
	 * @param ctx the parse tree
	 */
	void enterJstr(JSpecParser.JstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#jstr}.
	 * @param ctx the parse tree
	 */
	void exitJstr(JSpecParser.JstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link JSpecParser#str}.
	 * @param ctx the parse tree
	 */
	void enterStr(JSpecParser.StrContext ctx);
	/**
	 * Exit a parse tree produced by {@link JSpecParser#str}.
	 * @param ctx the parse tree
	 */
	void exitStr(JSpecParser.StrContext ctx);
}