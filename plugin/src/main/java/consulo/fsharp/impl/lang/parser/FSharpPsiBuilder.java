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

import consulo.fsharp.impl.lang.psi.FSharpTokenTypes;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderAdapter;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 26-Nov-16.
 */
public class FSharpPsiBuilder extends PsiBuilderAdapter
{
	private int myIndentSize = 0;

	public FSharpPsiBuilder(PsiBuilder delegate)
	{
		super(delegate);
	}

	public void advanceLexerNoAdvanceEof()
	{
		advanceLexerInner(false);
		super.advanceLexer();
	}

	@Override
	public void advanceLexer()
	{
		advanceLexerInner(true);
		super.advanceLexer();
	}

	@Nullable
	public IElementType getTokenTypeNoAdvanceEof()
	{
		advanceLexerInner(false);
		return super.getTokenType();
	}

	@Nullable
	@Override
	public IElementType getTokenType()
	{
		advanceLexerInner(true);
		return super.getTokenType();
	}

	@Nullable
	@Override
	public String getTokenText()
	{
		advanceLexerInner(true);
		return super.getTokenText();
	}

	public void wantEof()
	{
		IElementType tokenTypeNoEof = getTokenTypeNoAdvanceEof();
		if(tokenTypeNoEof == null)
		{
			return;
		}

		if(tokenTypeNoEof == FSharpTokenTypes.EOF)
		{
			super.advanceLexer();
		}
		else
		{
			error("Expected new line");
		}
	}

	private void advanceLexerInner(boolean eof)
	{
		while(true)
		{
			IElementType tokenType = super.getTokenType();
			if(tokenType == null)
			{
				return;
			}

			if(eof && tokenType == FSharpTokenTypes.EOF)
			{
				myIndentSize = -1;
				super.advanceLexer();
			}
			else if(tokenType == FSharpTokenTypes.WHITE_SPACE)
			{
				if(myIndentSize == -1)
				{
					myIndentSize = super.getTokenText().length();
				}
				super.advanceLexer();
			}
			else
			{
				return;
			}
		}
	}
}
