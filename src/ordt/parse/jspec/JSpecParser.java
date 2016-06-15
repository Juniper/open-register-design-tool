// Generated from /Users/snellenbach/Documents/workspace/ORDT/src/ordt/parse/grammars/JSpec.g4 by ANTLR 4.5.1
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
		T__73=74, T__74=75, WS=76, SL_COMMENT=77, ML_COMMENT=78, ID=79, NUM=80, 
		JSTR=81, STR=82, LJQUOTE=83, RJQUOTE=84, LSHIFT=85, RSHIFT=86, LBRACE=87, 
		RBRACE=88, LSQ=89, RSQ=90, LPAREN=91, RPAREN=92, AT=93, OR=94, AND=95, 
		SEMI=96, COLON=97, COMMA=98, DOT=99, STAR=100, EQ=101, PLUS=102, MINUS=103, 
		DIV=104, EXP=105, DOLLAR=106;
	public static final int
		RULE_root = 0, RULE_num_constant_def = 1, RULE_integer_constant_assign = 2, 
		RULE_width_constant_assign = 3, RULE_string_constant_def = 4, RULE_bracket_pair = 5, 
		RULE_type_definition = 6, RULE_enum_field_def = 7, RULE_enum_value_assign = 8, 
		RULE_transaction_def = 9, RULE_value_assign = 10, RULE_typedef_instance = 11, 
		RULE_param_block = 12, RULE_assign_parameter = 13, RULE_defined_attribute = 14, 
		RULE_defined_attribute_set = 15, RULE_register_set_def = 16, RULE_register_def = 17, 
		RULE_field_def = 18, RULE_field_set_def = 19, RULE_int_field_def = 20, 
		RULE_nop_field_def = 21, RULE_test_group_def = 22, RULE_array = 23, RULE_num_expression = 24, 
		RULE_id = 25, RULE_jstr = 26, RULE_str = 27;
	public static final String[] ruleNames = {
		"root", "num_constant_def", "integer_constant_assign", "width_constant_assign", 
		"string_constant_def", "bracket_pair", "type_definition", "enum_field_def", 
		"enum_value_assign", "transaction_def", "value_assign", "typedef_instance", 
		"param_block", "assign_parameter", "defined_attribute", "defined_attribute_set", 
		"register_set_def", "register_def", "field_def", "field_set_def", "int_field_def", 
		"nop_field_def", "test_group_def", "array", "num_expression", "id", "jstr", 
		"str"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'constant'", "'integer'", "'enum'", "'typedef'", "'transaction'", 
		"'param'", "'access_mode'", "'address_alignment'", "'addr_decode_allow_case'", 
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
		"'WIDTH'", null, null, null, null, null, null, null, "'\"{'", "'}\"'", 
		"'<<'", "'>>'", "'{'", "'}'", "'['", "']'", "'('", "')'", "'@'", "'|'", 
		"'&'", "';'", "':'", "','", "'.'", "'*'", "'='", "'+'", "'-'", "'/'", 
		"'^'", "'$'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, "WS", "SL_COMMENT", "ML_COMMENT", "ID", "NUM", 
		"JSTR", "STR", "LJQUOTE", "RJQUOTE", "LSHIFT", "RSHIFT", "LBRACE", "RBRACE", 
		"LSQ", "RSQ", "LPAREN", "RPAREN", "AT", "OR", "AND", "SEMI", "COLON", 
		"COMMA", "DOT", "STAR", "EQ", "PLUS", "MINUS", "DIV", "EXP", "DOLLAR"
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
			setState(63);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0 || _la==T__3 || _la==T__69 || _la==ID) {
				{
				setState(61);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(56);
					num_constant_def();
					}
					break;
				case 2:
					{
					setState(57);
					string_constant_def();
					}
					break;
				case 3:
					{
					setState(58);
					type_definition();
					}
					break;
				case 4:
					{
					setState(59);
					typedef_instance();
					}
					break;
				case 5:
					{
					setState(60);
					register_set_def();
					}
					break;
				}
				}
				setState(65);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(66);
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
			setState(68);
			match(T__0);
			setState(69);
			id();
			setState(70);
			str();
			setState(71);
			match(LBRACE);
			setState(78);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				setState(72);
				integer_constant_assign();
				}
				break;
			case 2:
				{
				setState(74); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(73);
					width_constant_assign();
					}
					}
					setState(76); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__1 || _la==T__2 || _la==ID );
				}
				break;
			}
			setState(80);
			match(RBRACE);
			setState(81);
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
			setState(83);
			match(T__1);
			setState(84);
			array();
			setState(85);
			match(EQ);
			setState(86);
			num_expression(0);
			setState(87);
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
			setState(116);
			switch (_input.LA(1)) {
			case T__2:
				{
				setState(89);
				match(T__2);
				setState(90);
				array();
				setState(91);
				match(LBRACE);
				setState(95);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << T__54) | (1L << T__55) | (1L << T__56) | (1L << T__57) | (1L << T__58) | (1L << T__59) | (1L << T__60) | (1L << T__61) | (1L << T__62))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (T__63 - 64)) | (1L << (T__64 - 64)) | (1L << (T__65 - 64)) | (1L << (T__66 - 64)) | (1L << (T__67 - 64)) | (1L << (T__68 - 64)) | (1L << (T__69 - 64)) | (1L << (T__70 - 64)) | (1L << (T__71 - 64)) | (1L << (T__72 - 64)) | (1L << (T__73 - 64)) | (1L << (T__74 - 64)) | (1L << (WS - 64)) | (1L << (SL_COMMENT - 64)) | (1L << (ML_COMMENT - 64)) | (1L << (ID - 64)) | (1L << (NUM - 64)) | (1L << (JSTR - 64)) | (1L << (STR - 64)) | (1L << (LJQUOTE - 64)) | (1L << (RJQUOTE - 64)) | (1L << (LSHIFT - 64)) | (1L << (RSHIFT - 64)) | (1L << (LSQ - 64)) | (1L << (RSQ - 64)) | (1L << (LPAREN - 64)) | (1L << (RPAREN - 64)) | (1L << (AT - 64)) | (1L << (OR - 64)) | (1L << (AND - 64)) | (1L << (SEMI - 64)) | (1L << (COLON - 64)) | (1L << (COMMA - 64)) | (1L << (DOT - 64)) | (1L << (STAR - 64)) | (1L << (EQ - 64)) | (1L << (PLUS - 64)) | (1L << (MINUS - 64)) | (1L << (DIV - 64)) | (1L << (EXP - 64)) | (1L << (DOLLAR - 64)))) != 0)) {
					{
					{
					setState(92);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==LBRACE || _la==RBRACE) ) {
					_errHandler.recoverInline(this);
					} else {
						consume();
					}
					}
					}
					setState(97);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(98);
				match(RBRACE);
				setState(99);
				match(SEMI);
				}
				break;
			case T__1:
				{
				setState(101);
				match(T__1);
				setState(102);
				id();
				setState(103);
				array();
				setState(104);
				str();
				setState(105);
				match(SEMI);
				}
				break;
			case ID:
				{
				setState(107);
				id();
				setState(109);
				_la = _input.LA(1);
				if (_la==ID) {
					{
					setState(108);
					id();
					}
				}

				setState(112);
				_la = _input.LA(1);
				if (_la==STR) {
					{
					setState(111);
					str();
					}
				}

				setState(114);
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
			setState(118);
			match(T__0);
			setState(119);
			id();
			setState(120);
			str();
			setState(121);
			match(LBRACE);
			setState(123); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(122);
				bracket_pair();
				}
				}
				setState(125); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << T__54) | (1L << T__55) | (1L << T__56) | (1L << T__57) | (1L << T__58) | (1L << T__59) | (1L << T__60) | (1L << T__61) | (1L << T__62))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (T__63 - 64)) | (1L << (T__64 - 64)) | (1L << (T__65 - 64)) | (1L << (T__66 - 64)) | (1L << (T__67 - 64)) | (1L << (T__68 - 64)) | (1L << (T__69 - 64)) | (1L << (T__70 - 64)) | (1L << (T__71 - 64)) | (1L << (T__72 - 64)) | (1L << (T__73 - 64)) | (1L << (T__74 - 64)) | (1L << (WS - 64)) | (1L << (SL_COMMENT - 64)) | (1L << (ML_COMMENT - 64)) | (1L << (ID - 64)) | (1L << (NUM - 64)) | (1L << (JSTR - 64)) | (1L << (STR - 64)) | (1L << (LJQUOTE - 64)) | (1L << (RJQUOTE - 64)) | (1L << (LSHIFT - 64)) | (1L << (RSHIFT - 64)) | (1L << (LSQ - 64)) | (1L << (RSQ - 64)) | (1L << (LPAREN - 64)) | (1L << (RPAREN - 64)) | (1L << (AT - 64)) | (1L << (OR - 64)) | (1L << (AND - 64)) | (1L << (SEMI - 64)) | (1L << (COLON - 64)) | (1L << (COMMA - 64)) | (1L << (DOT - 64)) | (1L << (STAR - 64)) | (1L << (EQ - 64)) | (1L << (PLUS - 64)) | (1L << (MINUS - 64)) | (1L << (DIV - 64)) | (1L << (EXP - 64)) | (1L << (DOLLAR - 64)))) != 0) );
			setState(127);
			match(RBRACE);
			setState(128);
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
			setState(131); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(130);
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
				setState(133); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(143);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1+1 ) {
					{
					{
					setState(135);
					match(LBRACE);
					setState(138);
					switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
					case 1:
						{
						setState(136);
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
						setState(137);
						bracket_pair();
						}
						break;
					}
					setState(140);
					match(RBRACE);
					}
					} 
				}
				setState(145);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			}
			setState(149);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(146);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==LBRACE || _la==RBRACE) ) {
					_errHandler.recoverInline(this);
					} else {
						consume();
					}
					}
					} 
				}
				setState(151);
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
			setState(152);
			match(T__3);
			setState(157);
			switch (_input.LA(1)) {
			case T__69:
				{
				setState(153);
				register_set_def();
				}
				break;
			case T__70:
				{
				setState(154);
				register_def();
				}
				break;
			case T__1:
			case T__2:
			case T__71:
			case T__72:
				{
				setState(155);
				field_def();
				}
				break;
			case T__4:
				{
				setState(156);
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
		enterRule(_localctx, 14, RULE_enum_field_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(159);
			match(T__2);
			setState(161);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(160);
				id();
				}
			}

			setState(163);
			array();
			setState(166);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(164);
				str();
				}
				break;
			case JSTR:
				{
				setState(165);
				jstr();
				}
				break;
			case LBRACE:
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(168);
			match(LBRACE);
			setState(170); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(169);
				enum_value_assign();
				}
				}
				setState(172); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ID );
			setState(174);
			match(RBRACE);
			setState(176);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(175);
				param_block();
				}
			}

			setState(178);
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
		enterRule(_localctx, 16, RULE_enum_value_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			id();
			setState(181);
			match(EQ);
			setState(182);
			num_expression(0);
			setState(185);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(183);
				str();
				}
				break;
			case JSTR:
				{
				setState(184);
				jstr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(187);
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
		enterRule(_localctx, 18, RULE_transaction_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(189);
			match(T__4);
			setState(190);
			id();
			setState(193);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(191);
				str();
				}
				break;
			case JSTR:
				{
				setState(192);
				jstr();
				}
				break;
			case LBRACE:
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(195);
			match(LBRACE);
			setState(197); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(196);
				bracket_pair();
				}
				}
				setState(199); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << T__54) | (1L << T__55) | (1L << T__56) | (1L << T__57) | (1L << T__58) | (1L << T__59) | (1L << T__60) | (1L << T__61) | (1L << T__62))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (T__63 - 64)) | (1L << (T__64 - 64)) | (1L << (T__65 - 64)) | (1L << (T__66 - 64)) | (1L << (T__67 - 64)) | (1L << (T__68 - 64)) | (1L << (T__69 - 64)) | (1L << (T__70 - 64)) | (1L << (T__71 - 64)) | (1L << (T__72 - 64)) | (1L << (T__73 - 64)) | (1L << (T__74 - 64)) | (1L << (WS - 64)) | (1L << (SL_COMMENT - 64)) | (1L << (ML_COMMENT - 64)) | (1L << (ID - 64)) | (1L << (NUM - 64)) | (1L << (JSTR - 64)) | (1L << (STR - 64)) | (1L << (LJQUOTE - 64)) | (1L << (RJQUOTE - 64)) | (1L << (LSHIFT - 64)) | (1L << (RSHIFT - 64)) | (1L << (LSQ - 64)) | (1L << (RSQ - 64)) | (1L << (LPAREN - 64)) | (1L << (RPAREN - 64)) | (1L << (AT - 64)) | (1L << (OR - 64)) | (1L << (AND - 64)) | (1L << (SEMI - 64)) | (1L << (COLON - 64)) | (1L << (COMMA - 64)) | (1L << (DOT - 64)) | (1L << (STAR - 64)) | (1L << (EQ - 64)) | (1L << (PLUS - 64)) | (1L << (MINUS - 64)) | (1L << (DIV - 64)) | (1L << (EXP - 64)) | (1L << (DOLLAR - 64)))) != 0) );
			setState(201);
			match(RBRACE);
			setState(202);
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
		enterRule(_localctx, 20, RULE_value_assign);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(204);
			assign_parameter();
			setState(205);
			match(EQ);
			setState(211);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				{
				setState(206);
				defined_attribute();
				}
				break;
			case 2:
				{
				setState(207);
				num_expression(0);
				}
				break;
			case 3:
				{
				setState(208);
				str();
				}
				break;
			case 4:
				{
				setState(209);
				jstr();
				}
				break;
			case 5:
				{
				setState(210);
				defined_attribute_set();
				}
				break;
			}
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
		enterRule(_localctx, 22, RULE_typedef_instance);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(215);
			id();
			setState(216);
			id();
			setState(217);
			str();
			setState(219);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(218);
				param_block();
				}
			}

			setState(221);
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
		enterRule(_localctx, 24, RULE_param_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(223);
			match(T__5);
			setState(224);
			match(LBRACE);
			setState(228);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30))) != 0) || _la==ID) {
				{
				{
				setState(225);
				value_assign();
				}
				}
				setState(230);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(231);
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
		enterRule(_localctx, 26, RULE_assign_parameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(233);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30))) != 0) || _la==ID) ) {
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
		enterRule(_localctx, 28, RULE_defined_attribute);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(235);
			_la = _input.LA(1);
			if ( !(((((_la - 32)) & ~0x3f) == 0 && ((1L << (_la - 32)) & ((1L << (T__31 - 32)) | (1L << (T__32 - 32)) | (1L << (T__33 - 32)) | (1L << (T__34 - 32)) | (1L << (T__35 - 32)) | (1L << (T__36 - 32)) | (1L << (T__37 - 32)) | (1L << (T__38 - 32)) | (1L << (T__39 - 32)) | (1L << (T__40 - 32)) | (1L << (T__41 - 32)) | (1L << (T__42 - 32)) | (1L << (T__43 - 32)) | (1L << (T__44 - 32)) | (1L << (T__45 - 32)) | (1L << (T__46 - 32)) | (1L << (T__47 - 32)) | (1L << (T__48 - 32)) | (1L << (T__49 - 32)) | (1L << (T__50 - 32)) | (1L << (T__51 - 32)) | (1L << (T__52 - 32)) | (1L << (T__53 - 32)) | (1L << (T__54 - 32)) | (1L << (T__55 - 32)) | (1L << (T__56 - 32)) | (1L << (T__57 - 32)) | (1L << (T__58 - 32)) | (1L << (T__59 - 32)) | (1L << (T__60 - 32)) | (1L << (T__61 - 32)) | (1L << (T__62 - 32)) | (1L << (T__63 - 32)) | (1L << (T__64 - 32)) | (1L << (T__65 - 32)) | (1L << (T__66 - 32)) | (1L << (T__67 - 32)) | (1L << (T__68 - 32)))) != 0)) ) {
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
		enterRule(_localctx, 30, RULE_defined_attribute_set);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(256);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(237);
				match(LBRACE);
				setState(238);
				defined_attribute();
				setState(243);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(239);
					match(COMMA);
					setState(240);
					defined_attribute();
					}
					}
					setState(245);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(246);
				match(RBRACE);
				}
				break;
			case T__31:
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
				{
				setState(248);
				defined_attribute();
				setState(253);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==OR) {
					{
					{
					setState(249);
					match(OR);
					setState(250);
					defined_attribute();
					}
					}
					setState(255);
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
		enterRule(_localctx, 32, RULE_register_set_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(258);
			match(T__69);
			setState(259);
			id();
			setState(262);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(260);
				str();
				}
				break;
			case JSTR:
				{
				setState(261);
				jstr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(264);
			match(LBRACE);
			setState(273); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(273);
				switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
				case 1:
					{
					setState(265);
					value_assign();
					}
					break;
				case 2:
					{
					setState(266);
					register_set_def();
					}
					break;
				case 3:
					{
					setState(267);
					register_def();
					}
					break;
				case 4:
					{
					setState(268);
					typedef_instance();
					}
					break;
				case 5:
					{
					setState(269);
					type_definition();
					}
					break;
				case 6:
					{
					setState(270);
					test_group_def();
					}
					break;
				case 7:
					{
					setState(271);
					num_constant_def();
					}
					break;
				case 8:
					{
					setState(272);
					string_constant_def();
					}
					break;
				}
				}
				setState(275); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__3) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30))) != 0) || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & ((1L << (T__69 - 70)) | (1L << (T__70 - 70)) | (1L << (T__73 - 70)) | (1L << (ID - 70)))) != 0) );
			setState(277);
			match(RBRACE);
			setState(278);
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
		enterRule(_localctx, 34, RULE_register_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			match(T__70);
			setState(281);
			id();
			setState(284);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(282);
				str();
				}
				break;
			case JSTR:
				{
				setState(283);
				jstr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(286);
			match(LBRACE);
			setState(291); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(291);
				switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
				case 1:
					{
					setState(287);
					value_assign();
					}
					break;
				case 2:
					{
					setState(288);
					field_def();
					}
					break;
				case 3:
					{
					setState(289);
					typedef_instance();
					}
					break;
				case 4:
					{
					setState(290);
					type_definition();
					}
					break;
				}
				}
				setState(293); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30))) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & ((1L << (T__71 - 72)) | (1L << (T__72 - 72)) | (1L << (ID - 72)))) != 0) );
			setState(295);
			match(RBRACE);
			setState(296);
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
		enterRule(_localctx, 36, RULE_field_def);
		try {
			setState(302);
			switch (_input.LA(1)) {
			case T__71:
				enterOuterAlt(_localctx, 1);
				{
				setState(298);
				field_set_def();
				}
				break;
			case T__1:
				enterOuterAlt(_localctx, 2);
				{
				setState(299);
				int_field_def();
				}
				break;
			case T__2:
				enterOuterAlt(_localctx, 3);
				{
				setState(300);
				enum_field_def();
				}
				break;
			case T__72:
				enterOuterAlt(_localctx, 4);
				{
				setState(301);
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
		enterRule(_localctx, 38, RULE_field_set_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(304);
			match(T__71);
			setState(305);
			id();
			setState(308);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(306);
				str();
				}
				break;
			case JSTR:
				{
				setState(307);
				jstr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(310);
			match(LBRACE);
			setState(319);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__2) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30))) != 0) || ((((_la - 72)) & ~0x3f) == 0 && ((1L << (_la - 72)) & ((1L << (T__71 - 72)) | (1L << (T__72 - 72)) | (1L << (ID - 72)))) != 0)) {
				{
				setState(317);
				switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
				case 1:
					{
					setState(311);
					value_assign();
					}
					break;
				case 2:
					{
					setState(312);
					int_field_def();
					}
					break;
				case 3:
					{
					setState(313);
					enum_field_def();
					}
					break;
				case 4:
					{
					setState(314);
					nop_field_def();
					}
					break;
				case 5:
					{
					setState(315);
					field_set_def();
					}
					break;
				case 6:
					{
					setState(316);
					typedef_instance();
					}
					break;
				}
				}
				setState(321);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(322);
			match(RBRACE);
			setState(323);
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
		enterRule(_localctx, 40, RULE_int_field_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(325);
			match(T__1);
			setState(327);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(326);
				id();
				}
			}

			setState(329);
			array();
			setState(332);
			switch (_input.LA(1)) {
			case STR:
				{
				setState(330);
				str();
				}
				break;
			case JSTR:
				{
				setState(331);
				jstr();
				}
				break;
			case T__5:
			case SEMI:
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(335);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(334);
				param_block();
				}
			}

			setState(337);
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
		enterRule(_localctx, 42, RULE_nop_field_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(339);
			match(T__72);
			setState(340);
			array();
			setState(342);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(341);
				param_block();
				}
			}

			setState(344);
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
		enterRule(_localctx, 44, RULE_test_group_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(346);
			match(T__73);
			setState(347);
			match(LBRACE);
			setState(348);
			id();
			setState(353);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(349);
				match(COMMA);
				setState(350);
				id();
				}
				}
				setState(355);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(356);
			match(SEMI);
			setState(357);
			match(RBRACE);
			setState(358);
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
		enterRule(_localctx, 46, RULE_array);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(360);
			match(LSQ);
			setState(361);
			num_expression(0);
			setState(362);
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
		int _startState = 48;
		enterRecursionRule(_localctx, 48, RULE_num_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(376);
			switch (_input.LA(1)) {
			case LPAREN:
				{
				setState(365);
				match(LPAREN);
				setState(366);
				num_expression(0);
				setState(367);
				match(RPAREN);
				}
				break;
			case T__74:
				{
				setState(369);
				match(T__74);
				setState(370);
				match(LPAREN);
				setState(371);
				id();
				setState(372);
				match(RPAREN);
				}
				break;
			case NUM:
				{
				setState(374);
				match(NUM);
				}
				break;
			case ID:
				{
				setState(375);
				match(ID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(383);
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
					setState(378);
					if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
					setState(379);
					((Num_expressionContext)_localctx).op = _input.LT(1);
					_la = _input.LA(1);
					if ( !(((((_la - 85)) & ~0x3f) == 0 && ((1L << (_la - 85)) & ((1L << (LSHIFT - 85)) | (1L << (RSHIFT - 85)) | (1L << (OR - 85)) | (1L << (AND - 85)) | (1L << (STAR - 85)) | (1L << (PLUS - 85)) | (1L << (MINUS - 85)) | (1L << (DIV - 85)) | (1L << (EXP - 85)))) != 0)) ) {
						((Num_expressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
					} else {
						consume();
					}
					setState(380);
					num_expression(6);
					}
					} 
				}
				setState(385);
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
		enterRule(_localctx, 50, RULE_id);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(386);
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
		enterRule(_localctx, 52, RULE_jstr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(388);
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
		enterRule(_localctx, 54, RULE_str);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(390);
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
		case 24:
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3l\u018b\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\3\2\3\2\3\2\3\2\3\2\7\2@\n\2"+
		"\f\2\16\2C\13\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\6\3M\n\3\r\3\16\3N\5\3"+
		"Q\n\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\7\5`\n\5\f\5"+
		"\16\5c\13\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5p\n\5\3\5\5"+
		"\5s\n\5\3\5\3\5\5\5w\n\5\3\6\3\6\3\6\3\6\3\6\6\6~\n\6\r\6\16\6\177\3\6"+
		"\3\6\3\6\3\7\6\7\u0086\n\7\r\7\16\7\u0087\3\7\3\7\3\7\5\7\u008d\n\7\3"+
		"\7\7\7\u0090\n\7\f\7\16\7\u0093\13\7\3\7\7\7\u0096\n\7\f\7\16\7\u0099"+
		"\13\7\3\b\3\b\3\b\3\b\3\b\5\b\u00a0\n\b\3\t\3\t\5\t\u00a4\n\t\3\t\3\t"+
		"\3\t\5\t\u00a9\n\t\3\t\3\t\6\t\u00ad\n\t\r\t\16\t\u00ae\3\t\3\t\5\t\u00b3"+
		"\n\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\5\n\u00bc\n\n\3\n\3\n\3\13\3\13\3\13"+
		"\3\13\5\13\u00c4\n\13\3\13\3\13\6\13\u00c8\n\13\r\13\16\13\u00c9\3\13"+
		"\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\5\f\u00d6\n\f\3\f\3\f\3\r\3\r\3"+
		"\r\3\r\5\r\u00de\n\r\3\r\3\r\3\16\3\16\3\16\7\16\u00e5\n\16\f\16\16\16"+
		"\u00e8\13\16\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\21\3\21\7\21\u00f4"+
		"\n\21\f\21\16\21\u00f7\13\21\3\21\3\21\3\21\3\21\3\21\7\21\u00fe\n\21"+
		"\f\21\16\21\u0101\13\21\5\21\u0103\n\21\3\22\3\22\3\22\3\22\5\22\u0109"+
		"\n\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\6\22\u0114\n\22\r\22"+
		"\16\22\u0115\3\22\3\22\3\22\3\23\3\23\3\23\3\23\5\23\u011f\n\23\3\23\3"+
		"\23\3\23\3\23\3\23\6\23\u0126\n\23\r\23\16\23\u0127\3\23\3\23\3\23\3\24"+
		"\3\24\3\24\3\24\5\24\u0131\n\24\3\25\3\25\3\25\3\25\5\25\u0137\n\25\3"+
		"\25\3\25\3\25\3\25\3\25\3\25\3\25\7\25\u0140\n\25\f\25\16\25\u0143\13"+
		"\25\3\25\3\25\3\25\3\26\3\26\5\26\u014a\n\26\3\26\3\26\3\26\5\26\u014f"+
		"\n\26\3\26\5\26\u0152\n\26\3\26\3\26\3\27\3\27\3\27\5\27\u0159\n\27\3"+
		"\27\3\27\3\30\3\30\3\30\3\30\3\30\7\30\u0162\n\30\f\30\16\30\u0165\13"+
		"\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3"+
		"\32\3\32\3\32\3\32\3\32\3\32\3\32\5\32\u017b\n\32\3\32\3\32\3\32\7\32"+
		"\u0180\n\32\f\32\16\32\u0183\13\32\3\33\3\33\3\34\3\34\3\35\3\35\3\35"+
		"\3\u0091\3\62\36\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62"+
		"\64\668\2\6\3\2YZ\4\2\t!QQ\3\2\"G\6\2WX`affhk\u01b7\2A\3\2\2\2\4F\3\2"+
		"\2\2\6U\3\2\2\2\bv\3\2\2\2\nx\3\2\2\2\f\u0085\3\2\2\2\16\u009a\3\2\2\2"+
		"\20\u00a1\3\2\2\2\22\u00b6\3\2\2\2\24\u00bf\3\2\2\2\26\u00ce\3\2\2\2\30"+
		"\u00d9\3\2\2\2\32\u00e1\3\2\2\2\34\u00eb\3\2\2\2\36\u00ed\3\2\2\2 \u0102"+
		"\3\2\2\2\"\u0104\3\2\2\2$\u011a\3\2\2\2&\u0130\3\2\2\2(\u0132\3\2\2\2"+
		"*\u0147\3\2\2\2,\u0155\3\2\2\2.\u015c\3\2\2\2\60\u016a\3\2\2\2\62\u017a"+
		"\3\2\2\2\64\u0184\3\2\2\2\66\u0186\3\2\2\28\u0188\3\2\2\2:@\5\4\3\2;@"+
		"\5\n\6\2<@\5\16\b\2=@\5\30\r\2>@\5\"\22\2?:\3\2\2\2?;\3\2\2\2?<\3\2\2"+
		"\2?=\3\2\2\2?>\3\2\2\2@C\3\2\2\2A?\3\2\2\2AB\3\2\2\2BD\3\2\2\2CA\3\2\2"+
		"\2DE\7\2\2\3E\3\3\2\2\2FG\7\3\2\2GH\5\64\33\2HI\58\35\2IP\7Y\2\2JQ\5\6"+
		"\4\2KM\5\b\5\2LK\3\2\2\2MN\3\2\2\2NL\3\2\2\2NO\3\2\2\2OQ\3\2\2\2PJ\3\2"+
		"\2\2PL\3\2\2\2QR\3\2\2\2RS\7Z\2\2ST\7b\2\2T\5\3\2\2\2UV\7\4\2\2VW\5\60"+
		"\31\2WX\7g\2\2XY\5\62\32\2YZ\7b\2\2Z\7\3\2\2\2[\\\7\5\2\2\\]\5\60\31\2"+
		"]a\7Y\2\2^`\n\2\2\2_^\3\2\2\2`c\3\2\2\2a_\3\2\2\2ab\3\2\2\2bd\3\2\2\2"+
		"ca\3\2\2\2de\7Z\2\2ef\7b\2\2fw\3\2\2\2gh\7\4\2\2hi\5\64\33\2ij\5\60\31"+
		"\2jk\58\35\2kl\7b\2\2lw\3\2\2\2mo\5\64\33\2np\5\64\33\2on\3\2\2\2op\3"+
		"\2\2\2pr\3\2\2\2qs\58\35\2rq\3\2\2\2rs\3\2\2\2st\3\2\2\2tu\7b\2\2uw\3"+
		"\2\2\2v[\3\2\2\2vg\3\2\2\2vm\3\2\2\2w\t\3\2\2\2xy\7\3\2\2yz\5\64\33\2"+
		"z{\58\35\2{}\7Y\2\2|~\5\f\7\2}|\3\2\2\2~\177\3\2\2\2\177}\3\2\2\2\177"+
		"\u0080\3\2\2\2\u0080\u0081\3\2\2\2\u0081\u0082\7Z\2\2\u0082\u0083\7b\2"+
		"\2\u0083\13\3\2\2\2\u0084\u0086\n\2\2\2\u0085\u0084\3\2\2\2\u0086\u0087"+
		"\3\2\2\2\u0087\u0085\3\2\2\2\u0087\u0088\3\2\2\2\u0088\u0091\3\2\2\2\u0089"+
		"\u008c\7Y\2\2\u008a\u008d\n\2\2\2\u008b\u008d\5\f\7\2\u008c\u008a\3\2"+
		"\2\2\u008c\u008b\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008e\3\2\2\2\u008e"+
		"\u0090\7Z\2\2\u008f\u0089\3\2\2\2\u0090\u0093\3\2\2\2\u0091\u0092\3\2"+
		"\2\2\u0091\u008f\3\2\2\2\u0092\u0097\3\2\2\2\u0093\u0091\3\2\2\2\u0094"+
		"\u0096\n\2\2\2\u0095\u0094\3\2\2\2\u0096\u0099\3\2\2\2\u0097\u0095\3\2"+
		"\2\2\u0097\u0098\3\2\2\2\u0098\r\3\2\2\2\u0099\u0097\3\2\2\2\u009a\u009f"+
		"\7\6\2\2\u009b\u00a0\5\"\22\2\u009c\u00a0\5$\23\2\u009d\u00a0\5&\24\2"+
		"\u009e\u00a0\5\24\13\2\u009f\u009b\3\2\2\2\u009f\u009c\3\2\2\2\u009f\u009d"+
		"\3\2\2\2\u009f\u009e\3\2\2\2\u00a0\17\3\2\2\2\u00a1\u00a3\7\5\2\2\u00a2"+
		"\u00a4\5\64\33\2\u00a3\u00a2\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a5\3"+
		"\2\2\2\u00a5\u00a8\5\60\31\2\u00a6\u00a9\58\35\2\u00a7\u00a9\5\66\34\2"+
		"\u00a8\u00a6\3\2\2\2\u00a8\u00a7\3\2\2\2\u00a8\u00a9\3\2\2\2\u00a9\u00aa"+
		"\3\2\2\2\u00aa\u00ac\7Y\2\2\u00ab\u00ad\5\22\n\2\u00ac\u00ab\3\2\2\2\u00ad"+
		"\u00ae\3\2\2\2\u00ae\u00ac\3\2\2\2\u00ae\u00af\3\2\2\2\u00af\u00b0\3\2"+
		"\2\2\u00b0\u00b2\7Z\2\2\u00b1\u00b3\5\32\16\2\u00b2\u00b1\3\2\2\2\u00b2"+
		"\u00b3\3\2\2\2\u00b3\u00b4\3\2\2\2\u00b4\u00b5\7b\2\2\u00b5\21\3\2\2\2"+
		"\u00b6\u00b7\5\64\33\2\u00b7\u00b8\7g\2\2\u00b8\u00bb\5\62\32\2\u00b9"+
		"\u00bc\58\35\2\u00ba\u00bc\5\66\34\2\u00bb\u00b9\3\2\2\2\u00bb\u00ba\3"+
		"\2\2\2\u00bc\u00bd\3\2\2\2\u00bd\u00be\7b\2\2\u00be\23\3\2\2\2\u00bf\u00c0"+
		"\7\7\2\2\u00c0\u00c3\5\64\33\2\u00c1\u00c4\58\35\2\u00c2\u00c4\5\66\34"+
		"\2\u00c3\u00c1\3\2\2\2\u00c3\u00c2\3\2\2\2\u00c3\u00c4\3\2\2\2\u00c4\u00c5"+
		"\3\2\2\2\u00c5\u00c7\7Y\2\2\u00c6\u00c8\5\f\7\2\u00c7\u00c6\3\2\2\2\u00c8"+
		"\u00c9\3\2\2\2\u00c9\u00c7\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca\u00cb\3\2"+
		"\2\2\u00cb\u00cc\7Z\2\2\u00cc\u00cd\7b\2\2\u00cd\25\3\2\2\2\u00ce\u00cf"+
		"\5\34\17\2\u00cf\u00d5\7g\2\2\u00d0\u00d6\5\36\20\2\u00d1\u00d6\5\62\32"+
		"\2\u00d2\u00d6\58\35\2\u00d3\u00d6\5\66\34\2\u00d4\u00d6\5 \21\2\u00d5"+
		"\u00d0\3\2\2\2\u00d5\u00d1\3\2\2\2\u00d5\u00d2\3\2\2\2\u00d5\u00d3\3\2"+
		"\2\2\u00d5\u00d4\3\2\2\2\u00d6\u00d7\3\2\2\2\u00d7\u00d8\7b\2\2\u00d8"+
		"\27\3\2\2\2\u00d9\u00da\5\64\33\2\u00da\u00db\5\64\33\2\u00db\u00dd\5"+
		"8\35\2\u00dc\u00de\5\32\16\2\u00dd\u00dc\3\2\2\2\u00dd\u00de\3\2\2\2\u00de"+
		"\u00df\3\2\2\2\u00df\u00e0\7b\2\2\u00e0\31\3\2\2\2\u00e1\u00e2\7\b\2\2"+
		"\u00e2\u00e6\7Y\2\2\u00e3\u00e5\5\26\f\2\u00e4\u00e3\3\2\2\2\u00e5\u00e8"+
		"\3\2\2\2\u00e6\u00e4\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7\u00e9\3\2\2\2\u00e8"+
		"\u00e6\3\2\2\2\u00e9\u00ea\7Z\2\2\u00ea\33\3\2\2\2\u00eb\u00ec\t\3\2\2"+
		"\u00ec\35\3\2\2\2\u00ed\u00ee\t\4\2\2\u00ee\37\3\2\2\2\u00ef\u00f0\7Y"+
		"\2\2\u00f0\u00f5\5\36\20\2\u00f1\u00f2\7d\2\2\u00f2\u00f4\5\36\20\2\u00f3"+
		"\u00f1\3\2\2\2\u00f4\u00f7\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f5\u00f6\3\2"+
		"\2\2\u00f6\u00f8\3\2\2\2\u00f7\u00f5\3\2\2\2\u00f8\u00f9\7Z\2\2\u00f9"+
		"\u0103\3\2\2\2\u00fa\u00ff\5\36\20\2\u00fb\u00fc\7`\2\2\u00fc\u00fe\5"+
		"\36\20\2\u00fd\u00fb\3\2\2\2\u00fe\u0101\3\2\2\2\u00ff\u00fd\3\2\2\2\u00ff"+
		"\u0100\3\2\2\2\u0100\u0103\3\2\2\2\u0101\u00ff\3\2\2\2\u0102\u00ef\3\2"+
		"\2\2\u0102\u00fa\3\2\2\2\u0103!\3\2\2\2\u0104\u0105\7H\2\2\u0105\u0108"+
		"\5\64\33\2\u0106\u0109\58\35\2\u0107\u0109\5\66\34\2\u0108\u0106\3\2\2"+
		"\2\u0108\u0107\3\2\2\2\u0109\u010a\3\2\2\2\u010a\u0113\7Y\2\2\u010b\u0114"+
		"\5\26\f\2\u010c\u0114\5\"\22\2\u010d\u0114\5$\23\2\u010e\u0114\5\30\r"+
		"\2\u010f\u0114\5\16\b\2\u0110\u0114\5.\30\2\u0111\u0114\5\4\3\2\u0112"+
		"\u0114\5\n\6\2\u0113\u010b\3\2\2\2\u0113\u010c\3\2\2\2\u0113\u010d\3\2"+
		"\2\2\u0113\u010e\3\2\2\2\u0113\u010f\3\2\2\2\u0113\u0110\3\2\2\2\u0113"+
		"\u0111\3\2\2\2\u0113\u0112\3\2\2\2\u0114\u0115\3\2\2\2\u0115\u0113\3\2"+
		"\2\2\u0115\u0116\3\2\2\2\u0116\u0117\3\2\2\2\u0117\u0118\7Z\2\2\u0118"+
		"\u0119\7b\2\2\u0119#\3\2\2\2\u011a\u011b\7I\2\2\u011b\u011e\5\64\33\2"+
		"\u011c\u011f\58\35\2\u011d\u011f\5\66\34\2\u011e\u011c\3\2\2\2\u011e\u011d"+
		"\3\2\2\2\u011f\u0120\3\2\2\2\u0120\u0125\7Y\2\2\u0121\u0126\5\26\f\2\u0122"+
		"\u0126\5&\24\2\u0123\u0126\5\30\r\2\u0124\u0126\5\16\b\2\u0125\u0121\3"+
		"\2\2\2\u0125\u0122\3\2\2\2\u0125\u0123\3\2\2\2\u0125\u0124\3\2\2\2\u0126"+
		"\u0127\3\2\2\2\u0127\u0125\3\2\2\2\u0127\u0128\3\2\2\2\u0128\u0129\3\2"+
		"\2\2\u0129\u012a\7Z\2\2\u012a\u012b\7b\2\2\u012b%\3\2\2\2\u012c\u0131"+
		"\5(\25\2\u012d\u0131\5*\26\2\u012e\u0131\5\20\t\2\u012f\u0131\5,\27\2"+
		"\u0130\u012c\3\2\2\2\u0130\u012d\3\2\2\2\u0130\u012e\3\2\2\2\u0130\u012f"+
		"\3\2\2\2\u0131\'\3\2\2\2\u0132\u0133\7J\2\2\u0133\u0136\5\64\33\2\u0134"+
		"\u0137\58\35\2\u0135\u0137\5\66\34\2\u0136\u0134\3\2\2\2\u0136\u0135\3"+
		"\2\2\2\u0137\u0138\3\2\2\2\u0138\u0141\7Y\2\2\u0139\u0140\5\26\f\2\u013a"+
		"\u0140\5*\26\2\u013b\u0140\5\20\t\2\u013c\u0140\5,\27\2\u013d\u0140\5"+
		"(\25\2\u013e\u0140\5\30\r\2\u013f\u0139\3\2\2\2\u013f\u013a\3\2\2\2\u013f"+
		"\u013b\3\2\2\2\u013f\u013c\3\2\2\2\u013f\u013d\3\2\2\2\u013f\u013e\3\2"+
		"\2\2\u0140\u0143\3\2\2\2\u0141\u013f\3\2\2\2\u0141\u0142\3\2\2\2\u0142"+
		"\u0144\3\2\2\2\u0143\u0141\3\2\2\2\u0144\u0145\7Z\2\2\u0145\u0146\7b\2"+
		"\2\u0146)\3\2\2\2\u0147\u0149\7\4\2\2\u0148\u014a\5\64\33\2\u0149\u0148"+
		"\3\2\2\2\u0149\u014a\3\2\2\2\u014a\u014b\3\2\2\2\u014b\u014e\5\60\31\2"+
		"\u014c\u014f\58\35\2\u014d\u014f\5\66\34\2\u014e\u014c\3\2\2\2\u014e\u014d"+
		"\3\2\2\2\u014e\u014f\3\2\2\2\u014f\u0151\3\2\2\2\u0150\u0152\5\32\16\2"+
		"\u0151\u0150\3\2\2\2\u0151\u0152\3\2\2\2\u0152\u0153\3\2\2\2\u0153\u0154"+
		"\7b\2\2\u0154+\3\2\2\2\u0155\u0156\7K\2\2\u0156\u0158\5\60\31\2\u0157"+
		"\u0159\5\32\16\2\u0158\u0157\3\2\2\2\u0158\u0159\3\2\2\2\u0159\u015a\3"+
		"\2\2\2\u015a\u015b\7b\2\2\u015b-\3\2\2\2\u015c\u015d\7L\2\2\u015d\u015e"+
		"\7Y\2\2\u015e\u0163\5\64\33\2\u015f\u0160\7d\2\2\u0160\u0162\5\64\33\2"+
		"\u0161\u015f\3\2\2\2\u0162\u0165\3\2\2\2\u0163\u0161\3\2\2\2\u0163\u0164"+
		"\3\2\2\2\u0164\u0166\3\2\2\2\u0165\u0163\3\2\2\2\u0166\u0167\7b\2\2\u0167"+
		"\u0168\7Z\2\2\u0168\u0169\7b\2\2\u0169/\3\2\2\2\u016a\u016b\7[\2\2\u016b"+
		"\u016c\5\62\32\2\u016c\u016d\7\\\2\2\u016d\61\3\2\2\2\u016e\u016f\b\32"+
		"\1\2\u016f\u0170\7]\2\2\u0170\u0171\5\62\32\2\u0171\u0172\7^\2\2\u0172"+
		"\u017b\3\2\2\2\u0173\u0174\7M\2\2\u0174\u0175\7]\2\2\u0175\u0176\5\64"+
		"\33\2\u0176\u0177\7^\2\2\u0177\u017b\3\2\2\2\u0178\u017b\7R\2\2\u0179"+
		"\u017b\7Q\2\2\u017a\u016e\3\2\2\2\u017a\u0173\3\2\2\2\u017a\u0178\3\2"+
		"\2\2\u017a\u0179\3\2\2\2\u017b\u0181\3\2\2\2\u017c\u017d\f\7\2\2\u017d"+
		"\u017e\t\5\2\2\u017e\u0180\5\62\32\b\u017f\u017c\3\2\2\2\u0180\u0183\3"+
		"\2\2\2\u0181\u017f\3\2\2\2\u0181\u0182\3\2\2\2\u0182\63\3\2\2\2\u0183"+
		"\u0181\3\2\2\2\u0184\u0185\7Q\2\2\u0185\65\3\2\2\2\u0186\u0187\7S\2\2"+
		"\u0187\67\3\2\2\2\u0188\u0189\7T\2\2\u01899\3\2\2\2.?ANPaorv\177\u0087"+
		"\u008c\u0091\u0097\u009f\u00a3\u00a8\u00ae\u00b2\u00bb\u00c3\u00c9\u00d5"+
		"\u00dd\u00e6\u00f5\u00ff\u0102\u0108\u0113\u0115\u011e\u0125\u0127\u0130"+
		"\u0136\u013f\u0141\u0149\u014e\u0151\u0158\u0163\u017a\u0181";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}