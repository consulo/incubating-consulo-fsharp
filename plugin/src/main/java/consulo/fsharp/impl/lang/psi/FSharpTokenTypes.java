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

package consulo.fsharp.impl.lang.psi;

import consulo.fsharp.FSharpLanguage;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;

/**
 * @author VISTALL
 * @since 23-Nov-16.
 */
public interface FSharpTokenTypes extends TokenType
{
	IElementType EOF = new IElementType("EOF", FSharpLanguage.INSTANCE);

	IElementType LINE_COMMENT = new IElementType("LINE_COMMENT", FSharpLanguage.INSTANCE);

	IElementType OPEN_KEYWORD = new IElementType("OPEN_KEYWORD", FSharpLanguage.INSTANCE);

	IElementType LET_KEYWORD = new IElementType("LET_KEYWORD", FSharpLanguage.INSTANCE);

	IElementType NEW_KEYWORD = new IElementType("NEW_KEYWORD", FSharpLanguage.INSTANCE);

	IElementType IDENTIFIER = new IElementType("IDENTIFIER", FSharpLanguage.INSTANCE);

	IElementType DOT = new IElementType("DOT", FSharpLanguage.INSTANCE);
}
