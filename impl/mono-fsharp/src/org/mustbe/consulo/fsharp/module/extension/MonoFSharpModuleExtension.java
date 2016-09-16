/*
 * Copyright 2013-2015 must-be.org
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

package org.mustbe.consulo.fsharp.module.extension;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.fsharp.FSharpFileType;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import consulo.annotations.RequiredReadAction;
import consulo.dotnet.compiler.DotNetCompileFailedException;
import consulo.dotnet.compiler.DotNetCompilerMessage;
import consulo.dotnet.compiler.DotNetCompilerOptionsBuilder;
import consulo.dotnet.module.extension.DotNetModuleExtension;
import consulo.extension.impl.ModuleExtensionImpl;
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

	@RequiredReadAction
	@NotNull
	@Override
	public PsiElement[] getEntryPointElements()
	{
		return new PsiElement[0];
	}

	@NotNull
	@Override
	public LanguageFileType getFileType()
	{
		return FSharpFileType.INSTANCE;
	}

	@RequiredReadAction
	@Nullable
	@Override
	public String getAssemblyTitle()
	{
		return null;
	}

	@NotNull
	@Override
	public DotNetCompilerOptionsBuilder createCompilerOptionsBuilder() throws DotNetCompileFailedException
	{
		return new DotNetCompilerOptionsBuilder()
		{
			@Nullable
			@Override
			public DotNetCompilerMessage convertToMessage(Module module, String s)
			{
				return null;
			}

			@NotNull
			@Override
			public GeneralCommandLine createCommandLine(@NotNull Module module,
					@NotNull VirtualFile[] virtualFiles,
					@NotNull DotNetModuleExtension<?> dotNetModuleExtension) throws IOException
			{
				return null;
			}
		};
	}

}
