/*
 * Copyright 2013-2016 consulo.io
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

package consulo.fsharp.microsoft.module.extension;

import consulo.dotnet.compiler.DotNetCompileFailedException;
import consulo.dotnet.compiler.DotNetCompilerOptionsBuilder;
import consulo.fsharp.module.extension.FSharpModuleExtension;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionBase;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23-Nov-16.
 */
public class MicrosoftFSharpModuleExtension extends ModuleExtensionBase<MicrosoftFSharpModuleExtension> implements FSharpModuleExtension<MicrosoftFSharpModuleExtension>
{
	public MicrosoftFSharpModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer)
	{
		super(id, moduleRootLayer);
	}

	@Nonnull
	@Override
	public DotNetCompilerOptionsBuilder createCompilerOptionsBuilder() throws DotNetCompileFailedException
	{
		return null;
	}
}
