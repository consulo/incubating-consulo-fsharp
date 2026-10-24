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
package consulo.fsharp.impl.lang.psi.light;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.dotnet.DotNetTypes;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.fsharp.impl.lang.psi.impl.FSharpPsiUtil;
import consulo.fsharp.impl.lang.psi.impl.FSharpTypeDeclarationImpl;
import consulo.fsharp.impl.lang.psi.light.FSharpLightMethodBase.ParameterInfo;
import consulo.fsharp.impl.lang.psi.resolve.FSharpTypeRefByQName;
import consulo.fsharp.impl.lang.psi.resolve.FSharpTypeRefs;
import consulo.fsharp.impl.lang.psi.stub.FSharpTypeKind;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.util.LanguageCachedValueUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compiled view of F# declarations, as other .NET languages see them.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public final class FSharpCompiledMembers {
    private FSharpCompiledMembers() {
    }

    @RequiredReadAction
    public static List<DotNetNamedElement> getMembers(FSharpTypeDeclarationImpl type) {
        return LanguageCachedValueUtil.getCachedValue(type,
            () -> CachedValueProvider.Result.create(build(type), PsiModificationTracker.MODIFICATION_COUNT));
    }

    @RequiredReadAction
    private static List<DotNetNamedElement> build(FSharpTypeDeclarationImpl type) {
        List<DotNetNamedElement> members = new ArrayList<>();
        FSharpTypeKind kind = type.getKind();
        if (!kind.isDotNetType()) {
            return members;
        }
        PsiElement body = type;
        if (kind == FSharpTypeKind.MODULE) {
            for (PsiElement child = type.getFirstChild(); child != null; child = child.getNextSibling()) {
                IElementType childType = child.getNode().getElementType();
                if (childType == FSharpElementTypes.LET_BINDINGS_DECLARATION) {
                    for (PsiElement binding : FSharpPsiUtil.children(child)) {
                        if (FSharpPsiUtil.is(binding, FSharpElementTypes.TOP_BINDING)) {
                            addBinding(type, binding, members);
                        }
                    }
                }
                else if (childType == FSharpElementTypes.BINDING_SIGNATURE) {
                    addBindingSignature(type, child, members);
                }
            }
            return members;
        }

        switch (kind) {
            case RECORD -> addRecord(type, members);
            case UNION -> addUnion(type, members);
            case ENUM -> addEnum(type, members);
            case EXCEPTION -> addException(type, members);
            default -> {
            }
        }

        for (PsiElement child = body.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType childType = child.getNode().getElementType();
            if (childType == FSharpElementTypes.CLASS_REPRESENTATION || childType == FSharpElementTypes.STRUCT_REPRESENTATION
                || childType == FSharpElementTypes.INTERFACE_REPRESENTATION) {
                for (PsiElement member = child.getFirstChild(); member != null; member = member.getNextSibling()) {
                    addTypeMember(type, member, members);
                }
            }
            else {
                addTypeMember(type, child, members);
            }
        }
        return members;
    }

    // modules

    @RequiredReadAction
    private static void addBinding(FSharpTypeDeclarationImpl module, PsiElement binding, List<DotNetNamedElement> members) {
        PsiElement head = FSharpPsiUtil.findChild(binding, FSharpElementTypes.TOP_REFERENCE_PAT);
        List<PsiElement> parameters = childrenOf(binding, FSharpElementTypes.PARAMETERS_PATTERN_DECLARATION);
        DotNetTypeRef typeRef = FSharpTypeRefs.fromTypeUsage(returnTypeUsage(binding));
        if (head instanceof PsiNamedElement named && named.getName() != null && isPlainName(named.getName())) {
            Set<DotNetModifier> modifiers = accessModifiers(head);
            modifiers.add(DotNetModifier.STATIC);
            if (!parameters.isEmpty()) {
                members.add(new FSharpLightMethod(head, module, named.getName(), modifiers, parameters(parameters), voidIfUnit(binding, typeRef)));
                return;
            }
            if (FSharpPsiUtil.findChild(binding, FSharpTokenType.MUTABLE) == null && hasAttribute(binding, "Literal")) {
                members.add(new FSharpLightField(head, module, named.getName(), modifiers, typeRef, true));
                return;
            }
            members.add(new FSharpLightProperty(head, module, named.getName(), modifiers, typeRef,
                FSharpPsiUtil.findChild(binding, FSharpTokenType.MUTABLE) != null));
            return;
        }
        // let a, b = ...: a property per name
        collectPatternValues(module, binding, members);
    }

    @RequiredReadAction
    private static void collectPatternValues(FSharpTypeDeclarationImpl module, PsiElement element, List<DotNetNamedElement> members) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType type = child.getNode().getElementType();
            if (type == FSharpElementTypes.TOP_REFERENCE_PAT) {
                if (child instanceof PsiNamedElement named && named.getName() != null && isPlainName(named.getName())
                    && Character.isLowerCase(named.getName().charAt(0))) {
                    Set<DotNetModifier> modifiers = accessModifiers(child);
                    modifiers.add(DotNetModifier.STATIC);
                    members.add(new FSharpLightProperty(child, module, named.getName(), modifiers, DotNetTypeRef.UNKNOWN_TYPE, false));
                }
            }
            else if (type == FSharpElementTypes.TUPLE_PAT || type == FSharpElementTypes.PAREN_PAT || type == FSharpElementTypes.AS_PAT
                || type == FSharpElementTypes.TYPED_PAT || type == FSharpElementTypes.ATTRIB_PAT) {
                collectPatternValues(module, child, members);
            }
        }
    }

    /**
     * {@code val f: int -> int} of a signature file.
     */
    @RequiredReadAction
    private static void addBindingSignature(FSharpTypeDeclarationImpl module, PsiElement signature, List<DotNetNamedElement> members) {
        PsiElement head = FSharpPsiUtil.findChild(signature, FSharpElementTypes.TOP_REFERENCE_PAT);
        if (!(head instanceof PsiNamedElement named) || named.getName() == null || !isPlainName(named.getName())) {
            return;
        }
        Set<DotNetModifier> modifiers = accessModifiers(signature);
        modifiers.add(DotNetModifier.STATIC);
        PsiElement type = returnTypeUsage(signature);
        if (FSharpPsiUtil.is(type, FSharpElementTypes.FUNCTION_TYPE_USAGE)) {
            addSignatureMethod(module, head, named.getName(), modifiers, type, members, false);
        }
        else {
            members.add(new FSharpLightProperty(head, module, named.getName(), modifiers, FSharpTypeRefs.fromTypeUsage(type), false));
        }
    }

    // type members

    @RequiredReadAction
    private static void addTypeMember(FSharpTypeDeclarationImpl type, PsiElement member, List<DotNetNamedElement> members) {
        IElementType memberType = member.getNode().getElementType();
        String name = member instanceof PsiNamedElement named ? named.getName() : null;
        if (memberType == FSharpElementTypes.PRIMARY_CONSTRUCTOR_DECLARATION) {
            Set<DotNetModifier> modifiers = accessModifiers(member);
            members.add(new FSharpLightConstructor(member, type, String.valueOf(type.getName()), modifiers,
                parameters(childrenOf(member, FSharpElementTypes.PARAMETERS_PATTERN_DECLARATION))));
            return;
        }
        if (memberType == FSharpElementTypes.SECONDARY_CONSTRUCTOR_DECLARATION) {
            Set<DotNetModifier> modifiers = accessModifiers(member);
            members.add(new FSharpLightConstructor(member, type, String.valueOf(type.getName()), modifiers,
                parameters(childrenOf(member, FSharpElementTypes.PARAMETERS_PATTERN_DECLARATION))));
            return;
        }
        if (name == null || !isPlainName(name)) {
            return;
        }
        if (memberType == FSharpElementTypes.MEMBER_DECLARATION || memberType == FSharpElementTypes.MEMBER_SIGNATURE) {
            Set<DotNetModifier> modifiers = accessModifiers(member);
            boolean isStatic = FSharpPsiUtil.findChild(member, FSharpTokenType.STATIC) != null;
            if (isStatic) {
                modifiers.add(DotNetModifier.STATIC);
            }
            List<PsiElement> accessors = childrenOf(member, FSharpElementTypes.ACCESSOR_DECLARATION);
            List<PsiElement> parameters = childrenOf(member, FSharpElementTypes.PARAMETERS_PATTERN_DECLARATION);
            DotNetTypeRef typeRef = FSharpTypeRefs.fromTypeUsage(returnTypeUsage(member));
            if (memberType == FSharpElementTypes.MEMBER_SIGNATURE && FSharpPsiUtil.is(returnTypeUsage(member), FSharpElementTypes.FUNCTION_TYPE_USAGE)) {
                addSignatureMethod(type, member, name, modifiers, returnTypeUsage(member), members, false);
                return;
            }
            if (!accessors.isEmpty() || parameters.isEmpty()) {
                members.add(new FSharpLightProperty(member, type, name, modifiers, typeRef, hasSetter(accessors)));
                return;
            }
            members.add(new FSharpLightMethod(member, type, name, modifiers, parameters(parameters), voidIfUnit(member, typeRef)));
            return;
        }
        if (memberType == FSharpElementTypes.ABSTRACT_MEMBER_DECLARATION) {
            Set<DotNetModifier> modifiers = accessModifiers(member);
            modifiers.add(DotNetModifier.ABSTRACT);
            if (FSharpPsiUtil.findChild(member, FSharpTokenType.STATIC) != null) {
                modifiers.add(DotNetModifier.STATIC);
            }
            PsiElement typeUsage = returnTypeUsage(member);
            List<PsiElement> accessors = childrenOf(member, FSharpElementTypes.ACCESSOR_DECLARATION);
            if (accessors.isEmpty() && FSharpPsiUtil.is(typeUsage, FSharpElementTypes.FUNCTION_TYPE_USAGE)) {
                addSignatureMethod(type, member, name, modifiers, typeUsage, members, false);
                return;
            }
            members.add(new FSharpLightProperty(member, type, name, modifiers, FSharpTypeRefs.fromTypeUsage(typeUsage), hasSetter(accessors)));
            return;
        }
        if (memberType == FSharpElementTypes.AUTO_PROPERTY_DECLARATION) {
            Set<DotNetModifier> modifiers = accessModifiers(member);
            if (FSharpPsiUtil.findChild(member, FSharpTokenType.STATIC) != null) {
                modifiers.add(DotNetModifier.STATIC);
            }
            List<PsiElement> accessors = childrenOf(member, FSharpElementTypes.ACCESSOR_DECLARATION);
            members.add(new FSharpLightProperty(member, type, name, modifiers, FSharpTypeRefs.fromTypeUsage(FSharpTypeRefs.findTypeUsage(member)),
                hasSetter(accessors)));
            return;
        }
        if (memberType == FSharpElementTypes.VAL_FIELD_DECLARATION) {
            Set<DotNetModifier> modifiers = accessModifiers(member);
            if (FSharpPsiUtil.findChild(member, FSharpTokenType.STATIC) != null) {
                modifiers.add(DotNetModifier.STATIC);
            }
            members.add(new FSharpLightField(member, type, name, modifiers, FSharpTypeRefs.fromTypeUsage(FSharpTypeRefs.findTypeUsage(member)), false));
        }
    }

    /**
     * Method of a signature type: {@code a: int -> b: string -> unit}; curried and tupled arguments are parameters.
     */
    @RequiredReadAction
    private static void addSignatureMethod(FSharpTypeDeclarationImpl type, PsiElement source, String name, Set<DotNetModifier> modifiers,
                                           @Nullable PsiElement typeUsage, List<DotNetNamedElement> members, boolean constructor) {
        List<ParameterInfo> parameters = new ArrayList<>();
        PsiElement current = typeUsage;
        while (current != null && FSharpPsiUtil.is(current, FSharpElementTypes.FUNCTION_TYPE_USAGE)) {
            List<PsiElement> parts = FSharpTypeRefs.findTypeUsages(current);
            if (parts.isEmpty()) {
                break;
            }
            addSignatureParameters(parts.get(0), parameters);
            current = parts.size() > 1 ? parts.get(1) : null;
        }
        DotNetTypeRef returnTypeRef = isUnit(current) ? new FSharpTypeRefByQName(source, DotNetTypes.System.Void) : FSharpTypeRefs.fromTypeUsage(current);
        if (constructor) {
            members.add(new FSharpLightConstructor(source, type, name, modifiers, parameters));
        }
        else {
            members.add(new FSharpLightMethod(source, type, name, modifiers, parameters, returnTypeRef));
        }
    }

    @RequiredReadAction
    private static void addSignatureParameters(PsiElement domain, List<ParameterInfo> parameters) {
        if (isUnit(domain)) {
            return;
        }
        if (FSharpPsiUtil.is(domain, FSharpElementTypes.TUPLE_TYPE_USAGE)) {
            for (PsiElement element : FSharpTypeRefs.findTypeUsages(domain)) {
                addSignatureParameters(element, parameters);
            }
            return;
        }
        String name = "arg" + parameters.size();
        ASTNode identifier = domain.getNode().findChildByType(FSharpTokenType.IDENTIFIER);
        if (FSharpPsiUtil.is(domain, FSharpElementTypes.PARAMETER_SIGNATURE_TYPE_USAGE) && identifier != null) {
            name = FSharpPsiUtil.unquoteName(identifier.getText());
        }
        parameters.add(new ParameterInfo(domain, name, FSharpTypeRefs.fromTypeUsage(domain)));
    }

    // records, unions, enums, exceptions

    @RequiredReadAction
    private static void addRecord(FSharpTypeDeclarationImpl type, List<DotNetNamedElement> members) {
        PsiElement representation = FSharpPsiUtil.findChild(type, FSharpElementTypes.RECORD_REPRESENTATION);
        PsiElement list = representation == null ? null : FSharpPsiUtil.findChild(representation, FSharpElementTypes.RECORD_MEMBER_DECLARATION_LIST);
        if (representation == null || list == null) {
            return;
        }
        List<ParameterInfo> parameters = new ArrayList<>();
        for (PsiElement field : childrenOf(list, FSharpElementTypes.RECORD_FIELD_DECLARATION)) {
            if (!(field instanceof PsiNamedElement named) || named.getName() == null) {
                continue;
            }
            DotNetTypeRef typeRef = FSharpTypeRefs.fromTypeUsage(FSharpTypeRefs.findTypeUsage(field));
            Set<DotNetModifier> modifiers = EnumSetLike.of(DotNetModifier.PUBLIC);
            members.add(new FSharpLightProperty(field, type, named.getName(), modifiers, typeRef,
                FSharpPsiUtil.findChild(field, FSharpTokenType.MUTABLE) != null));
            parameters.add(new ParameterInfo(field, lowerFirst(named.getName()), typeRef));
        }
        members.add(new FSharpLightConstructor(representation, type, String.valueOf(type.getName()), EnumSetLike.of(DotNetModifier.PUBLIC), parameters));
    }

    @RequiredReadAction
    private static void addUnion(FSharpTypeDeclarationImpl type, List<DotNetNamedElement> members) {
        PsiElement representation = FSharpPsiUtil.findChild(type, FSharpElementTypes.UNION_REPRESENTATION);
        if (representation == null) {
            return;
        }
        DotNetTypeRef unionTypeRef = selfTypeRef(type);
        DotNetTypeRef intTypeRef = new FSharpTypeRefByQName(type, DotNetTypes.System.Int32);
        DotNetTypeRef boolTypeRef = new FSharpTypeRefByQName(type, DotNetTypes.System.Boolean);
        List<PsiElement> cases = childrenOf(representation, FSharpElementTypes.UNION_CASE_DECLARATION);

        members.add(new FSharpLightProperty(representation, type, "Tag", EnumSetLike.of(DotNetModifier.PUBLIC), intTypeRef, false));
        members.add(new FSharpLightTypeDeclaration(representation, type, "Tags",
            EnumSetLike.of(DotNetModifier.PUBLIC, DotNetModifier.STATIC, DotNetModifier.SEALED),
            new FSharpTypeRefByQName(type, DotNetTypes.System.Object),
            tags -> {
                List<DotNetNamedElement> fields = new ArrayList<>();
                for (PsiElement unionCase : cases) {
                    if (unionCase instanceof PsiNamedElement named && named.getName() != null) {
                        fields.add(new FSharpLightField(unionCase, tags, named.getName(),
                            EnumSetLike.of(DotNetModifier.PUBLIC, DotNetModifier.STATIC), intTypeRef, true));
                    }
                }
                return fields;
            }));

        for (PsiElement unionCase : cases) {
            if (!(unionCase instanceof PsiNamedElement named) || named.getName() == null) {
                continue;
            }
            String name = named.getName();
            List<FieldInfo> fields = unionFields(unionCase, FSharpElementTypes.UNION_CASE_FIELD_DECLARATION, "Item");
            members.add(new FSharpLightProperty(unionCase, type, "Is" + name, EnumSetLike.of(DotNetModifier.PUBLIC), boolTypeRef, false));
            if (fields.isEmpty()) {
                members.add(new FSharpLightProperty(unionCase, type, name, EnumSetLike.of(DotNetModifier.PUBLIC, DotNetModifier.STATIC),
                    unionTypeRef, false));
                continue;
            }
            List<ParameterInfo> parameters = new ArrayList<>();
            for (FieldInfo field : fields) {
                parameters.add(new ParameterInfo(field.source(), lowerFirst(field.name()), field.typeRef()));
            }
            members.add(new FSharpLightMethod(unionCase, type, "New" + name, EnumSetLike.of(DotNetModifier.PUBLIC, DotNetModifier.STATIC),
                parameters, unionTypeRef));
            members.add(new FSharpLightTypeDeclaration(unionCase, type, name, EnumSetLike.of(DotNetModifier.PUBLIC, DotNetModifier.SEALED),
                unionTypeRef,
                caseType -> {
                    List<DotNetNamedElement> properties = new ArrayList<>();
                    for (FieldInfo field : fields) {
                        properties.add(new FSharpLightProperty(field.source(), caseType, field.name(), EnumSetLike.of(DotNetModifier.PUBLIC),
                            field.typeRef(), false));
                    }
                    return properties;
                }));
        }
    }

    @RequiredReadAction
    private static void addEnum(FSharpTypeDeclarationImpl type, List<DotNetNamedElement> members) {
        PsiElement representation = FSharpPsiUtil.findChild(type, FSharpElementTypes.ENUM_REPRESENTATION);
        if (representation == null) {
            return;
        }
        DotNetTypeRef enumTypeRef = selfTypeRef(type);
        for (PsiElement enumCase : childrenOf(representation, FSharpElementTypes.ENUM_CASE_DECLARATION)) {
            if (enumCase instanceof PsiNamedElement named && named.getName() != null) {
                members.add(new FSharpLightField(enumCase, type, named.getName(), EnumSetLike.of(DotNetModifier.PUBLIC, DotNetModifier.STATIC),
                    enumTypeRef, true));
            }
        }
    }

    @RequiredReadAction
    private static void addException(FSharpTypeDeclarationImpl type, List<DotNetNamedElement> members) {
        List<FieldInfo> fields = unionFields(type, FSharpElementTypes.EXCEPTION_FIELD_DECLARATION, "Data");
        List<ParameterInfo> parameters = new ArrayList<>();
        for (FieldInfo field : fields) {
            members.add(new FSharpLightProperty(field.source(), type, field.name(), EnumSetLike.of(DotNetModifier.PUBLIC), field.typeRef(), false));
            parameters.add(new ParameterInfo(field.source(), lowerFirst(field.name()), field.typeRef()));
        }
        members.add(new FSharpLightConstructor(type, type, String.valueOf(type.getName()), EnumSetLike.of(DotNetModifier.PUBLIC), parameters));
    }

    private record FieldInfo(PsiElement source, String name, DotNetTypeRef typeRef) {
    }

    /**
     * Fields of a union case or exception: named ones keep their names, others are {@code Item}, {@code Item1}... or
     * {@code Data0}, {@code Data1}...
     */
    @RequiredReadAction
    private static List<FieldInfo> unionFields(PsiElement owner, IElementType fieldType, String unnamedPrefix) {
        PsiElement list = FSharpPsiUtil.findChild(owner, FSharpElementTypes.UNION_CASE_FIELD_DECLARATION_LIST);
        if (list == null) {
            return List.of();
        }
        List<PsiElement> fields = childrenOf(list, fieldType);
        List<FieldInfo> result = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            PsiElement field = fields.get(i);
            ASTNode identifier = field.getNode().findChildByType(FSharpTokenType.IDENTIFIER);
            String name;
            if (identifier != null) {
                name = FSharpPsiUtil.unquoteName(identifier.getText());
            }
            else if (unnamedPrefix.equals("Data")) {
                name = "Data" + i;
            }
            else {
                name = fields.size() == 1 ? "Item" : "Item" + (i + 1);
            }
            result.add(new FieldInfo(field, name, FSharpTypeRefs.fromTypeUsage(FSharpTypeRefs.findTypeUsage(field))));
        }
        return result;
    }

    // helpers

    @RequiredReadAction
    private static DotNetTypeRef selfTypeRef(FSharpTypeDeclarationImpl type) {
        String vmQName = type.getVmQName();
        return vmQName == null ? DotNetTypeRef.UNKNOWN_TYPE : new FSharpTypeRefByQName(type, vmQName);
    }

    @RequiredReadAction
    private static List<ParameterInfo> parameters(List<PsiElement> groups) {
        List<ParameterInfo> result = new ArrayList<>();
        for (PsiElement group : groups) {
            for (PsiElement child = group.getFirstChild(); child != null; child = child.getNextSibling()) {
                addPatternParameters(child, result);
            }
        }
        return result;
    }

    @RequiredReadAction
    private static void addPatternParameters(PsiElement pattern, List<ParameterInfo> result) {
        IElementType type = pattern.getNode().getElementType();
        if (type == FSharpElementTypes.UNIT_PAT) {
            return;
        }
        if (type == FSharpElementTypes.PAREN_PAT) {
            for (PsiElement child = pattern.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (FSharpPsiUtil.is(child, FSharpElementTypes.TUPLE_PAT)) {
                    for (PsiElement element = child.getFirstChild(); element != null; element = element.getNextSibling()) {
                        addPatternParameter(element, result);
                    }
                    return;
                }
            }
            for (PsiElement child = pattern.getFirstChild(); child != null; child = child.getNextSibling()) {
                addPatternParameter(child, result);
            }
            return;
        }
        addPatternParameter(pattern, result);
    }

    @RequiredReadAction
    private static void addPatternParameter(PsiElement pattern, List<ParameterInfo> result) {
        IElementType type = pattern.getNode().getElementType();
        if (type == FSharpElementTypes.TYPED_PAT) {
            String name = "_arg" + (result.size() + 1);
            for (PsiElement child = pattern.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof PsiNamedElement named && named.getName() != null) {
                    name = named.getName();
                    break;
                }
            }
            result.add(new ParameterInfo(pattern, name, FSharpTypeRefs.fromTypeUsage(FSharpTypeRefs.findTypeUsage(pattern))));
            return;
        }
        if (type == FSharpElementTypes.ATTRIB_PAT || type == FSharpElementTypes.PAREN_PAT || type == FSharpElementTypes.OPTIONAL_VAL_PAT) {
            for (PsiElement child = pattern.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNode().getElementType() != FSharpElementTypes.ATTRIBUTE_LIST && child.getNode().getFirstChildNode() != null) {
                    addPatternParameter(child, result);
                    return;
                }
            }
            return;
        }
        if (type == FSharpElementTypes.LOCAL_REFERENCE_PAT && pattern instanceof PsiNamedElement named && named.getName() != null) {
            result.add(new ParameterInfo(pattern, named.getName(), DotNetTypeRef.UNKNOWN_TYPE));
            return;
        }
        if (type == FSharpElementTypes.WILD_PAT || type == FSharpElementTypes.TUPLE_PAT || type == FSharpElementTypes.PARAMETERS_OWNER_PAT
            || type == FSharpElementTypes.RECORD_PAT || type == FSharpElementTypes.LIST_PAT || type == FSharpElementTypes.ARRAY_PAT) {
            result.add(new ParameterInfo(pattern, "_arg" + (result.size() + 1), DotNetTypeRef.UNKNOWN_TYPE));
        }
    }

    private static @Nullable PsiElement returnTypeUsage(PsiElement declaration) {
        PsiElement info = FSharpPsiUtil.findChild(declaration, FSharpElementTypes.RETURN_TYPE_INFO);
        return FSharpTypeRefs.findTypeUsage(info);
    }

    @RequiredReadAction
    private static DotNetTypeRef voidIfUnit(PsiElement declaration, DotNetTypeRef typeRef) {
        return isUnit(returnTypeUsage(declaration)) ? new FSharpTypeRefByQName(declaration, DotNetTypes.System.Void) : typeRef;
    }

    @RequiredReadAction
    private static boolean isUnit(@Nullable PsiElement typeUsage) {
        if (FSharpPsiUtil.is(typeUsage, FSharpElementTypes.PARAMETER_SIGNATURE_TYPE_USAGE) || FSharpPsiUtil.is(typeUsage, FSharpElementTypes.PAREN_TYPE_USAGE)) {
            return isUnit(FSharpTypeRefs.findTypeUsage(typeUsage));
        }
        return FSharpPsiUtil.is(typeUsage, FSharpElementTypes.NAMED_TYPE_USAGE) && typeUsage != null && typeUsage.getText().trim().equals("unit");
    }

    private static boolean hasSetter(List<PsiElement> accessors) {
        for (PsiElement accessor : accessors) {
            if (accessor instanceof PsiNamedElement named && "set".equals(named.getName())) {
                return true;
            }
            ASTNode identifier = accessor.getNode().findChildByType(FSharpTokenType.IDENTIFIER);
            if (identifier != null && identifier.getText().equals("set")) {
                return true;
            }
        }
        return false;
    }

    private static List<PsiElement> childrenOf(PsiElement parent, IElementType type) {
        List<PsiElement> result = new ArrayList<>();
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode().getElementType() == type) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * Access modifier among direct children or in the head pattern.
     */
    private static Set<DotNetModifier> accessModifiers(PsiElement declaration) {
        Set<DotNetModifier> modifiers = new HashSet<>();
        ASTNode node = declaration.getNode();
        if (node.findChildByType(FSharpTokenType.PRIVATE) != null) {
            modifiers.add(DotNetModifier.PRIVATE);
        }
        else if (node.findChildByType(FSharpTokenType.INTERNAL) != null) {
            modifiers.add(DotNetModifier.INTERNAL);
        }
        else {
            modifiers.add(DotNetModifier.PUBLIC);
        }
        return modifiers;
    }

    private static boolean hasAttribute(PsiElement declaration, String name) {
        for (PsiElement child = declaration.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (FSharpPsiUtil.is(child, FSharpElementTypes.ATTRIBUTE_LIST)) {
                String text = child.getText();
                if (text.contains(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPlainName(String name) {
        return !name.isEmpty() && (Character.isLetter(name.charAt(0)) || name.charAt(0) == '_');
    }

    private static String lowerFirst(String name) {
        return name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Mutable modifier sets.
     */
    private static final class EnumSetLike {
        private static Set<DotNetModifier> of(DotNetModifier... modifiers) {
            return new HashSet<>(List.of(modifiers));
        }
    }
}
