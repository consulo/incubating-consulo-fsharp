/*
 * Copyright 2013-2016 consulo.io
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

package consulo.fsharp.impl.editor.highlight;

import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.fsharp.impl.lang.lexer.FSharpLexer;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.lexer.Lexer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 26-Nov-16.
 */
public class FSharpSyntaxHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> KEYS = new HashMap<>();

    static {
        fillMap(KEYS, FSharpTokenType.KEYWORDS, FSharpHighlightKey.KEYWORD);
        fillMap(KEYS, TokenSet.create(
            FSharpTokenType.LET_BANG,
            FSharpTokenType.USE_BANG,
            FSharpTokenType.DO_BANG,
            FSharpTokenType.YIELD_BANG,
            FSharpTokenType.RETURN_BANG,
            FSharpTokenType.MATCH_BANG,
            FSharpTokenType.AND_BANG,
            FSharpTokenType.WHILE_BANG,
            FSharpTokenType.KEYWORD_STRING_LINE,
            FSharpTokenType.KEYWORD_STRING_SOURCE_DIRECTORY,
            FSharpTokenType.KEYWORD_STRING_SOURCE_FILE
        ), FSharpHighlightKey.KEYWORD);
        fillMap(KEYS, FSharpTokenType.PP_KEYWORDS, FSharpHighlightKey.PREPROCESSOR_KEYWORD);

        safeMap(KEYS, FSharpTokenType.LINE_COMMENT, FSharpHighlightKey.LINE_COMMENT);
        safeMap(KEYS, FSharpTokenType.SHEBANG, FSharpHighlightKey.LINE_COMMENT);
        safeMap(KEYS, FSharpTokenType.BLOCK_COMMENT, FSharpHighlightKey.BLOCK_COMMENT);
        safeMap(KEYS, FSharpTokenType.DEAD_CODE, FSharpHighlightKey.BLOCK_COMMENT);

        fillMap(KEYS, FSharpTokenType.ALL_STRINGS, FSharpHighlightKey.STRING);
        safeMap(KEYS, FSharpTokenType.CHARACTER_LITERAL, FSharpHighlightKey.STRING);
        safeMap(KEYS, FSharpTokenType.BYTECHAR, FSharpHighlightKey.STRING);
        fillMap(KEYS, FSharpTokenType.NUMBERS, FSharpHighlightKey.NUMBER);

        fillMap(KEYS, TokenSet.create(FSharpTokenType.LPAREN, FSharpTokenType.RPAREN), FSharpHighlightKey.PARENTHESES);
        fillMap(KEYS, TokenSet.create(
            FSharpTokenType.LBRACK,
            FSharpTokenType.RBRACK,
            FSharpTokenType.LBRACK_BAR,
            FSharpTokenType.BAR_RBRACK
        ), FSharpHighlightKey.BRACKETS);
        fillMap(KEYS, TokenSet.create(
            FSharpTokenType.LBRACE,
            FSharpTokenType.RBRACE,
            FSharpTokenType.LBRACE_BAR,
            FSharpTokenType.BAR_RBRACE
        ), FSharpHighlightKey.BRACES);
        fillMap(KEYS, TokenSet.create(
            FSharpTokenType.LBRACK_LESS,
            FSharpTokenType.GREATER_RBRACK
        ), FSharpHighlightKey.ATTRIBUTE_BRACKETS);
        safeMap(KEYS, FSharpTokenType.DOT, FSharpHighlightKey.DOT);
        safeMap(KEYS, FSharpTokenType.COMMA, FSharpHighlightKey.COMMA);
        fillMap(KEYS, TokenSet.create(FSharpTokenType.SEMICOLON, FSharpTokenType.SEMICOLON_SEMICOLON), FSharpHighlightKey.SEMICOLON);
        fillMap(KEYS, TokenSet.create(
            FSharpTokenType.SYMBOLIC_OP,
            FSharpTokenType.RARROW,
            FSharpTokenType.LARROW,
            FSharpTokenType.EQUALS,
            FSharpTokenType.PLUS,
            FSharpTokenType.MINUS,
            FSharpTokenType.STAR,
            FSharpTokenType.LESS,
            FSharpTokenType.GREATER,
            FSharpTokenType.BAR,
            FSharpTokenType.AMP,
            FSharpTokenType.AMP_AMP,
            FSharpTokenType.COLON,
            FSharpTokenType.COLON_COLON,
            FSharpTokenType.COLON_EQUALS,
            FSharpTokenType.COLON_GREATER,
            FSharpTokenType.COLON_QMARK,
            FSharpTokenType.COLON_QMARK_GREATER,
            FSharpTokenType.QMARK,
            FSharpTokenType.QMARK_QMARK,
            FSharpTokenType.PERCENT,
            FSharpTokenType.PERCENT_PERCENT,
            FSharpTokenType.DOLLAR,
            FSharpTokenType.DOT_DOT,
            FSharpTokenType.DOT_DOT_DOT
        ), FSharpHighlightKey.OPERATION_SIGN);

        fillMap(KEYS, TokenSet.create(
            FSharpTokenType.BAD_CHARACTER,
            FSharpTokenType.BAD_SYMBOLIC_OP,
            FSharpTokenType.BAD_TAB,
            FSharpTokenType.PP_BAD_CHARACTER
        ), HighlighterColors.BAD_CHARACTER);
    }

    @Override
    public Lexer getHighlightingLexer() {
        return new FSharpLexer();
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType elementType) {
        return pack(KEYS.get(elementType));
    }
}
