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

package consulo.fsharp.module.extension;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.projectRoots.Sdk;
import consulo.dotnet.compiler.DotNetCompileFailedException;
import consulo.dotnet.compiler.DotNetCompilerOptionsBuilder;
import consulo.dotnet.module.extension.DotNetModuleExtension;
import consulo.fsharp.compiler.FSharpCompilerOptionsBuilder;
import consulo.module.extension.impl.ModuleExtensionImpl;
import consulo.mono.dotnet.sdk.MonoSdkType;
import consulo.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 12.06.2015
 */
public class MonoFSharpModuleExtension extends ModuleExtensionImpl<MonoFSharpModuleExtension> implements FSharpModuleExtension<MonoFSharpModuleExtension>
{
	public MonoFSharpModuleExtension(@NotNull String id, @NotNull ModuleRootLayer moduleRootLayer)
	{
		super(id, moduleRootLayer);
	}

	@NotNull
	@Override
	public DotNetCompilerOptionsBuilder createCompilerOptionsBuilder() throws DotNetCompileFailedException
	{
		FSharpCompilerOptionsBuilder builder = new FSharpCompilerOptionsBuilder();

		DotNetModuleExtension extension = getModuleRootLayer().getExtension(DotNetModuleExtension.class);
		assert extension != null;
		Sdk sdk = extension.getSdk();
		assert sdk != null;

		builder.setExecutable(MonoSdkType.getInstance().getExecutable(extension.getSdk()));
		builder.addProgramArgument(sdk.getHomePath() + "/fsc.exe");
		return builder;
	}
}
