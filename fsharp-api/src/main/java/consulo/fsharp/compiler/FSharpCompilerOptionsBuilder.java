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

import consulo.compiler.CompilerMessageCategory;
import consulo.content.bundle.Sdk;
import consulo.dotnet.DotNetTarget;
import consulo.dotnet.compiler.*;
import consulo.dotnet.module.extension.DotNetModuleExtension;
import consulo.fsharp.module.extension.FSharpModuleExtension;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 23-Nov-16.
 */
public class FSharpCompilerOptionsBuilder implements DotNetCompilerOptionsBuilder {
    private @Nullable String myExecutable;

    private final List<String> myArguments = new ArrayList<>();
    private final List<String> myProgramArguments = new ArrayList<>();

    public FSharpCompilerOptionsBuilder addArgument(String arg) {
        myArguments.add(arg + "\n");
        return this;
    }

    public FSharpCompilerOptionsBuilder addProgramArgument(String arg) {
        myProgramArguments.add(arg);
        return this;
    }

    public void setExecutable(String executable) {
        myExecutable = executable;
    }

    public void setExecutableFromSdk(Sdk sdk, String executableFromSdk) {
        myExecutable = sdk.getHomePath() + File.separatorChar + executableFromSdk;
    }

    @Override
    public @Nullable DotNetCompilerMessage convertToMessage(Module module, String line) {
        return new DotNetCompilerMessage(CompilerMessageCategory.WARNING, line, null, -1, 1);
    }

    @Override
    public GeneralCommandLine createCommandLine(Module module, Collection<Path> results, DotNetModuleExtension<?> extension) throws Exception {
        if (myExecutable == null) {
            throw new DotNetCompileFailedException("F# compiler is not found");
        }
        FSharpModuleExtension<?> fsharpExtension = ModuleUtilCore.getExtension(module, FSharpModuleExtension.class);

        assert fsharpExtension != null;

        String target = null;
        switch (extension.getTarget()) {
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

        File moduleDir = new File(module.getModuleDirPath());

        addArgument("-o:" + FileUtil.getRelativePath(moduleDir, new File(outputFile)));

        Set<File> libraryFiles = DotNetCompilerUtil.collectDependencies(module, DotNetTarget.LIBRARY, false, DotNetCompilerUtil.ACCEPT_ALL);
        if (!libraryFiles.isEmpty()) {
            for (File libraryFile : libraryFiles) {
                addArgument("-r:" + StringUtil.QUOTER.apply(FileUtil.toSystemDependentName(libraryFile.getAbsolutePath())));
            }
        }

        if (extension.isAllowDebugInfo()) {
            addArgument("--debug+");
        }

        addArgument("--fullpaths");
        addArgument("--nologo");
        addArgument("--utf8output");
        addArgument("--noframework");

        String defineVariables = StringUtil.join(extension.getVariables(), ";");
        if (!StringUtil.isEmpty(defineVariables)) {
            addArgument("--define:" + defineVariables);
        }

        for (Path result : results) {
            String relativePath = FileUtil.getRelativePath(moduleDir, result.toFile());
            addArgument(relativePath != null ? relativePath : result.toString());
        }

        for (String argument : myArguments) {
            commandLine.addParameter(argument.trim());
        }

        FileUtil.createParentDirs(new File(outputFile));

        commandLine.setRedirectErrorStream(true);

        return commandLine;
    }
}
