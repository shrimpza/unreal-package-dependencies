package net.shrimpworks.unreal.dependencies;

import java.util.Set;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.Import;

public class Resolved {

	public final Import imported;
	public final ResolvedTarget resolved;
	public final Set<Resolved> children;

	public Resolved(Import imported, ResolvedTarget resolved, Set<Resolved> children) {
		this.imported = imported;
		this.resolved = resolved;
		this.children = children;
	}

	public boolean resolved() {
		return resolved != null && children.stream().allMatch(Resolved::resolved);
	}

	public interface ResolvedTarget {

		static ResolvedTarget export(Export export) {
			return new ResolvedExport(export);
		}

		static ResolvedTarget nativeClass(String packageName, String className) {
			return new ResolvedNativeClass(packageName, className);
		}

		String name();
	}

	public static class ResolvedExport implements ResolvedTarget {

		public final Export exported;

		public ResolvedExport(Export exported) {
			this.exported = exported;
		}

		@Override
		public String name() {
			return exported.name.name;
		}
	}

	public static class ResolvedNativeClass implements ResolvedTarget {

		public final String packageName;
		public final String className;

		public ResolvedNativeClass(String packageName, String className) {
			this.packageName = packageName;
			this.className = className;
		}

		@Override
		public String name() {
			return className;
		}
	}

}
