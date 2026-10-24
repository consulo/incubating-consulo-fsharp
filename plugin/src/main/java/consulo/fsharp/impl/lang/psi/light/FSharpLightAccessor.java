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

import consulo.dotnet.psi.DotNetCodeBodyProxy;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.dotnet.psi.DotNetXAccessor;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpLightAccessor extends FSharpLightElement implements DotNetXAccessor {
    private final Kind myKind;

    public FSharpLightAccessor(PsiElement source, DotNetQualifiedElement owner, Kind kind, Set<DotNetModifier> modifiers) {
        super(source, owner, kind == Kind.GET ? "get" : "set", modifiers);
        myKind = kind;
    }

    @Override
    public @Nullable PsiElement getAccessorElement() {
        return null;
    }

    @Override
    public Kind getAccessorKind() {
        return myKind;
    }

    @Override
    public DotNetCodeBodyProxy getCodeBlock() {
        return DotNetCodeBodyProxy.EMPTY;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((FSharpLightAccessor) obj).myOwner.equals(myOwner);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 + myOwner.hashCode();
    }
}
