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

import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.FSharpFileImpl;
import consulo.fsharp.impl.lang.psi.stub.FSharpFileStub;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.DefaultStubBuilder;
import consulo.language.psi.stub.IStubFileElementType;
import consulo.language.psi.stub.StubBuilder;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.stub.StubInputStream;
import org.jspecify.annotations.Nullable;

/**
 * Implementation ({@code .fs}, {@code .fsx}) or signature ({@code .fsi}) file.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpFileStubElementType extends IStubFileElementType<FSharpFileStub> {
    private final String myExternalId;

    public FSharpFileStubElementType(String debugName, String externalId) {
        super(debugName, FSharpLanguage.INSTANCE);
        myExternalId = externalId;
    }

    @Override
    public StubBuilder getBuilder() {
        return new DefaultStubBuilder() {
            @Override
            protected StubElement createStubForFile(PsiFile file) {
                if (file instanceof FSharpFileImpl fsharpFile) {
                    return new FSharpFileStub(fsharpFile, FSharpFileStubElementType.this);
                }
                return super.createStubForFile(file);
            }
        };
    }

    @Override
    public FSharpFileStub deserialize(StubInputStream dataStream, @Nullable StubElement parentStub) {
        return new FSharpFileStub(null, this);
    }

    @Override
    public int getStubVersion() {
        return 2;
    }

    @Override
    public String getExternalId() {
        return myExternalId;
    }
}
