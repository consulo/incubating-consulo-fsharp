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

package consulo.fsharp.impl.lang.parser;

import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpTokenTypes;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.version.LanguageVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 24-Nov-16.
 */
public class FSharpPsiParser implements PsiParser
{
	@Nonnull
	@Override
	public ASTNode parse(@Nonnull IElementType rootElementType, @Nonnull PsiBuilder psiBuilder, @Nonnull LanguageVersion languageVersion)
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
