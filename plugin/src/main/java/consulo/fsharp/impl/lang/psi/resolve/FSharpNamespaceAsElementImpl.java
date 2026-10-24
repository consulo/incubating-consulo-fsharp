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

import consulo.dotnet.psi.impl.IndexBasedDotNetNamespaceAsElement;
import consulo.dotnet.psi.impl.resolve.impl.IndexBasedDotNetPsiSearcherExtension;
import consulo.fsharp.FSharpLanguage;
import consulo.project.Project;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpNamespaceAsElementImpl extends IndexBasedDotNetNamespaceAsElement {
    public FSharpNamespaceAsElementImpl(Project project, String indexKey, String qName, IndexBasedDotNetPsiSearcherExtension searcher) {
        super(project, FSharpLanguage.INSTANCE, indexKey, qName, searcher);
    }
}
