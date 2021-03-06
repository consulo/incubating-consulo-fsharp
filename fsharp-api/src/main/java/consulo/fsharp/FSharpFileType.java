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

package consulo.fsharp;

import com.intellij.openapi.fileTypes.LanguageFileType;
import consulo.fsharp.api.icon.FSharpApiIconGroup;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12.06.2015
 */
public class FSharpFileType extends LanguageFileType
{
	public static final FSharpFileType INSTANCE = new FSharpFileType();

	public FSharpFileType()
	{
		super(FSharpLanguage.INSTANCE);
	}

	@Nonnull
	@Override
	public String getId()
	{
		return "F#";
	}

	@Nonnull
	@Override
	public String getDescription()
	{
		return "F# file types";
	}

	@Nonnull
	@Override
	public String getDefaultExtension()
	{
		return "fs";
	}

	@Nullable
	@Override
	public Image getIcon()
	{
		return FSharpApiIconGroup.fsharp();
	}
}
