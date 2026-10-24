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
package consulo.fsharp.impl.lang.psi.stub.index;

import consulo.annotation.component.ExtensionImpl;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.language.psi.stub.IntStubIndexExtension;
import consulo.language.psi.stub.StubIndexKey;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class FSharpTypeByVmQNameIndex extends IntStubIndexExtension<DotNetTypeDeclaration> {
    @Override
    public StubIndexKey<Integer, DotNetTypeDeclaration> getKey() {
        return FSharpIndexKeys.TYPE_BY_VMQNAME_INDEX;
    }
}
