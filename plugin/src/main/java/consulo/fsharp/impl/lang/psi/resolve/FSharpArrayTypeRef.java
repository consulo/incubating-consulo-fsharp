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
import consulo.dotnet.DotNetTypes;
import consulo.dotnet.psi.resolve.DotNetArrayTypeRef;
import consulo.dotnet.psi.resolve.DotNetPsiSearcher;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.dotnet.psi.resolve.DotNetTypeResolveResult;
import consulo.dotnet.psi.resolve.SimpleTypeResolveResult;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.language.psi.PsiElement;

/**
 * {@code int[]}, {@code int array}
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpArrayTypeRef extends FSharpTypeRefWithCachedResult implements DotNetArrayTypeRef {
    private final DotNetTypeRef myInnerTypeRef;
    private final int myDimensions;

    public FSharpArrayTypeRef(PsiElement context, DotNetTypeRef innerTypeRef, int dimensions) {
        super(context);
        myInnerTypeRef = innerTypeRef;
        myDimensions = dimensions;
    }

    @Override
    public DotNetTypeRef getInnerTypeRef() {
        return myInnerTypeRef;
    }

    public int getDimensions() {
        return myDimensions;
    }

    @RequiredReadAction
    @Override
    protected DotNetTypeResolveResult resolveResult() {
        DotNetTypeDeclaration array = DotNetPsiSearcher.getInstance(getProject()).findType(DotNetTypes.System.Array, getResolveScope());
        return array == null ? DotNetTypeResolveResult.EMPTY : new SimpleTypeResolveResult(array);
    }

    @RequiredReadAction
    @Override
    public String getVmQName() {
        return myInnerTypeRef.getVmQName() + "[" + ",".repeat(myDimensions - 1) + "]";
    }

    @RequiredReadAction
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FSharpArrayTypeRef other && myDimensions == other.myDimensions && myInnerTypeRef.equals(other.myInnerTypeRef);
    }

    @RequiredReadAction
    @Override
    public int hashCode() {
        return myInnerTypeRef.hashCode() * 31 + myDimensions;
    }
}
