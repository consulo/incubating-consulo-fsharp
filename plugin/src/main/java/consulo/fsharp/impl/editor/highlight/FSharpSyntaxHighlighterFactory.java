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

import consulo.annotation.component.ExtensionImpl;
import consulo.fsharp.FSharpLanguage;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 26-Nov-16.
 */
@ExtensionImpl
public class FSharpSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory
{
	@Nonnull
	@Override
	protected SyntaxHighlighter createHighlighter()
	{
		return new FSharpSyntaxHighlighter();
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return FSharpLanguage.INSTANCE;
	}
}
