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
import consulo.dotnet.psi.DotNetMethodDeclaration;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.dotnet.psi.DotNetType;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Module function, member method, union case constructor ({@code NewCase}).
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpLightMethod extends FSharpLightMethodBase implements DotNetMethodDeclaration {
    public FSharpLightMethod(PsiElement source,
                             DotNetQualifiedElement owner,
                             String name,
                             Set<DotNetModifier> modifiers,
                             List<ParameterInfo> parameters,
                             DotNetTypeRef returnTypeRef) {
        super(source, owner, name, modifiers, parameters, returnTypeRef);
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
