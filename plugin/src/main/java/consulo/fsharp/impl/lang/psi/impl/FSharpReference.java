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
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.util.IncorrectOperationException;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Soft reference: unresolved names are not errors until resolve covers .NET and FSharp.Core.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpReference extends PsiReferenceBase<PsiElement> {
    private final PsiElement myIdentifier;
    private final Supplier<@Nullable PsiElement> myResolver;
    private final Supplier<Object[]> myVariants;

    public FSharpReference(PsiElement owner, PsiElement identifier, Supplier<@Nullable PsiElement> resolver, Supplier<Object[]> variants) {
        super(owner, identifier.getTextRange().shiftRight(-owner.getTextRange().getStartOffset()), true);
        myIdentifier = identifier;
        myResolver = resolver;
        myVariants = variants;
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement resolve() {
        return myResolver.get();
    }

    @RequiredReadAction
    @Override
    public Object[] getVariants() {
        return myVariants.get();
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        myIdentifier.replace(FSharpPsiUtil.createIdentifier(myElement.getProject(), newElementName));
        return myElement;
    }

    public TextRange getIdentifierRange() {
        return myIdentifier.getTextRange();
    }
}
