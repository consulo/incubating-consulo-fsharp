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

import consulo.dotnet.psi.DotNetFieldDeclaration;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.language.psi.PsiElement;

import java.util.Set;

/**
 * {@code val} field, enum case, {@code [<Literal>]} value, union case tag.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpLightField extends FSharpLightVariableBase implements DotNetFieldDeclaration {
    public FSharpLightField(PsiElement source, DotNetQualifiedElement owner, String name, Set<DotNetModifier> modifiers,
                            DotNetTypeRef typeRef, boolean constant) {
        super(source, owner, name, modifiers, typeRef, constant);
    }
}
