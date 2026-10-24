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

package consulo.fsharp.impl.lang.psi.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.DumbService;
import consulo.application.dumb.IndexNotReadyException;
import consulo.fsharp.impl.lang.psi.stub.FSharpContainerIndex;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.file.light.LightVirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static consulo.fsharp.impl.lang.psi.FSharpElementTypes.*;

/**
 * Name resolution without type inference.
 * <p>
 * Declarations are visible by F# rules: walking up from the reference, bindings are visible in their bodies,
 * parameters in function bodies, pattern variables in match clauses, module members only after their declaration
 * (unless the module is {@code rec}) and {@code open} brings in members of modules and namespaces. The first match
 * of the walk wins, which gives F# shadowing.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class FSharpResolver {
    /**
     * @return {@code false} to stop
     */
    public interface Processor {
        boolean process(PsiNamedElement element);
    }

    public enum Kind {
        /**
         * Anything that can be named in an expression.
         */
        VALUE,
        /**
         * Union cases, exceptions and active pattern cases: names in patterns that are not variables.
         */
        CONSTRUCTOR,
        /**
         * Types, modules and namespaces.
         */
        TYPE,
        /**
         * Record fields.
         */
        FIELD;

        boolean accepts(PsiElement element) {
            IElementType type = element.getNode().getElementType();
            return switch (this) {
                case VALUE -> type != RECORD_FIELD_DECLARATION && type != TOP_ACTIVE_PATTERN_CASE_DECLARATION;
                case CONSTRUCTOR -> type == UNION_CASE_DECLARATION || type == EXCEPTION_DECLARATION
                    || type == TOP_ACTIVE_PATTERN_CASE_DECLARATION;
                case TYPE -> CONTAINERS.contains(type) || type == FSHARP_TYPE_DECLARATION || type == EXCEPTION_DECLARATION
                    || type == MODULE_ABBREVIATION_DECLARATION;
                case FIELD -> type == RECORD_FIELD_DECLARATION;
            };
        }
    }

    private static final TokenSet MODULE_LIKE = TokenSet.create(
        ANON_MODULE_DECLARATION,
        NAMED_MODULE_DECLARATION,
        NESTED_MODULE_DECLARATION,
        NAMED_NAMESPACE_DECLARATION,
        GLOBAL_NAMESPACE_DECLARATION
    );

    private static final TokenSet CONTAINERS = TokenSet.create(
        NAMED_MODULE_DECLARATION,
        NESTED_MODULE_DECLARATION,
        NAMED_NAMESPACE_DECLARATION
    );

    private static final TokenSet PATTERNS = TokenSet.create(
        LOCAL_REFERENCE_PAT, TOP_REFERENCE_PAT, PARAMETERS_OWNER_PAT, AS_PAT, TYPED_PAT, ATTRIB_PAT, OR_PAT, ANDS_PAT,
        ARRAY_PAT, LIST_PAT, TUPLE_PAT, PAREN_PAT, RECORD_PAT, NAMED_UNION_CASE_FIELDS_PAT, FIELD_PAT,
        OPTIONAL_VAL_PAT, IS_INST_PAT, LIST_CONS_PAT, WILD_PAT, LITERAL_PAT, UNIT_PAT, NULL_PAT
    );

    private static final TokenSet MEMBERS = TokenSet.create(
        MEMBER_DECLARATION, ABSTRACT_MEMBER_DECLARATION, AUTO_PROPERTY_DECLARATION, VAL_FIELD_DECLARATION,
        MEMBER_SIGNATURE
    );

    private final Set<PsiElement> myVisitedContainers = new HashSet<>();
    private final Kind myKind;

    private FSharpResolver(Kind kind) {
        myKind = kind;
    }

    // Public API

    @RequiredReadAction
    public static @Nullable PsiNamedElement resolveInScope(PsiElement place, String name, Kind kind) {
        PsiNamedElement[] result = new PsiNamedElement[1];
        new FSharpResolver(kind).processScope(place, kind, element -> {
            if (name.equals(element.getName())) {
                result[0] = element;
                return false;
            }
            return true;
        });
        return result[0];
    }

    @RequiredReadAction
    public static List<PsiNamedElement> collectVariants(PsiElement place, Kind kind) {
        List<PsiNamedElement> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        new FSharpResolver(kind).processScope(place, kind, element -> {
            String name = element.getName();
            if (name != null && names.add(name)) {
                result.add(element);
            }
            return true;
        });
        return result;
    }

    /**
     * Member {@code name} of a module, namespace or type.
     */
    @RequiredReadAction
    public static @Nullable PsiNamedElement resolveInContainer(PsiElement container, String name, Kind kind) {
        PsiNamedElement[] result = new PsiNamedElement[1];
        new FSharpResolver(kind).processContents(container, element -> {
            if (kind.accepts(element) && name.equals(element.getName())) {
                result[0] = element;
                return false;
            }
            return true;
        });
        return result[0];
    }

    @RequiredReadAction
    public static List<PsiNamedElement> collectContainerVariants(PsiElement container, Kind kind) {
        List<PsiNamedElement> result = new ArrayList<>();
        new FSharpResolver(kind).processContents(container, element -> {
            if (kind.accepts(element)) {
                result.add(element);
            }
            return true;
        });
        return result;
    }

    /**
     * Namespaces fragments and modules named {@code name}: first relative to the namespaces and modules enclosing
     * {@code place} ({@code B} in {@code namespace A} may be {@code A.B}), then as a full name. Declarations of other
     * files come from {@link FSharpContainerIndex}.
     */
    @RequiredReadAction
    public static List<PsiElement> findContainers(PsiElement place, String name) {
        List<PsiElement> result = new ArrayList<>();
        for (String enclosing : enclosingContainerNames(place)) {
            findContainersByQualifiedName(place, enclosing + "." + name, result);
            if (!result.isEmpty()) {
                return result;
            }
        }
        findContainersByQualifiedName(place, name, result);
        return result;
    }

    /**
     * Qualified names of the namespaces and modules around {@code place}, innermost first.
     */
    @RequiredReadAction
    private static List<String> enclosingContainerNames(PsiElement place) {
        List<String> names = new ArrayList<>();
        for (PsiElement element = place.getParent(); element != null && !(element instanceof PsiFile); element = element.getParent()) {
            IElementType type = element.getNode().getElementType();
            if (CONTAINERS.contains(type)) {
                String name = FSharpPsiUtil.getQualifiedName(element);
                while (name != null) {
                    if (!names.contains(name)) {
                        names.add(name);
                    }
                    int dot = name.lastIndexOf('.');
                    name = dot < 0 ? null : name.substring(0, dot);
                }
            }
        }
        return names;
    }

    @RequiredReadAction
    private static void findContainersByQualifiedName(PsiElement place, String qualifiedName, List<PsiElement> result) {
        PsiFile file = place.getContainingFile();
        if (file == null) {
            return;
        }
        collectContainers(file, qualifiedName, result);
        for (PsiFile other : findFiles(place, qualifiedName)) {
            if (!other.equals(file.getOriginalFile()) && !other.equals(file)) {
                collectContainers(other, qualifiedName, result);
            }
        }
    }

    @RequiredReadAction
    private static List<PsiFile> findFiles(PsiElement place, String qualifiedName) {
        PsiFile file = place.getContainingFile();
        // in-memory files (code fragments, tests) are not part of the project index
        VirtualFile virtualFile = file == null ? null : file.getOriginalFile().getVirtualFile();
        if (virtualFile == null || virtualFile instanceof LightVirtualFile) {
            return List.of();
        }
        Project project = place.getProject();
        if (DumbService.isDumb(project)) {
            return List.of();
        }
        try {
            return FSharpContainerIndex.findFiles(project, qualifiedName, GlobalSearchScope.allScope(project));
        }
        catch (IndexNotReadyException e) {
            return List.of();
        }
    }

    /**
     * Namespaces and modules of a file, including its implicit module.
     */
    @RequiredReadAction
    public static List<PsiElement> collectAllContainers(PsiFile file) {
        List<PsiElement> result = new ArrayList<>();
        collectContainers(file, null, result);
        return result;
    }

    @RequiredReadAction
    private static void collectContainers(PsiElement element, @Nullable String qualifiedName, List<PsiElement> result) {
        for (PsiElement child : FSharpPsiUtil.children(element)) {
            IElementType type = child.getNode().getElementType();
            if (CONTAINERS.contains(type) || type == ANON_MODULE_DECLARATION) {
                if (qualifiedName == null || qualifiedName.equals(FSharpPsiUtil.getQualifiedName(child))) {
                    result.add(child);
                }
                collectContainers(child, qualifiedName, result);
            }
        }
    }

    // Scope walk

    @RequiredReadAction
    private void processScope(PsiElement place, Kind kind, Processor processor) {
        Processor filtered = element -> !kind.accepts(element) || processor.process(element);
        PsiElement child = place;
        PsiElement scope = place.getParent();
        while (scope != null && !(scope instanceof PsiFile)) {
            if (!processScopeNode(scope, child, filtered)) {
                return;
            }
            child = scope;
            scope = scope.getParent();
        }
    }

    @RequiredReadAction
    private boolean processScopeNode(PsiElement scope, PsiElement child, Processor processor) {
        IElementType type = scope.getNode().getElementType();

        if (type == LET_OR_USE_EXPR) {
            List<PsiElement> bindings = childrenOfType(scope, LOCAL_BINDING);
            boolean inBody = !bindings.contains(child);
            if (inBody || isRec(bindings)) {
                for (int i = bindings.size() - 1; i >= 0; i--) {
                    if (!processBindingHead(bindings.get(i), processor)) {
                        return false;
                    }
                }
            }
            return true;
        }

        if (type == LET_BINDINGS_DECLARATION) {
            List<PsiElement> bindings = childrenOfType(scope, TOP_BINDING);
            if (isRec(bindings)) {
                for (PsiElement binding : bindings) {
                    if (!processBindingHead(binding, processor)) {
                        return false;
                    }
                }
            }
            return true;
        }

        if (type == LOCAL_BINDING || type == TOP_BINDING || type == MEMBER_DECLARATION || type == ACCESSOR_DECLARATION
            || type == SECONDARY_CONSTRUCTOR_DECLARATION) {
            if (isAfterEquals(scope, child)) {
                for (PsiElement parameter : childrenOfType(scope, PARAMETERS_PATTERN_DECLARATION)) {
                    if (!processPattern(parameter, processor)) {
                        return false;
                    }
                }
                if (!processSelfId(scope, processor)) {
                    return false;
                }
            }
            if (type == MEMBER_DECLARATION && FSharpPsiUtil.is(child, ACCESSOR_DECLARATION)) {
                return processSelfId(scope, processor);
            }
            return true;
        }

        if (type == LAMBDA_EXPR) {
            if (!FSharpPsiUtil.is(child, LAMBDA_PARAMETERS_LIST)) {
                PsiElement parameters = FSharpPsiUtil.findChild(scope, LAMBDA_PARAMETERS_LIST);
                return parameters == null || processPattern(parameters, processor);
            }
            return true;
        }

        if (type == MATCH_CLAUSE) {
            PsiElement pattern = firstPattern(scope);
            if (pattern != null && pattern != child) {
                return processPattern(pattern, processor);
            }
            return true;
        }

        if (type == FOR_EACH_EXPR) {
            PsiElement pattern = firstPattern(scope);
            if (pattern != null && child == lastComposite(scope)) {
                return processPattern(pattern, processor);
            }
            return true;
        }

        if (type == FOR_EXPR) {
            PsiElement local = FSharpPsiUtil.findChild(scope, LOCAL_DECLARATION);
            if (local instanceof PsiNamedElement named && child == lastComposite(scope)) {
                return processor.process(named);
            }
            return true;
        }

        if (type == FSHARP_TYPE_DECLARATION || type == TYPE_EXTENSION_DECLARATION || type == CLASS_REPRESENTATION
            || type == STRUCT_REPRESENTATION) {
            return processTypeBody(scope, child, processor);
        }

        if (MODULE_LIKE.contains(type)) {
            return processModuleScope(scope, child, processor);
        }

        return true;
    }

    /**
     * Members of a class see primary constructor parameters, its self identifier and class let bindings.
     */
    @RequiredReadAction
    private boolean processTypeBody(PsiElement typeDeclaration, PsiElement child, Processor processor) {
        if (FSharpPsiUtil.is(child, PRIMARY_CONSTRUCTOR_DECLARATION)) {
            return true;
        }
        PsiElement declaration = FSharpPsiUtil.is(typeDeclaration, FSHARP_TYPE_DECLARATION) ? typeDeclaration : typeDeclaration.getParent();
        if (declaration == null) {
            return true;
        }
        for (PsiElement letBindings : childrenOfType(typeDeclaration, LET_BINDINGS_DECLARATION)) {
            for (PsiElement binding : childrenOfType(letBindings, TOP_BINDING)) {
                if (!processBindingHead(binding, processor)) {
                    return false;
                }
            }
        }
        if (typeDeclaration != declaration) {
            return true;
        }
        PsiElement constructor = FSharpPsiUtil.findChild(declaration, PRIMARY_CONSTRUCTOR_DECLARATION);
        if (constructor != null) {
            for (PsiElement parameters : childrenOfType(constructor, PARAMETERS_PATTERN_DECLARATION)) {
                if (!processPattern(parameters, processor)) {
                    return false;
                }
            }
            PsiElement self = FSharpPsiUtil.findChild(constructor, CTOR_SELF_ID);
            if (self instanceof PsiNamedElement named && !processor.process(named)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Members declared before {@code child} (all members in {@code rec} modules), latest first.
     */
    @RequiredReadAction
    private boolean processModuleScope(PsiElement module, PsiElement child, Processor processor) {
        boolean rec = FSharpPsiUtil.findChild(module, FSharpTokenType.REC) != null;
        List<PsiElement> members = FSharpPsiUtil.children(module);
        int index = members.indexOf(child);
        int last = rec || index < 0 ? members.size() - 1 : index;
        for (int i = last; i >= 0; i--) {
            PsiElement member = members.get(i);
            if (member == child && !FSharpPsiUtil.is(member, TYPE_DECLARATION_GROUP) && !rec) {
                continue;
            }
            if (FSharpPsiUtil.is(member, OPEN_STATEMENT)) {
                if (!processOpen(member, processor)) {
                    return false;
                }
                continue;
            }
            if (!processModuleMember(member, processor)) {
                return false;
            }
        }

        // the same and enclosing namespaces in other files
        if (FSharpPsiUtil.is(module, NAMED_NAMESPACE_DECLARATION) || FSharpPsiUtil.is(module, NAMED_MODULE_DECLARATION)) {
            String name = FSharpPsiUtil.getQualifiedName(module);
            if (name != null && FSharpPsiUtil.is(module, NAMED_MODULE_DECLARATION)) {
                int dot = name.lastIndexOf('.');
                name = dot < 0 ? null : name.substring(0, dot);
            }
            PsiFile file = module.getContainingFile();
            while (name != null) {
                for (PsiFile other : findFiles(module, name)) {
                    if (file == null || other.equals(file) || other.equals(file.getOriginalFile())) {
                        continue;
                    }
                    List<PsiElement> fragments = new ArrayList<>();
                    collectContainers(other, name, fragments);
                    for (PsiElement fragment : fragments) {
                        if (FSharpPsiUtil.is(fragment, NAMED_NAMESPACE_DECLARATION) && !processContents(fragment, processor)) {
                            return false;
                        }
                    }
                }
                int dot = name.lastIndexOf('.');
                name = dot < 0 ? null : name.substring(0, dot);
            }
        }

        // earlier fragments of the same namespace and enclosing namespaces of this file
        if (FSharpPsiUtil.is(module, NAMED_NAMESPACE_DECLARATION)) {
            String name = FSharpPsiUtil.getQualifiedName(module);
            PsiElement sibling = module.getPrevSibling();
            while (sibling != null && name != null) {
                if (FSharpPsiUtil.is(sibling, NAMED_NAMESPACE_DECLARATION)) {
                    String siblingName = FSharpPsiUtil.getQualifiedName(sibling);
                    if (siblingName != null && (name.equals(siblingName) || name.startsWith(siblingName + "."))) {
                        if (!processContents(sibling, processor)) {
                            return false;
                        }
                    }
                }
                sibling = sibling.getPrevSibling();
            }
        }
        return true;
    }

    @RequiredReadAction
    private boolean processOpen(PsiElement open, Processor processor) {
        PsiElement name = FSharpPsiUtil.findChild(open, TYPE_REFERENCE_NAME);
        if (!(name instanceof FSharpReferenceNameImpl referenceName)) {
            return true;
        }
        for (PsiElement container : referenceName.resolveContainers()) {
            if (!processContents(container, processor)) {
                return false;
            }
        }
        return true;
    }

    /**
     * All members of a module, namespace or type, in no particular order.
     */
    @RequiredReadAction
    boolean processContents(PsiElement container, Processor processor) {
        if (!myVisitedContainers.add(container)) {
            return true;
        }
        ASTNode node = container.getNode();
        // .NET namespaces and types (open System, open Cs.Core) have no F# tree - resolved by FSharpReferenceNameImpl#resolveDotNet
        if (node == null || container.getLanguage() != FSharpLanguage.INSTANCE) {
            return true;
        }
        IElementType type = node.getElementType();
        if (type == FSHARP_TYPE_DECLARATION || type == TYPE_EXTENSION_DECLARATION) {
            return processTypeContents(container, processor);
        }
        for (PsiElement member : FSharpPsiUtil.children(container)) {
            if (FSharpPsiUtil.is(member, OPEN_STATEMENT)) {
                continue;
            }
            if (!processModuleMember(member, processor)) {
                return false;
            }
        }
        return true;
    }

    @RequiredReadAction
    private boolean processTypeContents(PsiElement type, Processor processor) {
        List<PsiElement> scopes = new ArrayList<>();
        scopes.add(type);
        for (PsiElement child : FSharpPsiUtil.children(type)) {
            IElementType childType = child.getNode().getElementType();
            if (childType == CLASS_REPRESENTATION || childType == STRUCT_REPRESENTATION || childType == INTERFACE_REPRESENTATION) {
                scopes.add(child);
            }
            if (childType == UNION_REPRESENTATION || childType == ENUM_REPRESENTATION) {
                for (PsiElement unionCase : FSharpPsiUtil.children(child)) {
                    if (unionCase instanceof PsiNamedElement named && !processor.process(named)) {
                        return false;
                    }
                }
            }
            if (childType == RECORD_REPRESENTATION) {
                PsiElement list = FSharpPsiUtil.findChild(child, RECORD_MEMBER_DECLARATION_LIST);
                for (PsiElement field : list == null ? List.<PsiElement>of() : childrenOfType(list, RECORD_FIELD_DECLARATION)) {
                    if (field instanceof PsiNamedElement named && !processor.process(named)) {
                        return false;
                    }
                }
            }
        }
        for (PsiElement scope : scopes) {
            for (PsiElement member : FSharpPsiUtil.children(scope)) {
                if (MEMBERS.contains(member.getNode().getElementType()) && member instanceof PsiNamedElement named
                    && !processor.process(named)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Declarations of a module member: values of let bindings, types with their union cases and record fields,
     * nested modules and exceptions.
     */
    @RequiredReadAction
    private boolean processModuleMember(PsiElement member, Processor processor) {
        IElementType type = member.getNode().getElementType();
        if (type == LET_BINDINGS_DECLARATION) {
            List<PsiElement> bindings = childrenOfType(member, TOP_BINDING);
            for (int i = bindings.size() - 1; i >= 0; i--) {
                if (!processBindingHead(bindings.get(i), processor)) {
                    return false;
                }
            }
            return true;
        }
        if (type == TYPE_DECLARATION_GROUP) {
            for (PsiElement declaration : FSharpPsiUtil.children(member)) {
                IElementType declarationType = declaration.getNode().getElementType();
                if (declarationType != FSHARP_TYPE_DECLARATION) {
                    continue;
                }
                if (declaration instanceof PsiNamedElement named && !processor.process(named)) {
                    return false;
                }
                if (isRequireQualifiedAccess(declaration)) {
                    continue;
                }
                PsiElement union = FSharpPsiUtil.findChild(declaration, UNION_REPRESENTATION);
                if (union != null) {
                    for (PsiElement unionCase : childrenOfType(union, UNION_CASE_DECLARATION)) {
                        if (unionCase instanceof PsiNamedElement named && !processor.process(named)) {
                            return false;
                        }
                    }
                }
                PsiElement record = FSharpPsiUtil.findChild(declaration, RECORD_REPRESENTATION);
                PsiElement list = record == null ? null : FSharpPsiUtil.findChild(record, RECORD_MEMBER_DECLARATION_LIST);
                if (list != null) {
                    for (PsiElement field : childrenOfType(list, RECORD_FIELD_DECLARATION)) {
                        if (field instanceof PsiNamedElement named && !processor.process(named)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        if (type == NESTED_MODULE_DECLARATION || type == EXCEPTION_DECLARATION || type == MODULE_ABBREVIATION_DECLARATION) {
            if (member instanceof PsiNamedElement named && !processor.process(named)) {
                return false;
            }
            if (type == NESTED_MODULE_DECLARATION && isAutoOpen(member)) {
                return processContents(member, processor);
            }
            return true;
        }
        if (type == NAMED_MODULE_DECLARATION || type == NAMED_NAMESPACE_DECLARATION) {
            return !(member instanceof PsiNamedElement named) || processor.process(named);
        }
        return true;
    }

    private static boolean isRequireQualifiedAccess(PsiElement declaration) {
        return hasAttribute(declaration, "RequireQualifiedAccess");
    }

    private static boolean isAutoOpen(PsiElement declaration) {
        return hasAttribute(declaration, "AutoOpen");
    }

    private static boolean hasAttribute(PsiElement declaration, String shortName) {
        for (PsiElement list : childrenOfType(declaration, ATTRIBUTE_LIST)) {
            for (PsiElement attribute : childrenOfType(list, ATTRIBUTE)) {
                PsiElement name = FSharpPsiUtil.findChild(attribute, TYPE_REFERENCE_NAME);
                if (name == null) {
                    continue;
                }
                String text = name.getText();
                String simple = text.substring(text.lastIndexOf('.') + 1);
                if (simple.equals(shortName) || simple.equals(shortName + "Attribute")) {
                    return true;
                }
            }
        }
        return false;
    }

    // Patterns

    /**
     * Value declared by a binding: the head pattern, without the parameters of a function.
     */
    @RequiredReadAction
    private boolean processBindingHead(PsiElement binding, Processor processor) {
        PsiElement head = firstPattern(binding);
        if (head == null) {
            return true;
        }
        // active pattern cases are declared by the function name
        PsiElement name = FSharpPsiUtil.findChild(head, EXPRESSION_REFERENCE_NAME);
        PsiElement activePattern = name == null ? null : FSharpPsiUtil.findChild(name, ACTIVE_PATTERN_ID);
        if (activePattern != null) {
            for (PsiElement activeCase : childrenOfType(activePattern, TOP_ACTIVE_PATTERN_CASE_DECLARATION)) {
                if (activeCase instanceof PsiNamedElement named && !processor.process(named)) {
                    return false;
                }
            }
        }
        return processPattern(head, processor);
    }

    @RequiredReadAction
    private boolean processPattern(PsiElement pattern, Processor processor) {
        if (myKind != Kind.VALUE) {
            // only values are declared by patterns
            return true;
        }
        if (pattern instanceof FSharpReferencePatImpl reference) {
            if (reference.getNameIdentifier() == null || reference.isConstructorReference()) {
                return true;
            }
            return processor.process(reference);
        }
        IElementType type = pattern.getNode().getElementType();
        for (PsiElement child : FSharpPsiUtil.children(pattern)) {
            IElementType childType = child.getNode().getElementType();
            // the name of a union case pattern is a reference
            if (type == PARAMETERS_OWNER_PAT && childType == EXPRESSION_REFERENCE_NAME) {
                continue;
            }
            if (type == FIELD_PAT && childType == EXPRESSION_REFERENCE_NAME) {
                continue;
            }
            if (PATTERNS.contains(childType) || childType == PARAMETERS_PATTERN_DECLARATION || childType == LAMBDA_PARAMETERS_LIST) {
                if (!processPattern(child, processor)) {
                    return false;
                }
            }
        }
        return true;
    }

    @RequiredReadAction
    private static boolean processSelfId(PsiElement member, Processor processor) {
        PsiElement self = FSharpPsiUtil.findChild(member, NAMED_SELF_ID);
        return !(self instanceof PsiNamedElement named) || processor.process(named);
    }

    // Tree helpers

    private static boolean isRec(List<PsiElement> bindings) {
        return !bindings.isEmpty() && FSharpPsiUtil.findChild(bindings.get(0), FSharpTokenType.REC) != null;
    }

    private static boolean isAfterEquals(PsiElement owner, PsiElement child) {
        boolean afterEquals = false;
        for (PsiElement element : FSharpPsiUtil.children(owner)) {
            if (element == child) {
                return afterEquals;
            }
            if (FSharpPsiUtil.is(element, FSharpTokenType.EQUALS)) {
                afterEquals = true;
            }
        }
        return false;
    }

    private static @Nullable PsiElement firstPattern(PsiElement element) {
        for (PsiElement child : FSharpPsiUtil.children(element)) {
            if (PATTERNS.contains(child.getNode().getElementType())) {
                return child;
            }
        }
        return null;
    }

    private static @Nullable PsiElement lastComposite(PsiElement element) {
        PsiElement last = null;
        for (PsiElement child : FSharpPsiUtil.children(element)) {
            if (child.getNode().getFirstChildNode() != null) {
                last = child;
            }
        }
        return last;
    }

    private static List<PsiElement> childrenOfType(PsiElement element, IElementType type) {
        List<PsiElement> result = new ArrayList<>();
        for (PsiElement child : FSharpPsiUtil.children(element)) {
            if (Objects.equals(child.getNode().getElementType(), type)) {
                result.add(child);
            }
        }
        return result;
    }
}
