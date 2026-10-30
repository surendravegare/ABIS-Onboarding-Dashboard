package com.student.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.student.exception.UserFacingException;

@Service
public class FileStorageService {

	// Profile photos may not be larger than 1 MB
	public static final long MAX_PHOTO_BYTES = 1L * 1024 * 1024;
	
	@Value("${app.upload.dir:uploads/photos}")
	private String uploadDir;

	// Saves the uploaded photo to disk with a unique filename and returns
	// the URL (served via WebConfig's /uploads/** resource handler, admins only)
	// to be stored as the student's photoUrl. Returns null if no file was sent.
	//
	// Only real JPG / PNG / GIF / WEBP images are accepted. The type is detected
	// from the file's own bytes and the stored extension is chosen by the server,
	// so the filename or content type sent by the browser is never trusted.
	public String storePhoto(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			return null;
		}

		if (file.getSize() > MAX_PHOTO_BYTES) {
			throw new UserFacingException("Profile photo must be 1 MB or smaller (yours is "
					+ (file.getSize() + 1023) / 1024 + " KB).");
		}

		String extension;
		try (InputStream in = file.getInputStream()) {
			extension = detectImageExtension(in.readNBytes(12));
		}
		if (extension == null) {
			throw new UserFacingException("Only JPG, PNG, GIF or WEBP images can be uploaded as a photo.");
		}

		Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
		Files.createDirectories(dirPath);

		String filename = UUID.randomUUID().toString() + extension;
		Path target = dirPath.resolve(filename);

		try (InputStream in = file.getInputStream()) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}

		return "/uploads/" + filename;
	}

	// Returns ".jpg", ".png", ".gif" or ".webp", or null if the bytes are not one of those images
	static String detectImageExtension(byte[] h) {
		if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
			return ".jpg";
		}
		if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
				&& h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
			return ".png";
		}
		if (h.length >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8'
				&& (h[4] == '7' || h[4] == '9') && h[5] == 'a') {
			return ".gif";
		}
		if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
				&& h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
			return ".webp";
		}
		return null;
	}
	
	public void deletePhoto(String photoUrl) {
	    if (photoUrl == null || !photoUrl.startsWith("/uploads/")) {
	        return;
	    }
	    try {
	        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
	        Path file = dir.resolve(photoUrl.substring("/uploads/".length())).normalize();
	        if (file.startsWith(dir)) {
	            Files.deleteIfExists(file);
	        }
	    } catch (IOException ex) {
	        // not critical: the record change already succeeded
	    }
	 }
}