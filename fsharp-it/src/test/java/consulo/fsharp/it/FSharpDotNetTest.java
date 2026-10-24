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
import consulo.dotnet.psi.DotNetLikeMethodDeclaration;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.dotnet.psi.resolve.DotNetNamespaceAsElement;
import consulo.dotnet.psi.resolve.DotNetPsiSearcher;
import consulo.dotnet.psi.resolve.DotNetShortNameSearcher;
import consulo.fsharp.FSharpLanguage;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F# declarations as other .NET languages see them: types by VM names through {@link DotNetPsiSearcher}, the compiled
 * view of members, and C# code resolving to F#.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtendWith(HeadlessProjectExtension.class)
public class FSharpDotNetTest {
    @Test
    public void searcher(HeadlessProjects projects) throws Exception {
        Map<String, PsiFile> files = FSharpTesting.openSources(projects, "dotnet/Shapes");
        Project project = files.get("Shapes.fs").getProject();

        ReadAction.run(() -> {
            GlobalSearchScope scope = GlobalSearchScope.allScope(project);
            DotNetPsiSearcher searcher = DotNetPsiSearcher.getInstance(project);

            DotNetTypeDeclaration shape = searcher.findType("Fs.Geometry.Shape", scope);
            assertThat(shape).isNotNull();
            assertThat(shape.getLanguage()).isEqualTo(FSharpLanguage.INSTANCE);
            assertThat(names(shape)).contains("Tag", "Tags", "IsCircle", "NewCircle", "Circle", "NewRectangle", "Rectangle", "Empty",
                "IsEmpty");
            assertThat(names(shape)).doesNotContain("NewEmpty");

            DotNetTypeDeclaration circle = searcher.findType("Fs.Geometry.Shape/Circle", scope);
            assertThat(circle).isNotNull();
            assertThat(names(circle)).containsExactly("Radius");
            assertThat(names(searcher.findType("Fs.Geometry.Shape/Rectangle", scope))).containsExactly("Width", "Height");
            assertThat(names(searcher.findType("Fs.Geometry.Shape/Tags", scope))).containsExactly("Circle", "Rectangle", "Empty");

            DotNetTypeDeclaration measurement = searcher.findType("Fs.Geometry.Measurement", scope);
            assertThat(measurement).isNotNull();
            assertThat(names(measurement)).contains("Name", "Area", "Perimeter");

            DotNetTypeDeclaration square = searcher.findType("Fs.Geometry.Square", scope);
            assertThat(square).isNotNull();
            assertThat(names(square)).contains("Side", "Area", "Unit");

            DotNetTypeDeclaration area = searcher.findType("Fs.Geometry.IArea", scope);
            assertThat(area).isNotNull();
            assertThat(area.isInterface()).isTrue();

            DotNetTypeDeclaration shapeOps = searcher.findType("Fs.Geometry.ShapeOps", scope);
            assertThat(shapeOps).isNotNull();
            assertThat(shapeOps.hasModifier(DotNetModifier.STATIC)).isTrue();
            assertThat(names(shapeOps)).containsExactlyInAnyOrder("name", "area", "scale", "defaultName", "Sides");
            DotNetLikeMethodDeclaration areaMethod = method(shapeOps, "area");
            assertThat(areaMethod.getParameters()).hasSize(1);
            assertThat(areaMethod.getParameterTypeRefs()[0].getVmQName()).isEqualTo("Fs.Geometry.Shape");
            assertThat(areaMethod.getReturnTypeRef().getVmQName()).isEqualTo("System.Double");
            assertThat(method(shapeOps, "scale").getParameters()).hasSize(2);

            // a module next to a type of the same name gets the Module suffix
            assertThat(searcher.findType("Fs.Geometry.ShapeModule", scope)).isNotNull();

            DotNetNamespaceAsElement namespace = searcher.findNamespace("Fs.Geometry", scope);
            assertThat(namespace).isNotNull();
            Collection<PsiElement> children = namespace.getChildren(scope, DotNetNamespaceAsElement.ChildrenFilter.ONLY_ELEMENTS);
            assertThat(children).extracting(it -> ((DotNetTypeDeclaration) it).getVmQName())
                .contains("Fs.Geometry.Shape", "Fs.Geometry.Measurement", "Fs.Geometry.Square", "Fs.Geometry.ShapeOps");
            assertThat(searcher.findNamespace("Fs", scope)).isNotNull();

            List<String> typeNames = List.copyOf(DotNetShortNameSearcher.getInstance(project).getTypeNames(scope, null));
            assertThat(typeNames).contains("Shape", "Measurement", "Square", "ShapeOps");
        });
    }

    /**
     * References of a C# file to F# types and members.
     */
    @Test
    public void csharpToFSharp(HeadlessProjects projects) throws Exception {
        Map<String, PsiFile> files = FSharpTesting.openSources(projects, "dotnet/Shapes");
        PsiFile csharp = files.get("Use.cs");

        ReadAction.run(() -> {
            String text = csharp.getText();
            List<String> unresolved = new ArrayList<>();
            for (String usage : List.of("Shape shape", "NewCircle", "Shape.Circle c", "Radius", "Tags", "Tags.Circle", "ShapeModule",
                "isRound", "Square square", "Side", "Measurement m", "Name;", "defaultName", "ShapeOps.area", "area(")) {
                String identifier = usage.contains(".") ? usage.substring(usage.lastIndexOf('.') + 1) : usage;
                identifier = identifier.replaceAll("[^A-Za-z].*", "");
                int offset = text.indexOf(usage);
                assertThat(offset).as(usage).isGreaterThanOrEqualTo(0);
                offset += usage.indexOf(identifier);
                PsiElement target = resolve(csharp.findReferenceAt(offset));
                PsiElement navigation = target == null ? null : target.getNavigationElement();
                if (navigation == null || navigation.getContainingFile() == null
                    || !navigation.getContainingFile().getName().equals("Shapes.fs")) {
                    PsiReference reference = csharp.findReferenceAt(offset);
                    unresolved.add(usage + " -> " + target + " ref=" + (reference == null ? null : reference.getClass().getSimpleName() + ":"
                        + reference.getCanonicalText()));
                }
            }
            assertThat(unresolved).isEmpty();
        });
    }

    private static PsiElement resolve(PsiReference reference) {
        if (reference == null) {
            return null;
        }
        if (reference instanceof PsiPolyVariantReference poly) {
            for (ResolveResult result : poly.multiResolve(false)) {
                if (result.isValidResult() && result.getElement() != null) {
                    return result.getElement();
                }
            }
        }
        return reference.resolve();
    }

    private static List<String> names(DotNetTypeDeclaration type) {
        assertThat(type).isNotNull();
        return Arrays.stream(type.getMembers()).map(DotNetNamedElement::getName).toList();
    }

    private static DotNetLikeMethodDeclaration method(DotNetTypeDeclaration type, String name) {
        for (DotNetNamedElement member : type.getMembers()) {
            if (member instanceof DotNetLikeMethodDeclaration method && name.equals(method.getName())) {
                return method;
            }
        }
        throw new AssertionError("no method " + name);
    }
}
