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

package consulo.fsharp.impl.editor.highlight;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;

/**
 * Colors of F# code: lexer tokens ({@link FSharpSyntaxHighlighter}) and declarations and resolved references
 * ({@link FSharpHighlightVisitor}), like {@code CSharpHighlightKey}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public interface FSharpHighlightKey {
    TextAttributesKey KEYWORD = TextAttributesKey.of("FSHARP_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    TextAttributesKey PREPROCESSOR_KEYWORD = TextAttributesKey.of("FSHARP_PREPROCESSOR_KEYWORD", DefaultLanguageHighlighterColors.MACRO_KEYWORD);
    TextAttributesKey LINE_COMMENT = TextAttributesKey.of("FSHARP_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    TextAttributesKey BLOCK_COMMENT = TextAttributesKey.of("FSHARP_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    TextAttributesKey STRING = TextAttributesKey.of("FSHARP_STRING", DefaultLanguageHighlighterColors.STRING);
    TextAttributesKey NUMBER = TextAttributesKey.of("FSHARP_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    TextAttributesKey PARENTHESES = TextAttributesKey.of("FSHARP_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
    TextAttributesKey BRACKETS = TextAttributesKey.of("FSHARP_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    TextAttributesKey BRACES = TextAttributesKey.of("FSHARP_BRACES", DefaultLanguageHighlighterColors.BRACES);
    TextAttributesKey ATTRIBUTE_BRACKETS = TextAttributesKey.of("FSHARP_ATTRIBUTE_BRACKETS", DefaultLanguageHighlighterColors.METADATA);
    TextAttributesKey DOT = TextAttributesKey.of("FSHARP_DOT", DefaultLanguageHighlighterColors.DOT);
    TextAttributesKey COMMA = TextAttributesKey.of("FSHARP_COMMA", DefaultLanguageHighlighterColors.COMMA);
    TextAttributesKey SEMICOLON = TextAttributesKey.of("FSHARP_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    TextAttributesKey OPERATION_SIGN = TextAttributesKey.of("FSHARP_OPERATION_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    TextAttributesKey CLASS_NAME = TextAttributesKey.of("FSHARP_CLASS_NAME", DefaultLanguageHighlighterColors.CLASS_NAME);
    TextAttributesKey INTERFACE_NAME = TextAttributesKey.of("FSHARP_INTERFACE_NAME", DefaultLanguageHighlighterColors.INTERFACE_NAME);
    TextAttributesKey STRUCT_NAME = TextAttributesKey.of("FSHARP_STRUCT_NAME", CLASS_NAME);
    TextAttributesKey ENUM_NAME = TextAttributesKey.of("FSHARP_ENUM_NAME", CLASS_NAME);
    TextAttributesKey RECORD_NAME = TextAttributesKey.of("FSHARP_RECORD_NAME", CLASS_NAME);
    TextAttributesKey UNION_NAME = TextAttributesKey.of("FSHARP_UNION_NAME", CLASS_NAME);
    TextAttributesKey MODULE_NAME = TextAttributesKey.of("FSHARP_MODULE_NAME", CLASS_NAME);
    /**
     * {@code int}, {@code string}, {@code unit}: type abbreviations of FSharp.Core, colored like keywords by default
     */
    TextAttributesKey PREDEFINED_TYPE = TextAttributesKey.of("FSHARP_PREDEFINED_TYPE", KEYWORD);
    TextAttributesKey TYPE_PARAMETER = TextAttributesKey.of("FSHARP_TYPE_PARAMETER", DefaultLanguageHighlighterColors.TYPE_ALIAS_NAME);

    TextAttributesKey UNION_CASE = TextAttributesKey.of("FSHARP_UNION_CASE", DefaultLanguageHighlighterColors.CONSTANT);
    TextAttributesKey CONSTANT = TextAttributesKey.of("FSHARP_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT);

    TextAttributesKey FUNCTION = TextAttributesKey.of("FSHARP_FUNCTION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
    TextAttributesKey VALUE = TextAttributesKey.of("FSHARP_VALUE", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);
    TextAttributesKey METHOD = TextAttributesKey.of("FSHARP_METHOD", DefaultLanguageHighlighterColors.INSTANCE_METHOD);
    TextAttributesKey STATIC_METHOD = TextAttributesKey.of("FSHARP_STATIC_METHOD", DefaultLanguageHighlighterColors.STATIC_METHOD);
    TextAttributesKey FIELD = TextAttributesKey.of("FSHARP_FIELD", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    TextAttributesKey PROPERTY = TextAttributesKey.of("FSHARP_PROPERTY", FIELD);
    TextAttributesKey STATIC_FIELD = TextAttributesKey.of("FSHARP_STATIC_FIELD", DefaultLanguageHighlighterColors.STATIC_FIELD);

    TextAttributesKey PARAMETER = TextAttributesKey.of("FSHARP_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER);
    TextAttributesKey LOCAL_VARIABLE = TextAttributesKey.of("FSHARP_LOCAL_VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
}
