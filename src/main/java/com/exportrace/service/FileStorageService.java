package com.exportrace.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String subDirectory);
    Resource loadFileAsResource(String fileName, String subDirectory);
    void deleteFile(String fileName, String subDirectory);
}
