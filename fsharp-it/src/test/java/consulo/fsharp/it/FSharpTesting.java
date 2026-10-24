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
package consulo.fsharp.it;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.extension.MutableModuleExtension;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A project with one mono .NET module (mono-dotnet and mono-csharp extensions, mscorlib of mono 4.5 from {@code mockSdk})
 * over the files of a test resource directory, indexed and ready to resolve in.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public final class FSharpTesting {
    private FSharpTesting() {
    }

    /**
     * @return files by name
     */
    public static Map<String, PsiFile> openSources(HeadlessProjects projects, String resourceDirectory) throws Exception {
        URL url = FSharpTesting.class.getClassLoader().getResource(resourceDirectory);
        assertThat(url).as(resourceDirectory).isNotNull();
        Path data = Path.of(url.toURI());

        Path directory = projects.newDirectory();
        Path src = Files.createDirectories(directory.resolve("src"));
        List<String> names = new ArrayList<>();
        try (Stream<Path> list = Files.list(data)) {
            for (Path file : list.sorted().toList()) {
                String name = file.getFileName().toString();
                Files.writeString(src.resolve(name), Files.readString(file, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                names.add(name);
            }
        }

        Project project = projects.open(directory);
        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(root).isNotNull();
        VirtualFile srcRoot = root.findChild("src");
        assertThat(srcRoot).isNotNull();

        Module module = HeadlessModules.createModule(project, "main", root);
        HeadlessModules.addSourceRoot(module, srcRoot);
        setupModule(module, List.of(mockSdk().resolve("mscorlib.zip"), mockSdk().resolve("System.zip")));
        HeadlessProjects.awaitIdle(project);

        Map<String, PsiFile> files = new LinkedHashMap<>();
        for (String name : names) {
            VirtualFile file = srcRoot.findChild(name);
            assertThat(file).as(name).isNotNull();
            PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(file));
            assertThat(psiFile).as(name).isNotNull();
            files.put(name, psiFile);
        }
        return files;
    }

    private static Path mockSdk() {
        // surefire runs with the module directory as the working directory
        Path directory = Path.of("").toAbsolutePath().getParent().resolve("mockSdk").resolve("mono4.5");
        assertThat(directory).isDirectory();
        return directory;
    }

    private static void setupModule(Module module, List<Path> libraries) {
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();

            MutableModuleExtension<?> dotnet = rootModel.getExtensionWithoutCheck("mono-dotnet");
            assertThat(dotnet).as("mono-dotnet extension").isNotNull();
            dotnet.setEnabled(true);

            for (Path path : libraries) {
                VirtualFile localFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
                assertThat(localFile).as(path.toString()).isNotNull();
                VirtualFile archiveRoot = ArchiveVfsUtil.getArchiveRootForLocalFile(localFile);
                assertThat(archiveRoot).as("archive root of " + path).isNotNull();

                Library library = rootModel.getModuleLibraryTable().createLibrary(path.getFileName().toString());
                Library.ModifiableModel libraryModel = library.getModifiableModel();
                libraryModel.addRoot(archiveRoot, BinariesOrderRootType.ID);
                libraryModel.commit();
            }

            MutableModuleExtension<?> csharp = rootModel.getExtensionWithoutCheck("mono-csharp");
            assertThat(csharp).as("mono-csharp extension").isNotNull();
            csharp.setEnabled(true);

            rootModel.commit();
        });
    }
}
