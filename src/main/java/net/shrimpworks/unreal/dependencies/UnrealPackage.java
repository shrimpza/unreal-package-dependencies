package net.shrimpworks.unreal.dependencies;

import java.io.IOException;
import java.nio.file.Path;

import net.shrimpworks.unreal.packages.Package;

public class UnrealPackage {

	public final String name;
	public final Package pkg;

	public UnrealPackage(Path path) throws IOException {
		this(plainName(path), new Package(path));
	}

	public UnrealPackage(String name, Package pkg) {
		this.name = name;
		this.pkg = pkg;
	}

	static String plainName(Path path) {
		return plainName(path.toString());
	}

	static String plainName(String path) {
		String tmp = path.replaceAll("\\\\", "/");
		tmp = tmp.substring(Math.max(0, tmp.lastIndexOf("/") + 1));
		return tmp.substring(0, tmp.lastIndexOf(".")).replaceAll("/", "").trim().replaceAll("[^\\x20-\\x7E]", "");
	}

	@Override
	public String toString() {
		return String.format("UnrealPackage [name=%s, pkg=%s]", name, pkg);
	}
}
