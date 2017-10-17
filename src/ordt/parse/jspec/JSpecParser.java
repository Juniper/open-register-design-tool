// Generated from /Users/snellenbach/git/open-register-design-tool/src/ordt/parse/grammars/JSpec.g4 by ANTLR 4.5.1
package ordt.parse.jspec;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JSpecParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, T__47=48, T__48=49, T__49=50, T__50=51, T__51=52, 
		T__52=53, T__53=54, T__54=55, T__55=56, T__56=57, T__57=58, T__58=59, 
		T__59=60, T__60=61, T__61=62, T__62=63, T__63=64, T__64=65, T__65=66, 
		T__66=67, T__67=68, T__68=69, T__69=70, T__70=71, T__71=72, T__72=73, 
		T__73=74, T__74=75, T__75=76, WS=77, SL_COMMENT=78, ML_COMMENT=79, ID=80, 
		USER_PARAMETER=81, NUM=82, JSTR=83, STR=84, LJQUOTE=85, RJQUOTE=86, LSHIFT=87, 
		RSHIFT=88, LBRACE=89, RBRACE=90, LSQ=91, RSQ=92, LPAREN=93, RPAREN=94, 
		AT=95, OR=96, AND=97, SEMI=98, COLON=99, COMMA=100, DOT=101, STAR=102, 
		EQ=103, PLUS=104, MINUS=105, DIV=106, EXP=107, DOLLAR=108;
	public static final int
		RULE_root = 0, RULE_num_constant_def = 1, RULE_integer_constant_assign = 2, 
		RULE_width_constant_assign = 3, RULE_string_constant_def = 4, RULE_bracket_pair = 5, 
		RULE_type_definition = 6, RULE_param_type_definition = 7, RULE_enum_field_def = 8, 
		RULE_enum_value_assign = 9, RULE_transaction_def = 10, RULE_value_assign = 11, 
		RULE_typedef_instance = 12, RULE_param_block = 13, RULE_assign_parameter = 14, 
		RULE_defined_attribute = 15, RULE_defined_attribute_set = 16, RULE_register_set_def = 17, 
		RULE_register_def = 18, RULE_field_def = 19, RULE_field_set_def = 20, 
		RULE_int_field_def = 21, RULE_nop_field_def = 22, RULE_test_group_def = 23, 
		RULE_array = 24, RULE_num_expression = 25, RULE_id = 26, RULE_jstr = 27, 
		RULE_str = 28;
	public static final String[] ruleNames = {
		"root", "num_constant_def", "integer_constant_assign", "width_constant_assign", 
		"string_constant_def", "bracket_pair", "type_definition", "param_type_definition", 
		"enum_field_def", "enum_value_assign", "transaction_def", "value_assign", 
		"typedef_instance", "param_block", "assign_parameter", "defined_attribute", 
		"defined_attribute_set", "register_set_def", "register_def", "field_def", 
		"field_set_def", "int_field_def", "nop_field_def", "test_group_def", "array", 
		"num_expression", "id", "jstr", "str"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'constant'", "'integer'", "'enum'", "'typedef'", "'param'", "'string'", 
		"'transaction'", "'access_mode'", "'address_alignment'", "'addr_decode_allow_case'", 
		"'addr_decode_allow_x'", "'addr_decode_instances'", "'addr_decode_fixed_width'", 
		"'addr_decode_lsb'", "'addr_decode_msb'", "'anonymous'", "'attributes'", 
		"'autogen'", "'category'", "'description'", "'macro_name'", "'macro_mode'", 
		"'operational_access_mode'", "'overlay'", "'overlay_tag'", "'overlay_tag_width'", 
		"'pack_msb'", "'pad_msb'", "'register_width'", "'register_set_size'", 
		"'sub_category'", "'superset_check'", "'unknown'", "'true'", "'false'", 
		"'True'", "'False'", "'TRUE'", "'FALSE'", "'READ_WRITE'", "'READ_ONLY'", 
		"'READ_TO_CLEAR'", "'CLEAR_ON_READ'", "'WRITE_ONLY'", "'WRITE_ONE_TO_CLEAR'", 
		"'WRITE_ONE_TO_SET'", "'DIAGNOSTIC'", "'ERROR_COUNTER'", "'STAT_COUNTER'", 
		"'CONSTRAINED_CONFIG'", "'DYNAMIC_CONFIG'", "'STATIC_CONFIG'", "'INTERRUPT'", 
		"'DEBUG'", "'INFO'", "'CGATE_UNSAFE'", "'FATAL'", "'MAJOR'", "'STATE'", 
		"'NOEXPORT'", "'JS_ATTRIB_INT_STATUS'", "'JS_ATTRIB_DO_NOT_TEST'", "'JS_ATTRIB_TEST_ACCESS_ONLY'", 
		"'JS_ATTRIB_VOLATILE'", "'JS_ATTRIB_EXTERNAL_MEMORY'", "'SKIP_CHILDREN'", 
		"'SKIP_ADDRESS'", "'SKIP_REGISTER_SET_SIZE'", "'MANUAL'", "'STANDARD'", 
		"'register_set'", "'register'", "'field_set'", "'nop'", "'test_group'", 
		"'WIDTH'", null, null, null, null, "'XUSER_PARAMETERX'", null, null, null, 
		"'\"{'", "'}\"'", "'<<'", "'>>'", "'{'", "'}'", "'['", "']'", "'('", "')'", 
		"'@'", "'|'", "'&'", "';'", "':'", "','", "'.'", "'*'", "'='", "'+'", 
		"'-'", "'/'", "'^'", "'$'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, "WS", "SL_COMMENT", "ML_COMMENT", "ID", 
		"USER_PARAMETER", "NUM", "JSTR", "STR", "LJQUOTE", "RJQUOTE", "LSHIFT", 
		"RSHIFT", "LBRACE", "RBRACE", "LSQ", "RSQ", "LPAREN", "RPAREN", "AT", 
		"OR", "AND", "SEMI", "COLON", "COMMA", "DOT", "STAR", "EQ", "PLUS", "MINUS", 
		"DIV", "EXP", "DOLLAR"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "JSpec.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public JSpecParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class RootContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(JSpecParser.EOF, 0); }
		public List<Num_constant_defContext> num_constant_def() {
			return getRuleContexts(Num_constant_defContext.class);
		}
		public Num_constant_defContext num_constant_def(int i) {
			return getRuleContext(Num_constant_defContext.class,i);
		}
		public List<String_constant_defContext> string_constant_def() {
			return getRuleContexts(String_constant_defContext.class);
		}
		public String_constant_defContext string_constant_def(int i) {
			return getRuleContext(String_constant_defContext.class,i);
		}
		public List<Type_definitionContext> type_definition() {
			return getRuleContexts(Type_definitionContext.class);
		}
		public Type_definitionContext type_definition(int i) {
			return getRuleContext(Type_definitionContext.class,i);
		}
		public List<Param_type_definitionContext> param_type_definition() {
			return getRuleContexts(Param_type_definitionContext.class);
		}
		public Param_type_definitionContext param_type_definition(int i) {
			return getRuleContext(Param_type_definitionContext.class,i);
		}
		public List<Typedef_instanceContext> typedef_instance() {
			return getRuleContexts(Typedef_instanceContext.class);
		}
		public Typedef_instanceContext typedef_instance(int i) {
			return getRuleContext(Typedef_instanceContext.class,i);
		}
		public List<Register_set_defContext> register_set_def() {
			return getRuleContexts(Register_set_defContext.class);
		}
		public Register_set_defContext register_set_def(int i) {
			return getRuleContext(Register_set_defContext.class,i);
		}
		public RootContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_root; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterRoot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitRoot(this);
		}
	}

	public final RootContext root() throws RecognitionException {
		RootContext _localctx = new RootContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_root);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(66);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0 || _la==T__3 || _la==T__70 || _la==ID) {
				{
				setState(64);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(58);
					num_constant_def();
					}
					break;
				case 2:
					{
					setState(59);
					string_constant_def();
					}
					break;
				case 3:
					{
					setState(60);
					type_definition();
					}
					break;
				case 4:
					{
					setState(61);
					param_type_definition();
					}
					break;
				case 5:
					{
					setState(62);
					typedef_instance();
					}
					break;
				case 6:
					{
					setState(63);
					register_set_def();
					}
					break;
				}
				}
				setState(68);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(69);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Num_constant_defContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public Integer_constant_assignContext integer_constant_assign() {
			return getRuleContext(Integer_constant_assignContext.class,0);
		}
		public List<Width_constant_assignContext> width_constant_assign() {
			return getRuleContexts(Width_constant_assignContext.class);
		}
		public Width_constant_assignContext width_constant_assign(int i) {
			return getRuleContext(Width_constant_assignContext.class,i);
		}
		public Num_constant_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_num_constant_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterNum_constant_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitNum_constant_def(this);
		}
	}

	public final Num_constant_defContext num_constant_def() throws RecognitionException {
		Num_constant_defContext _localctx = new Num_constant_defContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_num_constant_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			match(T__0);
			setState(72);
			id();
			setState(73);
			str();
			setState(74);
			match(LBRACE);
			setState(81);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				setState(75);
				integer_constant_assign();
				}
				break;
			case 2:
				{
				setState(77); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(76);
					width_constant_assign();
					}
					}
					setState(79); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__1 || _la==T__2 || _la==ID );
				}
				break;
			}
			setState(83);
			match(RBRACE);
			setState(84);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Integer_constant_assignContext extends ParserRuleContext {
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public TerminalNode EQ() { return getToken(JSpecParser.EQ, 0); }
		public Num_expressionContext num_expression() {
			return getRuleContext(Num_expressionContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public Integer_constant_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_integer_constant_assign; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterInteger_constant_assign(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitInteger_constant_assign(this);
		}
	}

	public final Integer_constant_assignContext integer_constant_assign() throws RecognitionException {
		Integer_constant_assignContext _localctx = new Integer_constant_assignContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_integer_constant_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			match(T__1);
			setState(87);
			array();
			setState(88);
			match(EQ);
			setState(89);
			num_expression(0);
			setState(90);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Width_constant_assignContext extends ParserRuleContext {
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public List<TerminalNode> LBRACE() { return getTokens(JSpecParser.LBRACE); }
		public TerminalNode LBRACE(int i) {
			return getToken(JSpecParser.LBRACE, i);
		}
		public List<TerminalNode> RBRACE() { return getTokens(JSpecParser.RBRACE); }
		public TerminalNode RBRACE(int i) {
			return getToken(JSpecParser.RBRACE, i);
		}
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public Width_constant_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_width_constant_assign; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterWidth_constant_assign(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitWidth_constant_assign(this);
		}
	}

	public final Width_constant_assignContext width_constant_assign() throws RecognitionException {
		Width_constant_assignContext _localctx = new Width_constant_assignContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_width_constant_assign);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			switch (_input.LA(1)) {
			case T__2:
				{
				setState(92);
				match(T__2);
				setState(93);
				array();
				setState(94);
				match(LBRACE);
				setState(98);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << T__54) | (1L << T__55) | (1L << T__56) | (1L << T__57) | (1L << T__58) | (1L << T__59) | (1L << T__60) | (1L << T__61) | (1L << T__62))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (T__63 - 64)) | (1L << (T__64 - 64)) | (1L << (T__65 - 64)) | (1L << (T__66 - 64)) | (1L << (T__67 - 64)) | (1L << (T__68 - 64)) | (1L << (T__69 - 64)) | (1L << (T__70 - 64)) | (1L << (T__71 - 64)) | (1L << (T__72 - 64)) | (1L << (T__73 - 64)) | (1L << (T__74 - 64)) | (1L << (T__75 - 64)) | (1L << (WS - 64)) | (1L << (SL_COMMENT - 64)) | (1L << (ML_COMMENT - 64)) | (1L << (ID - 64)) | (1L << (USER_PARAMETER - 64)) | (1L << (NUM - 64)) | (1L << (JSTR - 64)) | (1L << (STR - 64)) | (1L << (LJQUOTE - 64)) | (1L << (RJQUOTE - 64)) | (1L << (LSHIFT - 64)) | (1L << (RSHIFT - 64)) | (1L << (LSQ - 64)) | (1L << (RSQ - 64)) | (1L << (LPAREN - 64)) | (1L << (RPAREN - 64)) | (1L << (AT - 64)) | (1L << (OR - 64)) | (1L << (AND - 64)) | (1L << (SEMI - 64)) | (1L << (COLON - 64)) | (1L << (COMMA - 64)) | (1L << (DOT - 64)) | (1L << (STAR - 64)) | (1L << (EQ - 64)) | (1L << (PLUS - 64)) | (1L << (MINUS - 64)) | (1L << (DIV - 64)) | (1L << (EXP - 64)) | (1L << (DOLLAR - 64)))) != 0)) {
					{
					{
					setState(95);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==LBRACE || _la==RBRACE) ) {
					_errHandler.recoverInline(this);
					} else {
						consume();
					}
					}
					}
					setState(100);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(101);
				match(RBRACE);
				setState(102);
				match(SEMI);
				}
				break;
			case T__1:
				{
				setState(104);
				match(T__1);
				setState(105);
				id();
				setState(106);
				array();
				setState(107);
				str();
				setState(108);
				match(SEMI);
				}
				break;
			case ID:
				{
				setState(110);
				id();
				setState(112);
				_la = _input.LA(1);
				if (_la==ID) {
					{
					setState(111);
					id();
					}
				}

				setState(115);
				_la = _input.LA(1);
				if (_la==STR) {
					{
					setState(114);
					str();
					}
				}

				setState(117);
				match(SEMI);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class String_constant_defContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public List<Bracket_pairContext> bracket_pair() {
			return getRuleContexts(Bracket_pairContext.class);
		}
		public Bracket_pairContext bracket_pair(int i) {
			return getRuleContext(Bracket_pairContext.class,i);
		}
		public String_constant_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string_constant_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterString_constant_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitString_constant_def(this);
		}
	}

	public final String_constant_defContext string_constant_def() throws RecognitionException {
		String_constant_defContext _localctx = new String_constant_defContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_string_constant_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(121);
			match(T__0);
			setState(122);
			id();
			setState(123);
			str();
			setState(124);
			match(LBRACE);
			setState(126); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(125);
				bracket_pair();
				}
				}
				setState(128); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << T__54) | (1L << T__55) | (1L << T__56) | (1L << T__57) | (1L << T__58) | (1L << T__59) | (1L << T__60) | (1L << T__61) | (1L << T__62))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (T__63 - 64)) | (1L << (T__64 - 64)) | (1L << (T__65 - 64)) | (1L << (T__66 - 64)) | (1L << (T__67 - 64)) | (1L << (T__68 - 64)) | (1L << (T__69 - 64)) | (1L << (T__70 - 64)) | (1L << (T__71 - 64)) | (1L << (T__72 - 64)) | (1L << (T__73 - 64)) | (1L << (T__74 - 64)) | (1L << (T__75 - 64)) | (1L << (WS - 64)) | (1L << (SL_COMMENT - 64)) | (1L << (ML_COMMENT - 64)) | (1L << (ID - 64)) | (1L << (USER_PARAMETER - 64)) | (1L << (NUM - 64)) | (1L << (JSTR - 64)) | (1L << (STR - 64)) | (1L << (LJQUOTE - 64)) | (1L << (RJQUOTE - 64)) | (1L << (LSHIFT - 64)) | (1L << (RSHIFT - 64)) | (1L << (LSQ - 64)) | (1L << (RSQ - 64)) | (1L << (LPAREN - 64)) | (1L << (RPAREN - 64)) | (1L << (AT - 64)) | (1L << (OR - 64)) | (1L << (AND - 64)) | (1L << (SEMI - 64)) | (1L << (COLON - 64)) | (1L << (COMMA - 64)) | (1L << (DOT - 64)) | (1L << (STAR - 64)) | (1L << (EQ - 64)) | (1L << (PLUS - 64)) | (1L << (MINUS - 64)) | (1L << (DIV - 64)) | (1L << (EXP - 64)) | (1L << (DOLLAR - 64)))) != 0) );
			setState(130);
			match(RBRACE);
			setState(131);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Bracket_pairContext extends ParserRuleContext {
		public List<TerminalNode> LBRACE() { return getTokens(JSpecParser.LBRACE); }
		public TerminalNode LBRACE(int i) {
			return getToken(JSpecParser.LBRACE, i);
		}
		public List<TerminalNode> RBRACE() { return getTokens(JSpecParser.RBRACE); }
		public TerminalNode RBRACE(int i) {
			return getToken(JSpecParser.RBRACE, i);
		}
		public List<Bracket_pairContext> bracket_pair() {
			return getRuleContexts(Bracket_pairContext.class);
		}
		public Bracket_pairContext bracket_pair(int i) {
			return getRuleContext(Bracket_pairContext.class,i);
		}
		public Bracket_pairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bracket_pair; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterBracket_pair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitBracket_pair(this);
		}
	}

	public final Bracket_pairContext bracket_pair() throws RecognitionException {
		Bracket_pairContext _localctx = new Bracket_pairContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_bracket_pair);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(134); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(133);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==LBRACE || _la==RBRACE) ) {
					_errHandler.recoverInline(this);
					} else {
						consume();
					}
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(136); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(146);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1+1 ) {
					{
					{
					setState(138);
					match(LBRACE);
					setState(141);
					switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
					case 1:
						{
						setState(139);
						_la = _input.LA(1);
						if ( _la <= 0 || (_la==LBRACE || _la==RBRACE) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						}
						break;
					case 2:
						{
						setState(140);
						bracket_pair();
						}
						break;
					}
					setState(143);
					match(RBRACE);
					}
					} 
				}
				setState(148);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			}
			setState(152);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(149);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==LBRACE || _la==RBRACE) ) {
					_errHandler.recoverInline(this);
					} else {
						consume();
					}
					}
					} 
				}
				setState(154);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Type_definitionContext extends ParserRuleContext {
		public Register_set_defContext register_set_def() {
			return getRuleContext(Register_set_defContext.class,0);
		}
		public Register_defContext register_def() {
			return getRuleContext(Register_defContext.class,0);
		}
		public Field_defContext field_def() {
			return getRuleContext(Field_defContext.class,0);
		}
		public Transaction_defContext transaction_def() {
			return getRuleContext(Transaction_defContext.class,0);
		}
		public Type_definitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterType_definition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitType_definition(this);
		}
	}

	public final Type_definitionContext type_definition() throws RecognitionException {
		Type_definitionContext _localctx = new Type_definitionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_type_definition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(155);
			match(T__3);
			setState(160);
			switch (_input.LA(1)) {
			case T__70:
				{
				setState(156);
				register_set_def();
				}
				break;
			case T__71:
				{
				setState(157);
				register_def();
				}
				break;
			case T__1:
			case T__2:
			case T__72:
			case T__73:
				{
				setState(158);
				field_def();
				}
				break;
			case T__6:
				{
				setState(159);
				transaction_def();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Param_type_definitionContext extends ParserRuleContext {
		public IdContext id;
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode EQ() { return getToken(JSpecParser.EQ, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public Param_type_definitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_param_type_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterParam_type_definition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitParam_type_definition(this);
		}
	}

	public final Param_type_definitionContext param_type_definition() throws RecognitionException {
		Param_type_definitionContext _localctx = new Param_type_definitionContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_param_type_definition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162);
			match(T__3);
			setState(163);
			match(T__4);
			setState(164);
			((Param_type_definitionContext)_localctx).id = id();
			 JSpecLexer.addUserParameter((((Param_type_definitionContext)_localctx).id!=null?_input.getText(((Param_type_definitionContext)_localctx).id.start,((Param_type_definitionContext)_localctx).id.stop):null)); 
			setState(166);
			match(EQ);
			setState(167);
			match(T__5);
			setState(168);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Enum_field_defContext extends ParserRuleContext {
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public JstrContext jstr() {
			return getRuleContext(JstrContext.class,0);
		}
		public List<Enum_value_assignContext> enum_value_assign() {
			return getRuleContexts(Enum_value_assignContext.class);
		}
		public Enum_value_assignContext enum_value_assign(int i) {
			return getRuleContext(Enum_value_assignContext.class,i);
		}
		public Param_blockContext param_block() {
			return getRuleContext(Param_blockContext.class,0);
		}
		public Enum_field_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_field_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterEnum_field_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitEnum_field_def(this);
		}
	}

	public final Enum_field_defContext enum_field_def() throws RecognitionException {
		Enum_field_defContext _localctx = new Enum_field_defContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_enum_field_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(170);
			match(T__2);
			setState(172);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(171);
				id();
				}
			}

			setState(174);
			array();
			setState(177);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(175);
				str();
				}
				break;
			case JSTR:
				{
				setState(176);
				jstr();
				}
				break;
			case LBRACE:
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(179);
			match(LBRACE);
			setState(181); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(180);
				enum_value_assign();
				}
				}
				setState(183); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ID );
			setState(185);
			match(RBRACE);
			setState(187);
			_la = _input.LA(1);
			if (_la==T__4) {
				{
				setState(186);
				param_block();
				}
			}

			setState(189);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Enum_value_assignContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode EQ() { return getToken(JSpecParser.EQ, 0); }
		public Num_expressionContext num_expression() {
			return getRuleContext(Num_expressionContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public JstrContext jstr() {
			return getRuleContext(JstrContext.class,0);
		}
		public Enum_value_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_value_assign; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterEnum_value_assign(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitEnum_value_assign(this);
		}
	}

	public final Enum_value_assignContext enum_value_assign() throws RecognitionException {
		Enum_value_assignContext _localctx = new Enum_value_assignContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_enum_value_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(191);
			id();
			setState(192);
			match(EQ);
			setState(193);
			num_expression(0);
			setState(196);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(194);
				str();
				}
				break;
			case JSTR:
				{
				setState(195);
				jstr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(198);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Transaction_defContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public JstrContext jstr() {
			return getRuleContext(JstrContext.class,0);
		}
		public List<Bracket_pairContext> bracket_pair() {
			return getRuleContexts(Bracket_pairContext.class);
		}
		public Bracket_pairContext bracket_pair(int i) {
			return getRuleContext(Bracket_pairContext.class,i);
		}
		public Transaction_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transaction_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterTransaction_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitTransaction_def(this);
		}
	}

	public final Transaction_defContext transaction_def() throws RecognitionException {
		Transaction_defContext _localctx = new Transaction_defContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_transaction_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(200);
			match(T__6);
			setState(201);
			id();
			setState(204);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(202);
				str();
				}
				break;
			case JSTR:
				{
				setState(203);
				jstr();
				}
				break;
			case LBRACE:
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(206);
			match(LBRACE);
			setState(208); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(207);
				bracket_pair();
				}
				}
				setState(210); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << T__54) | (1L << T__55) | (1L << T__56) | (1L << T__57) | (1L << T__58) | (1L << T__59) | (1L << T__60) | (1L << T__61) | (1L << T__62))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (T__63 - 64)) | (1L << (T__64 - 64)) | (1L << (T__65 - 64)) | (1L << (T__66 - 64)) | (1L << (T__67 - 64)) | (1L << (T__68 - 64)) | (1L << (T__69 - 64)) | (1L << (T__70 - 64)) | (1L << (T__71 - 64)) | (1L << (T__72 - 64)) | (1L << (T__73 - 64)) | (1L << (T__74 - 64)) | (1L << (T__75 - 64)) | (1L << (WS - 64)) | (1L << (SL_COMMENT - 64)) | (1L << (ML_COMMENT - 64)) | (1L << (ID - 64)) | (1L << (USER_PARAMETER - 64)) | (1L << (NUM - 64)) | (1L << (JSTR - 64)) | (1L << (STR - 64)) | (1L << (LJQUOTE - 64)) | (1L << (RJQUOTE - 64)) | (1L << (LSHIFT - 64)) | (1L << (RSHIFT - 64)) | (1L << (LSQ - 64)) | (1L << (RSQ - 64)) | (1L << (LPAREN - 64)) | (1L << (RPAREN - 64)) | (1L << (AT - 64)) | (1L << (OR - 64)) | (1L << (AND - 64)) | (1L << (SEMI - 64)) | (1L << (COLON - 64)) | (1L << (COMMA - 64)) | (1L << (DOT - 64)) | (1L << (STAR - 64)) | (1L << (EQ - 64)) | (1L << (PLUS - 64)) | (1L << (MINUS - 64)) | (1L << (DIV - 64)) | (1L << (EXP - 64)) | (1L << (DOLLAR - 64)))) != 0) );
			setState(212);
			match(RBRACE);
			setState(213);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Value_assignContext extends ParserRuleContext {
		public Assign_parameterContext assign_parameter() {
			return getRuleContext(Assign_parameterContext.class,0);
		}
		public TerminalNode EQ() { return getToken(JSpecParser.EQ, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public Defined_attributeContext defined_attribute() {
			return getRuleContext(Defined_attributeContext.class,0);
		}
		public Num_expressionContext num_expression() {
			return getRuleContext(Num_expressionContext.class,0);
		}
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public JstrContext jstr() {
			return getRuleContext(JstrContext.class,0);
		}
		public Defined_attribute_setContext defined_attribute_set() {
			return getRuleContext(Defined_attribute_setContext.class,0);
		}
		public Value_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value_assign; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterValue_assign(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitValue_assign(this);
		}
	}

	public final Value_assignContext value_assign() throws RecognitionException {
		Value_assignContext _localctx = new Value_assignContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_value_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(215);
			assign_parameter();
			setState(216);
			match(EQ);
			setState(222);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				{
				setState(217);
				defined_attribute();
				}
				break;
			case 2:
				{
				setState(218);
				num_expression(0);
				}
				break;
			case 3:
				{
				setState(219);
				str();
				}
				break;
			case 4:
				{
				setState(220);
				jstr();
				}
				break;
			case 5:
				{
				setState(221);
				defined_attribute_set();
				}
				break;
			}
			setState(224);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Typedef_instanceContext extends ParserRuleContext {
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public Param_blockContext param_block() {
			return getRuleContext(Param_blockContext.class,0);
		}
		public Typedef_instanceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typedef_instance; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterTypedef_instance(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitTypedef_instance(this);
		}
	}

	public final Typedef_instanceContext typedef_instance() throws RecognitionException {
		Typedef_instanceContext _localctx = new Typedef_instanceContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_typedef_instance);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			id();
			setState(227);
			id();
			setState(228);
			str();
			setState(230);
			_la = _input.LA(1);
			if (_la==T__4) {
				{
				setState(229);
				param_block();
				}
			}

			setState(232);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Param_blockContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public List<Value_assignContext> value_assign() {
			return getRuleContexts(Value_assignContext.class);
		}
		public Value_assignContext value_assign(int i) {
			return getRuleContext(Value_assignContext.class,i);
		}
		public Param_blockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_param_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterParam_block(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitParam_block(this);
		}
	}

	public final Param_blockContext param_block() throws RecognitionException {
		Param_blockContext _localctx = new Param_blockContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_param_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(234);
			match(T__4);
			setState(235);
			match(LBRACE);
			setState(239);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31))) != 0) || _la==ID) {
				{
				{
				setState(236);
				value_assign();
				}
				}
				setState(241);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(242);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Assign_parameterContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JSpecParser.ID, 0); }
		public Assign_parameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assign_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterAssign_parameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitAssign_parameter(this);
		}
	}

	public final Assign_parameterContext assign_parameter() throws RecognitionException {
		Assign_parameterContext _localctx = new Assign_parameterContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_assign_parameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31))) != 0) || _la==ID) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_attributeContext extends ParserRuleContext {
		public Defined_attributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_attribute; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterDefined_attribute(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitDefined_attribute(this);
		}
	}

	public final Defined_attributeContext defined_attribute() throws RecognitionException {
		Defined_attributeContext _localctx = new Defined_attributeContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_defined_attribute);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(246);
			_la = _input.LA(1);
			if ( !(((((_la - 33)) & ~0x3f) == 0 && ((1L << (_la - 33)) & ((1L << (T__32 - 33)) | (1L << (T__33 - 33)) | (1L << (T__34 - 33)) | (1L << (T__35 - 33)) | (1L << (T__36 - 33)) | (1L << (T__37 - 33)) | (1L << (T__38 - 33)) | (1L << (T__39 - 33)) | (1L << (T__40 - 33)) | (1L << (T__41 - 33)) | (1L << (T__42 - 33)) | (1L << (T__43 - 33)) | (1L << (T__44 - 33)) | (1L << (T__45 - 33)) | (1L << (T__46 - 33)) | (1L << (T__47 - 33)) | (1L << (T__48 - 33)) | (1L << (T__49 - 33)) | (1L << (T__50 - 33)) | (1L << (T__51 - 33)) | (1L << (T__52 - 33)) | (1L << (T__53 - 33)) | (1L << (T__54 - 33)) | (1L << (T__55 - 33)) | (1L << (T__56 - 33)) | (1L << (T__57 - 33)) | (1L << (T__58 - 33)) | (1L << (T__59 - 33)) | (1L << (T__60 - 33)) | (1L << (T__61 - 33)) | (1L << (T__62 - 33)) | (1L << (T__63 - 33)) | (1L << (T__64 - 33)) | (1L << (T__65 - 33)) | (1L << (T__66 - 33)) | (1L << (T__67 - 33)) | (1L << (T__68 - 33)) | (1L << (T__69 - 33)))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_attribute_setContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public List<Defined_attributeContext> defined_attribute() {
			return getRuleContexts(Defined_attributeContext.class);
		}
		public Defined_attributeContext defined_attribute(int i) {
			return getRuleContext(Defined_attributeContext.class,i);
		}
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JSpecParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JSpecParser.COMMA, i);
		}
		public List<TerminalNode> OR() { return getTokens(JSpecParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(JSpecParser.OR, i);
		}
		public Defined_attribute_setContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_attribute_set; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterDefined_attribute_set(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitDefined_attribute_set(this);
		}
	}

	public final Defined_attribute_setContext defined_attribute_set() throws RecognitionException {
		Defined_attribute_setContext _localctx = new Defined_attribute_setContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_defined_attribute_set);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(248);
				match(LBRACE);
				setState(249);
				defined_attribute();
				setState(254);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(250);
					match(COMMA);
					setState(251);
					defined_attribute();
					}
					}
					setState(256);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(257);
				match(RBRACE);
				}
				break;
			case T__32:
			case T__33:
			case T__34:
			case T__35:
			case T__36:
			case T__37:
			case T__38:
			case T__39:
			case T__40:
			case T__41:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case T__46:
			case T__47:
			case T__48:
			case T__49:
			case T__50:
			case T__51:
			case T__52:
			case T__53:
			case T__54:
			case T__55:
			case T__56:
			case T__57:
			case T__58:
			case T__59:
			case T__60:
			case T__61:
			case T__62:
			case T__63:
			case T__64:
			case T__65:
			case T__66:
			case T__67:
			case T__68:
			case T__69:
				{
				setState(259);
				defined_attribute();
				setState(264);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==OR) {
					{
					{
					setState(260);
					match(OR);
					setState(261);
					defined_attribute();
					}
					}
					setState(266);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Register_set_defContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public JstrContext jstr() {
			return getRuleContext(JstrContext.class,0);
		}
		public List<Value_assignContext> value_assign() {
			return getRuleContexts(Value_assignContext.class);
		}
		public Value_assignContext value_assign(int i) {
			return getRuleContext(Value_assignContext.class,i);
		}
		public List<Register_set_defContext> register_set_def() {
			return getRuleContexts(Register_set_defContext.class);
		}
		public Register_set_defContext register_set_def(int i) {
			return getRuleContext(Register_set_defContext.class,i);
		}
		public List<Register_defContext> register_def() {
			return getRuleContexts(Register_defContext.class);
		}
		public Register_defContext register_def(int i) {
			return getRuleContext(Register_defContext.class,i);
		}
		public List<Typedef_instanceContext> typedef_instance() {
			return getRuleContexts(Typedef_instanceContext.class);
		}
		public Typedef_instanceContext typedef_instance(int i) {
			return getRuleContext(Typedef_instanceContext.class,i);
		}
		public List<Type_definitionContext> type_definition() {
			return getRuleContexts(Type_definitionContext.class);
		}
		public Type_definitionContext type_definition(int i) {
			return getRuleContext(Type_definitionContext.class,i);
		}
		public List<Param_type_definitionContext> param_type_definition() {
			return getRuleContexts(Param_type_definitionContext.class);
		}
		public Param_type_definitionContext param_type_definition(int i) {
			return getRuleContext(Param_type_definitionContext.class,i);
		}
		public List<Test_group_defContext> test_group_def() {
			return getRuleContexts(Test_group_defContext.class);
		}
		public Test_group_defContext test_group_def(int i) {
			return getRuleContext(Test_group_defContext.class,i);
		}
		public List<Num_constant_defContext> num_constant_def() {
			return getRuleContexts(Num_constant_defContext.class);
		}
		public Num_constant_defContext num_constant_def(int i) {
			return getRuleContext(Num_constant_defContext.class,i);
		}
		public List<String_constant_defContext> string_constant_def() {
			return getRuleContexts(String_constant_defContext.class);
		}
		public String_constant_defContext string_constant_def(int i) {
			return getRuleContext(String_constant_defContext.class,i);
		}
		public Register_set_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_register_set_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterRegister_set_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitRegister_set_def(this);
		}
	}

	public final Register_set_defContext register_set_def() throws RecognitionException {
		Register_set_defContext _localctx = new Register_set_defContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_register_set_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			match(T__70);
			setState(270);
			id();
			setState(273);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(271);
				str();
				}
				break;
			case JSTR:
				{
				setState(272);
				jstr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(275);
			match(LBRACE);
			setState(285); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(285);
				switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
				case 1:
					{
					setState(276);
					value_assign();
					}
					break;
				case 2:
					{
					setState(277);
					register_set_def();
					}
					break;
				case 3:
					{
					setState(278);
					register_def();
					}
					break;
				case 4:
					{
					setState(279);
					typedef_instance();
					}
					break;
				case 5:
					{
					setState(280);
					type_definition();
					}
					break;
				case 6:
					{
					setState(281);
					param_type_definition();
					}
					break;
				case 7:
					{
					setState(282);
					test_group_def();
					}
					break;
				case 8:
					{
					setState(283);
					num_constant_def();
					}
					break;
				case 9:
					{
					setState(284);
					string_constant_def();
					}
					break;
				}
				}
				setState(287); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__3) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (T__70 - 71)) | (1L << (T__71 - 71)) | (1L << (T__74 - 71)) | (1L << (ID - 71)))) != 0) );
			setState(289);
			match(RBRACE);
			setState(290);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Register_defContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public JstrContext jstr() {
			return getRuleContext(JstrContext.class,0);
		}
		public List<Value_assignContext> value_assign() {
			return getRuleContexts(Value_assignContext.class);
		}
		public Value_assignContext value_assign(int i) {
			return getRuleContext(Value_assignContext.class,i);
		}
		public List<Field_defContext> field_def() {
			return getRuleContexts(Field_defContext.class);
		}
		public Field_defContext field_def(int i) {
			return getRuleContext(Field_defContext.class,i);
		}
		public List<Typedef_instanceContext> typedef_instance() {
			return getRuleContexts(Typedef_instanceContext.class);
		}
		public Typedef_instanceContext typedef_instance(int i) {
			return getRuleContext(Typedef_instanceContext.class,i);
		}
		public List<Type_definitionContext> type_definition() {
			return getRuleContexts(Type_definitionContext.class);
		}
		public Type_definitionContext type_definition(int i) {
			return getRuleContext(Type_definitionContext.class,i);
		}
		public Register_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_register_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterRegister_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitRegister_def(this);
		}
	}

	public final Register_defContext register_def() throws RecognitionException {
		Register_defContext _localctx = new Register_defContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_register_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(292);
			match(T__71);
			setState(293);
			id();
			setState(296);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(294);
				str();
				}
				break;
			case JSTR:
				{
				setState(295);
				jstr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(298);
			match(LBRACE);
			setState(303); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(303);
				switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
				case 1:
					{
					setState(299);
					value_assign();
					}
					break;
				case 2:
					{
					setState(300);
					field_def();
					}
					break;
				case 3:
					{
					setState(301);
					typedef_instance();
					}
					break;
				case 4:
					{
					setState(302);
					type_definition();
					}
					break;
				}
				}
				setState(305); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31))) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & ((1L << (T__72 - 73)) | (1L << (T__73 - 73)) | (1L << (ID - 73)))) != 0) );
			setState(307);
			match(RBRACE);
			setState(308);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Field_defContext extends ParserRuleContext {
		public Field_set_defContext field_set_def() {
			return getRuleContext(Field_set_defContext.class,0);
		}
		public Int_field_defContext int_field_def() {
			return getRuleContext(Int_field_defContext.class,0);
		}
		public Enum_field_defContext enum_field_def() {
			return getRuleContext(Enum_field_defContext.class,0);
		}
		public Nop_field_defContext nop_field_def() {
			return getRuleContext(Nop_field_defContext.class,0);
		}
		public Field_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterField_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitField_def(this);
		}
	}

	public final Field_defContext field_def() throws RecognitionException {
		Field_defContext _localctx = new Field_defContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_field_def);
		try {
			setState(314);
			switch (_input.LA(1)) {
			case T__72:
				enterOuterAlt(_localctx, 1);
				{
				setState(310);
				field_set_def();
				}
				break;
			case T__1:
				enterOuterAlt(_localctx, 2);
				{
				setState(311);
				int_field_def();
				}
				break;
			case T__2:
				enterOuterAlt(_localctx, 3);
				{
				setState(312);
				enum_field_def();
				}
				break;
			case T__73:
				enterOuterAlt(_localctx, 4);
				{
				setState(313);
				nop_field_def();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Field_set_defContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public JstrContext jstr() {
			return getRuleContext(JstrContext.class,0);
		}
		public List<Value_assignContext> value_assign() {
			return getRuleContexts(Value_assignContext.class);
		}
		public Value_assignContext value_assign(int i) {
			return getRuleContext(Value_assignContext.class,i);
		}
		public List<Int_field_defContext> int_field_def() {
			return getRuleContexts(Int_field_defContext.class);
		}
		public Int_field_defContext int_field_def(int i) {
			return getRuleContext(Int_field_defContext.class,i);
		}
		public List<Enum_field_defContext> enum_field_def() {
			return getRuleContexts(Enum_field_defContext.class);
		}
		public Enum_field_defContext enum_field_def(int i) {
			return getRuleContext(Enum_field_defContext.class,i);
		}
		public List<Nop_field_defContext> nop_field_def() {
			return getRuleContexts(Nop_field_defContext.class);
		}
		public Nop_field_defContext nop_field_def(int i) {
			return getRuleContext(Nop_field_defContext.class,i);
		}
		public List<Field_set_defContext> field_set_def() {
			return getRuleContexts(Field_set_defContext.class);
		}
		public Field_set_defContext field_set_def(int i) {
			return getRuleContext(Field_set_defContext.class,i);
		}
		public List<Typedef_instanceContext> typedef_instance() {
			return getRuleContexts(Typedef_instanceContext.class);
		}
		public Typedef_instanceContext typedef_instance(int i) {
			return getRuleContext(Typedef_instanceContext.class,i);
		}
		public Field_set_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field_set_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterField_set_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitField_set_def(this);
		}
	}

	public final Field_set_defContext field_set_def() throws RecognitionException {
		Field_set_defContext _localctx = new Field_set_defContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_field_set_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(316);
			match(T__72);
			setState(317);
			id();
			setState(320);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(318);
				str();
				}
				break;
			case JSTR:
				{
				setState(319);
				jstr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(322);
			match(LBRACE);
			setState(331);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__2) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31))) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & ((1L << (T__72 - 73)) | (1L << (T__73 - 73)) | (1L << (ID - 73)))) != 0)) {
				{
				setState(329);
				switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
				case 1:
					{
					setState(323);
					value_assign();
					}
					break;
				case 2:
					{
					setState(324);
					int_field_def();
					}
					break;
				case 3:
					{
					setState(325);
					enum_field_def();
					}
					break;
				case 4:
					{
					setState(326);
					nop_field_def();
					}
					break;
				case 5:
					{
					setState(327);
					field_set_def();
					}
					break;
				case 6:
					{
					setState(328);
					typedef_instance();
					}
					break;
				}
				}
				setState(333);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(334);
			match(RBRACE);
			setState(335);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Int_field_defContext extends ParserRuleContext {
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public StrContext str() {
			return getRuleContext(StrContext.class,0);
		}
		public JstrContext jstr() {
			return getRuleContext(JstrContext.class,0);
		}
		public Param_blockContext param_block() {
			return getRuleContext(Param_blockContext.class,0);
		}
		public Int_field_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_int_field_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterInt_field_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitInt_field_def(this);
		}
	}

	public final Int_field_defContext int_field_def() throws RecognitionException {
		Int_field_defContext _localctx = new Int_field_defContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_int_field_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(337);
			match(T__1);
			setState(339);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(338);
				id();
				}
			}

			setState(341);
			array();
			setState(344);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(342);
				str();
				}
				break;
			case JSTR:
				{
				setState(343);
				jstr();
				}
				break;
			case T__4:
			case SEMI:
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(347);
			_la = _input.LA(1);
			if (_la==T__4) {
				{
				setState(346);
				param_block();
				}
			}

			setState(349);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nop_field_defContext extends ParserRuleContext {
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(JSpecParser.SEMI, 0); }
		public Param_blockContext param_block() {
			return getRuleContext(Param_blockContext.class,0);
		}
		public Nop_field_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nop_field_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterNop_field_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitNop_field_def(this);
		}
	}

	public final Nop_field_defContext nop_field_def() throws RecognitionException {
		Nop_field_defContext _localctx = new Nop_field_defContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_nop_field_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(351);
			match(T__73);
			setState(352);
			array();
			setState(354);
			_la = _input.LA(1);
			if (_la==T__4) {
				{
				setState(353);
				param_block();
				}
			}

			setState(356);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Test_group_defContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(JSpecParser.LBRACE, 0); }
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<TerminalNode> SEMI() { return getTokens(JSpecParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(JSpecParser.SEMI, i);
		}
		public TerminalNode RBRACE() { return getToken(JSpecParser.RBRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(JSpecParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(JSpecParser.COMMA, i);
		}
		public Test_group_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_test_group_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterTest_group_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitTest_group_def(this);
		}
	}

	public final Test_group_defContext test_group_def() throws RecognitionException {
		Test_group_defContext _localctx = new Test_group_defContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_test_group_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(358);
			match(T__74);
			setState(359);
			match(LBRACE);
			setState(360);
			id();
			setState(365);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(361);
				match(COMMA);
				setState(362);
				id();
				}
				}
				setState(367);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(368);
			match(SEMI);
			setState(369);
			match(RBRACE);
			setState(370);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayContext extends ParserRuleContext {
		public TerminalNode LSQ() { return getToken(JSpecParser.LSQ, 0); }
		public Num_expressionContext num_expression() {
			return getRuleContext(Num_expressionContext.class,0);
		}
		public TerminalNode RSQ() { return getToken(JSpecParser.RSQ, 0); }
		public ArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitArray(this);
		}
	}

	public final ArrayContext array() throws RecognitionException {
		ArrayContext _localctx = new ArrayContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_array);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(372);
			match(LSQ);
			setState(373);
			num_expression(0);
			setState(374);
			match(RSQ);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Num_expressionContext extends ParserRuleContext {
		public Token op;
		public TerminalNode LPAREN() { return getToken(JSpecParser.LPAREN, 0); }
		public List<Num_expressionContext> num_expression() {
			return getRuleContexts(Num_expressionContext.class);
		}
		public Num_expressionContext num_expression(int i) {
			return getRuleContext(Num_expressionContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(JSpecParser.RPAREN, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode NUM() { return getToken(JSpecParser.NUM, 0); }
		public TerminalNode ID() { return getToken(JSpecParser.ID, 0); }
		public TerminalNode PLUS() { return getToken(JSpecParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(JSpecParser.MINUS, 0); }
		public TerminalNode STAR() { return getToken(JSpecParser.STAR, 0); }
		public TerminalNode DIV() { return getToken(JSpecParser.DIV, 0); }
		public TerminalNode EXP() { return getToken(JSpecParser.EXP, 0); }
		public TerminalNode LSHIFT() { return getToken(JSpecParser.LSHIFT, 0); }
		public TerminalNode RSHIFT() { return getToken(JSpecParser.RSHIFT, 0); }
		public TerminalNode OR() { return getToken(JSpecParser.OR, 0); }
		public TerminalNode AND() { return getToken(JSpecParser.AND, 0); }
		public Num_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_num_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterNum_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitNum_expression(this);
		}
	}

	public final Num_expressionContext num_expression() throws RecognitionException {
		return num_expression(0);
	}

	private Num_expressionContext num_expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Num_expressionContext _localctx = new Num_expressionContext(_ctx, _parentState);
		Num_expressionContext _prevctx = _localctx;
		int _startState = 50;
		enterRecursionRule(_localctx, 50, RULE_num_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(388);
			switch (_input.LA(1)) {
			case LPAREN:
				{
				setState(377);
				match(LPAREN);
				setState(378);
				num_expression(0);
				setState(379);
				match(RPAREN);
				}
				break;
			case T__75:
				{
				setState(381);
				match(T__75);
				setState(382);
				match(LPAREN);
				setState(383);
				id();
				setState(384);
				match(RPAREN);
				}
				break;
			case NUM:
				{
				setState(386);
				match(NUM);
				}
				break;
			case ID:
				{
				setState(387);
				match(ID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(395);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,43,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Num_expressionContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_num_expression);
					setState(390);
					if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
					setState(391);
					((Num_expressionContext)_localctx).op = _input.LT(1);
					_la = _input.LA(1);
					if ( !(((((_la - 87)) & ~0x3f) == 0 && ((1L << (_la - 87)) & ((1L << (LSHIFT - 87)) | (1L << (RSHIFT - 87)) | (1L << (OR - 87)) | (1L << (AND - 87)) | (1L << (STAR - 87)) | (1L << (PLUS - 87)) | (1L << (MINUS - 87)) | (1L << (DIV - 87)) | (1L << (EXP - 87)))) != 0)) ) {
						((Num_expressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
					} else {
						consume();
					}
					setState(392);
					num_expression(6);
					}
					} 
				}
				setState(397);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,43,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class IdContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(JSpecParser.ID, 0); }
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitId(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_id);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(398);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JstrContext extends ParserRuleContext {
		public TerminalNode JSTR() { return getToken(JSpecParser.JSTR, 0); }
		public JstrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jstr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterJstr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitJstr(this);
		}
	}

	public final JstrContext jstr() throws RecognitionException {
		JstrContext _localctx = new JstrContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_jstr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(400);
			match(JSTR);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StrContext extends ParserRuleContext {
		public TerminalNode STR() { return getToken(JSpecParser.STR, 0); }
		public StrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_str; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).enterStr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JSpecListener ) ((JSpecListener)listener).exitStr(this);
		}
	}

	public final StrContext str() throws RecognitionException {
		StrContext _localctx = new StrContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_str);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(402);
			match(STR);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 25:
			return num_expression_sempred((Num_expressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean num_expression_sempred(Num_expressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 5);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3n\u0197\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\3\2\3\2\3\2\3\2\3\2"+
		"\3\2\7\2C\n\2\f\2\16\2F\13\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\6\3P\n\3"+
		"\r\3\16\3Q\5\3T\n\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3"+
		"\5\7\5c\n\5\f\5\16\5f\13\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\5\5s\n\5\3\5\5\5v\n\5\3\5\3\5\5\5z\n\5\3\6\3\6\3\6\3\6\3\6\6\6\u0081"+
		"\n\6\r\6\16\6\u0082\3\6\3\6\3\6\3\7\6\7\u0089\n\7\r\7\16\7\u008a\3\7\3"+
		"\7\3\7\5\7\u0090\n\7\3\7\7\7\u0093\n\7\f\7\16\7\u0096\13\7\3\7\7\7\u0099"+
		"\n\7\f\7\16\7\u009c\13\7\3\b\3\b\3\b\3\b\3\b\5\b\u00a3\n\b\3\t\3\t\3\t"+
		"\3\t\3\t\3\t\3\t\3\t\3\n\3\n\5\n\u00af\n\n\3\n\3\n\3\n\5\n\u00b4\n\n\3"+
		"\n\3\n\6\n\u00b8\n\n\r\n\16\n\u00b9\3\n\3\n\5\n\u00be\n\n\3\n\3\n\3\13"+
		"\3\13\3\13\3\13\3\13\5\13\u00c7\n\13\3\13\3\13\3\f\3\f\3\f\3\f\5\f\u00cf"+
		"\n\f\3\f\3\f\6\f\u00d3\n\f\r\f\16\f\u00d4\3\f\3\f\3\f\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\5\r\u00e1\n\r\3\r\3\r\3\16\3\16\3\16\3\16\5\16\u00e9\n\16"+
		"\3\16\3\16\3\17\3\17\3\17\7\17\u00f0\n\17\f\17\16\17\u00f3\13\17\3\17"+
		"\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\22\3\22\7\22\u00ff\n\22\f\22\16"+
		"\22\u0102\13\22\3\22\3\22\3\22\3\22\3\22\7\22\u0109\n\22\f\22\16\22\u010c"+
		"\13\22\5\22\u010e\n\22\3\23\3\23\3\23\3\23\5\23\u0114\n\23\3\23\3\23\3"+
		"\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\6\23\u0120\n\23\r\23\16\23\u0121"+
		"\3\23\3\23\3\23\3\24\3\24\3\24\3\24\5\24\u012b\n\24\3\24\3\24\3\24\3\24"+
		"\3\24\6\24\u0132\n\24\r\24\16\24\u0133\3\24\3\24\3\24\3\25\3\25\3\25\3"+
		"\25\5\25\u013d\n\25\3\26\3\26\3\26\3\26\5\26\u0143\n\26\3\26\3\26\3\26"+
		"\3\26\3\26\3\26\3\26\7\26\u014c\n\26\f\26\16\26\u014f\13\26\3\26\3\26"+
		"\3\26\3\27\3\27\5\27\u0156\n\27\3\27\3\27\3\27\5\27\u015b\n\27\3\27\5"+
		"\27\u015e\n\27\3\27\3\27\3\30\3\30\3\30\5\30\u0165\n\30\3\30\3\30\3\31"+
		"\3\31\3\31\3\31\3\31\7\31\u016e\n\31\f\31\16\31\u0171\13\31\3\31\3\31"+
		"\3\31\3\31\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33"+
		"\3\33\3\33\3\33\3\33\5\33\u0187\n\33\3\33\3\33\3\33\7\33\u018c\n\33\f"+
		"\33\16\33\u018f\13\33\3\34\3\34\3\35\3\35\3\36\3\36\3\36\3\u0094\3\64"+
		"\37\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:\2\6"+
		"\3\2[\\\4\2\n\"RR\3\2#H\6\2YZbchhjm\u01c4\2D\3\2\2\2\4I\3\2\2\2\6X\3\2"+
		"\2\2\by\3\2\2\2\n{\3\2\2\2\f\u0088\3\2\2\2\16\u009d\3\2\2\2\20\u00a4\3"+
		"\2\2\2\22\u00ac\3\2\2\2\24\u00c1\3\2\2\2\26\u00ca\3\2\2\2\30\u00d9\3\2"+
		"\2\2\32\u00e4\3\2\2\2\34\u00ec\3\2\2\2\36\u00f6\3\2\2\2 \u00f8\3\2\2\2"+
		"\"\u010d\3\2\2\2$\u010f\3\2\2\2&\u0126\3\2\2\2(\u013c\3\2\2\2*\u013e\3"+
		"\2\2\2,\u0153\3\2\2\2.\u0161\3\2\2\2\60\u0168\3\2\2\2\62\u0176\3\2\2\2"+
		"\64\u0186\3\2\2\2\66\u0190\3\2\2\28\u0192\3\2\2\2:\u0194\3\2\2\2<C\5\4"+
		"\3\2=C\5\n\6\2>C\5\16\b\2?C\5\20\t\2@C\5\32\16\2AC\5$\23\2B<\3\2\2\2B"+
		"=\3\2\2\2B>\3\2\2\2B?\3\2\2\2B@\3\2\2\2BA\3\2\2\2CF\3\2\2\2DB\3\2\2\2"+
		"DE\3\2\2\2EG\3\2\2\2FD\3\2\2\2GH\7\2\2\3H\3\3\2\2\2IJ\7\3\2\2JK\5\66\34"+
		"\2KL\5:\36\2LS\7[\2\2MT\5\6\4\2NP\5\b\5\2ON\3\2\2\2PQ\3\2\2\2QO\3\2\2"+
		"\2QR\3\2\2\2RT\3\2\2\2SM\3\2\2\2SO\3\2\2\2TU\3\2\2\2UV\7\\\2\2VW\7d\2"+
		"\2W\5\3\2\2\2XY\7\4\2\2YZ\5\62\32\2Z[\7i\2\2[\\\5\64\33\2\\]\7d\2\2]\7"+
		"\3\2\2\2^_\7\5\2\2_`\5\62\32\2`d\7[\2\2ac\n\2\2\2ba\3\2\2\2cf\3\2\2\2"+
		"db\3\2\2\2de\3\2\2\2eg\3\2\2\2fd\3\2\2\2gh\7\\\2\2hi\7d\2\2iz\3\2\2\2"+
		"jk\7\4\2\2kl\5\66\34\2lm\5\62\32\2mn\5:\36\2no\7d\2\2oz\3\2\2\2pr\5\66"+
		"\34\2qs\5\66\34\2rq\3\2\2\2rs\3\2\2\2su\3\2\2\2tv\5:\36\2ut\3\2\2\2uv"+
		"\3\2\2\2vw\3\2\2\2wx\7d\2\2xz\3\2\2\2y^\3\2\2\2yj\3\2\2\2yp\3\2\2\2z\t"+
		"\3\2\2\2{|\7\3\2\2|}\5\66\34\2}~\5:\36\2~\u0080\7[\2\2\177\u0081\5\f\7"+
		"\2\u0080\177\3\2\2\2\u0081\u0082\3\2\2\2\u0082\u0080\3\2\2\2\u0082\u0083"+
		"\3\2\2\2\u0083\u0084\3\2\2\2\u0084\u0085\7\\\2\2\u0085\u0086\7d\2\2\u0086"+
		"\13\3\2\2\2\u0087\u0089\n\2\2\2\u0088\u0087\3\2\2\2\u0089\u008a\3\2\2"+
		"\2\u008a\u0088\3\2\2\2\u008a\u008b\3\2\2\2\u008b\u0094\3\2\2\2\u008c\u008f"+
		"\7[\2\2\u008d\u0090\n\2\2\2\u008e\u0090\5\f\7\2\u008f\u008d\3\2\2\2\u008f"+
		"\u008e\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u0091\3\2\2\2\u0091\u0093\7\\"+
		"\2\2\u0092\u008c\3\2\2\2\u0093\u0096\3\2\2\2\u0094\u0095\3\2\2\2\u0094"+
		"\u0092\3\2\2\2\u0095\u009a\3\2\2\2\u0096\u0094\3\2\2\2\u0097\u0099\n\2"+
		"\2\2\u0098\u0097\3\2\2\2\u0099\u009c\3\2\2\2\u009a\u0098\3\2\2\2\u009a"+
		"\u009b\3\2\2\2\u009b\r\3\2\2\2\u009c\u009a\3\2\2\2\u009d\u00a2\7\6\2\2"+
		"\u009e\u00a3\5$\23\2\u009f\u00a3\5&\24\2\u00a0\u00a3\5(\25\2\u00a1\u00a3"+
		"\5\26\f\2\u00a2\u009e\3\2\2\2\u00a2\u009f\3\2\2\2\u00a2\u00a0\3\2\2\2"+
		"\u00a2\u00a1\3\2\2\2\u00a3\17\3\2\2\2\u00a4\u00a5\7\6\2\2\u00a5\u00a6"+
		"\7\7\2\2\u00a6\u00a7\5\66\34\2\u00a7\u00a8\b\t\1\2\u00a8\u00a9\7i\2\2"+
		"\u00a9\u00aa\7\b\2\2\u00aa\u00ab\7d\2\2\u00ab\21\3\2\2\2\u00ac\u00ae\7"+
		"\5\2\2\u00ad\u00af\5\66\34\2\u00ae\u00ad\3\2\2\2\u00ae\u00af\3\2\2\2\u00af"+
		"\u00b0\3\2\2\2\u00b0\u00b3\5\62\32\2\u00b1\u00b4\5:\36\2\u00b2\u00b4\5"+
		"8\35\2\u00b3\u00b1\3\2\2\2\u00b3\u00b2\3\2\2\2\u00b3\u00b4\3\2\2\2\u00b4"+
		"\u00b5\3\2\2\2\u00b5\u00b7\7[\2\2\u00b6\u00b8\5\24\13\2\u00b7\u00b6\3"+
		"\2\2\2\u00b8\u00b9\3\2\2\2\u00b9\u00b7\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba"+
		"\u00bb\3\2\2\2\u00bb\u00bd\7\\\2\2\u00bc\u00be\5\34\17\2\u00bd\u00bc\3"+
		"\2\2\2\u00bd\u00be\3\2\2\2\u00be\u00bf\3\2\2\2\u00bf\u00c0\7d\2\2\u00c0"+
		"\23\3\2\2\2\u00c1\u00c2\5\66\34\2\u00c2\u00c3\7i\2\2\u00c3\u00c6\5\64"+
		"\33\2\u00c4\u00c7\5:\36\2\u00c5\u00c7\58\35\2\u00c6\u00c4\3\2\2\2\u00c6"+
		"\u00c5\3\2\2\2\u00c7\u00c8\3\2\2\2\u00c8\u00c9\7d\2\2\u00c9\25\3\2\2\2"+
		"\u00ca\u00cb\7\t\2\2\u00cb\u00ce\5\66\34\2\u00cc\u00cf\5:\36\2\u00cd\u00cf"+
		"\58\35\2\u00ce\u00cc\3\2\2\2\u00ce\u00cd\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf"+
		"\u00d0\3\2\2\2\u00d0\u00d2\7[\2\2\u00d1\u00d3\5\f\7\2\u00d2\u00d1\3\2"+
		"\2\2\u00d3\u00d4\3\2\2\2\u00d4\u00d2\3\2\2\2\u00d4\u00d5\3\2\2\2\u00d5"+
		"\u00d6\3\2\2\2\u00d6\u00d7\7\\\2\2\u00d7\u00d8\7d\2\2\u00d8\27\3\2\2\2"+
		"\u00d9\u00da\5\36\20\2\u00da\u00e0\7i\2\2\u00db\u00e1\5 \21\2\u00dc\u00e1"+
		"\5\64\33\2\u00dd\u00e1\5:\36\2\u00de\u00e1\58\35\2\u00df\u00e1\5\"\22"+
		"\2\u00e0\u00db\3\2\2\2\u00e0\u00dc\3\2\2\2\u00e0\u00dd\3\2\2\2\u00e0\u00de"+
		"\3\2\2\2\u00e0\u00df\3\2\2\2\u00e1\u00e2\3\2\2\2\u00e2\u00e3\7d\2\2\u00e3"+
		"\31\3\2\2\2\u00e4\u00e5\5\66\34\2\u00e5\u00e6\5\66\34\2\u00e6\u00e8\5"+
		":\36\2\u00e7\u00e9\5\34\17\2\u00e8\u00e7\3\2\2\2\u00e8\u00e9\3\2\2\2\u00e9"+
		"\u00ea\3\2\2\2\u00ea\u00eb\7d\2\2\u00eb\33\3\2\2\2\u00ec\u00ed\7\7\2\2"+
		"\u00ed\u00f1\7[\2\2\u00ee\u00f0\5\30\r\2\u00ef\u00ee\3\2\2\2\u00f0\u00f3"+
		"\3\2\2\2\u00f1\u00ef\3\2\2\2\u00f1\u00f2\3\2\2\2\u00f2\u00f4\3\2\2\2\u00f3"+
		"\u00f1\3\2\2\2\u00f4\u00f5\7\\\2\2\u00f5\35\3\2\2\2\u00f6\u00f7\t\3\2"+
		"\2\u00f7\37\3\2\2\2\u00f8\u00f9\t\4\2\2\u00f9!\3\2\2\2\u00fa\u00fb\7["+
		"\2\2\u00fb\u0100\5 \21\2\u00fc\u00fd\7f\2\2\u00fd\u00ff\5 \21\2\u00fe"+
		"\u00fc\3\2\2\2\u00ff\u0102\3\2\2\2\u0100\u00fe\3\2\2\2\u0100\u0101\3\2"+
		"\2\2\u0101\u0103\3\2\2\2\u0102\u0100\3\2\2\2\u0103\u0104\7\\\2\2\u0104"+
		"\u010e\3\2\2\2\u0105\u010a\5 \21\2\u0106\u0107\7b\2\2\u0107\u0109\5 \21"+
		"\2\u0108\u0106\3\2\2\2\u0109\u010c\3\2\2\2\u010a\u0108\3\2\2\2\u010a\u010b"+
		"\3\2\2\2\u010b\u010e\3\2\2\2\u010c\u010a\3\2\2\2\u010d\u00fa\3\2\2\2\u010d"+
		"\u0105\3\2\2\2\u010e#\3\2\2\2\u010f\u0110\7I\2\2\u0110\u0113\5\66\34\2"+
		"\u0111\u0114\5:\36\2\u0112\u0114\58\35\2\u0113\u0111\3\2\2\2\u0113\u0112"+
		"\3\2\2\2\u0114\u0115\3\2\2\2\u0115\u011f\7[\2\2\u0116\u0120\5\30\r\2\u0117"+
		"\u0120\5$\23\2\u0118\u0120\5&\24\2\u0119\u0120\5\32\16\2\u011a\u0120\5"+
		"\16\b\2\u011b\u0120\5\20\t\2\u011c\u0120\5\60\31\2\u011d\u0120\5\4\3\2"+
		"\u011e\u0120\5\n\6\2\u011f\u0116\3\2\2\2\u011f\u0117\3\2\2\2\u011f\u0118"+
		"\3\2\2\2\u011f\u0119\3\2\2\2\u011f\u011a\3\2\2\2\u011f\u011b\3\2\2\2\u011f"+
		"\u011c\3\2\2\2\u011f\u011d\3\2\2\2\u011f\u011e\3\2\2\2\u0120\u0121\3\2"+
		"\2\2\u0121\u011f\3\2\2\2\u0121\u0122\3\2\2\2\u0122\u0123\3\2\2\2\u0123"+
		"\u0124\7\\\2\2\u0124\u0125\7d\2\2\u0125%\3\2\2\2\u0126\u0127\7J\2\2\u0127"+
		"\u012a\5\66\34\2\u0128\u012b\5:\36\2\u0129\u012b\58\35\2\u012a\u0128\3"+
		"\2\2\2\u012a\u0129\3\2\2\2\u012b\u012c\3\2\2\2\u012c\u0131\7[\2\2\u012d"+
		"\u0132\5\30\r\2\u012e\u0132\5(\25\2\u012f\u0132\5\32\16\2\u0130\u0132"+
		"\5\16\b\2\u0131\u012d\3\2\2\2\u0131\u012e\3\2\2\2\u0131\u012f\3\2\2\2"+
		"\u0131\u0130\3\2\2\2\u0132\u0133\3\2\2\2\u0133\u0131\3\2\2\2\u0133\u0134"+
		"\3\2\2\2\u0134\u0135\3\2\2\2\u0135\u0136\7\\\2\2\u0136\u0137\7d\2\2\u0137"+
		"\'\3\2\2\2\u0138\u013d\5*\26\2\u0139\u013d\5,\27\2\u013a\u013d\5\22\n"+
		"\2\u013b\u013d\5.\30\2\u013c\u0138\3\2\2\2\u013c\u0139\3\2\2\2\u013c\u013a"+
		"\3\2\2\2\u013c\u013b\3\2\2\2\u013d)\3\2\2\2\u013e\u013f\7K\2\2\u013f\u0142"+
		"\5\66\34\2\u0140\u0143\5:\36\2\u0141\u0143\58\35\2\u0142\u0140\3\2\2\2"+
		"\u0142\u0141\3\2\2\2\u0143\u0144\3\2\2\2\u0144\u014d\7[\2\2\u0145\u014c"+
		"\5\30\r\2\u0146\u014c\5,\27\2\u0147\u014c\5\22\n\2\u0148\u014c\5.\30\2"+
		"\u0149\u014c\5*\26\2\u014a\u014c\5\32\16\2\u014b\u0145\3\2\2\2\u014b\u0146"+
		"\3\2\2\2\u014b\u0147\3\2\2\2\u014b\u0148\3\2\2\2\u014b\u0149\3\2\2\2\u014b"+
		"\u014a\3\2\2\2\u014c\u014f\3\2\2\2\u014d\u014b\3\2\2\2\u014d\u014e\3\2"+
		"\2\2\u014e\u0150\3\2\2\2\u014f\u014d\3\2\2\2\u0150\u0151\7\\\2\2\u0151"+
		"\u0152\7d\2\2\u0152+\3\2\2\2\u0153\u0155\7\4\2\2\u0154\u0156\5\66\34\2"+
		"\u0155\u0154\3\2\2\2\u0155\u0156\3\2\2\2\u0156\u0157\3\2\2\2\u0157\u015a"+
		"\5\62\32\2\u0158\u015b\5:\36\2\u0159\u015b\58\35\2\u015a\u0158\3\2\2\2"+
		"\u015a\u0159\3\2\2\2\u015a\u015b\3\2\2\2\u015b\u015d\3\2\2\2\u015c\u015e"+
		"\5\34\17\2\u015d\u015c\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u015f\3\2\2\2"+
		"\u015f\u0160\7d\2\2\u0160-\3\2\2\2\u0161\u0162\7L\2\2\u0162\u0164\5\62"+
		"\32\2\u0163\u0165\5\34\17\2\u0164\u0163\3\2\2\2\u0164\u0165\3\2\2\2\u0165"+
		"\u0166\3\2\2\2\u0166\u0167\7d\2\2\u0167/\3\2\2\2\u0168\u0169\7M\2\2\u0169"+
		"\u016a\7[\2\2\u016a\u016f\5\66\34\2\u016b\u016c\7f\2\2\u016c\u016e\5\66"+
		"\34\2\u016d\u016b\3\2\2\2\u016e\u0171\3\2\2\2\u016f\u016d\3\2\2\2\u016f"+
		"\u0170\3\2\2\2\u0170\u0172\3\2\2\2\u0171\u016f\3\2\2\2\u0172\u0173\7d"+
		"\2\2\u0173\u0174\7\\\2\2\u0174\u0175\7d\2\2\u0175\61\3\2\2\2\u0176\u0177"+
		"\7]\2\2\u0177\u0178\5\64\33\2\u0178\u0179\7^\2\2\u0179\63\3\2\2\2\u017a"+
		"\u017b\b\33\1\2\u017b\u017c\7_\2\2\u017c\u017d\5\64\33\2\u017d\u017e\7"+
		"`\2\2\u017e\u0187\3\2\2\2\u017f\u0180\7N\2\2\u0180\u0181\7_\2\2\u0181"+
		"\u0182\5\66\34\2\u0182\u0183\7`\2\2\u0183\u0187\3\2\2\2\u0184\u0187\7"+
		"T\2\2\u0185\u0187\7R\2\2\u0186\u017a\3\2\2\2\u0186\u017f\3\2\2\2\u0186"+
		"\u0184\3\2\2\2\u0186\u0185\3\2\2\2\u0187\u018d\3\2\2\2\u0188\u0189\f\7"+
		"\2\2\u0189\u018a\t\5\2\2\u018a\u018c\5\64\33\b\u018b\u0188\3\2\2\2\u018c"+
		"\u018f\3\2\2\2\u018d\u018b\3\2\2\2\u018d\u018e\3\2\2\2\u018e\65\3\2\2"+
		"\2\u018f\u018d\3\2\2\2\u0190\u0191\7R\2\2\u0191\67\3\2\2\2\u0192\u0193"+
		"\7U\2\2\u01939\3\2\2\2\u0194\u0195\7V\2\2\u0195;\3\2\2\2.BDQSdruy\u0082"+
		"\u008a\u008f\u0094\u009a\u00a2\u00ae\u00b3\u00b9\u00bd\u00c6\u00ce\u00d4"+
		"\u00e0\u00e8\u00f1\u0100\u010a\u010d\u0113\u011f\u0121\u012a\u0131\u0133"+
		"\u013c\u0142\u014b\u014d\u0155\u015a\u015d\u0164\u016f\u0186\u018d";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}