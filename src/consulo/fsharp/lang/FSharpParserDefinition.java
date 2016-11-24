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

package consulo.fsharp.lang;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.EmptyLexer;
import com.intellij.lexer.Lexer;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.lang.psi.FSharpFileImpl;
import consulo.lang.LanguageVersion;

/**
 * @author VISTALL
 * @since 23-Nov-16.
 */
public class FSharpParserDefinition implements ParserDefinition
{
	private static final IFileElementType ourFileElementType = new IFileElementType(FSharpLanguage.INSTANCE);

	@NotNull
	@Override
	public Lexer createLexer(@NotNull LanguageVersion languageVersion)
	{
		return new EmptyLexer();
	}

	@NotNull
	@Override
	public PsiParser createParser(@NotNull LanguageVersion languageVersion)
	{
		return new PsiParser()
		{
			@NotNull
			@Override
			public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder, @NotNull LanguageVersion languageVersion)
			{
				PsiBuilder.Marker mark = builder.mark();
				while(!builder.eof())
				{
					builder.advanceLexer();
				}
				mark.done(root);
				return builder.getTreeBuilt();
			}
		};
	}

	@NotNull
	@Override
	public IFileElementType getFileNodeType()
	{
		return ourFileElementType;
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens(@NotNull LanguageVersion languageVersion)
	{
		return TokenSet.create(TokenType.WHITE_SPACE);
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens(@NotNull LanguageVersion languageVersion)
	{
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements(@NotNull LanguageVersion languageVersion)
	{
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public PsiElement createElement(@NotNull ASTNode astNode)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PsiFile createFile(@NotNull FileViewProvider fileViewProvider)
	{
		return new FSharpFileImpl(fileViewProvider);
	}

	@NotNull
	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1)
	{
		return SpaceRequirements.MAY;
	}
}
