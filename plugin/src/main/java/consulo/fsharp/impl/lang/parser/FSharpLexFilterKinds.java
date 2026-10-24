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

package consulo.fsharp.impl.lang.parser;

import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static consulo.fsharp.impl.lang.parser.FSharpLexFilterKind.*;

/**
 * Maps our {@link FSharpTokenType} tokens to the compiler token kinds used by {@link FSharpLexFilter}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class FSharpLexFilterKinds {
    /**
     * Directives handled by the compiler lexer itself: they and the rest of their line produce no parser tokens.
     */
    public static final TokenSet LINE_DIRECTIVES = TokenSet.create(
        FSharpTokenType.PP_LIGHT,
        FSharpTokenType.PP_LINE,
        FSharpTokenType.PP_HELP,
        FSharpTokenType.PP_LOAD,
        FSharpTokenType.PP_QUIT,
        FSharpTokenType.PP_I,
        FSharpTokenType.PP_NOWARN,
        FSharpTokenType.PP_WARNON,
        FSharpTokenType.PP_REFERENCE,
        FSharpTokenType.PP_TIME,
        FSharpTokenType.PP_DIRECTIVE
    );

    private static final Map<IElementType, FSharpLexFilterKind> KINDS = new HashMap<>();

    static {
        for (IElementType type : FSharpTokenType.KEYWORDS.getTypes()) {
            FSharpLexFilterKind kind = byName(type.toString());
            if (kind != null) {
                KINDS.put(type, kind);
            }
        }
        for (IElementType type : new IElementType[]{
            FSharpTokenType.BAR, FSharpTokenType.RARROW, FSharpTokenType.LARROW, FSharpTokenType.DOT, FSharpTokenType.COLON,
            FSharpTokenType.LPAREN, FSharpTokenType.RPAREN, FSharpTokenType.STAR, FSharpTokenType.LBRACK, FSharpTokenType.RBRACK,
            FSharpTokenType.LBRACK_LESS, FSharpTokenType.GREATER_RBRACK, FSharpTokenType.LBRACK_BAR, FSharpTokenType.LBRACE_BAR,
            FSharpTokenType.LESS, FSharpTokenType.GREATER, FSharpTokenType.GREATER_BAR_RBRACK, FSharpTokenType.BAR_RBRACK,
            FSharpTokenType.BAR_RBRACE, FSharpTokenType.LBRACE, FSharpTokenType.RBRACE, FSharpTokenType.QUOTE, FSharpTokenType.HASH,
            FSharpTokenType.COLON_QMARK_GREATER, FSharpTokenType.COLON_QMARK, FSharpTokenType.COLON_GREATER, FSharpTokenType.DOT_DOT,
            FSharpTokenType.DOT_DOT_DOT, FSharpTokenType.COLON_COLON, FSharpTokenType.COLON_EQUALS, FSharpTokenType.SEMICOLON_SEMICOLON,
            FSharpTokenType.SEMICOLON, FSharpTokenType.EQUALS, FSharpTokenType.UNDERSCORE, FSharpTokenType.QMARK, FSharpTokenType.QMARK_QMARK,
            FSharpTokenType.LPAREN_STAR_RPAREN, FSharpTokenType.MINUS, FSharpTokenType.DOLLAR, FSharpTokenType.AMP, FSharpTokenType.AMP_AMP,
            FSharpTokenType.COMMA, FSharpTokenType.INT16, FSharpTokenType.INT32, FSharpTokenType.INT64, FSharpTokenType.NATIVEINT,
            FSharpTokenType.UINT16, FSharpTokenType.UINT32, FSharpTokenType.UINT64, FSharpTokenType.UNATIVEINT, FSharpTokenType.DECIMAL,
            FSharpTokenType.BIGNUM, FSharpTokenType.IEEE32, FSharpTokenType.IEEE64, FSharpTokenType.BYTEARRAY,
        }) {
            FSharpLexFilterKind kind = byName(type.toString());
            if (kind == null) {
                throw new IllegalStateException(type.toString());
            }
            KINDS.put(type, kind);
        }
        KINDS.put(FSharpTokenType.IDENTIFIER, IDENT);
        KINDS.put(FSharpTokenType.RESERVED_IDENT_FORMATS, IDENT);
        KINDS.put(FSharpTokenType.USE, LET);
        KINDS.put(FSharpTokenType.RETURN, YIELD);
        KINDS.put(FSharpTokenType.LET_BANG, BINDER);
        KINDS.put(FSharpTokenType.USE_BANG, BINDER);
        KINDS.put(FSharpTokenType.AND_BANG, AND_BANG);
        KINDS.put(FSharpTokenType.DO_BANG, DO_BANG);
        KINDS.put(FSharpTokenType.MATCH_BANG, MATCH_BANG);
        KINDS.put(FSharpTokenType.WHILE_BANG, WHILE_BANG);
        KINDS.put(FSharpTokenType.YIELD_BANG, YIELD_BANG);
        KINDS.put(FSharpTokenType.RETURN_BANG, YIELD_BANG);
        KINDS.put(FSharpTokenType.SBYTE, INT8);
        KINDS.put(FSharpTokenType.BYTE, UINT8);
        KINDS.put(FSharpTokenType.BYTECHAR, UINT8);
        KINDS.put(FSharpTokenType.CHARACTER_LITERAL, CHAR);
        KINDS.put(FSharpTokenType.VERBATIM_BYTEARRAY, BYTEARRAY);
        KINDS.put(FSharpTokenType.KEYWORD_STRING_SOURCE_DIRECTORY, KEYWORD_STRING);
        KINDS.put(FSharpTokenType.KEYWORD_STRING_SOURCE_FILE, KEYWORD_STRING);
        KINDS.put(FSharpTokenType.KEYWORD_STRING_LINE, KEYWORD_STRING);
        for (IElementType type : FSharpTokenType.STRINGS.getTypes()) {
            if (!KINDS.containsKey(type)) {
                KINDS.put(type, STRING);
            }
        }
        for (IElementType type : FSharpTokenType.INTERPOLATED_STRINGS.getTypes()) {
            String name = type.toString();
            FSharpLexFilterKind kind;
            if (name.endsWith("_START")) {
                kind = INTERP_STRING_BEGIN_PART;
            }
            else if (name.endsWith("_MIDDLE")) {
                kind = INTERP_STRING_PART;
            }
            else if (name.endsWith("_END")) {
                kind = INTERP_STRING_END;
            }
            else {
                kind = INTERP_STRING_BEGIN_END;
            }
            KINDS.put(type, kind);
        }
        KINDS.put(FSharpTokenType.LQUOTE_TYPED, LQUOTE);
        KINDS.put(FSharpTokenType.LQUOTE_UNTYPED, LQUOTE);
        KINDS.put(FSharpTokenType.RQUOTE_TYPED, RQUOTE);
        KINDS.put(FSharpTokenType.RQUOTE_UNTYPED, RQUOTE);
    }

    private FSharpLexFilterKinds() {
    }

    /**
     * Collects real tokens (whitespace and comments must be skipped by the caller), dropping line directives.
     */
    public static final class Collector {
        private final CharSequence myText;
        private final List<FSharpLexFilter.RealToken> myTokens = new ArrayList<>();
        private int mySkipUntil = -1;

        public Collector(CharSequence text) {
            myText = text;
        }

        public void add(IElementType type, int start, int end) {
            if (start < mySkipUntil) {
                return;
            }
            if (LINE_DIRECTIVES.contains(type)) {
                int lineEnd = start;
                while (lineEnd < myText.length() && myText.charAt(lineEnd) != '\n' && myText.charAt(lineEnd) != '\r') {
                    lineEnd++;
                }
                mySkipUntil = lineEnd;
                return;
            }
            myTokens.add(toReal(type, myText.subSequence(start, end).toString(), start, end));
        }

        public List<FSharpLexFilter.RealToken> getTokens() {
            return myTokens;
        }
    }

    private static @Nullable FSharpLexFilterKind byName(String name) {
        try {
            return FSharpLexFilterKind.valueOf(name);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return the real token for the filter, unknown tokens are {@link FSharpLexFilterKind#OTHER}
     */
    public static FSharpLexFilter.RealToken toReal(IElementType type, String text, int start, int end) {
        if (type == FSharpTokenType.PLUS || type == FSharpTokenType.SYMBOLIC_OP || type == FSharpTokenType.BAD_SYMBOLIC_OP
            || type == FSharpTokenType.PERCENT || type == FSharpTokenType.PERCENT_PERCENT || type == FSharpTokenType.RESERVED_SYMBOLIC_SEQUENCE) {
            return new FSharpLexFilter.RealToken(operatorKind(text), text, false, start, end);
        }
        if (type == FSharpTokenType.IDENTIFIER && "_".equals(text)) {
            // our lexer gives '_' type arguments as identifiers
            return new FSharpLexFilter.RealToken(UNDERSCORE, text, false, start, end);
        }
        FSharpLexFilterKind kind = KINDS.get(type);
        if (kind == null) {
            return new FSharpLexFilter.RealToken(OTHER, text, false, start, end);
        }
        boolean flag = type == FSharpTokenType.LQUOTE_UNTYPED || type == FSharpTokenType.RQUOTE_UNTYPED;
        return new FSharpLexFilter.RealToken(kind, text, flag, start, end);
    }

    /**
     * Operator classification of the compiler lexer ({@code lex.fsl}).
     */
    public static FSharpLexFilterKind operatorKind(String text) {
        switch (text) {
            case "%", "%%":
                return PERCENT_OP;
            case "$":
                return DOLLAR;
            case "?":
                return QMARK;
            case "??":
                return QMARK_QMARK;
            case "..^":
                return DOT_DOT_HAT;
            case "-":
                return MINUS;
            case "*":
                return STAR;
            case "&":
                return AMP;
            case "&&":
                return AMP_AMP;
            case "||":
                return BAR_BAR;
            default:
                break;
        }
        if (text.startsWith(".[") || text.startsWith(".(")) {
            return FUNKY_OPERATOR_NAME;
        }
        int i = 0;
        while (i < text.length() && (text.charAt(i) == '.' || text.charAt(i) == '$' || text.charAt(i) == '?')) {
            i++;
        }
        if (i == text.length()) {
            return text.indexOf('$') >= 0 ? INFIX_COMPARE_OP : PREFIX_OP;
        }
        if (text.startsWith("**", i)) {
            return INFIX_STAR_STAR_OP;
        }
        char c = text.charAt(i);
        return switch (c) {
            case '*', '/', '%' -> INFIX_STAR_DIV_MOD_OP;
            case '+', '-' -> PLUS_MINUS_OP;
            case '@', '^' -> INFIX_AT_HAT_OP;
            case '=', '<', '>' -> INFIX_COMPARE_OP;
            case '!' -> text.startsWith("!=", i) ? INFIX_COMPARE_OP : PREFIX_OP;
            case '&' -> INFIX_AMP_OP;
            case '|' -> INFIX_BAR_OP;
            default -> PREFIX_OP;
        };
    }
}
