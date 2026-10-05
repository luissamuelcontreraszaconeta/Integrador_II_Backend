package com.exportrace.service.impl;

import com.exportrace.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path baseStorageLocation;
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp", "image/jpg");
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public LocalFileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo crear el directorio de almacenamiento base: " + uploadDir, ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo subido está vacío.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo se aceptan imágenes JPEG, PNG o WEBP.");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence.jpg");
        String extension = "";
        int extIndex = originalFileName.lastIndexOf(".");
        if (extIndex > 0) {
            extension = originalFileName.substring(extIndex).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Extensión de archivo inválida. Permitidas: .jpg, .jpeg, .png, .webp");
        }

        try {
            Path targetDir = subDirectory != null && !subDirectory.trim().isEmpty()
                    ? this.baseStorageLocation.resolve(subDirectory).normalize()
                    : this.baseStorageLocation;

            Files.createDirectories(targetDir);

            String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension;
            Path targetLocation = targetDir.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return (subDirectory != null && !subDirectory.isEmpty() ? subDirectory + "/" : "") + uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Error al almacenar el archivo fotográfico en disco.", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName, String subDirectory) {
        try {
            Path targetDir = subDirectory != null && !subDirectory.trim().isEmpty()
                    ? this.baseStorageLocation.resolve(subDirectory).normalize()
                    : this.baseStorageLocation;

            Path filePath = targetDir.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Archivo fotográfico no encontrado o no accesible: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Ruta de archivo malformada: " + fileName, ex);
        }
    }

    @Override
    public void deleteFile(String fileName, String subDirectory) {
        try {
            Path targetDir = subDirectory != null && !subDirectory.trim().isEmpty()
                    ? this.baseStorageLocation.resolve(subDirectory).normalize()
                    : this.baseStorageLocation;

            Path filePath = targetDir.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println("No se pudo eliminar el archivo físico: " + fileName + " (" + ex.getMessage() + ")");
        }
    }
}
