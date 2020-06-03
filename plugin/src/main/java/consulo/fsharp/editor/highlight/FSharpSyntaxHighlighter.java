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

package consulo.fsharp.editor.highlight;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import consulo.fsharp.lang.lexer._FSharpLexer;
import consulo.fsharp.lang.psi.FSharpTokenTypes;

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
