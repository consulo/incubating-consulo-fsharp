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
import consulo.dotnet.DotNetTypes;
import consulo.dotnet.psi.DotNetGenericParameter;
import consulo.dotnet.psi.DotNetGenericParameterList;
import consulo.dotnet.psi.DotNetInheritUtil;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetModifierList;
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.dotnet.psi.DotNetTypeList;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpStubElements;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.fsharp.impl.lang.psi.light.FSharpCompiledMembers;
import consulo.fsharp.impl.lang.psi.resolve.FSharpTypeRefByQName;
import consulo.fsharp.impl.lang.psi.resolve.FSharpTypeRefs;
import consulo.fsharp.impl.lang.psi.stub.FSharpTypeKind;
import consulo.fsharp.impl.lang.psi.stub.FSharpTypeStub;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Module, type or exception as a .NET type.
 * <p>
 * Members are the compiled view of the F# declarations ({@link FSharpCompiledMembers}): module {@code let}s are static
 * methods and properties, union cases are nested types and {@code NewCase} methods, record fields are properties.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpTypeDeclarationImpl extends FSharpStubElementImpl<FSharpTypeStub> implements DotNetTypeDeclaration {
    public FSharpTypeDeclarationImpl(ASTNode node) {
        super(node);
    }

    public FSharpTypeDeclarationImpl(FSharpTypeStub stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @RequiredReadAction
    @Override
    public @Nullable String getName() {
        FSharpTypeStub stub = getGreenStub();
        if (stub != null) {
            return stub.getName();
        }
        if (getNode().getElementType() == FSharpStubElements.ANON_MODULE_DECLARATION) {
            return FSharpPsiUtil.getImplicitModuleName(getContainingFile());
        }
        return super.getName();
    }

    @RequiredReadAction
    public FSharpTypeKind getKind() {
        FSharpTypeStub stub = getGreenStub();
        if (stub != null) {
            return stub.getKind();
        }
        IElementType elementType = getNode().getElementType();
        if (FSharpDeclarationUtil.MODULES.contains(elementType)) {
            return FSharpTypeKind.MODULE;
        }
        if (elementType == FSharpStubElements.EXCEPTION_DECLARATION) {
            return FSharpTypeKind.EXCEPTION;
        }
        if (FSharpDeclarationUtil.hasAttribute(this, "Measure")) {
            return FSharpTypeKind.MEASURE;
        }
        boolean members = false;
        boolean concrete = false;
        for (ASTNode child = getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType type = child.getElementType();
            if (type == FSharpElementTypes.RECORD_REPRESENTATION) {
                return FSharpTypeKind.RECORD;
            }
            if (type == FSharpElementTypes.UNION_REPRESENTATION) {
                return FSharpTypeKind.UNION;
            }
            if (type == FSharpElementTypes.ENUM_REPRESENTATION) {
                return FSharpTypeKind.ENUM;
            }
            if (type == FSharpElementTypes.CLASS_REPRESENTATION) {
                return FSharpTypeKind.CLASS;
            }
            if (type == FSharpElementTypes.STRUCT_REPRESENTATION) {
                return FSharpTypeKind.STRUCT;
            }
            if (type == FSharpElementTypes.INTERFACE_REPRESENTATION) {
                return FSharpTypeKind.INTERFACE;
            }
            if (type == FSharpElementTypes.DELEGATE_REPRESENTATION) {
                return FSharpTypeKind.DELEGATE;
            }
            if (type == FSharpElementTypes.TYPE_ABBREVIATION_REPRESENTATION) {
                return FSharpTypeKind.ABBREVIATION;
            }
            if (type == FSharpElementTypes.ABSTRACT_MEMBER_DECLARATION || type == FSharpElementTypes.INTERFACE_INHERIT) {
                members = true;
            }
            else if (type == FSharpElementTypes.PRIMARY_CONSTRUCTOR_DECLARATION || type == FSharpElementTypes.MEMBER_DECLARATION
                || type == FSharpElementTypes.LET_BINDINGS_DECLARATION || type == FSharpElementTypes.TYPE_INHERIT
                || type == FSharpElementTypes.SECONDARY_CONSTRUCTOR_DECLARATION || type == FSharpElementTypes.VAL_FIELD_DECLARATION
                || type == FSharpElementTypes.AUTO_PROPERTY_DECLARATION || type == FSharpElementTypes.INTERFACE_IMPLEMENTATION
                || type == FSharpElementTypes.DO_STATEMENT) {
                concrete = true;
            }
        }
        if (FSharpDeclarationUtil.hasAttribute(this, "Interface")) {
            return FSharpTypeKind.INTERFACE;
        }
        if (FSharpDeclarationUtil.hasAttribute(this, "Struct")) {
            return FSharpTypeKind.STRUCT;
        }
        if (FSharpDeclarationUtil.hasAttribute(this, "Class")) {
            return FSharpTypeKind.CLASS;
        }
        return members && !concrete ? FSharpTypeKind.INTERFACE : FSharpTypeKind.CLASS;
    }

    @RequiredReadAction
    public boolean isModule() {
        return getKind() == FSharpTypeKind.MODULE;
    }

    @RequiredReadAction
    public int computeFlags() {
        int flags = 0;
        ASTNode access = findAccessModifier();
        if (access != null && access.getElementType() == FSharpTokenType.PRIVATE) {
            flags |= FSharpTypeStub.PRIVATE;
        }
        else if (access != null && access.getElementType() == FSharpTokenType.INTERNAL) {
            flags |= FSharpTypeStub.INTERNAL;
        }
        FSharpTypeKind kind = getKind();
        if (kind == FSharpTypeKind.INTERFACE || FSharpDeclarationUtil.hasAttribute(this, "AbstractClass")) {
            flags |= FSharpTypeStub.ABSTRACT;
        }
        if (kind == FSharpTypeKind.RECORD || kind == FSharpTypeKind.UNION || kind == FSharpTypeKind.MODULE || kind == FSharpTypeKind.STRUCT
            || kind == FSharpTypeKind.ENUM || kind == FSharpTypeKind.DELEGATE || FSharpDeclarationUtil.hasAttribute(this, "Sealed")) {
            flags |= FSharpTypeStub.SEALED;
        }
        if (FSharpDeclarationUtil.is(FSharpDeclarationUtil.getContainer(this), FSharpDeclarationUtil.MODULES)) {
            flags |= FSharpTypeStub.NESTED;
        }
        if ((kind == FSharpTypeKind.RECORD || kind == FSharpTypeKind.UNION) && FSharpDeclarationUtil.hasAttribute(this, "Struct")) {
            flags |= FSharpTypeStub.STRUCT;
        }
        return flags;
    }

    private int getFlags() {
        FSharpTypeStub stub = getGreenStub();
        return stub != null ? stub.getFlags() : computeFlags();
    }

    /**
     * Access modifier of the declaration itself: after the keyword, before the name.
     */
    private @Nullable ASTNode findAccessModifier() {
        for (ASTNode child = getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType type = child.getElementType();
            if (type == FSharpTokenType.PRIVATE || type == FSharpTokenType.INTERNAL || type == FSharpTokenType.PUBLIC) {
                return child;
            }
            if (type == FSharpTokenType.IDENTIFIER || type == FSharpTokenType.EQUALS) {
                return null;
            }
        }
        return null;
    }

    @RequiredReadAction
    @Override
    public boolean isInterface() {
        return getKind() == FSharpTypeKind.INTERFACE;
    }

    @RequiredReadAction
    @Override
    public boolean isStruct() {
        return getKind() == FSharpTypeKind.STRUCT || (getFlags() & FSharpTypeStub.STRUCT) != 0;
    }

    @RequiredReadAction
    @Override
    public boolean isEnum() {
        return getKind() == FSharpTypeKind.ENUM;
    }

    @RequiredReadAction
    @Override
    public boolean isNested() {
        return (getFlags() & FSharpTypeStub.NESTED) != 0;
    }

    @Override
    public @Nullable DotNetTypeList getExtendList() {
        return null;
    }

    /**
     * {@code inherit} and implemented interfaces; the implicit base type otherwise.
     */
    @RequiredReadAction
    @Override
    public DotNetTypeRef[] getExtendTypeRefs() {
        List<DotNetTypeRef> result = new ArrayList<>();
        boolean hasBaseType = false;
        for (ASTNode child = getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType type = child.getElementType();
            if (type == FSharpElementTypes.TYPE_INHERIT || type == FSharpElementTypes.INTERFACE_INHERIT
                || type == FSharpElementTypes.INTERFACE_IMPLEMENTATION) {
                ASTNode name = child.findChildByType(FSharpElementTypes.TYPE_REFERENCE_NAME);
                if (name != null && name.getPsi() != null) {
                    result.add(FSharpTypeRefs.fromReferenceName(name.getPsi()));
                    hasBaseType |= type == FSharpElementTypes.TYPE_INHERIT;
                }
            }
        }
        String implicitBase = switch (getKind()) {
            case CLASS, MODULE, RECORD, UNION -> DotNetTypes.System.Object;
            case STRUCT -> DotNetTypes.System.ValueType;
            case ENUM -> DotNetTypes.System.Enum;
            case DELEGATE -> DotNetTypes.System.MulticastDelegate;
            case EXCEPTION -> DotNetTypes.System.Exception;
            case INTERFACE, ABBREVIATION, MEASURE -> null;
        };
        if (!hasBaseType && implicitBase != null) {
            result.add(0, new FSharpTypeRefByQName(this, implicitBase));
        }
        return result.toArray(DotNetTypeRef[]::new);
    }

    @RequiredReadAction
    @Override
    public boolean isInheritor(String otherVmQName, boolean deep) {
        return DotNetInheritUtil.isInheritor(this, otherVmQName, deep);
    }

    @RequiredReadAction
    @Override
    public DotNetTypeRef getTypeRefForEnumConstants() {
        return new FSharpTypeRefByQName(this, DotNetTypes.System.Int32);
    }

    // names

    @RequiredReadAction
    @Override
    public String getPresentableQName() {
        String parent = getPresentableParentQName();
        String name = getName();
        if (name == null) {
            return parent;
        }
        return parent.isEmpty() ? name : parent + "." + name;
    }

    @RequiredReadAction
    @Override
    public String getPresentableParentQName() {
        FSharpTypeStub stub = getGreenStub();
        if (stub != null) {
            return stub.getParentQName();
        }
        IElementType elementType = getNode().getElementType();
        if (elementType == FSharpStubElements.ANON_MODULE_DECLARATION) {
            return "";
        }
        if (elementType == FSharpStubElements.NAMED_MODULE_DECLARATION) {
            ASTNode qualifier = getNode().findChildByType(FSharpElementTypes.TYPE_REFERENCE_NAME);
            return qualifier == null ? "" : qualifier.getText().replaceAll("\\s", "");
        }
        PsiElement container = FSharpDeclarationUtil.getContainer(this);
        if (container instanceof FSharpNamespaceDeclarationImpl namespace) {
            return namespace.getPresentableQName();
        }
        if (container instanceof FSharpTypeDeclarationImpl module) {
            return module.getPresentableQName();
        }
        return "";
    }

    @RequiredReadAction
    @Override
    public @Nullable String getVmName() {
        String name = getName();
        if (name == null) {
            return null;
        }
        if (isModule() && hasModuleSuffix()) {
            name = name + "Module";
        }
        int count = getGenericParametersCount();
        return count == 0 ? name : name + "`" + count;
    }

    /**
     * {@code [<CompilationRepresentation(ModuleSuffix)>]} or, implicitly, a type with the same name next to the module.
     */
    @RequiredReadAction
    private boolean hasModuleSuffix() {
        if (getNode().getElementType() == FSharpStubElements.ANON_MODULE_DECLARATION) {
            return false;
        }
        String compilation = FSharpDeclarationUtil.getAttributeArgumentText(this, "CompilationRepresentation");
        if (compilation != null && compilation.contains("ModuleSuffix")) {
            return true;
        }
        PsiElement container = FSharpDeclarationUtil.getContainer(this);
        String name = getName();
        if (container == null || name == null) {
            return false;
        }
        for (DotNetNamedElement sibling : FSharpDeclarationUtil.getDeclarations(container)) {
            if (sibling != this && sibling instanceof FSharpTypeDeclarationImpl type && !type.isModule() && name.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    @Override
    public @Nullable String getVmQName() {
        FSharpTypeStub stub = getGreenStub();
        if (stub != null) {
            return stub.getVmQName();
        }
        if (!getKind().isDotNetType()) {
            return null;
        }
        String vmName = getVmName();
        if (vmName == null) {
            return null;
        }
        IElementType elementType = getNode().getElementType();
        if (elementType == FSharpStubElements.ANON_MODULE_DECLARATION) {
            return vmName;
        }
        if (elementType == FSharpStubElements.NAMED_MODULE_DECLARATION) {
            String parent = getPresentableParentQName();
            return parent.isEmpty() ? vmName : parent + "." + vmName;
        }
        PsiElement container = FSharpDeclarationUtil.getContainer(this);
        if (container instanceof FSharpTypeDeclarationImpl module) {
            String parent = module.getVmQName();
            return parent == null ? vmName : parent + "/" + vmName;
        }
        if (container instanceof FSharpNamespaceDeclarationImpl namespace) {
            String parent = namespace.getPresentableQName();
            return parent.isEmpty() ? vmName : parent + "." + vmName;
        }
        return vmName;
    }

    // generics

    @RequiredReadAction
    @Override
    public @Nullable DotNetGenericParameterList getGenericParameterList() {
        return null;
    }

    @RequiredReadAction
    @Override
    public DotNetGenericParameter[] getGenericParameters() {
        return DotNetGenericParameter.EMPTY_ARRAY;
    }

    @RequiredReadAction
    @Override
    public int getGenericParametersCount() {
        FSharpTypeStub stub = getGreenStub();
        if (stub != null) {
            return stub.getGenericParametersCount();
        }
        int count = 0;
        for (ASTNode child = getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType type = child.getElementType();
            if (type == FSharpElementTypes.POSTFIX_TYPE_PARAMETER_DECLARATION_LIST || type == FSharpElementTypes.PREFIX_TYPE_PARAMETER_DECLARATION_LIST) {
                for (ASTNode parameter = child.getFirstChildNode(); parameter != null; parameter = parameter.getTreeNext()) {
                    if (parameter.getElementType() == FSharpElementTypes.TYPE_PARAMETER_OF_TYPE_DECLARATION) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // members

    @RequiredReadAction
    @Override
    public DotNetNamedElement[] getMembers() {
        List<DotNetNamedElement> members = new ArrayList<>();
        if (isModule()) {
            members.addAll(FSharpDeclarationUtil.getDeclarations(this));
        }
        members.addAll(FSharpCompiledMembers.getMembers(this));
        return members.toArray(DotNetNamedElement[]::new);
    }

    // modifiers

    @RequiredReadAction
    @Override
    public boolean hasModifier(DotNetModifier modifier) {
        int flags = getFlags();
        if (modifier == DotNetModifier.PUBLIC) {
            return (flags & (FSharpTypeStub.PRIVATE | FSharpTypeStub.INTERNAL)) == 0;
        }
        if (modifier == DotNetModifier.PRIVATE) {
            return (flags & FSharpTypeStub.PRIVATE) != 0;
        }
        if (modifier == DotNetModifier.INTERNAL) {
            return (flags & FSharpTypeStub.INTERNAL) != 0;
        }
        if (modifier == DotNetModifier.STATIC) {
            return isModule();
        }
        if (modifier == DotNetModifier.ABSTRACT) {
            return (flags & FSharpTypeStub.ABSTRACT) != 0;
        }
        if (modifier == DotNetModifier.SEALED) {
            return (flags & FSharpTypeStub.SEALED) != 0;
        }
        return false;
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetModifierList getModifierList() {
        return null;
    }
}
