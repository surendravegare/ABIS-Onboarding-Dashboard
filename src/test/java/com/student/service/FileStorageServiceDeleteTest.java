package com.student.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class FileStorageServiceDeleteTest {

	@TempDir
	Path tmp;

	private FileStorageService service() {
		FileStorageService s = new FileStorageService();
		ReflectionTestUtils.setField(s, "uploadDir", tmp.resolve("photos").toString());
		return s;
	}

	@Test
	void deletesAnExistingPhoto() throws Exception {
		Path photos = Files.createDirectories(tmp.resolve("photos"));
		Path file = Files.writeString(photos.resolve("a.png"), "x");
		service().deletePhoto("/uploads/a.png");
		assertFalse(Files.exists(file));
	}

	@Test
	void neverDeletesOutsideTheUploadFolder() throws Exception {
		Files.createDirectories(tmp.resolve("photos"));
		Path outside = Files.writeString(tmp.resolve("secret.txt"), "keep");
		service().deletePhoto("/uploads/../secret.txt");
		assertTrue(Files.exists(outside));
	}

	@Test
	void ignoresNullAndForeignUrls() {
		service().deletePhoto(null);
		service().deletePhoto("http://example.com/x.png");
	}
}