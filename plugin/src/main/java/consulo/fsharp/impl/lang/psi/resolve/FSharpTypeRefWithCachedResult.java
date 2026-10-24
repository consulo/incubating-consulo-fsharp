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
package consulo.fsharp.impl.lang.psi.resolve;

import consulo.annotation.access.RequiredReadAction;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.dotnet.psi.resolve.DotNetTypeResolveResult;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * Type reference made for a PSI element; the resolve result is cached.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public abstract class FSharpTypeRefWithCachedResult implements DotNetTypeRef {
    protected final PsiElement myContext;

    private volatile @Nullable DotNetTypeResolveResult myResult;

    protected FSharpTypeRefWithCachedResult(PsiElement context) {
        myContext = context;
    }

    @Override
    public Project getProject() {
        return myContext.getProject();
    }

    @RequiredReadAction
    @Override
    public GlobalSearchScope getResolveScope() {
        return myContext.getResolveScope();
    }

    @RequiredReadAction
    @Override
    public final DotNetTypeResolveResult resolve() {
        DotNetTypeResolveResult result = myResult;
        if (result == null) {
            result = resolveResult();
            myResult = result;
        }
        return result;
    }

    @RequiredReadAction
    protected abstract DotNetTypeResolveResult resolveResult();

    @RequiredReadAction
    @Override
    public String toString() {
        return getVmQName();
    }
}
