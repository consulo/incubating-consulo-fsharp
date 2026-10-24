/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.fsharp.impl.lang.psi;

import consulo.language.ast.IElementType;
import consulo.language.ast.StandardTokenTypes;
import consulo.language.ast.TokenSet;

/**
 * F# token types. Debug names follow the ReSharper F# backend, so its lexer and parser
 * test data can be reused as is.
 * <p>
 * Ported from JetBrains resharper-fsharp (Apache 2.0).
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public interface FSharpTokenType extends StandardTokenTypes {
    IElementType IDENTIFIER = new FSharpTokenNodeType("IDENTIFIER");
    IElementType DEAD_CODE = new FSharpTokenNodeType("DEAD_CODE");

    IElementType BIGNUM = new FSharpTokenNodeType("BIGNUM");
    IElementType BLOCK_COMMENT = new FSharpTokenNodeType("BLOCK_COMMENT");
    IElementType BYTE = new FSharpTokenNodeType("BYTE");
    IElementType BYTEARRAY = new FSharpTokenNodeType("BYTEARRAY");
    IElementType BYTECHAR = new FSharpTokenNodeType("BYTECHAR");
    IElementType CHARACTER_LITERAL = new FSharpTokenNodeType("CHARACTER_LITERAL");
    IElementType DECIMAL = new FSharpTokenNodeType("DECIMAL");
    IElementType LINE_COMMENT = new FSharpTokenNodeType("LINE_COMMENT");
    IElementType SHEBANG = new FSharpTokenNodeType("SHEBANG");
    IElementType IEEE32 = new FSharpTokenNodeType("IEEE32");
    IElementType IEEE64 = new FSharpTokenNodeType("IEEE64");
    IElementType INT16 = new FSharpTokenNodeType("INT16");
    IElementType INT32 = new FSharpTokenNodeType("INT32");
    IElementType INT64 = new FSharpTokenNodeType("INT64");
    IElementType NATIVEINT = new FSharpTokenNodeType("NATIVEINT");
    IElementType LQUOTE_TYPED = new FSharpTokenNodeType("LQUOTE_TYPED");
    IElementType RQUOTE_TYPED = new FSharpTokenNodeType("RQUOTE_TYPED");
    IElementType LQUOTE_UNTYPED = new FSharpTokenNodeType("LQUOTE_UNTYPED");
    IElementType RQUOTE_UNTYPED = new FSharpTokenNodeType("RQUOTE_UNTYPED");
    IElementType RESERVED_IDENT_FORMATS = new FSharpTokenNodeType("RESERVED_IDENT_FORMATS");
    IElementType RESERVED_LITERAL_FORMATS = new FSharpTokenNodeType("RESERVED_LITERAL_FORMATS");
    IElementType RESERVED_SYMBOLIC_SEQUENCE = new FSharpTokenNodeType("RESERVED_SYMBOLIC_SEQUENCE");
    IElementType SBYTE = new FSharpTokenNodeType("SBYTE");
    IElementType STRING = new FSharpTokenNodeType("STRING");
    IElementType RAW_INTERPOLATED_STRING = new FSharpTokenNodeType("RAW_INTERPOLATED_STRING");
    IElementType RAW_INTERPOLATED_STRING_START = new FSharpTokenNodeType("RAW_INTERPOLATED_STRING_START");
    IElementType RAW_INTERPOLATED_STRING_MIDDLE = new FSharpTokenNodeType("RAW_INTERPOLATED_STRING_MIDDLE");
    IElementType RAW_INTERPOLATED_STRING_END = new FSharpTokenNodeType("RAW_INTERPOLATED_STRING_END");
    IElementType REGULAR_INTERPOLATED_STRING = new FSharpTokenNodeType("REGULAR_INTERPOLATED_STRING");
    IElementType REGULAR_INTERPOLATED_STRING_START = new FSharpTokenNodeType("REGULAR_INTERPOLATED_STRING_START");
    IElementType REGULAR_INTERPOLATED_STRING_MIDDLE = new FSharpTokenNodeType("REGULAR_INTERPOLATED_STRING_MIDDLE");
    IElementType REGULAR_INTERPOLATED_STRING_END = new FSharpTokenNodeType("REGULAR_INTERPOLATED_STRING_END");
    IElementType LET_BANG = new FSharpTokenNodeType("LET_BANG");
    IElementType USE_BANG = new FSharpTokenNodeType("USE_BANG");
    IElementType DO_BANG = new FSharpTokenNodeType("DO_BANG");
    IElementType YIELD_BANG = new FSharpTokenNodeType("YIELD_BANG");
    IElementType RETURN_BANG = new FSharpTokenNodeType("RETURN_BANG");
    IElementType MATCH_BANG = new FSharpTokenNodeType("MATCH_BANG");
    IElementType AND_BANG = new FSharpTokenNodeType("AND_BANG");
    IElementType WHILE_BANG = new FSharpTokenNodeType("WHILE_BANG");
    IElementType BAR = new FSharpTokenNodeType("BAR");
    IElementType RARROW = new FSharpTokenNodeType("RARROW");
    IElementType LARROW = new FSharpTokenNodeType("LARROW");
    IElementType DOT = new FSharpTokenNodeType("DOT");
    IElementType COLON = new FSharpTokenNodeType("COLON");
    IElementType LPAREN = new FSharpTokenNodeType("LPAREN");
    IElementType RPAREN = new FSharpTokenNodeType("RPAREN");
    IElementType STAR = new FSharpTokenNodeType("STAR");
    IElementType LBRACK = new FSharpTokenNodeType("LBRACK");
    IElementType RBRACK = new FSharpTokenNodeType("RBRACK");
    IElementType LBRACK_LESS = new FSharpTokenNodeType("LBRACK_LESS");
    IElementType GREATER_RBRACK = new FSharpTokenNodeType("GREATER_RBRACK");
    IElementType LBRACK_BAR = new FSharpTokenNodeType("LBRACK_BAR");
    IElementType LBRACE_BAR = new FSharpTokenNodeType("LBRACE_BAR");
    IElementType LESS = new FSharpTokenNodeType("LESS");
    IElementType GREATER = new FSharpTokenNodeType("GREATER");
    IElementType GREATER_BAR_RBRACK = new FSharpTokenNodeType("GREATER_BAR_RBRACK");
    IElementType BAR_RBRACK = new FSharpTokenNodeType("BAR_RBRACK");
    IElementType BAR_RBRACE = new FSharpTokenNodeType("BAR_RBRACE");
    IElementType LBRACE = new FSharpTokenNodeType("LBRACE");
    IElementType RBRACE = new FSharpTokenNodeType("RBRACE");
    IElementType QUOTE = new FSharpTokenNodeType("QUOTE");
    IElementType HASH = new FSharpTokenNodeType("HASH");
    IElementType COLON_QMARK_GREATER = new FSharpTokenNodeType("COLON_QMARK_GREATER");
    IElementType COLON_QMARK = new FSharpTokenNodeType("COLON_QMARK");
    IElementType COLON_GREATER = new FSharpTokenNodeType("COLON_GREATER");
    IElementType DOT_DOT = new FSharpTokenNodeType("DOT_DOT");
    IElementType DOT_DOT_DOT = new FSharpTokenNodeType("DOT_DOT_DOT");
    IElementType COLON_COLON = new FSharpTokenNodeType("COLON_COLON");
    IElementType COLON_EQUALS = new FSharpTokenNodeType("COLON_EQUALS");
    IElementType SEMICOLON_SEMICOLON = new FSharpTokenNodeType("SEMICOLON_SEMICOLON");
    IElementType SEMICOLON = new FSharpTokenNodeType("SEMICOLON");
    IElementType EQUALS = new FSharpTokenNodeType("EQUALS");
    IElementType UNDERSCORE = new FSharpTokenNodeType("UNDERSCORE");
    IElementType QMARK = new FSharpTokenNodeType("QMARK");
    IElementType QMARK_QMARK = new FSharpTokenNodeType("QMARK_QMARK");
    IElementType LPAREN_STAR_RPAREN = new FSharpTokenNodeType("LPAREN_STAR_RPAREN");
    IElementType MINUS = new FSharpTokenNodeType("MINUS");
    IElementType PLUS = new FSharpTokenNodeType("PLUS");
    IElementType SYMBOLIC_OP = new FSharpTokenNodeType("SYMBOLIC_OP");
    IElementType BAD_SYMBOLIC_OP = new FSharpTokenNodeType("BAD_SYMBOLIC_OP");
    IElementType TRIPLE_QUOTED_STRING = new FSharpTokenNodeType("TRIPLE_QUOTED_STRING");
    IElementType TRIPLE_QUOTE_INTERPOLATED_STRING = new FSharpTokenNodeType("TRIPLE_QUOTE_INTERPOLATED_STRING");
    IElementType TRIPLE_QUOTE_INTERPOLATED_STRING_START = new FSharpTokenNodeType("TRIPLE_QUOTE_INTERPOLATED_STRING_START");
    IElementType TRIPLE_QUOTE_INTERPOLATED_STRING_MIDDLE = new FSharpTokenNodeType("TRIPLE_QUOTE_INTERPOLATED_STRING_MIDDLE");
    IElementType TRIPLE_QUOTE_INTERPOLATED_STRING_END = new FSharpTokenNodeType("TRIPLE_QUOTE_INTERPOLATED_STRING_END");
    IElementType UINT16 = new FSharpTokenNodeType("UINT16");
    IElementType UINT32 = new FSharpTokenNodeType("UINT32");
    IElementType UINT64 = new FSharpTokenNodeType("UINT64");
    IElementType UNATIVEINT = new FSharpTokenNodeType("UNATIVEINT");
    IElementType VERBATIM_BYTEARRAY = new FSharpTokenNodeType("VERBATIM_BYTEARRAY");
    IElementType VERBATIM_STRING = new FSharpTokenNodeType("VERBATIM_STRING");
    IElementType VERBATIM_INTERPOLATED_STRING = new FSharpTokenNodeType("VERBATIM_INTERPOLATED_STRING");
    IElementType VERBATIM_INTERPOLATED_STRING_START = new FSharpTokenNodeType("VERBATIM_INTERPOLATED_STRING_START");
    IElementType VERBATIM_INTERPOLATED_STRING_MIDDLE = new FSharpTokenNodeType("VERBATIM_INTERPOLATED_STRING_MIDDLE");
    IElementType VERBATIM_INTERPOLATED_STRING_END = new FSharpTokenNodeType("VERBATIM_INTERPOLATED_STRING_END");
    IElementType NEW_LINE = new FSharpTokenNodeType("NEW_LINE");
    IElementType KEYWORD_STRING_SOURCE_DIRECTORY = new FSharpTokenNodeType("KEYWORD_STRING_SOURCE_DIRECTORY");
    IElementType KEYWORD_STRING_SOURCE_FILE = new FSharpTokenNodeType("KEYWORD_STRING_SOURCE_FILE");
    IElementType KEYWORD_STRING_LINE = new FSharpTokenNodeType("KEYWORD_STRING_LINE");
    IElementType UNFINISHED_STRING = new FSharpTokenNodeType("UNFINISHED_STRING");
    IElementType UNFINISHED_VERBATIM_STRING = new FSharpTokenNodeType("UNFINISHED_VERBATIM_STRING");
    IElementType UNFINISHED_TRIPLE_QUOTED_STRING = new FSharpTokenNodeType("UNFINISHED_TRIPLE_QUOTED_STRING");
    IElementType UNFINISHED_RAW_INTERPOLATED_STRING = new FSharpTokenNodeType("UNFINISHED_RAW_INTERPOLATED_STRING");
    IElementType UNFINISHED_REGULAR_INTERPOLATED_STRING = new FSharpTokenNodeType("UNFINISHED_REGULAR_INTERPOLATED_STRING");
    IElementType UNFINISHED_VERBATIM_INTERPOLATED_STRING = new FSharpTokenNodeType("UNFINISHED_VERBATIM_INTERPOLATED_STRING");
    IElementType UNFINISHED_TRIPLE_QUOTE_INTERPOLATED_STRING = new FSharpTokenNodeType("UNFINISHED_TRIPLE_QUOTE_INTERPOLATED_STRING");
    IElementType DOLLAR = new FSharpTokenNodeType("DOLLAR");
    IElementType PERCENT = new FSharpTokenNodeType("PERCENT");
    IElementType PERCENT_PERCENT = new FSharpTokenNodeType("PERCENT_PERCENT");
    IElementType AMP = new FSharpTokenNodeType("AMP");
    IElementType AMP_AMP = new FSharpTokenNodeType("AMP_AMP");
    IElementType COMMA = new FSharpTokenNodeType("COMMA");
    IElementType BAD_TAB = new FSharpTokenNodeType("BAD_TAB");
    IElementType PP_LIGHT = new FSharpTokenNodeType("PP_LIGHT");
    IElementType PP_ELIF_SECTION = new FSharpTokenNodeType("PP_ELIF_SECTION");
    IElementType PP_ELSE_SECTION = new FSharpTokenNodeType("PP_ELSE_SECTION");
    IElementType PP_ENDIF = new FSharpTokenNodeType("PP_ENDIF");
    IElementType PP_LINE = new FSharpTokenNodeType("PP_LINE");
    IElementType PP_HELP = new FSharpTokenNodeType("PP_HELP");
    IElementType PP_LOAD = new FSharpTokenNodeType("PP_LOAD");
    IElementType PP_QUIT = new FSharpTokenNodeType("PP_QUIT");
    IElementType PP_I = new FSharpTokenNodeType("PP_I");
    IElementType PP_NOWARN = new FSharpTokenNodeType("PP_NOWARN");
    IElementType PP_WARNON = new FSharpTokenNodeType("PP_WARNON");
    IElementType PP_REFERENCE = new FSharpTokenNodeType("PP_REFERENCE");
    IElementType PP_TIME = new FSharpTokenNodeType("PP_TIME");
    IElementType PP_IF_SECTION = new FSharpTokenNodeType("PP_IF_SECTION");
    IElementType PP_BAD_CHARACTER = new FSharpTokenNodeType("PP_BAD_CHARACTER");
    IElementType PP_CONDITIONAL_SYMBOL = new FSharpTokenNodeType("PP_CONDITIONAL_SYMBOL");
    IElementType PP_OR = new FSharpTokenNodeType("PP_OR");
    IElementType PP_AND = new FSharpTokenNodeType("PP_AND");
    IElementType PP_NOT = new FSharpTokenNodeType("PP_NOT");
    IElementType PP_LPAR = new FSharpTokenNodeType("PP_LPAR");
    IElementType PP_RPAR = new FSharpTokenNodeType("PP_RPAR");
    IElementType PP_DIRECTIVE = new FSharpTokenNodeType("PP_DIRECTIVE");

    IElementType ABSTRACT = new FSharpKeywordTokenNodeType("ABSTRACT", "abstract");
    IElementType AND = new FSharpKeywordTokenNodeType("AND", "and");
    IElementType AS = new FSharpKeywordTokenNodeType("AS", "as");
    IElementType ASSERT = new FSharpKeywordTokenNodeType("ASSERT", "assert");
    IElementType BASE = new FSharpKeywordTokenNodeType("BASE", "base");
    IElementType BEGIN = new FSharpKeywordTokenNodeType("BEGIN", "begin");
    IElementType CLASS = new FSharpKeywordTokenNodeType("CLASS", "class");
    IElementType CONST = new FSharpKeywordTokenNodeType("CONST", "const");
    IElementType DEFAULT = new FSharpKeywordTokenNodeType("DEFAULT", "default");
    IElementType DELEGATE = new FSharpKeywordTokenNodeType("DELEGATE", "delegate");
    IElementType DO = new FSharpKeywordTokenNodeType("DO", "do");
    IElementType DONE = new FSharpKeywordTokenNodeType("DONE", "done");
    IElementType DOWNCAST = new FSharpKeywordTokenNodeType("DOWNCAST", "downcast");
    IElementType DOWNTO = new FSharpKeywordTokenNodeType("DOWNTO", "downto");
    IElementType ELIF = new FSharpKeywordTokenNodeType("ELIF", "elif");
    IElementType ELSE = new FSharpKeywordTokenNodeType("ELSE", "else");
    IElementType END = new FSharpKeywordTokenNodeType("END", "end");
    IElementType EXCEPTION = new FSharpKeywordTokenNodeType("EXCEPTION", "exception");
    IElementType EXTERN = new FSharpKeywordTokenNodeType("EXTERN", "extern");
    IElementType FALSE = new FSharpKeywordTokenNodeType("FALSE", "false");
    IElementType FINALLY = new FSharpKeywordTokenNodeType("FINALLY", "finally");
    IElementType FIXED = new FSharpKeywordTokenNodeType("FIXED", "fixed");
    IElementType FOR = new FSharpKeywordTokenNodeType("FOR", "for");
    IElementType FUN = new FSharpKeywordTokenNodeType("FUN", "fun");
    IElementType FUNCTION = new FSharpKeywordTokenNodeType("FUNCTION", "function");
    IElementType GLOBAL = new FSharpKeywordTokenNodeType("GLOBAL", "global");
    IElementType IF = new FSharpKeywordTokenNodeType("IF", "if");
    IElementType IN = new FSharpKeywordTokenNodeType("IN", "in");
    IElementType INHERIT = new FSharpKeywordTokenNodeType("INHERIT", "inherit");
    IElementType INLINE = new FSharpKeywordTokenNodeType("INLINE", "inline");
    IElementType INTERFACE = new FSharpKeywordTokenNodeType("INTERFACE", "interface");
    IElementType INTERNAL = new FSharpKeywordTokenNodeType("INTERNAL", "internal");
    IElementType LAZY = new FSharpKeywordTokenNodeType("LAZY", "lazy");
    IElementType LET = new FSharpKeywordTokenNodeType("LET", "let");
    IElementType MATCH = new FSharpKeywordTokenNodeType("MATCH", "match");
    IElementType MEMBER = new FSharpKeywordTokenNodeType("MEMBER", "member");
    IElementType MODULE = new FSharpKeywordTokenNodeType("MODULE", "module");
    IElementType MUTABLE = new FSharpKeywordTokenNodeType("MUTABLE", "mutable");
    IElementType NAMESPACE = new FSharpKeywordTokenNodeType("NAMESPACE", "namespace");
    IElementType NEW = new FSharpKeywordTokenNodeType("NEW", "new");
    IElementType NULL = new FSharpKeywordTokenNodeType("NULL", "null");
    IElementType OF = new FSharpKeywordTokenNodeType("OF", "of");
    IElementType OPEN = new FSharpKeywordTokenNodeType("OPEN", "open");
    IElementType OR = new FSharpKeywordTokenNodeType("OR", "or");
    IElementType OVERRIDE = new FSharpKeywordTokenNodeType("OVERRIDE", "override");
    IElementType PRIVATE = new FSharpKeywordTokenNodeType("PRIVATE", "private");
    IElementType PUBLIC = new FSharpKeywordTokenNodeType("PUBLIC", "public");
    IElementType REC = new FSharpKeywordTokenNodeType("REC", "rec");
    IElementType RETURN = new FSharpKeywordTokenNodeType("RETURN", "return");
    IElementType STATIC = new FSharpKeywordTokenNodeType("STATIC", "static");
    IElementType STRUCT = new FSharpKeywordTokenNodeType("STRUCT", "struct");
    IElementType THEN = new FSharpKeywordTokenNodeType("THEN", "then");
    IElementType TO = new FSharpKeywordTokenNodeType("TO", "to");
    IElementType TRUE = new FSharpKeywordTokenNodeType("TRUE", "true");
    IElementType TRY = new FSharpKeywordTokenNodeType("TRY", "try");
    IElementType TYPE = new FSharpKeywordTokenNodeType("TYPE", "type");
    IElementType UPCAST = new FSharpKeywordTokenNodeType("UPCAST", "upcast");
    IElementType USE = new FSharpKeywordTokenNodeType("USE", "use");
    IElementType VAL = new FSharpKeywordTokenNodeType("VAL", "val");
    IElementType VOID = new FSharpKeywordTokenNodeType("VOID", "void");
    IElementType WHEN = new FSharpKeywordTokenNodeType("WHEN", "when");
    IElementType WHILE = new FSharpKeywordTokenNodeType("WHILE", "while");
    IElementType WITH = new FSharpKeywordTokenNodeType("WITH", "with");
    IElementType YIELD = new FSharpKeywordTokenNodeType("YIELD", "yield");

    TokenSet KEYWORDS = TokenSet.create(
        ABSTRACT,
        AND,
        AS,
        ASSERT,
        BASE,
        BEGIN,
        CLASS,
        CONST,
        DEFAULT,
        DELEGATE,
        DO,
        DONE,
        DOWNCAST,
        DOWNTO,
        ELIF,
        ELSE,
        END,
        EXCEPTION,
        EXTERN,
        FALSE,
        FINALLY,
        FIXED,
        FOR,
        FUN,
        FUNCTION,
        GLOBAL,
        IF,
        IN,
        INHERIT,
        INLINE,
        INTERFACE,
        INTERNAL,
        LAZY,
        LET,
        MATCH,
        MEMBER,
        MODULE,
        MUTABLE,
        NAMESPACE,
        NEW,
        NULL,
        OF,
        OPEN,
        OR,
        OVERRIDE,
        PRIVATE,
        PUBLIC,
        REC,
        RETURN,
        STATIC,
        STRUCT,
        THEN,
        TO,
        TRUE,
        TRY,
        TYPE,
        UPCAST,
        USE,
        VAL,
        VOID,
        WHEN,
        WHILE,
        WITH,
        YIELD
    );

    TokenSet COMMENTS = TokenSet.create(BLOCK_COMMENT, LINE_COMMENT, SHEBANG);

    TokenSet WHITESPACES = TokenSet.create(WHITE_SPACE, NEW_LINE, BAD_TAB);

    TokenSet STRINGS = TokenSet.create(
        STRING,
        UNFINISHED_STRING,
        UNFINISHED_VERBATIM_STRING,
        UNFINISHED_TRIPLE_QUOTED_STRING,
        VERBATIM_STRING,
        TRIPLE_QUOTED_STRING,
        BYTEARRAY,
        VERBATIM_BYTEARRAY
    );

    TokenSet INTERPOLATED_STRINGS = TokenSet.create(
        RAW_INTERPOLATED_STRING,
        RAW_INTERPOLATED_STRING_START,
        RAW_INTERPOLATED_STRING_MIDDLE,
        RAW_INTERPOLATED_STRING_END,
        REGULAR_INTERPOLATED_STRING,
        REGULAR_INTERPOLATED_STRING_START,
        REGULAR_INTERPOLATED_STRING_MIDDLE,
        REGULAR_INTERPOLATED_STRING_END,
        VERBATIM_INTERPOLATED_STRING,
        VERBATIM_INTERPOLATED_STRING_START,
        VERBATIM_INTERPOLATED_STRING_MIDDLE,
        VERBATIM_INTERPOLATED_STRING_END,
        TRIPLE_QUOTE_INTERPOLATED_STRING,
        TRIPLE_QUOTE_INTERPOLATED_STRING_START,
        TRIPLE_QUOTE_INTERPOLATED_STRING_MIDDLE,
        TRIPLE_QUOTE_INTERPOLATED_STRING_END,
        UNFINISHED_RAW_INTERPOLATED_STRING,
        UNFINISHED_REGULAR_INTERPOLATED_STRING,
        UNFINISHED_VERBATIM_INTERPOLATED_STRING,
        UNFINISHED_TRIPLE_QUOTE_INTERPOLATED_STRING
    );

    TokenSet ALL_STRINGS = TokenSet.orSet(STRINGS, INTERPOLATED_STRINGS);

    TokenSet INTERPOLATED_STRING_ENDS = TokenSet.create(
        RAW_INTERPOLATED_STRING_END,
        REGULAR_INTERPOLATED_STRING_END,
        VERBATIM_INTERPOLATED_STRING_END,
        TRIPLE_QUOTE_INTERPOLATED_STRING_END
    );

    TokenSet NUMBERS = TokenSet.create(
        SBYTE,
        BYTE,
        INT16,
        UINT16,
        INT32,
        UINT32,
        NATIVEINT,
        UNATIVEINT,
        INT64,
        UINT64,
        BIGNUM,
        IEEE32,
        IEEE64,
        DECIMAL
    );

    TokenSet PP_KEYWORDS = TokenSet.create(
        PP_IF_SECTION,
        PP_ELIF_SECTION,
        PP_ELSE_SECTION,
        PP_ENDIF,
        PP_LIGHT,
        PP_LINE,
        PP_REFERENCE,
        PP_LOAD,
        PP_I,
        PP_TIME,
        PP_NOWARN,
        PP_WARNON,
        PP_HELP,
        PP_QUIT,
        PP_DIRECTIVE
    );

    TokenSet LEFT_BRACES = TokenSet.create(
        LPAREN,
        LBRACE,
        LBRACK,
        LQUOTE_UNTYPED,
        LBRACK_BAR,
        LBRACK_LESS,
        LQUOTE_TYPED,
        LBRACE_BAR
    );

    TokenSet RIGHT_BRACES = TokenSet.create(
        RPAREN,
        RBRACE,
        RBRACK,
        RQUOTE_UNTYPED,
        BAR_RBRACK,
        RQUOTE_TYPED,
        GREATER_RBRACK,
        BAR_RBRACE
    );
}
