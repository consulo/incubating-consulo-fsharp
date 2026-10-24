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
import consulo.fsharp.impl.lang.lexer.FSharpPreprocessedLexer;
import consulo.fsharp.impl.lang.parser.FSharpPsiParser;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpFileImpl;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
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

import java.util.Set;

/**
 * @author VISTALL
 * @since 23-Nov-16.
 */
@ExtensionImpl
public class FSharpParserDefinition implements ParserDefinition
{

	/**
	 * Comments, inactive code and conditional directives: the parser does not see them.
	 */
	public static final TokenSet COMMENTS = TokenSet.orSet(FSharpTokenType.COMMENTS, TokenSet.create(
		FSharpTokenType.DEAD_CODE,
		FSharpTokenType.PP_IF_SECTION,
		FSharpTokenType.PP_ELIF_SECTION,
		FSharpTokenType.PP_ELSE_SECTION,
		FSharpTokenType.PP_ENDIF,
		FSharpTokenType.PP_CONDITIONAL_SYMBOL,
		FSharpTokenType.PP_AND,
		FSharpTokenType.PP_OR,
		FSharpTokenType.PP_NOT,
		FSharpTokenType.PP_LPAR,
		FSharpTokenType.PP_RPAR,
		FSharpTokenType.PP_BAD_CHARACTER
	));

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
		// TODO [VISTALL] defined constants of the module
		return new FSharpPreprocessedLexer(Set.of());
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
		return FSharpElementTypes.FSHARP_IMPL_FILE;
	}

	@Nonnull
	@Override
	public TokenSet getWhitespaceTokens(@Nonnull LanguageVersion languageVersion)
	{
		return FSharpTokenType.WHITESPACES;
	}

	@Nonnull
	@Override
	public TokenSet getCommentTokens(@Nonnull LanguageVersion languageVersion)
	{
		return COMMENTS;
	}

	@Nonnull
	@Override
	public TokenSet getStringLiteralElements(@Nonnull LanguageVersion languageVersion)
	{
		return FSharpTokenType.STRINGS;
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
