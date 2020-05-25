package net.shrimpworks.unreal.dependencies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShippedPackagesTest {

	@Test
	public void packagesContain() {
		assertTrue(ShippedPackages.UNREAL_GOLD.contains("DmDeck16"));
		assertTrue(ShippedPackages.UNREAL_TOURNAMENT.contains("DM-Deck16]["));
		assertTrue(ShippedPackages.UNREAL_TOURNAMENT_2004.contains("DM-Deck17"));

		assertFalse(ShippedPackages.UNREAL_GOLD.contains("DM-Deck17"));
		assertFalse(ShippedPackages.UNREAL_TOURNAMENT.contains("DmDeck16"));
		assertFalse(ShippedPackages.UNREAL_TOURNAMENT_2004.contains("DM-Deck16]["));
	}
}
