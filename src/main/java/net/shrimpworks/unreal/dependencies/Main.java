package net.shrimpworks.unreal.dependencies;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.Import;

public class Main {

	private enum Verbosity {
		FILES,
		PACKAGES,
		MISSING_PACKAGES,
		MISSING_DETAIL,
		ALL
	}

	private static final String PAD_SIZE = "  ";

	private static final String ANSI_RED = "\u001B[31m";
	private static final String ANSI_GREEN = "\u001B[32m";
	private static final String ANSI_RESET = "\u001B[0m";

	private static final String OK = String.format("%so%s", ANSI_GREEN, ANSI_RESET);
	private static final String BAD = String.format("%sx%s", ANSI_RED, ANSI_RESET);

	public static void main(String[] args) throws IOException {
		CLI cli = CLI.parse(Map.of("show", "all"), args);

		if (cli.args().length < 1) {
			System.err.println("A search path is expected!");
			usage();
			System.exit(2);
		}

		if (cli.args().length < 2) {
			System.err.println("One or more packages are expected!");
			usage();
			System.exit(2);
		}

		final Verbosity verbosity = Verbosity.valueOf(cli.option("show", "all").toUpperCase());

		Path searchPath = Paths.get(cli.args()[0]).toAbsolutePath();
		NativePackages nativePackages = new NativePackages();
		DependencyResolver resolver = new DependencyResolver(searchPath, nativePackages);

		while (true) {
			for (int i = 1; i < cli.args().length; i++) {
				UnrealPackage pkg;

				Path p = Paths.get(cli.args()[i]).toAbsolutePath();
				if (Files.exists(p)) pkg = resolver.findPackage(p);
				else pkg = resolver.findPackage(cli.args()[i]);

				Map<String, Set<Resolved>> resolved = resolver.resolve(pkg);
				printResolved(resolver, pkg, resolved, verbosity, System.out);
			}

		}
	}

	// --- private helpers

	/**
	 * Prints the results of a dependency resolution check, with varying levels
	 * of output depending on the specified {@link Verbosity}.
	 *
	 * @param resolver  dependency resolver instance
	 * @param pkg       the package which was checked
	 * @param resolved  resolution output from {@link DependencyResolver#resolve(UnrealPackage)}
	 * @param verbosity amount of information to output, see {@link Verbosity}
	 * @param out       output stream to write to
	 */
	private static void printResolved(DependencyResolver resolver, UnrealPackage pkg, Map<String, Set<Resolved>> resolved,
									  Verbosity verbosity, PrintStream out) {
		String fileResolved = resolved.values().stream().flatMap(Set::stream).allMatch(Resolved::resolved)
							  && resolved.entrySet().stream().noneMatch(e -> e.getValue().isEmpty()) ? OK : BAD;
		out.printf("%s %s%n", fileResolved, pkg.name);
		if (verbosity != Verbosity.FILES) {
			resolved.forEach((k, v) -> {
				boolean pkgResolved = !v.isEmpty() && v.stream().allMatch(Resolved::resolved);
				if ((verbosity == Verbosity.PACKAGES || verbosity == Verbosity.ALL) ||
					(!pkgResolved && (verbosity == Verbosity.MISSING_PACKAGES || verbosity == Verbosity.MISSING_DETAIL))) {
					out.printf("%s %s%s%n", pkgResolved ? OK : BAD, PAD_SIZE, k);
					if (verbosity == Verbosity.ALL || (!pkgResolved && verbosity == Verbosity.MISSING_DETAIL)) {
						out.print(prettyResolved(v, verbosity == Verbosity.MISSING_DETAIL, String.format("%s%s", PAD_SIZE, PAD_SIZE)));
					}
				}
			});
		}
	}

	/**
	 * Print a resolved elements tree.
	 *
	 * @param resolved    package import resolution result
	 * @param missingOnly only show elements which are missing
	 * @param padded      depth of padding of the tree
	 * @return a printable string
	 */
	private static String prettyResolved(Set<Resolved> resolved, boolean missingOnly, String padded) {
		StringBuilder sb = new StringBuilder();
		resolved.stream().sorted(Comparator.comparing(r -> r.imported.name)).forEach(r -> {
			String childPad = String.format("%s%s", PAD_SIZE, padded);
			boolean parentResolved = r.resolved();
			if (!missingOnly || !parentResolved) {
				sb.append(String.format("%s %s%s: %s%n", parentResolved ? OK : BAD,
										padded, r.imported.name().name, r.imported.className.name));
			}

			r.children.stream().sorted(Comparator.comparing(child -> child.imported.name)).forEach(child -> {
				boolean childResolved = r.resolved();
				if (!missingOnly || !childResolved) {
					sb.append(String.format("%s %s%s: %s%n", childResolved ? OK : BAD,
											childPad, child.imported.name.name, child.imported.className.name));

					Set<Resolved> subChildren = child.children;
					if (!subChildren.isEmpty()) {
						sb.append(prettyResolved(subChildren, missingOnly, String.format("%s%s", PAD_SIZE, childPad)));
					}
				}
			});
		});

		return sb.toString();
	}

	/**
	 * Utility to print a package's export tree.
	 *
	 * @return printable string
	 */
	private static String prettyPrintExports(Collection<Export> exports, String padded) {
		StringBuilder sb = new StringBuilder();
		exports.stream().sorted(Comparator.comparing(Export::name)).forEach(e -> {
			String childPad = String.format("  %s", padded);
			sb.append(String.format("%s%s: %s%n", padded, e.name().name, e.classIndex.get().name().name));
			e.children().stream().sorted(Comparator.comparing(Export::name)).forEach(child -> {
				sb.append(String.format("%s%s: %s%n", childPad, child.name.name, child.classIndex.get().name().name));
				Set<Export> subChildren = child.children();
				if (!subChildren.isEmpty()) {
					sb.append(prettyPrintExports(subChildren, String.format("  %s", childPad)));
				}
			});
		});
		return sb.toString();
	}

	/**
	 * Utility to print a package's import tree.
	 *
	 * @return printable string
	 */
	private static String prettyPrintImports(Collection<Import> imports, String padded) {
		StringBuilder sb = new StringBuilder();
		imports.stream().sorted(Comparator.comparing(Import::name)).forEach(i -> {
			String childPad = String.format("  %s", padded);
			sb.append(String.format("%s%s: %s%n", padded, i.name().name, i.className.name));
			i.children().stream().sorted(Comparator.comparing(Import::name)).forEach(child -> {
				sb.append(String.format("%s%s: %s%n", childPad, child.name.name, child.className.name));
				Set<Import> subChildren = child.children();
				if (!subChildren.isEmpty()) {
					sb.append(prettyPrintImports(subChildren, String.format("  %s", childPad)));
				}
			});
		});
		return sb.toString();
	}

	private static void usage() {
		System.out.println("Package Dependency Resolver");
		System.out.println();
		System.out.println("Usage: package-dependency.jar [options] <search path> <packages, ...>");
		System.out.println();
		System.out.println("Inspects and resolves the packages and individual textures, classes, models");
		System.out.println("etc required by the package or packages specified, using the <search path>");
		System.out.println("provided.");
		System.out.println();
		System.out.println("In addition to displaying missing packages, a non-zero exit code indicates");
		System.out.println("failure to resolve all dependencies of any package.");
		System.out.println();
		System.out.println("Options:");
		System.out.println(" --show=[files,all,packages,missing_packages,missing_detail]");
		System.out.println("   limit printed output");
	}
}
