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
import consulo.document.util.TextRange;
import consulo.fsharp.impl.editor.highlight.FSharpHighlightVisitor;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every range {@link FSharpHighlightVisitor} colors in {@code highlight/<name>/<name>.fs}, as {@code line: text KEY},
 * compared with {@code highlight/<name>.txt}. Other files of the directory (C# code) are referenced from F#.
 * <p>
 * A missing or different gold file fails the test, the actual output is written to {@code target/highlight}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtendWith(HeadlessProjectExtension.class)
public class FSharpHighlightTest {
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"Highlight"})
    public void highlight(String testName, HeadlessProjects projects) throws Exception {
        Map<String, PsiFile> files = FSharpTesting.openSources(projects, "highlight/" + testName);
        PsiFile file = files.get(testName + ".fs");
        assertThat(file).isNotNull();

        String actual = ReadAction.compute(() -> {
            String text = file.getText();
            StringBuilder builder = new StringBuilder();
            PsiTreeUtil.processElements(file, element -> {
                FSharpHighlightVisitor.highlight(element, (identifier, key) -> {
                    TextRange range = identifier.getTextRange();
                    int line = text.substring(0, range.getStartOffset()).split("\n", -1).length;
                    builder.append(line).append(": ").append(identifier.getText()).append(' ').append(key.getExternalName()).append('\n');
                });
                return true;
            });
            return builder.toString().strip();
        });

        String goldName = "highlight/" + testName + ".txt";
        URL gold = FSharpHighlightTest.class.getClassLoader().getResource(goldName);
        String expected = gold == null ? null : Files.readString(Path.of(gold.toURI()), StandardCharsets.UTF_8).strip();
        if (!actual.equals(expected)) {
            Path out = Path.of("target", goldName);
            Files.createDirectories(out.getParent());
            Files.writeString(out, actual + "\n", StandardCharsets.UTF_8);
        }
        assertThat(actual).as(goldName).isEqualTo(expected);
    }
}
