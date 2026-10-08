package com.exportrace.service.impl;

import com.exportrace.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class PersistentFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(PersistentFileStorageService.class);

    private final Path baseStorageLocation;
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp", "image/jpg");
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public PersistentFileStorageService(@Value("${file.upload-dir:${FILE_UPLOAD_DIR:./data/uploads}}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageLocation);
            log.info("[Storage] Base persistent upload directory initialized at: {}", this.baseStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo inicializar el directorio de almacenamiento persistente: " + uploadDir, ex);
        }
    }

    public Path getBaseStorageLocation() {
        return this.baseStorageLocation;
    }

    @Override
    public StoredFile storeEvidence(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo subido está vacío o es nulo.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo excede el tamaño máximo permitido de 5MB.");
        }

        // 1. Sanitize original filename (neutralize path traversal ../)
        String rawOriginalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence.jpg";
        String cleanOriginalName = Paths.get(rawOriginalName).getFileName().toString();
        cleanOriginalName = StringUtils.cleanPath(cleanOriginalName).replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleanOriginalName.isEmpty()) {
            cleanOriginalName = "evidence.jpg";
        }

        // 2. Validate declared extension
        String extension = "";
        int extIndex = cleanOriginalName.lastIndexOf(".");
        if (extIndex > 0) {
            extension = cleanOriginalName.substring(extIndex).toLowerCase();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe tener una extensión válida (.jpg, .jpeg, .png, .webp).");
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extensión de archivo no permitida: " + extension + ". Solo se admiten JPG, PNG y WEBP.");
        }

        // 3. Validate declared MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo MIME no permitido (" + contentType + "). Solo se aceptan imágenes JPEG, PNG o WEBP.");
        }

        // 4. Verify Magic Bytes against declared format
        byte[] fileHeader = readHeaderBytes(file, 16);
        validateMagicBytes(fileHeader, extension, contentType);

        // 5. Generate secure UUID filename to prevent collision & traversal
        String storedFileName = UUID.randomUUID().toString() + extension;

        // 6. Resolve safe target directory
        String sanitizedSubDir = subDirectory != null ? sanitizeSubPath(subDirectory) : "";
        Path targetDir = sanitizedSubDir.isEmpty() 
                ? this.baseStorageLocation 
                : this.baseStorageLocation.resolve(sanitizedSubDir).normalize();

        // Enforce boundary check
        if (!targetDir.startsWith(this.baseStorageLocation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intento de path traversal detectado en subdirectorio.");
        }

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFileName).normalize();
            if (!targetFile.startsWith(this.baseStorageLocation)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intento de path traversal detectado.");
            }

            // 7. Atomic stream copy with SHA-256 calculation
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, sha256Digest);
                 OutputStream os = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                
                byte[] buffer = new byte[8192];
                int read;
                while ((read = dis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }

            String sha256Hex = bytesToHex(sha256Digest.digest());
            String relativeStoragePath = (sanitizedSubDir.isEmpty() ? "" : sanitizedSubDir + "/") + storedFileName;

            log.info("[Storage] Saved evidence: original='{}', stored='{}', size={} bytes, sha256={}",
                    cleanOriginalName, relativeStoragePath, file.getSize(), sha256Hex);

            return new StoredFile(
                    cleanOriginalName,
                    storedFileName,
                    relativeStoragePath,
                    contentType,
                    file.getSize(),
                    sha256Hex
            );

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible.", e);
        } catch (IOException e) {
            log.error("[Storage] Error al escribir archivo en disco persistente: {}", e.getMessage(), e);
            throw new RuntimeException("Error al almacenar el archivo en disco persistente.", e);
        }
    }

    private static final long MAX_DOCUMENT_SIZE = 15 * 1024 * 1024; // 15MB

    @Override
    public StoredFile storeDocument(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo de documento está vacío o es nulo.");
        }

        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El documento excede el tamaño máximo permitido de 15MB.");
        }

        // 1. Sanitize original filename (neutralize path traversal ../)
        String rawOriginalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        String cleanOriginalName = Paths.get(rawOriginalName).getFileName().toString();
        cleanOriginalName = StringUtils.cleanPath(cleanOriginalName).replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleanOriginalName.isEmpty()) {
            cleanOriginalName = "document.pdf";
        }

        // 2. Validate declared extension
        if (!cleanOriginalName.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe tener extensión .pdf.");
        }

        // 3. Verify Magic Bytes for PDF: %PDF- (0x25, 0x50, 0x44, 0x46)
        byte[] fileHeader = readHeaderBytes(file, 5);
        if (!isPdfHeader(fileHeader)) {
            log.warn("[Storage Security] Spoofed PDF file detected. Header bytes: {}", bytesToHex(fileHeader));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo no es un documento PDF válido. Cabecera mágica corrupta o incompatible.");
        }

        // 4. Generate secure UUID filename to prevent collision
        String storedFileName = UUID.randomUUID().toString() + ".pdf";

        // 5. Resolve safe target directory
        String sanitizedSubDir = subDirectory != null ? sanitizeSubPath(subDirectory) : "documents";
        Path targetDir = sanitizedSubDir.isEmpty()
                ? this.baseStorageLocation
                : this.baseStorageLocation.resolve(sanitizedSubDir).normalize();

        // Enforce boundary check
        if (!targetDir.startsWith(this.baseStorageLocation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intento de path traversal detectado en subdirectorio.");
        }

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFileName).normalize();
            if (!targetFile.startsWith(this.baseStorageLocation)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intento de path traversal detectado.");
            }

            // 6. Atomic stream copy with SHA-256 calculation
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, sha256Digest);
                 OutputStream os = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

                byte[] buffer = new byte[8192];
                int read;
                while ((read = dis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }

            String sha256Hex = bytesToHex(sha256Digest.digest());
            String relativeStoragePath = (sanitizedSubDir.isEmpty() ? "" : sanitizedSubDir + "/") + storedFileName;

            log.info("[Storage] Saved document: original='{}', stored='{}', size={} bytes, sha256={}",
                    cleanOriginalName, relativeStoragePath, file.getSize(), sha256Hex);

            return new StoredFile(
                    cleanOriginalName,
                    storedFileName,
                    relativeStoragePath,
                    "application/pdf",
                    file.getSize(),
                    sha256Hex
            );

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible.", e);
        } catch (IOException e) {
            log.error("[Storage] Error al escribir documento en disco persistente: {}", e.getMessage(), e);
            throw new RuntimeException("Error al almacenar el documento en disco persistente.", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        StoredFile stored = storeEvidence(file, subDirectory);
        return stored.relativeStoragePath();
    }

    @Override
    public Resource loadFileAsResource(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta de archivo no especificada.");
        }

        try {
            String sanitized = sanitizeSubPath(relativePath);
            Path filePath = this.baseStorageLocation.resolve(sanitized).normalize();

            // Security boundary check against Path Traversal
            if (!filePath.startsWith(this.baseStorageLocation)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: ruta fuera del directorio permitido.");
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo físico no existe o no es accesible: " + relativePath);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El recurso no pudo ser leído: " + relativePath);
            }

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta malformada: " + relativePath, e);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName, String subDirectory) {
        String fullRelative = (subDirectory != null && !subDirectory.trim().isEmpty())
                ? subDirectory.trim() + "/" + fileName
                : fileName;
        return loadFileAsResource(fullRelative);
    }

    @Override
    public boolean verifyIntegrity(String relativePath, String expectedSha256) {
        if (expectedSha256 == null || expectedSha256.trim().isEmpty()) {
            return false;
        }
        try {
            Resource resource = loadFileAsResource(relativePath);
            try (InputStream is = resource.getInputStream()) {
                String actualSha256 = calculateSha256(is);
                return expectedSha256.equalsIgnoreCase(actualSha256);
            }
        } catch (Exception e) {
            log.warn("[Storage] Integrity check failed for {}: {}", relativePath, e.getMessage());
            return false;
        }
    }

    @Override
    public String calculateSha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("Error calculando SHA-256: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileName, String subDirectory) {
        String path = (subDirectory != null && !subDirectory.trim().isEmpty())
                ? subDirectory.trim() + "/" + fileName
                : fileName;
        deleteFile(path);
    }

    @Override
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) return;
        try {
            String sanitized = sanitizeSubPath(relativePath);
            Path filePath = this.baseStorageLocation.resolve(sanitized).normalize();
            if (filePath.startsWith(this.baseStorageLocation)) {
                Files.deleteIfExists(filePath);
                log.info("[Storage] Deleted physical file: {}", relativePath);
            }
        } catch (IOException e) {
            log.warn("[Storage] Could not delete physical file {}: {}", relativePath, e.getMessage());
        }
    }

    // ==========================================
    // Magic Bytes & Header Verification Helpers
    // ==========================================

    private byte[] readHeaderBytes(MultipartFile file, int length) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[length];
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo es demasiado pequeño o está corrupto.");
            }
            return Arrays.copyOf(header, bytesRead);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al leer encabezado binario del archivo.", e);
        }
    }

    private void validateMagicBytes(byte[] header, String extension, String contentType) {
        if (header == null || header.length < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo corrupto o ilegible.");
        }

        boolean isJpeg = isJpegHeader(header);
        boolean isPng = isPngHeader(header);
        boolean isWebp = isWebpHeader(header);

        if (!isJpeg && !isPng && !isWebp) {
            log.warn("[Storage Security] Spoofed file detected. Header bytes: {}", bytesToHex(header));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Contenido binario inválido. El archivo no corresponde a una imagen válida (JPEG, PNG o WEBP).");
        }

        // Match detected type against declared extension
        if (isJpeg && !(extension.equals(".jpg") || extension.equals(".jpeg"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extensión no coincide con el formato binario JPEG detectado.");
        }
        if (isPng && !extension.equals(".png")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extensión no coincide con el formato binario PNG detectado.");
        }
        if (isWebp && !extension.equals(".webp")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extensión no coincide con el formato binario WEBP detectado.");
        }
    }

    private boolean isPdfHeader(byte[] b) {
        // %PDF- starts with 0x25, 0x50, 0x44, 0x46 (ASCII: %PDF)
        return b != null && b.length >= 4 &&
                (b[0] & 0xFF) == 0x25 &&
                (b[1] & 0xFF) == 0x50 &&
                (b[2] & 0xFF) == 0x44 &&
                (b[3] & 0xFF) == 0x46;
    }

    private boolean isJpegHeader(byte[] b) {
        // JPEG starts with FF D8 FF
        return b.length >= 3 &&
                (b[0] & 0xFF) == 0xFF &&
                (b[1] & 0xFF) == 0xD8 &&
                (b[2] & 0xFF) == 0xFF;
    }

    private boolean isPngHeader(byte[] b) {
        // PNG starts with 89 50 4E 47 0D 0A 1A 0A
        return b.length >= 8 &&
                (b[0] & 0xFF) == 0x89 &&
                (b[1] & 0xFF) == 0x50 &&
                (b[2] & 0xFF) == 0x4E &&
                (b[3] & 0xFF) == 0x47 &&
                (b[4] & 0xFF) == 0x0D &&
                (b[5] & 0xFF) == 0x0A &&
                (b[6] & 0xFF) == 0x1A &&
                (b[7] & 0xFF) == 0x0A;
    }

    private boolean isWebpHeader(byte[] b) {
        // WEBP starts with 'RIFF' (52 49 46 46) and at offset 8 'WEBP' (57 45 42 50)
        if (b.length < 12) return false;
        boolean riff = (b[0] & 0xFF) == 0x52 && (b[1] & 0xFF) == 0x49 && (b[2] & 0xFF) == 0x46 && (b[3] & 0xFF) == 0x46;
        boolean webp = (b[8] & 0xFF) == 0x57 && (b[9] & 0xFF) == 0x45 && (b[10] & 0xFF) == 0x42 && (b[11] & 0xFF) == 0x50;
        return riff && webp;
    }

    private String sanitizeSubPath(String path) {
        if (path == null) return "";
        return path.replace('\\', '/')
                   .replaceAll("\\.\\.+", "")
                   .replaceAll("^/+", "")
                   .replaceAll("/+$", "")
                   .trim();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
