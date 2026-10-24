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
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.dotnet.psi.DotNetPropertyDeclaration;
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.dotnet.psi.DotNetType;
import consulo.dotnet.psi.DotNetXAccessor;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Module value, member property, record field, union case field and test.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpLightProperty extends FSharpLightVariableBase implements DotNetPropertyDeclaration {
    private final DotNetXAccessor[] myAccessors;

    public FSharpLightProperty(PsiElement source, DotNetQualifiedElement owner, String name, Set<DotNetModifier> modifiers,
                               DotNetTypeRef typeRef, boolean setter) {
        super(source, owner, name, modifiers, typeRef, false);
        Set<DotNetModifier> accessorModifiers = Set.of(DotNetModifier.PUBLIC);
        myAccessors = setter
            ? new DotNetXAccessor[]{new FSharpLightAccessor(source, this, DotNetXAccessor.Kind.GET, accessorModifiers),
            new FSharpLightAccessor(source, this, DotNetXAccessor.Kind.SET, accessorModifiers)}
            : new DotNetXAccessor[]{new FSharpLightAccessor(source, this, DotNetXAccessor.Kind.GET, accessorModifiers)};
    }

    @RequiredReadAction
    @Override
    public DotNetXAccessor[] getAccessors() {
        return myAccessors;
    }

    @RequiredReadAction
    @Override
    public DotNetNamedElement[] getMembers() {
        return myAccessors;
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetType getTypeForImplement() {
        return null;
    }

    @RequiredReadAction
    @Override
    public DotNetTypeRef getTypeRefForImplement() {
        return DotNetTypeRef.ERROR_TYPE;
    }
}
