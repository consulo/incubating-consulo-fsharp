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
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.dotnet.psi.resolve.DotNetPsiSearcher;
import consulo.dotnet.psi.resolve.DotNetTypeResolveResult;
import consulo.dotnet.psi.resolve.SimpleTypeResolveResult;
import consulo.language.psi.PsiElement;
import consulo.project.DumbService;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Type by VM qualified name. A name as written has several candidates (one per open and enclosing namespace or module):
 * the first one found wins.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpTypeRefByQName extends FSharpTypeRefWithCachedResult {
    private final String myName;
    private final List<String> myCandidates;

    public FSharpTypeRefByQName(PsiElement context, String vmQName) {
        this(context, vmQName, List.of(vmQName));
    }

    /**
     * @param name the name as written, the VM name while the type is not found
     */
    public FSharpTypeRefByQName(PsiElement context, String name, List<String> candidates) {
        super(context);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("no candidates");
        }
        myName = name;
        myCandidates = List.copyOf(candidates);
    }

    @RequiredReadAction
    @Override
    protected DotNetTypeResolveResult resolveResult() {
        DotNetTypeDeclaration type = findType();
        return type == null ? DotNetTypeResolveResult.EMPTY : new SimpleTypeResolveResult(type);
    }

    @RequiredReadAction
    private @Nullable DotNetTypeDeclaration findType() {
        if (DumbService.isDumb(getProject())) {
            return null;
        }
        DotNetPsiSearcher searcher = DotNetPsiSearcher.getInstance(getProject());
        for (String candidate : myCandidates) {
            DotNetTypeDeclaration type = searcher.findType(candidate, getResolveScope());
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    public List<String> getCandidates() {
        return myCandidates;
    }

    /**
     * The name of the resolved type, or the name as written when the type is not found.
     */
    @RequiredReadAction
    @Override
    public String getVmQName() {
        if (myCandidates.size() > 1 && resolve().getElement() instanceof DotNetTypeDeclaration type) {
            String vmQName = type.getVmQName();
            if (vmQName != null) {
                return vmQName;
            }
        }
        return myCandidates.size() == 1 ? myCandidates.get(0) : myName;
    }

    @RequiredReadAction
    @Override
    public boolean isEqualToVmQName(String vmQName) {
        return vmQName.equals(getVmQName());
    }

    @RequiredReadAction
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FSharpTypeRefByQName other && getVmQName().equals(other.getVmQName());
    }

    @RequiredReadAction
    @Override
    public int hashCode() {
        return getVmQName().hashCode();
    }
}
