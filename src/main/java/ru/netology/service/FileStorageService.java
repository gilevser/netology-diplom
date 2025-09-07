package ru.netology.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.entity.FileEntity;
import ru.netology.entity.User;
import ru.netology.repository.FileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FileStorageService {
    private final FileRepository fileRepository;
    private final Path rootLocation;

    public FileStorageService(FileRepository fileRepository,
                              @Value("${cloud.storage.path}") String storagePath) {
        this.fileRepository = fileRepository;
        this.rootLocation = Paths.get(storagePath);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public void store(MultipartFile file, String filename, User user) throws IOException {
        Path userDir = rootLocation.resolve(user.getId().toString());
        Files.createDirectories(userDir);

        Path destinationFile = userDir.resolve(filename);
        Files.copy(file.getInputStream(), destinationFile);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(filename);
        fileEntity.setSize(file.getSize());
        fileEntity.setFilePath(destinationFile.toString());
        fileEntity.setUser(user);
        fileEntity.setCreatedDate(LocalDateTime.now());

        fileRepository.save(fileEntity);
    }

    public List<FileEntity> loadAll(User user) {
        try {
            return fileRepository.findByUserOrderByCreatedDateDesc(user);
        } catch (Exception e) {
            System.out.println("Error loading files for user " + user.getLogin() + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<FileEntity> load(String filename, User user) {
        return fileRepository.findByUserAndFilename(user, filename);
    }

    public void delete(String filename, User user) throws IOException {
        FileEntity fileEntity = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Files.deleteIfExists(Paths.get(fileEntity.getFilePath()));
        fileRepository.delete(fileEntity);
    }
}