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

import consulo.dotnet.psi.DotNetConstructorDeclaration;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.language.psi.PsiElement;

import java.util.List;
import java.util.Set;

/**
 * Primary or secondary constructor, record and exception constructors.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpLightConstructor extends FSharpLightMethodBase implements DotNetConstructorDeclaration {
    public FSharpLightConstructor(PsiElement source, DotNetQualifiedElement owner, String name, Set<DotNetModifier> modifiers,
                                  List<ParameterInfo> parameters) {
        super(source, owner, name, modifiers, parameters, DotNetTypeRef.UNKNOWN_TYPE);
    }

    @Override
    public boolean isDeConstructor() {
        return false;
    }
}
