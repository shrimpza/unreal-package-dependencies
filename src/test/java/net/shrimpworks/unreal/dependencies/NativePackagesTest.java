package net.shrimpworks.unreal.dependencies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NativePackagesTest {

	@Test
	public void loadNativePackages() {
		NativePackages pkgs = new NativePackages();
		assertNotNull(pkgs.get("Core"));
		assertTrue(pkgs.get("Engine").contains("Level"));
	}
}
