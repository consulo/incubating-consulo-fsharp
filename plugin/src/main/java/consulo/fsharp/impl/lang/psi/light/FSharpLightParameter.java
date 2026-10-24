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
import consulo.dotnet.psi.DotNetExpression;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetModifierList;
import consulo.dotnet.psi.DotNetParameter;
import consulo.dotnet.psi.DotNetParameterListOwner;
import consulo.dotnet.psi.DotNetType;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.fsharp.FSharpLanguage;
import consulo.language.impl.psi.LightElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.util.IncorrectOperationException;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpLightParameter extends LightElement implements DotNetParameter {
    private final PsiElement mySource;
    private final DotNetParameterListOwner myOwner;
    private final String myName;
    private final DotNetTypeRef myTypeRef;
    private final int myIndex;

    public FSharpLightParameter(PsiElement source, DotNetParameterListOwner owner, String name, DotNetTypeRef typeRef, int index) {
        super(source.getManager(), FSharpLanguage.INSTANCE);
        mySource = source;
        myOwner = owner;
        myName = name;
        myTypeRef = typeRef;
        myIndex = index;
        setNavigationElement(source);
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

    @Override
    public DotNetParameterListOwner getOwner() {
        return myOwner;
    }

    @Override
    public int getIndex() {
        return myIndex;
    }

    @RequiredReadAction
    @Override
    public boolean isConstant() {
        return false;
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getConstantKeywordElement() {
        return null;
    }

    @RequiredReadAction
    @Override
    public DotNetTypeRef toTypeRef(boolean resolveFromInitializer) {
        return myTypeRef;
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetType getType() {
        return null;
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetExpression getInitializer() {
        return null;
    }

    @RequiredReadAction
    @Override
    public boolean hasModifier(DotNetModifier modifier) {
        return false;
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetModifierList getModifierList() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FSharpLightParameter other && other.myOwner.equals(myOwner) && other.myIndex == myIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myOwner, myIndex);
    }

    @Override
    public String toString() {
        return "FSharpLightParameter:" + myName;
    }
}
