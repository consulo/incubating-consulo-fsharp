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

package consulo.fsharp.impl.lang.psi.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * Named pattern ({@code LOCAL_REFERENCE_PAT}, {@code TOP_REFERENCE_PAT}): declares a value unless it names a union
 * case or an active pattern.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpReferencePatImpl extends FSharpNamedElementImpl {
    public FSharpReferencePatImpl(ASTNode node) {
        super(node);
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getNameIdentifier() {
        ASTNode name = getNode().findChildByType(FSharpElementTypes.EXPRESSION_REFERENCE_NAME);
        if (name == null) {
            return null;
        }
        // qualified names (A.B) are references, not declarations
        if (name.findChildByType(FSharpElementTypes.EXPRESSION_REFERENCE_NAME) != null) {
            return null;
        }
        ASTNode identifier = name.findChildByType(FSharpTokenType.IDENTIFIER);
        return identifier == null ? null : identifier.getPsi();
    }

    /**
     * {@code | None ->}: an upper case name of a visible union case, exception or active pattern is a reference.
     */
    @RequiredReadAction
    public boolean isConstructorReference() {
        String name = getName();
        if (name == null || name.isEmpty() || !Character.isUpperCase(name.charAt(0))) {
            return false;
        }
        return FSharpResolver.resolveInScope(this, name, FSharpResolver.Kind.CONSTRUCTOR) != null;
    }
}
