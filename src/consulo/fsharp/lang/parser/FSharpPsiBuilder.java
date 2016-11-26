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

import org.jetbrains.annotations.Nullable;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.impl.PsiBuilderAdapter;
import com.intellij.psi.tree.IElementType;
import consulo.fsharp.lang.psi.FSharpTokenTypes;

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

	@Override
	public void advanceLexer()
	{
		advanceLexerInner();
		super.advanceLexer();
	}

	@Nullable
	@Override
	public IElementType getTokenType()
	{
		advanceLexerInner();
		return super.getTokenType();
	}

	@Nullable
	@Override
	public String getTokenText()
	{
		advanceLexerInner();
		return super.getTokenText();
	}

	private void advanceLexerInner()
	{
		while(true)
		{
			IElementType tokenType = super.getTokenType();
			if(tokenType == null)
			{
				return;
			}

			if(tokenType == FSharpTokenTypes.EOF)
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
