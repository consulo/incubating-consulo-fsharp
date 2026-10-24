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
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * Declared types without inference: type annotations of values.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class FSharpTypeOfUtil {
    private FSharpTypeOfUtil() {
    }

    /**
     * {@code (x: R)} or {@code let x: R = ...}: the declaration of {@code R}.
     */
    @RequiredReadAction
    public static @Nullable PsiElement resolveAnnotatedType(PsiElement value) {
        PsiElement typeUsage = null;
        PsiElement parent = value.getParent();
        if (parent != null && parent.getNode().getElementType() == FSharpElementTypes.TYPED_PAT) {
            typeUsage = parent.getLastChild();
        }
        else if (parent != null) {
            IElementType parentType = parent.getNode().getElementType();
            if (parentType == FSharpElementTypes.TOP_BINDING || parentType == FSharpElementTypes.LOCAL_BINDING) {
                PsiElement info = FSharpPsiUtil.findChild(parent, FSharpElementTypes.RETURN_TYPE_INFO);
                if (info != null && FSharpPsiUtil.findChild(parent, FSharpElementTypes.PARAMETERS_PATTERN_DECLARATION) == null) {
                    typeUsage = info.getLastChild();
                }
            }
        }
        if (typeUsage == null || typeUsage.getNode().getElementType() != FSharpElementTypes.NAMED_TYPE_USAGE) {
            return null;
        }
        PsiElement name = FSharpPsiUtil.findChild(typeUsage, FSharpElementTypes.TYPE_REFERENCE_NAME);
        if (name instanceof FSharpReferenceNameImpl referenceName) {
            PsiElement target = referenceName.resolve();
            if (target != null && target.getNode().getElementType() == FSharpElementTypes.FSHARP_TYPE_DECLARATION) {
                return target;
            }
        }
        return null;
    }
}
