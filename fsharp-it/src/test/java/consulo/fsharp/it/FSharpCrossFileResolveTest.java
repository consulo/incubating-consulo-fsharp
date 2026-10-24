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
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolve across the files of a project: {@code <caret>} in one file of {@code resolve/<test>}, {@code <target>} at
 * the expected declaration name, maybe in another file.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtendWith(HeadlessProjectExtension.class)
public class FSharpCrossFileResolveTest {
    private static final String CARET = "<caret>";
    private static final String TARGET = "<target>";

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "Qualified module",
        "Open module",
        "Namespace fragment",
        "Implicit module",
        "Relative to namespace",
        "Open namespace"
    })
    public void resolve(String testName, HeadlessProjects projects) throws Exception {
        URL url = getClass().getResource("/resolve/" + testName);
        assertThat(url).as(testName).isNotNull();
        Path data = Path.of(url.toURI());

        Path directory = projects.newDirectory();
        Path src = Files.createDirectories(directory.resolve("src"));

        String caretFile = null;
        int caretOffset = -1;
        String targetFile = null;
        int targetOffset = -1;
        List<Path> files;
        try (Stream<Path> list = Files.list(data)) {
            files = list.sorted().toList();
        }
        for (Path file : files) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            String name = file.getFileName().toString();
            int caret = text.indexOf(CARET);
            int target = text.indexOf(TARGET);
            if (caret >= 0 && target >= 0) {
                if (target < caret) {
                    caret -= TARGET.length();
                }
                else {
                    target -= CARET.length();
                }
            }
            if (caret >= 0) {
                caretFile = name;
                caretOffset = caret;
            }
            if (target >= 0) {
                targetFile = name;
                targetOffset = target;
            }
            Files.writeString(src.resolve(name), text.replace(CARET, "").replace(TARGET, ""), StandardCharsets.UTF_8);
        }
        assertThat(caretFile).as("caret").isNotNull();

        Project project = projects.open(directory);
        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(root).isNotNull();
        VirtualFile srcRoot = root.findChild("src");
        assertThat(srcRoot).isNotNull();
        Module module = HeadlessModules.createModule(project, "main", root);
        HeadlessModules.addSourceRoot(module, srcRoot);
        HeadlessProjects.awaitIdle(project);

        VirtualFile caretVirtualFile = srcRoot.findChild(caretFile);
        assertThat(caretVirtualFile).isNotNull();
        int offset = caretOffset;
        String expectedFile = targetFile;
        int expectedOffset = targetOffset;
        ReadAction.run(() -> {
            PsiFile file = PsiManager.getInstance(project).findFile(caretVirtualFile);
            assertThat(file).isNotNull();
            PsiReference reference = file.findReferenceAt(offset);
            assertThat(reference).as("reference at caret").isNotNull();
            PsiElement resolved = reference.resolve();
            if (expectedFile == null) {
                assertThat(resolved).isNull();
                return;
            }
            assertThat(resolved).as("resolved").isNotNull();
            assertThat(resolved.getContainingFile().getName()).isEqualTo(expectedFile);
            assertThat(resolved.getTextOffset()).as(resolved.getText()).isEqualTo(expectedOffset);
        });
    }
}
