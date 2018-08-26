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

package consulo.fsharp.lang.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import consulo.fsharp.lang.psi.FSharpElementTypes;
import consulo.fsharp.lang.psi.FSharpTokenTypes;
import consulo.lang.LanguageVersion;

/**
 * @author VISTALL
 * @since 24-Nov-16.
 */
public class FSharpPsiParser implements PsiParser
{
	@NotNull
	@Override
	public ASTNode parse(@NotNull IElementType rootElementType, @NotNull PsiBuilder psiBuilder, @NotNull LanguageVersion languageVersion)
	{
		FSharpPsiBuilder builder = new FSharpPsiBuilder(psiBuilder);

		PsiBuilder.Marker mark = builder.mark();

		while(!builder.eof())
		{
			IElementType tokenType = builder.getTokenTypeNoAdvanceEof();
			if(tokenType == FSharpTokenTypes.OPEN_KEYWORD)
			{
				parseImportDeclaration(builder);
			}
			else
			{
				builder.advanceLexerNoAdvanceEof();
			}
		}

		mark.done(rootElementType);
		return builder.getTreeBuilt();
	}

	private void parseImportDeclaration(FSharpPsiBuilder builder)
	{
		PsiBuilder.Marker mark = builder.mark();

		builder.advanceLexerNoAdvanceEof();

		if(parseQualifiedReferenceExpression(builder, null) == null)
		{
			builder.error("Expected expression");
		}

		builder.wantEof();

		mark.done(FSharpElementTypes.IMPORT_DECLARATION);
	}

	private PsiBuilder.Marker parseQualifiedReferenceExpression(FSharpPsiBuilder builder, @Nullable PsiBuilder.Marker prevMarker)
	{
		IElementType tokenType = builder.getTokenTypeNoAdvanceEof();

		if(tokenType == FSharpTokenTypes.IDENTIFIER)
		{
			PsiBuilder.Marker mark = prevMarker == null ? builder.mark() : prevMarker;
			builder.advanceLexerNoAdvanceEof();
			mark.done(FSharpElementTypes.REFERENCE_EXPRESSION);

			IElementType nextTokenType = builder.getTokenTypeNoAdvanceEof();
			if(nextTokenType == FSharpTokenTypes.DOT)
			{
				builder.advanceLexerNoAdvanceEof();

				return parseQualifiedReferenceExpression(builder, mark.precede());
			}

			return mark;
		}
		else
		{
			if(prevMarker != null)
			{
				prevMarker.error("Expected identifier");
			}
			return null;
		}
	}

}
