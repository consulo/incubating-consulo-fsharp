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
import consulo.fsharp.impl.lang.psi.impl.FSharpNamespaceDeclarationImpl;
import consulo.fsharp.impl.lang.psi.stub.FSharpNamespaceStub;
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
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpNamespaceStubElementType extends FSharpStubElementType<FSharpNamespaceStub, FSharpNamespaceDeclarationImpl> {
    public FSharpNamespaceStubElementType(String debugName) {
        super(debugName);
    }

    @Override
    public PsiElement createElement(ASTNode node) {
        return new FSharpNamespaceDeclarationImpl(node);
    }

    @Override
    public FSharpNamespaceDeclarationImpl createPsi(FSharpNamespaceStub stub) {
        return new FSharpNamespaceDeclarationImpl(stub, this);
    }

    @RequiredReadAction
    @Override
    public FSharpNamespaceStub createStub(FSharpNamespaceDeclarationImpl psi, StubElement parentStub) {
        return new FSharpNamespaceStub(parentStub, this, psi.getPresentableQName());
    }

    @Override
    public void serialize(FSharpNamespaceStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getQName());
    }

    @Override
    public FSharpNamespaceStub deserialize(StubInputStream dataStream, @Nullable StubElement parentStub) throws IOException {
        String qName = dataStream.readNameString();
        return new FSharpNamespaceStub(parentStub, this, qName == null ? "" : qName);
    }

    @Override
    public void indexStub(FSharpNamespaceStub stub, IndexSink sink) {
        if (stub.getQName().isEmpty()) {
            return;
        }
        // makes the namespace itself known, even without types
        DotNetNamespaceStubUtil.indexStub(sink,
            FSharpIndexKeys.MEMBER_BY_NAMESPACE_QNAME_INDEX,
            FSharpIndexKeys.MEMBER_BY_ALL_NAMESPACE_QNAME_INDEX,
            "",
            stub.getQName());
    }
}
