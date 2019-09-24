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

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.Import;

public class DependencyResolver {

	private static final Set<String> FILE_TYPES = Set.of("u", "unr", "utx", "uax", "umx");

	public final Path rootPath;
	public final Map<String, Set<UnrealPackage>> knownPackages;
	private final Map<String, Set<UnrealPackage>> lowerNames;
	private final NativePackages nativePackages;

	public DependencyResolver(Path rootPath, NativePackages nativePackages) throws IOException {
		this.rootPath = rootPath;
		this.nativePackages = nativePackages;
		this.knownPackages = new HashMap<>();
		this.lowerNames = new HashMap<>();

		Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String ext = extension(file).toLowerCase();
				if (FILE_TYPES.contains(ext)) {
					UnrealPackage pkg = new UnrealPackage(file);
					knownPackages.computeIfAbsent(pkg.name, n -> new HashSet<>()).add(pkg);
				}
				return super.visitFile(file, attrs);
			}
		});

		knownPackages.forEach((k, v) -> lowerNames.put(k.toLowerCase(), v));
	}

	public UnrealPackage findPackage(String pkgName) {
		return lowerNames.get(pkgName.toLowerCase()).stream().findFirst()
						 .orElseThrow(() -> new NoSuchElementException("Could not find package with name " + pkgName));
	}

	public UnrealPackage findPackage(Path path) {
		return knownPackages.values().stream().flatMap(Set::stream).filter(p -> p.path.equals(path)).findFirst()
							.orElseThrow(() -> new NoSuchElementException("Could not find package for path " + path.toString()));
	}

	public Map<String, Set<Resolved>> resolve(String pkgName) {
		return resolve(findPackage(pkgName));
	}

	public Map<String, Set<Resolved>> resolve(UnrealPackage unrealPackage) {
		Map<String, Set<Resolved>> importPackages = new HashMap<>();
		for (Import rootImport : unrealPackage.pkg.packageImports()) {
			Set<UnrealPackage> candidatePackages = lowerNames.getOrDefault(rootImport.name.name.toLowerCase(), Collections.emptySet());
			if (candidatePackages.isEmpty()) {
				importPackages.put(rootImport.name.name, Collections.emptySet());
			} else {
				Set<Resolved> candidates = new HashSet<>();
				for (Import i : rootImport.children()) {
					for (UnrealPackage pkg : candidatePackages) {
						Export match = pkg.pkg.rootExports().stream()
											  .filter(e -> e.name.name.equalsIgnoreCase(i.name.name))
											  .findFirst().orElse(null);
						if (match != null) {
							Resolved candidate = resolve(i, match);
							candidates.add(candidate);
						} else {
							// no regular exports found, maybe we can find a native export
							NativePackages.NativePackage nativePackage = nativePackages.get(rootImport.name.name);
							if (nativePackage != null && nativePackage.contains(i.name.name)) {
								candidates.add(new Resolved(i, Resolved.ResolvedTarget.nativeClass(nativePackage.name, i.name.name),
															Collections.emptySet()));
							} else {
								candidates.add(new Resolved(i, null, Collections.emptySet()));
							}
						}
					}
				}
				importPackages.put(rootImport.name.name, candidates);
			}
		}

		return importPackages;
	}

	private Resolved resolve(Import anImport, Export anExport) {
		Set<Resolved> children = new HashSet<>();
		for (Import i : anImport.children()) {
			for (Export e : anExport.children()) {
				if (e.name.equals(i.name)) {
					children.add(resolve(i, e));
					break;
				}
			}
		}
		return new Resolved(anImport, Resolved.ResolvedTarget.export(anExport), children);
	}

	private static String extension(Path path) {
		String pathString = path.toString();
		return pathString.substring(pathString.lastIndexOf(".") + 1);
	}
}
