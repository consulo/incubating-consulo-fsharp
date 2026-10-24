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

package consulo.fsharp.impl.lang.psi;

import consulo.fsharp.FSharpLanguage;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiReference;
import consulo.test.junit.impl.extension.ConsuloProjectLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@code <caret>} marks the reference, {@code <target>} the name of the expected declaration; without
 * {@code <target>} the reference must not resolve.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtendWith(ConsuloProjectLoader.class)
public class FSharpResolveTest {
    private static final Path TEST_DATA = Path.of("src/test/resources/resolve");
    private static final String CARET = "<caret>";
    private static final String TARGET = "<target>";

    @TestFactory
    public Stream<DynamicTest> resolve(PsiFileFactory factory) throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.list(TEST_DATA)) {
            sources = files.filter(it -> it.getFileName().toString().endsWith(".fs")).sorted().toList();
        }
        return sources.stream().map(source -> DynamicTest.dynamicTest(source.getFileName().toString(), () -> {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            int caret = text.indexOf(CARET);
            int target = text.indexOf(TARGET);
            Assertions.assertTrue(caret >= 0, "no caret");
            if (target >= 0 && target < caret) {
                caret -= TARGET.length();
            }
            else if (target > caret) {
                target -= CARET.length();
            }
            String clean = text.replace(CARET, "").replace(TARGET, "");

            PsiFile file = factory.createFileFromText("test.fs", FSharpLanguage.INSTANCE, clean);
            PsiReference reference = file.findReferenceAt(caret);
            Assertions.assertNotNull(reference, "no reference at caret");
            PsiElement resolved = reference.resolve();
            if (target < 0) {
                Assertions.assertNull(resolved, "must not resolve");
                return;
            }
            Assertions.assertNotNull(resolved, "not resolved");
            Assertions.assertEquals(target, resolved.getTextOffset(), "resolved to " + resolved + ": " + resolved.getText());
        }));
    }
}
