package net.shrimpworks.unreal.dependencies;

import java.io.IOException;
import java.nio.file.Path;

import net.shrimpworks.unreal.packages.Package;

public class UnrealPackage {

	public final Path path;
	public final String name;
	public final Package pkg;

	public UnrealPackage(Path path) throws IOException {
		this.path = path;
		this.name = plainName(path);
		this.pkg = new Package(path);
	}

	private static String plainName(Path path) {
		String tmp = path.toString().replaceAll("\\\\", "/");
		tmp = tmp.substring(Math.max(0, tmp.lastIndexOf("/") + 1));
		return tmp.substring(0, tmp.lastIndexOf(".")).replaceAll("/", "").trim().replaceAll("[^\\x20-\\x7E]", "");
	}

	@Override
	public String toString() {
		return String.format("UnrealPackage [path=%s, name=%s, pkg=%s]", path, name, pkg);
	}
}
