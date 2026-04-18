/**
 * @author VISTALL
 * @since 17/01/2023
 */
module consulo.fsharp.api
{
	requires transitive consulo.dotnet.api;
	requires transitive consulo.dotnet.impl;

	requires consulo.compiler.api;
	requires consulo.application.content.api;
	requires consulo.language.api;
	requires consulo.localize.api;
	requires consulo.module.api;
	requires consulo.process.api;
	requires consulo.ui.api;
	requires consulo.util.io;
	requires consulo.util.lang;
	requires consulo.virtual.file.system.api;

	exports consulo.fsharp;
	exports consulo.fsharp.compiler;
	exports consulo.fsharp.icon;
	exports consulo.fsharp.module.extension;
}