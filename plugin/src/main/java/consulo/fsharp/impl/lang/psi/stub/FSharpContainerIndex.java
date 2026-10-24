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

package consulo.fsharp.impl.lang.psi.stub;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.fsharp.FSharpFileType;
import consulo.fsharp.impl.lang.psi.impl.FSharpPsiUtil;
import consulo.fsharp.impl.lang.psi.impl.FSharpResolver;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qualified names of the namespaces and modules declared in F# files, including the implicit module of a file
 * without a module or namespace declaration.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class FSharpContainerIndex extends ScalarIndexExtension<String> {
    private static final ID<String, Void> NAME = ID.create("fsharp.container");
    private static final int VERSION = 1;

    @Override
    public ID<String, Void> getName() {
        return NAME;
    }

    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Void> result = new HashMap<>();
            for (PsiElement container : FSharpResolver.collectAllContainers(inputData.getPsiFile())) {
                String name = FSharpPsiUtil.getQualifiedName(container);
                if (name != null) {
                    result.put(name, null);
                }
            }
            return result;
        };
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> file.getFileType() == FSharpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    /**
     * F# files of {@code scope} declaring a namespace or module named {@code qualifiedName}.
     */
    @RequiredReadAction
    public static List<PsiFile> findFiles(Project project, String qualifiedName, GlobalSearchScope scope) {
        List<PsiFile> result = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(NAME, qualifiedName, scope)) {
            PsiFile file = psiManager.findFile(virtualFile);
            if (file != null) {
                result.add(file);
            }
        }
        return result;
    }
}
