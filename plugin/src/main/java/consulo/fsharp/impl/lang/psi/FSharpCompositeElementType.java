/*
 * Copyright 2013-2026 consulo.io
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
import consulo.fsharp.impl.lang.psi.impl.FSharpElementImpl;
import consulo.language.ast.ASTNode;
import consulo.language.ast.ElementTypeAsPsiFactory;
import consulo.language.psi.PsiElement;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpCompositeElementType extends ElementTypeAsPsiFactory {
    public FSharpCompositeElementType(String debugName) {
        this(debugName, FSharpElementImpl::new);
    }

    public FSharpCompositeElementType(String debugName, Function<ASTNode, ? extends PsiElement> factory) {
        super(debugName, FSharpLanguage.INSTANCE, factory);
    }
}
