package com.hero.bikestore.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * Stores the given file under the given directory (relative to application root or absolute).
     * Returns the stored file name (not full path) that can be saved into DB.
     *
     * @param uploadDir directory where file should be stored (e.g. "uploads/bikes")
     * @param file multipart file from client
     * @return stored file name (UUID + extension)
     */
    String storeFile(String uploadDir, MultipartFile file);
}
