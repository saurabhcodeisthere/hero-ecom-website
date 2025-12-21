package com.hero.bikestore.service.impl;

import com.hero.bikestore.exception.BadRequestException;
import com.hero.bikestore.exception.FileStorageException;
import com.hero.bikestore.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    // Allowed image content types
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // Max size ~ 5MB (adjust if needed)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    public String storeFile(String uploadDir, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided or file is empty.");
        }

        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported file type: " + contentType);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File too large. Max allowed size is " + (MAX_FILE_SIZE / (1024 * 1024)) + " MB");
        }

        try {
            // Prepare directory (create if not exists)
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // Extract extension
            String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
            String extension = "";

            int idx = originalFilename.lastIndexOf('.');
            if (idx > 0) extension = originalFilename.substring(idx).toLowerCase(); // includes the dot

            // Build new filename (UUID + timestamp + extension)
            String newFileName = UUID.randomUUID().toString() + "-" + System.currentTimeMillis() + extension;

            Path target = uploadPath.resolve(newFileName).normalize();

            // Copy file (use REPLACE_EXISTING just in case)
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            log.info("Stored file '{}' as '{}'", originalFilename, target.toString());
            return newFileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file. Please try again.", ex);
        }
    }
}
