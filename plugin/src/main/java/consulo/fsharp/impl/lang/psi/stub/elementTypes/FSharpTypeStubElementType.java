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
package consulo.fsharp.impl.lang.psi.stub.elementTypes;

import consulo.annotation.access.RequiredReadAction;
import consulo.dotnet.psi.impl.stub.DotNetNamespaceStubUtil;
import consulo.fsharp.impl.lang.psi.impl.FSharpTypeDeclarationImpl;
import consulo.fsharp.impl.lang.psi.stub.FSharpTypeKind;
import consulo.fsharp.impl.lang.psi.stub.FSharpTypeStub;
import consulo.fsharp.impl.lang.psi.stub.index.FSharpIndexKeys;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.stub.StubInputStream;
import consulo.language.psi.stub.StubOutputStream;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Modules, types and exceptions: .NET types.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpTypeStubElementType extends FSharpStubElementType<FSharpTypeStub, FSharpTypeDeclarationImpl> {
    public FSharpTypeStubElementType(String debugName) {
        super(debugName);
    }

    @Override
    public PsiElement createElement(ASTNode node) {
        return new FSharpTypeDeclarationImpl(node);
    }

    @Override
    public FSharpTypeDeclarationImpl createPsi(FSharpTypeStub stub) {
        return new FSharpTypeDeclarationImpl(stub, this);
    }

    @RequiredReadAction
    @Override
    public FSharpTypeStub createStub(FSharpTypeDeclarationImpl psi, StubElement parentStub) {
        return new FSharpTypeStub(parentStub,
            this,
            psi.getName(),
            psi.getPresentableParentQName(),
            psi.getVmQName(),
            psi.getKind(),
            psi.computeFlags(),
            psi.getGenericParametersCount());
    }

    @Override
    public void serialize(FSharpTypeStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeName(stub.getParentQName());
        dataStream.writeName(stub.getVmQName());
        dataStream.writeVarInt(stub.getKind().ordinal());
        dataStream.writeVarInt(stub.getFlags());
        dataStream.writeVarInt(stub.getGenericParametersCount());
    }

    @Override
    public FSharpTypeStub deserialize(StubInputStream dataStream, @Nullable StubElement parentStub) throws IOException {
        String name = dataStream.readNameString();
        String parentQName = dataStream.readNameString();
        String vmQName = dataStream.readNameString();
        FSharpTypeKind kind = FSharpTypeKind.values()[dataStream.readVarInt()];
        int flags = dataStream.readVarInt();
        int genericParametersCount = dataStream.readVarInt();
        return new FSharpTypeStub(parentStub, this, name, parentQName == null ? "" : parentQName, vmQName, kind, flags,
            genericParametersCount);
    }

    @Override
    public void indexStub(FSharpTypeStub stub, IndexSink sink) {
        String name = stub.getName();
        String vmQName = stub.getVmQName();
        if (name == null || name.isEmpty() || vmQName == null) {
            return;
        }
        sink.occurrence(FSharpIndexKeys.TYPE_INDEX, name);
        if (!stub.is(FSharpTypeStub.NESTED)) {
            // other languages see the compiled name: module Shape next to type Shape is ShapeModule
            int dot = vmQName.lastIndexOf('.');
            DotNetNamespaceStubUtil.indexStub(sink,
                FSharpIndexKeys.MEMBER_BY_NAMESPACE_QNAME_INDEX,
                FSharpIndexKeys.MEMBER_BY_ALL_NAMESPACE_QNAME_INDEX,
                stub.getParentQName(),
                dot < 0 ? vmQName : vmQName.substring(dot + 1));
        }
        sink.occurrence(FSharpIndexKeys.TYPE_BY_VMQNAME_INDEX, vmQName.hashCode());
    }
}
