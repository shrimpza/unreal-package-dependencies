package net.shrimpworks.unreal.dependencies;

import org.junit.jupiter.api.Test;

public class ShippedPackagesTest {

	@Test
	public void packagesContain() {
		assert(ShippedPackages.UNREAL_GOLD.contains("DmDeck16"));
		assert(ShippedPackages.UNREAL_TOURNAMENT.contains("DM-Deck16]["));
		assert(ShippedPackages.UNREAL_TOURNAMENT_2004.contains("DM-Deck17"));
	}
}
