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

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.fsharp.impl.lang.lexer._FSharpLexer;
import consulo.fsharp.impl.lang.psi.FSharpTokenTypes;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.lexer.Lexer;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 26-Nov-16.
 */
public class FSharpSyntaxHighlighter extends SyntaxHighlighterBase
{
	private static Map<IElementType, TextAttributesKey> ourKeys = new HashMap<IElementType, TextAttributesKey>();

	static
	{
		safeMap(ourKeys, FSharpTokenTypes.OPEN_KEYWORD, DefaultLanguageHighlighterColors.KEYWORD);
		safeMap(ourKeys, FSharpTokenTypes.LET_KEYWORD, DefaultLanguageHighlighterColors.KEYWORD);
		safeMap(ourKeys, FSharpTokenTypes.NEW_KEYWORD, DefaultLanguageHighlighterColors.KEYWORD);

		safeMap(ourKeys, FSharpTokenTypes.BAD_CHARACTER, HighlighterColors.BAD_CHARACTER);
	}

	@Nonnull
	@Override
	public Lexer getHighlightingLexer()
	{
		return new _FSharpLexer();
	}

	@Nonnull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType iElementType)
	{
		return pack(ourKeys.get(iElementType));
	}
}
