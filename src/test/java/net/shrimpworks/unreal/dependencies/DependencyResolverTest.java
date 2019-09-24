package net.shrimpworks.unreal.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DependencyResolverTest {

	private Path unrMap;

	@BeforeAll
	public void setup() throws IOException {
		unrMap = Files.createTempFile("test-map-", ".unr");
		try (InputStream is = DependencyResolverTest.class.getResourceAsStream("SCR-CityStreet.unr.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, unrMap, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@AfterAll
	public void teardown() throws IOException {
		Files.deleteIfExists(unrMap);
	}

	@Test
	public void loadPackages() {
		fail("todo");
	}

	@Test
	public void resolveDependencies() {
		fail("todo");
	}
}
