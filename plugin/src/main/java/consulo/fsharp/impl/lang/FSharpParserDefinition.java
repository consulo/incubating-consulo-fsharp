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

package consulo.fsharp.impl.lang;

import consulo.annotation.component.ExtensionImpl;
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.lexer._FSharpLexer;
import consulo.fsharp.impl.lang.parser.FSharpPsiParser;
import consulo.fsharp.impl.lang.psi.FSharpFileImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23-Nov-16.
 */
@ExtensionImpl
public class FSharpParserDefinition implements ParserDefinition
{
	private static final IFileElementType ourFileElementType = new IFileElementType(FSharpLanguage.INSTANCE);

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return FSharpLanguage.INSTANCE;
	}

	@Nonnull
	@Override
	public Lexer createLexer(@Nonnull LanguageVersion languageVersion)
	{
		return new _FSharpLexer();
	}

	@Nonnull
	@Override
	public PsiParser createParser(@Nonnull LanguageVersion languageVersion)
	{
		return new FSharpPsiParser();
	}

	@Nonnull
	@Override
	public IFileElementType getFileNodeType()
	{
		return ourFileElementType;
	}

	@Nonnull
	@Override
	public TokenSet getWhitespaceTokens(@Nonnull LanguageVersion languageVersion)
	{
		return TokenSet.EMPTY;
		//return TokenSet.create(TokenType.WHITE_SPACE, FSharpTokenTypes.EOF);
	}

	@Nonnull
	@Override
	public TokenSet getCommentTokens(@Nonnull LanguageVersion languageVersion)
	{
		return TokenSet.EMPTY;
	}

	@Nonnull
	@Override
	public TokenSet getStringLiteralElements(@Nonnull LanguageVersion languageVersion)
	{
		return TokenSet.EMPTY;
	}

	@Nonnull
	@Override
	public PsiElement createElement(@Nonnull ASTNode astNode)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PsiFile createFile(@Nonnull FileViewProvider fileViewProvider)
	{
		return new FSharpFileImpl(fileViewProvider);
	}

	@Nonnull
	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1)
	{
		return SpaceRequirements.MAY;
	}
}
