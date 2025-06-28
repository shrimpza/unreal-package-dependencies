package net.shrimpworks.unreal.dependencies;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.Umod;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.Import;

public class DependencyResolver {

	// known file types of unreal packages
	private static final Set<String> FILE_TYPES = Set.of("u", "unr", "utx", "uax", "umx", "usx", "usm", "ut2", "ukx", "upk", "ut3", "run", "ums");
	private static final Set<String> UMODS = Set.of("umod", "ut2mod", "ut4mod", "rmod");

	public final Path rootPath;
	public final Map<String, Set<UnrealPackage>> knownPackages;
	private final Map<String, Set<UnrealPackage>> lowerNames;
	private final NativePackages nativePackages;

	public DependencyResolver(Path rootPath) throws IOException {
		this(rootPath, NativePackages.DEFAULT);
	}

	public DependencyResolver(Path rootPath, NativePackages nativePackages) throws IOException, ResolutionException {
		this(rootPath, NativePackages.DEFAULT, e -> {
			throw e;
		});
	}

	public DependencyResolver(Path rootPath, NativePackages nativePackages, Consumer<ResolutionException> exceptionHandler)
		throws IOException, ResolutionException {
		this.rootPath = rootPath;
		this.nativePackages = nativePackages;
		this.knownPackages = new HashMap<>();
		this.lowerNames = new HashMap<>();

		Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String ext = extension(file).toLowerCase();
				try {
					if (FILE_TYPES.contains(ext)) {
						UnrealPackage pkg = new UnrealPackage(file);
						knownPackages.computeIfAbsent(pkg.name, n -> new HashSet<>()).add(pkg);
					} else if (UMODS.contains(ext)) {
						try (Umod umod = new Umod(file)) {
							for (Umod.UmodFile umodFile : umod.files) {
								String umExt = extension(umodFile.name).toLowerCase();
								if (FILE_TYPES.contains(umExt)) {
									UnrealPackage pkg = new UnrealPackage(UnrealPackage.plainName(umodFile.name),
																		  new Package(new PackageReader(umodFile.read())));
									knownPackages.computeIfAbsent(pkg.name, n -> new HashSet<>()).add(pkg);
								}
							}
						}
					}
				} catch (Exception e) {
					exceptionHandler.accept(
						new ResolutionException(file, String.format("Failed to read file %s: %s", file, e), e)
					);
				}
				return super.visitFile(file, attrs);
			}
		});

		knownPackages.forEach((k, v) -> lowerNames.put(k.toLowerCase(), v));
	}

	/**
	 * Find a package by it's name - excluding file extensions (case
	 * insensitive).
	 *
	 * @param pkgName package to find
	 * @return found package
	 * @throws NoSuchElementException the package could not be found
	 */
	public UnrealPackage findPackage(String pkgName) {
		return lowerNames.get(pkgName.toLowerCase()).stream().findFirst()
						 .orElseThrow(() -> new NoSuchElementException("Could not find package with name " + pkgName));
	}

	/**
	 * Resolve a packages dependencies.
	 * <p>
	 * Also see {@link #findPackage(String)}.
	 *
	 * @param pkgName name of the package to resolve dependencies for.
	 * @return resolution result
	 */
	public Map<String, Set<Resolved>> resolve(String pkgName) {
		return resolve(findPackage(pkgName));
	}

	/**
	 * Resolve a packages dependencies.
	 *
	 * @param unrealPackage package to resolve dependencies for
	 * @return resolution result
	 */
	public Map<String, Set<Resolved>> resolve(UnrealPackage unrealPackage) {
		Map<String, Set<Resolved>> importPackages = new HashMap<>();
		for (Import rootImport : unrealPackage.pkg.packageImports()) {
			Set<UnrealPackage> candidatePackages = lowerNames.getOrDefault(rootImport.name.name.toLowerCase(), Collections.emptySet());
			Set<Resolved> candidates = new HashSet<>();
			for (Import i : rootImport.children()) {
				// required package is missing completely
				if (candidatePackages.isEmpty()) candidates.add(resolve(i, null));

				for (UnrealPackage pkg : candidatePackages) {
					// prefer exports with children, they have stuff to import
					pkg.pkg.rootExports().stream()
						   .filter(e -> e.name.name.equalsIgnoreCase(i.name.name))
						   .min((a, b) -> a.children().isEmpty() ? 1 : -1)
						   .ifPresentOrElse(
							   found -> candidates.add(resolve(i, found)),
							   () -> {
								   // no regular exports found, maybe we can find a native export
								   NativePackages.NativePackage nativePackage = nativePackages.get(rootImport.name.name);
								   if (nativePackage != null && nativePackage.contains(i.name.name)) {
									   candidates.add(new Resolved(i, Resolved.ResolvedTarget.nativeClass(nativePackage.name,
																										  i.name.name),
																   Collections.emptySet()));
								   } else {
									   // we didn't find a sub-package or export we were looking for, so add the rest of the imports
									   candidates.add(resolve(i, null));
								   }
							   });
				}
			}
			importPackages.put(rootImport.name.name, candidates);
		}

		return importPackages;
	}

	// --- private helpers

	private Resolved resolve(Import anImport, Export anExport) {
		Set<Resolved> children = new HashSet<>();
		for (Import i : anImport.children()) {
			Resolved found = null;
			if (anExport != null) {
				for (Export e : anExport.children()) {
					if (e.name.name.equalsIgnoreCase(i.name.name)) {
						found = resolve(i, e);
						break;
					}
				}
			}
			if (found == null) found = resolve(i, null);
			children.add(found);
		}
		return new Resolved(anImport, anExport == null ? null : Resolved.ResolvedTarget.export(anExport), children);
	}

	private static String extension(Path path) {
		return extension(path.toString());
	}

	private static String extension(String pathString) {
		return pathString.substring(pathString.lastIndexOf(".") + 1);
	}

	public static class ResolutionException extends RuntimeException {

		public final Path file;

		public ResolutionException(Path file, String message, Throwable cause) {
			super(message, cause);
			this.file = file;
		}
	}
}
