package net.shrimpworks.unreal.dependencies;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages a collection of native package contents which are exported from DLLs
 * not exposed via the related Unreal package.
 * <p>
 * Maintaining a collection of native exports allows us to resolve imports
 * which cannot be matched to package exports.
 */
public class NativePackages {

	private static final String[] PACKAGES = {
			"Core", "Engine", "Fire", "IpDrv", "Render", "UWeb", "Window"
	};

	private final Map<String, NativePackage> packages;

	public static final NativePackages DEFAULT = new NativePackages();

	public NativePackages() {
		this.packages = new HashMap<>();
		for (String pkg : PACKAGES) {
			packages.put(pkg.toLowerCase(), new NativePackage(pkg));
		}
	}

	/**
	 * Find a native package.
	 *
	 * @param name package name, case insensitive
	 * @return a native package for the name requested, or <code>null</code> if not found
	 */
	public NativePackage get(String name) {
		return packages.get(name.toLowerCase());
	}

	@Override
	public String toString() {
		return String.format("NativePackages [packages=%s]", packages.values());
	}

	public static class NativePackage {

		public final String name;
		public final Set<String> classes;

		public NativePackage(String name) {
			this(name, loadClasses(name));
		}

		public NativePackage(String name, Set<String> classes) {
			this.name = name;
			this.classes = classes;
		}

		/**
		 * Check whether this native package exports a class with the name provided.
		 *
		 * @param className class name, case insensitive
		 * @return true of this package exports the class
		 */
		public boolean contains(String className) {
			return classes.contains(className.toLowerCase());
		}

		private static Set<String> loadClasses(String name) {
			Set<String> classNames = new HashSet<>();
			try (InputStream is = NativePackages.class.getResourceAsStream(String.format("native/%s.txt", name));
				 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
				String line;
				while ((line = reader.readLine()) != null) {
					classNames.add(line.trim().toLowerCase());
				}
			} catch (IOException e) {
				throw new RuntimeException("Failed to load native classes for package " + name, e);
			}
			return classNames;
		}

		@Override
		public String toString() {
			return String.format("NativePackage [name=%s, classes=%s]", name, classes);
		}
	}
}
