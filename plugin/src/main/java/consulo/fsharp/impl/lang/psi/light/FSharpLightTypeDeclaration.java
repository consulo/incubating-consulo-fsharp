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
import consulo.dotnet.psi.DotNetGenericParameter;
import consulo.dotnet.psi.DotNetGenericParameterList;
import consulo.dotnet.psi.DotNetInheritUtil;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.dotnet.psi.DotNetTypeList;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Generated nested type: union case class ({@code Shape.Circle}), {@code Tags}.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpLightTypeDeclaration extends FSharpLightElement implements DotNetTypeDeclaration {
    private final DotNetTypeDeclaration myOuter;
    private final DotNetTypeRef myBaseTypeRef;
    private final DotNetNamedElement[] myMembers;

    public FSharpLightTypeDeclaration(PsiElement source,
                                      DotNetTypeDeclaration outer,
                                      String name,
                                      Set<DotNetModifier> modifiers,
                                      DotNetTypeRef baseTypeRef,
                                      Function<FSharpLightTypeDeclaration, List<? extends DotNetNamedElement>> members) {
        super(source, outer, name, modifiers);
        myOuter = outer;
        myBaseTypeRef = baseTypeRef;
        myMembers = members.apply(this).toArray(DotNetNamedElement[]::new);
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isStruct() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public boolean isNested() {
        return true;
    }

    @Override
    public @Nullable DotNetTypeList getExtendList() {
        return null;
    }

    @Override
    public DotNetTypeRef[] getExtendTypeRefs() {
        return new DotNetTypeRef[]{myBaseTypeRef};
    }

    @RequiredReadAction
    @Override
    public boolean isInheritor(String otherVmQName, boolean deep) {
        return DotNetInheritUtil.isInheritor(this, otherVmQName, deep);
    }

    @Override
    public DotNetTypeRef getTypeRefForEnumConstants() {
        return DotNetTypeRef.ERROR_TYPE;
    }

    @RequiredReadAction
    @Override
    public @Nullable String getVmQName() {
        String outer = myOuter.getVmQName();
        return outer == null ? myName : outer + "/" + myName;
    }

    @Override
    public String getVmName() {
        return myName;
    }

    @Override
    public @Nullable DotNetGenericParameterList getGenericParameterList() {
        return null;
    }

    @Override
    public DotNetGenericParameter[] getGenericParameters() {
        return DotNetGenericParameter.EMPTY_ARRAY;
    }

    @Override
    public int getGenericParametersCount() {
        return 0;
    }

    @Override
    public DotNetNamedElement[] getMembers() {
        return myMembers;
    }
}
