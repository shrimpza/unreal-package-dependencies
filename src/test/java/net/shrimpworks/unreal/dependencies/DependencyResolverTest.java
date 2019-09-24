package net.shrimpworks.unreal.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DependencyResolverTest {

	private Path tmpDir;
	private Path unrMap;

	@BeforeAll
	public void setup() throws IOException {
		tmpDir = Files.createTempDirectory("deps-test");
		unrMap = Files.createFile(tmpDir.resolve("SCR-CityStreet.unr"));
		try (InputStream is = DependencyResolverTest.class.getResourceAsStream("SCR-CityStreet.unr.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, unrMap, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@AfterAll
	public void teardown() throws IOException {
		Files.deleteIfExists(unrMap);
		Files.deleteIfExists(tmpDir);
	}

	@Test
	public void loadPackages() throws IOException {
		DependencyResolver resolver = new DependencyResolver(unrMap.getParent(), new NativePackages());
		assertNotNull(resolver.findPackage(unrMap));
		assertNotNull(resolver.findPackage("SCR-CityStreet"));
	}

	@Test
	public void resolveDependencies() throws IOException {
		DependencyResolver resolver = new DependencyResolver(unrMap.getParent(), new NativePackages());
		Map<String, Set<Resolved>> resolve = resolver.resolve("SCR-CityStreet");
		assertTrue(resolve.get("SoccerScores").isEmpty());
	}
}
