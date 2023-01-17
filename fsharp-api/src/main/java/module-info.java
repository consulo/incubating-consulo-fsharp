/**
 * @author VISTALL
 * @since 17/01/2023
 */
module consulo.fsharp.api
{
	requires transitive consulo.ide.api;
	requires consulo.dotnet.api;
	requires consulo.dotnet.impl;

	exports consulo.fsharp;
	exports consulo.fsharp.compiler;
	exports consulo.fsharp.icon;
	exports consulo.fsharp.module.extension;
}