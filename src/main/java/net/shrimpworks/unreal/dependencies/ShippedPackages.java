package net.shrimpworks.unreal.dependencies;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class which lists known packages shipped with retail versions of
 * the various Unreal games.
 */
public class ShippedPackages {

	private final Set<String> packages;

	private ShippedPackages(Set<String> packages) {
		this.packages = packages;
	}

	private ShippedPackages(String gameName) {
		this(loadPackages(gameName));
	}

	/**
	 * Check whether a standard retail game includes a certain package.
	 * <p>
	 * Case-insensitive.
	 *
	 * @param packageName package to check
	 * @return true if the package ships with the game
	 */
	public boolean contains(String packageName) {
		return packages.contains(packageName.toLowerCase());
	}

	public static final ShippedPackages UNREAL_GOLD = new ShippedPackages("UnrealGold");
	public static final ShippedPackages UNREAL_TOURNAMENT = new ShippedPackages("UnrealTournament");
	public static final ShippedPackages UNREAL_TOURNAMENT_2004 = new ShippedPackages("UnrealTournament2004");

	private static Set<String> loadPackages(String gameName) {
		Set<String> packageNames = new HashSet<>();
		try (InputStream is = ShippedPackages.class.getResourceAsStream(String.format("shipped/%s.txt", gameName));
			 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				packageNames.add(line.trim().toLowerCase());
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load shipped packages for game " + gameName, e);
		}
		return packageNames;
	}

}