package com.shiptrack.service;

import com.shiptrack.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local disk storage for delivery photos and signatures. Swap the body of store() for the
 * AWS S3 or Cloudinary SDK when moving to a real deployment; the rest of the app is unaffected.
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.storage.location:./uploads}") String location) {
        this.root = Paths.get(location).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Upload folder could not be created at " + root, ex);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose a file to upload");
        }
        String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        if (!extension.matches("(?i)\\.(jpg|jpeg|png|webp|pdf)?")) {
            throw ApiException.badRequest("Only JPG, PNG, WEBP or PDF files can be uploaded");
        }

        String filename = UUID.randomUUID() + extension.toLowerCase();
        try {
            Path target = root.resolve(filename).normalize();
            if (!target.getParent().equals(root)) {
                throw ApiException.badRequest("That filename is not allowed");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "The file could not be saved");
        }
        return "/api/files/" + filename;
    }

    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename).normalize();
            if (!file.getParent().equals(root)) {
                throw ApiException.badRequest("That filename is not allowed");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw ApiException.notFound("File not found");
            }
            return resource;
        } catch (IOException ex) {
            throw ApiException.notFound("File not found");
        }
    }
}
