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

package consulo.fsharp.impl.lang.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.dotnet.psi.DotNetFile;
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.impl.FSharpDeclarationUtil;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23-Nov-16.
 */
public class FSharpFileImpl extends PsiFileBase implements DotNetFile
{
	public FSharpFileImpl(@Nonnull FileViewProvider viewProvider)
	{
		super(viewProvider, FSharpLanguage.INSTANCE);
		if (viewProvider.getVirtualFile().getName().endsWith(".fsi"))
		{
			init(FSharpElementTypes.FSHARP_SIG_FILE, FSharpElementTypes.FSHARP_SIG_FILE);
		}
	}

	/**
	 * Top level types and modules, also those in namespaces.
	 */
	@RequiredReadAction
	@Override
	public DotNetNamedElement[] getMembers()
	{
		java.util.List<DotNetNamedElement> members = new java.util.ArrayList<>();
		for(ASTNode child = getNode().getFirstChildNode(); child != null; child = child.getTreeNext())
		{
			if(FSharpDeclarationUtil.NAMESPACES.contains(child.getElementType()) && child.getPsi() != null)
			{
				members.addAll(FSharpDeclarationUtil.getDeclarations(child.getPsi()));
			}
			else if(FSharpDeclarationUtil.TYPE_LIKE.contains(child.getElementType()) && child.getPsi() instanceof DotNetNamedElement named)
			{
				members.add(named);
			}
		}
		return members.toArray(DotNetNamedElement[]::new);
	}

	public boolean isSignatureFile()
	{
		return getNode().getElementType() == FSharpElementTypes.FSHARP_SIG_FILE;
	}
}
