package ru.netology.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.entity.FileEntity;
import ru.netology.entity.User;
import ru.netology.repository.FileRepository;
import ru.netology.repository.UserRepository;
import ru.netology.security.JwtTokenUtil;
import ru.netology.service.FileStorageService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FileController {
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public FileController(FileStorageService fileStorageService,
                          UserRepository userRepository,
                          FileRepository fileRepository,
                          JwtTokenUtil jwtTokenUtil) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    private User getUserFromToken(String authToken) {
        String token = authToken.startsWith("Bearer ") ? authToken.substring(7) : authToken;
        String username = jwtTokenUtil.getUsernameFromToken(token);
        return userRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFileList(@RequestHeader("auth-token") String authToken,
                                         @RequestParam(defaultValue = "10") int limit) {
        try {
            User user = getUserFromToken(authToken);
            List<FileEntity> files = fileStorageService.loadAll(user);

            if (files == null) {
                files = new ArrayList<>();
            }

            if (files.size() > limit) {
                files = files.subList(0, limit);
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (FileEntity file : files) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("filename", file.getFilename());
                fileInfo.put("size", file.getSize());
                response.add(fileInfo);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Error in /list: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestHeader("auth-token") String authToken,
                                        @RequestParam String filename,
                                        @RequestParam("file") MultipartFile file) {
        try {
            User user = getUserFromToken(authToken);
            fileStorageService.store(file, filename, user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            return ResponseEntity.ok().body(response);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "File upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping(value = "/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> downloadFile(@RequestHeader("auth-token") String authToken,
                                          @RequestParam String filename) {
        try {
            User user = getUserFromToken(authToken);
            FileEntity fileEntity = fileStorageService.load(filename, user)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            Path filePath = Paths.get(fileEntity.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileEntity.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid file path");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(@RequestHeader("auth-token") String authToken,
                                        @RequestParam String filename) {
        try {
            User user = getUserFromToken(authToken);
            fileStorageService.delete(filename, user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File deleted successfully");
            return ResponseEntity.ok().body(response);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "File deletion failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/file")
    public ResponseEntity<?> renameFile(@RequestHeader("auth-token") String authToken,
                                        @RequestParam String filename,
                                        @RequestBody Map<String, String> request) {
        try {
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8.toString());
            System.out.println("PUT /file request - rename from: '" + decodedFilename + "'");
            System.out.println("Request body: " + request);

            if (request == null || request.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Request body is required");
                return ResponseEntity.badRequest().body(error);
            }

            String newFilename = request.get("filename");

            if (newFilename == null || newFilename.isBlank()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "New filename is required in 'filename' field. Use format: {'filename': 'newfilename.txt'}");
                return ResponseEntity.badRequest().body(error);
            }

            System.out.println("New filename: " + newFilename);

            User user = getUserFromToken(authToken);
            System.out.println("User: " + user.getLogin());

            FileEntity fileEntity = fileStorageService.load(decodedFilename, user)
                    .orElseThrow(() -> {
                        System.out.println("File not found: " + decodedFilename);
                        return new RuntimeException("File not found: " + decodedFilename);
                    });

            System.out.println("Found file: " + fileEntity.getFilename() + ", path: " + fileEntity.getFilePath());

            if (fileRepository.existsByUserAndFilename(user, newFilename)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "File with name '" + newFilename + "' already exists");
                return ResponseEntity.badRequest().body(error);
            }

            Path oldPath = Paths.get(fileEntity.getFilePath());
            Path newPath = oldPath.resolveSibling(newFilename);
            System.out.println("Moving file from: " + oldPath + " to: " + newPath);

            if (!Files.exists(oldPath)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Source file not found on disk: " + oldPath);
                return ResponseEntity.badRequest().body(error);
            }

            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);

            fileEntity.setFilename(newFilename);
            fileEntity.setFilePath(newPath.toString());
            fileRepository.save(fileEntity);

            System.out.println("File successfully renamed to: " + newFilename);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File renamed successfully");
            response.put("oldFilename", decodedFilename);
            response.put("newFilename", newFilename);
            return ResponseEntity.ok().body(response);

        } catch (UnsupportedEncodingException e) {
            System.out.println("Encoding error: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Filename encoding error");
            return ResponseEntity.badRequest().body(error);

        } catch (NoSuchFileException e) {
            System.out.println("File not found on disk: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "File not found on disk");
            return ResponseEntity.badRequest().body(error);

        } catch (IOException e) {
            System.out.println("IOException during rename: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "File operation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            System.out.println("Unexpected exception during rename: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Internal server error: " + e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}