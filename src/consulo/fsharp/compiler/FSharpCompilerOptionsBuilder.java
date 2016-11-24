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

package consulo.fsharp.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.dotnet.DotNetTarget;
import consulo.dotnet.compiler.DotNetCompileFailedException;
import consulo.dotnet.compiler.DotNetCompilerMessage;
import consulo.dotnet.compiler.DotNetCompilerOptionsBuilder;
import consulo.dotnet.compiler.DotNetCompilerUtil;
import consulo.dotnet.compiler.DotNetMacroUtil;
import consulo.dotnet.module.extension.DotNetModuleExtension;
import consulo.fsharp.module.extension.FSharpModuleExtension;

/**
 * @author VISTALL
 * @since 23-Nov-16.
 */
public class FSharpCompilerOptionsBuilder implements DotNetCompilerOptionsBuilder
{
	@Nullable
	private String myExecutable;

	private final List<String> myArguments = new ArrayList<String>();
	private final List<String> myProgramArguments = new ArrayList<String>();

	public FSharpCompilerOptionsBuilder addArgument(@NotNull String arg)
	{
		myArguments.add(arg + "\n");
		return this;
	}

	public FSharpCompilerOptionsBuilder addProgramArgument(@NotNull String arg)
	{
		myProgramArguments.add(arg);
		return this;
	}

	public void setExecutable(@Nullable String executable)
	{
		myExecutable = executable;
	}

	public void setExecutableFromSdk(@NotNull Sdk sdk, @NotNull String executableFromSdk)
	{
		myExecutable = sdk.getHomePath() + File.separatorChar + executableFromSdk;
	}

	@Nullable
	@Override
	public DotNetCompilerMessage convertToMessage(Module module, String s)
	{
		return new DotNetCompilerMessage(CompilerMessageCategory.WARNING, s, null, -1, 1);
	}

	@NotNull
	@Override
	public GeneralCommandLine createCommandLine(@NotNull Module module,
			@NotNull VirtualFile[] results,
			@NotNull DotNetModuleExtension<?> extension) throws Exception
	{
		if(myExecutable == null)
		{
			throw new DotNetCompileFailedException("F# compiler is not found");
		}
		FSharpModuleExtension<?> fsharpExtesion = ModuleUtilCore.getExtension(module, FSharpModuleExtension.class);

		assert fsharpExtesion != null;

		String target = null;
		switch(extension.getTarget())
		{
			case EXECUTABLE:
				target = "exe";
				break;
			case WIN_EXECUTABLE:
				target = "winexe";
				break;
			case LIBRARY:
				target = "library";
				break;
			case NET_MODULE:
				target = "module";
				break;
		}

		GeneralCommandLine commandLine = new GeneralCommandLine();
		commandLine.setExePath(myExecutable);
		commandLine.setWorkDirectory(module.getModuleDirPath());
		commandLine.addParameters(myProgramArguments);

		addArgument("--target:" + target);
		String outputFile = DotNetMacroUtil.expandOutputFile(extension);

		addArgument("-o:" + FileUtil.getRelativePath(new File(module.getModuleDirPath()), new File(outputFile)));

		Set<File> libraryFiles = DotNetCompilerUtil.collectDependencies(module, DotNetTarget.LIBRARY, false, DotNetCompilerUtil.ACCEPT_ALL);
		if(!libraryFiles.isEmpty())
		{
			for(File libraryFile : libraryFiles)
			{
				addArgument("-r:" + StringUtil.QUOTER.fun(FileUtil.toSystemDependentName(libraryFile.getAbsolutePath())));
			}
		}

		/*Set<File> moduleFiles = DotNetCompilerUtil.collectDependencies(module, DotNetTarget.NET_MODULE, false, DotNetCompilerUtil.ACCEPT_ALL);
		if(!moduleFiles.isEmpty())
		{
			addArgument("/addmodule:" + StringUtil.join(moduleFiles, new Function<File, String>()
			{
				@Override
				public String fun(File file)
				{
					return StringUtil.QUOTER.fun(file.getAbsolutePath());
				}
			}, ","));
		}*/

		if(extension.isAllowDebugInfo())
		{
			addArgument("--debug+");
		}

		/*if(csharpExtension.isAllowUnsafeCode())
		{
			addArgument("--unsafe");
		}

		if(csharpExtension.isOptimizeCode())
		{
			addArgument("/optimize+");
		}

		switch(csharpExtension.getPlatform())
		{
			case ANY_CPU:
				addArgument("--platform:anycpu");
				break;
			case ANY_CPU_32BIT_PREFERRED:
				addArgument("--platform:anycpu32bitpreferred");
				break;
			case ARM:
				addArgument("--platform:ARM");
				break;
			case X86:
				addArgument("--platform:x86");
				break;
			case X64:
				addArgument("--platform:x64");
				break;
			case ITANIUM:
				addArgument("--platform:Itanium");
				break;
		}*/
		addArgument("--fullpaths");
		addArgument("--nologo");
		addArgument("--utf8output");
		addArgument("--noframework");
		//addArgument("--simpleresolution");

		String defineVariables = StringUtil.join(extension.getVariables(), ";");
		if(!StringUtil.isEmpty(defineVariables))
		{
			addArgument("--define:" + defineVariables);
		}

		for(VirtualFile result : results)
		{
			addArgument(VfsUtil.getRelativePath(result, module.getModuleDir()));
		}

		for(String argument : myArguments)
		{
			commandLine.addParameter(argument.trim());
		}

		FileUtil.createParentDirs(new File(outputFile));

		commandLine.setWorkDirectory(module.getModuleDirPath());
		commandLine.setRedirectErrorStream(true);

		return commandLine;
	}
}
