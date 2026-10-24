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
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.dotnet.psi.DotNetType;
import consulo.dotnet.psi.DotNetVariable;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public abstract class FSharpLightVariableBase extends FSharpLightElement implements DotNetVariable {
    private final DotNetTypeRef myTypeRef;
    private final boolean myConstant;

    protected FSharpLightVariableBase(PsiElement source, DotNetQualifiedElement owner, String name, Set<DotNetModifier> modifiers,
                                      DotNetTypeRef typeRef, boolean constant) {
        super(source, owner, name, modifiers);
        myTypeRef = typeRef;
        myConstant = constant;
    }

    @RequiredReadAction
    @Override
    public boolean isConstant() {
        return myConstant;
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
}
