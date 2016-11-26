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

package consulo.fsharp.lang.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.psi.tree.IElementType;
import consulo.fsharp.FSharpLanguage;
import consulo.psi.tree.ElementTypeAsPsiFactory;

/**
 * @author VISTALL
 * @since 24-Nov-16.
 */
public interface FSharpElementTypes
{
	IElementType IMPORT_DECLARATION = new ElementTypeAsPsiFactory("IMPORT_DECLARATION", FSharpLanguage.INSTANCE, ASTWrapperPsiElement.class);

	IElementType REFERENCE_EXPRESSION = new ElementTypeAsPsiFactory("REFERENCE_EXPRESSION", FSharpLanguage.INSTANCE, ASTWrapperPsiElement.class);
}
