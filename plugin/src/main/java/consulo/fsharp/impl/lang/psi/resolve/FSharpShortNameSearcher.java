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

import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.SearchScope;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.dotnet.psi.resolve.DotNetShortNameSearcher;
import consulo.fsharp.impl.lang.psi.stub.index.FSharpIndexKeys;
import consulo.language.psi.stub.IdFilter;
import consulo.language.psi.stub.StubIndex;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Type names for "go to class" and completion of all .NET languages.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class FSharpShortNameSearcher extends DotNetShortNameSearcher {
    @Inject
    public FSharpShortNameSearcher(Project project) {
        super(project);
    }

    @Override
    public void collectTypeNames(Predicate<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        StubIndex.getInstance().processAllKeys(FSharpIndexKeys.TYPE_INDEX, processor, (ProjectAwareSearchScope) scope, filter);
    }

    @Override
    public void collectTypes(String key, SearchScope scope, @Nullable IdFilter filter, Predicate<DotNetTypeDeclaration> processor) {
        StubIndex.getInstance().processElements(FSharpIndexKeys.TYPE_INDEX, key, myProject, (ProjectAwareSearchScope) scope, filter,
            DotNetTypeDeclaration.class, processor);
    }
}
