package com.qagenie.testbe.project.tls;

import com.qagenie.testbe.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores uploaded keystore/truststore files under a private directory on
 * disk (never in a DB column), keyed by PROJECT id - one shared keystore/
 * truststore per Project, reused by every Application/Environment under
 * it. In production, prefer a secrets manager (Vault/KMS) over filesystem
 * storage; this is a pragmatic default for a single-node deployment.
 */
@Component
public class TlsMaterialService {

    @Value("${qagenie.certs.storage-dir:/var/qagenie/certs}")
    private String storageDir;

    public String storeKeystore(Long projectId, MultipartFile file, String kind) {
        try {
            Path dir = Path.of(storageDir, "project-" + projectId);
            Files.createDirectories(dir);
            String safeName = kind + "-" + System.currentTimeMillis() + "-" + sanitize(file.getOriginalFilename());
            Path target = dir.resolve(safeName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new BusinessException("Unable to store " + kind + " file: " + e.getMessage(), "TLS_STORE_ERROR");
        }
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
