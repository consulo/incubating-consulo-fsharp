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
package consulo.fsharp.impl.lang.psi.light;

import consulo.annotation.access.RequiredReadAction;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetModifierList;
import consulo.dotnet.psi.DotNetModifierListOwner;
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.fsharp.FSharpLanguage;
import consulo.language.impl.psi.LightElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.util.IncorrectOperationException;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * A member of the compiled view of F# declarations: navigates to its F# source.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public abstract class FSharpLightElement extends LightElement implements DotNetQualifiedElement, DotNetModifierListOwner,
    PsiNameIdentifierOwner {
    protected final PsiElement mySource;
    protected final DotNetQualifiedElement myOwner;
    protected final String myName;
    protected final Set<DotNetModifier> myModifiers;

    protected FSharpLightElement(PsiElement source, DotNetQualifiedElement owner, String name, Set<DotNetModifier> modifiers) {
        super(source.getManager(), FSharpLanguage.INSTANCE);
        mySource = source;
        myOwner = owner;
        myName = name;
        myModifiers = Set.copyOf(modifiers);
        setNavigationElement(source);
    }

    public PsiElement getSource() {
        return mySource;
    }

    @Override
    public PsiElement getParent() {
        return myOwner;
    }

    @Override
    public @Nullable PsiFile getContainingFile() {
        return mySource.getContainingFile();
    }

    @Override
    public boolean isValid() {
        return mySource.isValid();
    }

    @RequiredReadAction
    @Override
    public int getTextOffset() {
        return mySource.getTextOffset();
    }

    @RequiredReadAction
    @Override
    public String getName() {
        return myName;
    }

    @Override
    public PsiElement setName(String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Rename the F# declaration");
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return mySource instanceof PsiNameIdentifierOwner owner ? owner.getNameIdentifier() : null;
    }

    @RequiredReadAction
    @Override
    public @Nullable String getPresentableParentQName() {
        return myOwner.getPresentableQName();
    }

    @RequiredReadAction
    @Override
    public @Nullable String getPresentableQName() {
        String parent = getPresentableParentQName();
        return parent == null || parent.isEmpty() ? myName : parent + "." + myName;
    }

    @RequiredReadAction
    @Override
    public boolean hasModifier(DotNetModifier modifier) {
        return myModifiers.contains(modifier);
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetModifierList getModifierList() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass() && mySource.equals(((FSharpLightElement) obj).mySource)
            && myName.equals(((FSharpLightElement) obj).myName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), mySource, myName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + myName;
    }
}
