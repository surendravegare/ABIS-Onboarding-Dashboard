package com.student.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.student.exception.UserFacingException;

class FileStorageServiceTest {

	@Test
	void detectsRealImagesFromTheirBytes() {
		assertEquals(".jpg", FileStorageService.detectImageExtension(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 }));
		assertEquals(".png", FileStorageService.detectImageExtension(
				new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A }));
		assertEquals(".gif", FileStorageService.detectImageExtension("GIF89a".getBytes()));
		assertEquals(".webp", FileStorageService.detectImageExtension("RIFF1234WEBP".getBytes()));
	}

	@Test
	void rejectsEverythingElse() {
		assertNull(FileStorageService.detectImageExtension("<html><script>".getBytes()));
		assertNull(FileStorageService.detectImageExtension(new byte[0]));
	}

	@TempDir
	Path tmp;

	private FileStorageService service() {
		FileStorageService s = new FileStorageService();
		ReflectionTestUtils.setField(s, "uploadDir", tmp.resolve("photos").toString());
		return s;
	}

	private static byte[] jpegOfSize(int size) {
		byte[] data = new byte[size];
		data[0] = (byte) 0xFF;
		data[1] = (byte) 0xD8;
		data[2] = (byte) 0xFF;
		return data;
	}

	@Test
	void acceptsAPhotoOfExactlyFiftyKb() throws Exception {
		MockMultipartFile file = new MockMultipartFile("photoFile", "p.jpg", "image/jpeg",
				jpegOfSize((int) FileStorageService.MAX_PHOTO_BYTES));
		assertNotNull(service().storePhoto(file));
	}

	@Test
	void rejectsAPhotoLargerThanFiftyKb() {
		MockMultipartFile file = new MockMultipartFile("photoFile", "p.jpg", "image/jpeg",
				jpegOfSize((int) FileStorageService.MAX_PHOTO_BYTES + 1));
		assertThrows(UserFacingException.class, () -> service().storePhoto(file));
	}
}