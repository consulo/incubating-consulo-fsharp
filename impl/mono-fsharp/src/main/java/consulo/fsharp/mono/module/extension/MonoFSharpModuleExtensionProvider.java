package consulo.fsharp.mono.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.fsharp.icon.FSharpIconGroup;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 17/01/2023
 */
@ExtensionImpl
public class MonoFSharpModuleExtensionProvider implements ModuleExtensionProvider<MonoFSharpModuleExtension>
{
	@Nonnull
	@Override
	public String getId()
	{
		return "mono-fsharp";
	}

	@Nullable
	@Override
	public String getParentId()
	{
		return "mono-dotnet";
	}

	@Nonnull
	@Override
	public LocalizeValue getName()
	{
		return LocalizeValue.localizeTODO("F#");
	}

	@Nonnull
	@Override
	public Image getIcon()
	{
		return FSharpIconGroup.fsharp();
	}

	@Nonnull
	@Override
	public ModuleExtension<MonoFSharpModuleExtension> createImmutableExtension(@Nonnull ModuleRootLayer moduleRootLayer)
	{
		return new MonoFSharpModuleExtension(getId(), moduleRootLayer);
	}

	@Nonnull
	@Override
	public MutableModuleExtension<MonoFSharpModuleExtension> createMutableExtension(@Nonnull ModuleRootLayer moduleRootLayer)
	{
		return new MonoFSharpMutableModuleExtension(getId(), moduleRootLayer);
	}
}
