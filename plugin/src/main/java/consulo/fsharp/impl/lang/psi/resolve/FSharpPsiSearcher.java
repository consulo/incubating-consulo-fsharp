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
import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.SearchScope;
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.dotnet.psi.DotNetQualifiedElement;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.dotnet.psi.impl.resolve.impl.IndexBasedDotNetPsiSearcherExtension;
import consulo.dotnet.psi.resolve.DotNetNamespaceAsElement;
import consulo.fsharp.impl.lang.psi.stub.index.FSharpIndexKeys;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.project.DumbService;
import consulo.project.Project;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * F# namespaces, modules and types for all .NET languages, by VM names.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class FSharpPsiSearcher extends IndexBasedDotNetPsiSearcherExtension {
    @Inject
    public FSharpPsiSearcher(Project project) {
        super(project);
    }

    @Override
    protected DotNetNamespaceAsElement createNamespace(String indexKey, String qName) {
        return new FSharpNamespaceAsElementImpl(myProject, indexKey, qName, this);
    }

    @Override
    public StubIndexKey<String, DotNetQualifiedElement> getElementByQNameIndexKey() {
        return FSharpIndexKeys.MEMBER_BY_NAMESPACE_QNAME_INDEX;
    }

    @Override
    public StubIndexKey<String, DotNetQualifiedElement> getNamespaceIndexKey() {
        return FSharpIndexKeys.MEMBER_BY_ALL_NAMESPACE_QNAME_INDEX;
    }

    @RequiredReadAction
    @Override
    public Collection<? extends DotNetTypeDeclaration> findTypesImpl(String vmQName, SearchScope scope) {
        if (DumbService.isDumb(myProject) || !(scope instanceof GlobalSearchScope globalScope)) {
            return List.of();
        }
        List<DotNetTypeDeclaration> result = new ArrayList<>();
        for (DotNetTypeDeclaration type : StubIndex.getElements(FSharpIndexKeys.TYPE_BY_VMQNAME_INDEX, vmQName.hashCode(), myProject,
            globalScope, DotNetTypeDeclaration.class)) {
            // hash collisions
            if (vmQName.equals(type.getVmQName())) {
                result.add(type);
            }
        }
        if (result.isEmpty()) {
            // generated nested types: union cases, Tags
            int nested = vmQName.lastIndexOf('/');
            if (nested > 0) {
                String name = vmQName.substring(nested + 1);
                for (DotNetTypeDeclaration outer : findTypesImpl(vmQName.substring(0, nested), scope)) {
                    for (DotNetNamedElement member : outer.getMembers()) {
                        if (member instanceof DotNetTypeDeclaration type && name.equals(type.getVmName())) {
                            result.add(type);
                        }
                    }
                }
            }
        }
        return result;
    }
}
