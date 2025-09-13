package ru.netology.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.dto.FileInfoDto;
import ru.netology.entity.FileEntity;
import ru.netology.entity.User;
import ru.netology.repository.FileRepository;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileOperationService {
    private static final Logger logger = LoggerFactory.getLogger(FileOperationService.class);

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    public FileOperationService(FileRepository fileRepository, FileStorageService fileStorageService) {
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<FileInfoDto> getUserFiles(User user, int limit) {
        List<FileEntity> files = fileStorageService.loadAll(user);

        return files.stream()
                .limit(limit)
                .map(file -> new FileInfoDto(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
    }

    public void uploadFile(MultipartFile file, String filename, User user) throws IOException {
        fileStorageService.store(file, filename, user);
    }

    public FileEntity getFileForDownload(String filename, User user) {
        return fileStorageService.load(filename, user)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    public void deleteFile(String filename, User user) throws IOException {
        fileStorageService.delete(filename, user);
    }

    public void renameFile(String oldFilename, String newFilename, User user) throws IOException {
        String decodedFilename = decodeFilename(oldFilename);

        FileEntity fileEntity = fileStorageService.load(decodedFilename, user)
                .orElseThrow(() -> new RuntimeException("File not found: " + decodedFilename));

        validateNewFilename(newFilename, user);

        Path oldPath = Paths.get(fileEntity.getFilePath());
        Path newPath = oldPath.resolveSibling(newFilename);

        validateFileExists(oldPath);
        moveFile(oldPath, newPath);

        updateFileEntity(fileEntity, newFilename, newPath.toString());

        logger.info("File successfully renamed from '{}' to '{}'", decodedFilename, newFilename);
    }

    private String decodeFilename(String filename) throws UnsupportedEncodingException {
        return URLDecoder.decode(filename, StandardCharsets.UTF_8.toString());
    }

    private void validateNewFilename(String newFilename, User user) {
        if (fileRepository.existsByUserAndFilename(user, newFilename)) {
            throw new RuntimeException("File with name '" + newFilename + "' already exists");
        }
    }

    private void validateFileExists(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Source file not found on disk: " + filePath);
        }
    }

    private void moveFile(Path oldPath, Path newPath) throws IOException {
        Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void updateFileEntity(FileEntity fileEntity, String newFilename, String newFilePath) {
        fileEntity.setFilename(newFilename);
        fileEntity.setFilePath(newFilePath);
        fileRepository.save(fileEntity);
    }
}