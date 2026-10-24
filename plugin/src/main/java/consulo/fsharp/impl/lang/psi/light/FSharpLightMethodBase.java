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
import consulo.dotnet.psi.DotNetCodeBodyProxy;
import consulo.dotnet.psi.DotNetGenericParameter;
import consulo.dotnet.psi.DotNetGenericParameterList;
import consulo.dotnet.psi.DotNetLikeMethodDeclaration;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetParameter;
import consulo.dotnet.psi.DotNetParameterList;
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.dotnet.psi.DotNetType;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public abstract class FSharpLightMethodBase extends FSharpLightElement implements DotNetLikeMethodDeclaration {
    /**
     * Parameter as declared: source, name and type.
     */
    public record ParameterInfo(PsiElement source, String name, DotNetTypeRef typeRef) {
    }

    private final DotNetTypeRef myReturnTypeRef;
    private final DotNetParameter[] myParameters;

    protected FSharpLightMethodBase(PsiElement source,
                                    DotNetQualifiedElement owner,
                                    String name,
                                    Set<DotNetModifier> modifiers,
                                    List<ParameterInfo> parameters,
                                    DotNetTypeRef returnTypeRef) {
        super(source, owner, name, modifiers);
        myReturnTypeRef = returnTypeRef;
        List<DotNetParameter> list = new ArrayList<>(parameters.size());
        for (ParameterInfo parameter : parameters) {
            list.add(new FSharpLightParameter(parameter.source(), this, parameter.name(), parameter.typeRef(), list.size()));
        }
        myParameters = list.toArray(DotNetParameter[]::new);
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetType getReturnType() {
        return null;
    }

    @RequiredReadAction
    @Override
    public DotNetTypeRef getReturnTypeRef() {
        return myReturnTypeRef;
    }

    @RequiredReadAction
    @Override
    public DotNetTypeRef[] getParameterTypeRefs() {
        DotNetTypeRef[] typeRefs = new DotNetTypeRef[myParameters.length];
        for (int i = 0; i < myParameters.length; i++) {
            typeRefs[i] = myParameters[i].toTypeRef(false);
        }
        return typeRefs;
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetParameterList getParameterList() {
        return null;
    }

    @RequiredReadAction
    @Override
    public DotNetParameter[] getParameters() {
        return myParameters;
    }

    @Override
    public DotNetCodeBodyProxy getCodeBlock() {
        return DotNetCodeBodyProxy.EMPTY;
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetGenericParameterList getGenericParameterList() {
        return null;
    }

    @RequiredReadAction
    @Override
    public DotNetGenericParameter[] getGenericParameters() {
        return DotNetGenericParameter.EMPTY_ARRAY;
    }

    @RequiredReadAction
    @Override
    public int getGenericParametersCount() {
        return 0;
    }
}
