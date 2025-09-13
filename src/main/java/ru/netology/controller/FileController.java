package ru.netology.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.netology.dto.FileInfoDto;
import ru.netology.dto.FileResponse;
import ru.netology.dto.RenameRequest;
import ru.netology.entity.User;
import ru.netology.security.JwtTokenUtil;
import ru.netology.service.FileOperationService;
import ru.netology.repository.UserRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileOperationService fileOperationService;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public FileController(FileOperationService fileOperationService,
                          UserRepository userRepository,
                          JwtTokenUtil jwtTokenUtil) {
        this.fileOperationService = fileOperationService;
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    private User getUserFromToken(String authToken) {
        String token = authToken.startsWith("Bearer ") ? authToken.substring(7) : authToken;
        String username = jwtTokenUtil.getUsernameFromToken(token);
        return userRepository.findByLogin(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfoDto>> getFileList(@RequestHeader("auth-token") String authToken,
                                                         @RequestParam(defaultValue = "10") int limit) {
        try {
            User user = getUserFromToken(authToken);
            List<FileInfoDto> files = fileOperationService.getUserFiles(user, limit);
            return ResponseEntity.ok(files);

        } catch (Exception e) {
            logger.error("Error retrieving file list for user", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> uploadFile(@RequestHeader("auth-token") String authToken,
                                                   @RequestParam String filename,
                                                   @RequestParam("file") MultipartFile file) {
        try {
            User user = getUserFromToken(authToken);
            fileOperationService.uploadFile(file, filename, user);

            return ResponseEntity.ok(new FileResponse("File uploaded successfully"));

        } catch (IOException e) {
            logger.warn("File upload failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File upload failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during file upload", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @GetMapping(value = "/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> downloadFile(@RequestHeader("auth-token") String authToken,
                                          @RequestParam String filename) {
        try {
            User user = getUserFromToken(authToken);
            var fileEntity = fileOperationService.getFileForDownload(filename, user);

            Path filePath = Paths.get(fileEntity.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found or not readable");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileEntity.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            logger.warn("Invalid file path for download: {}", filename);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        } catch (Exception e) {
            logger.error("Error downloading file: {}", filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading file");
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<FileResponse> deleteFile(@RequestHeader("auth-token") String authToken,
                                                   @RequestParam String filename) {
        try {
            User user = getUserFromToken(authToken);
            fileOperationService.deleteFile(filename, user);

            return ResponseEntity.ok(new FileResponse("File deleted successfully"));

        } catch (IOException e) {
            logger.warn("File deletion failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File deletion failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting file: {}", filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting file");
        }
    }

    @PutMapping("/file")
    public ResponseEntity<FileResponse> renameFile(@RequestHeader("auth-token") String authToken,
                                                   @RequestParam String filename,
                                                   @RequestBody RenameRequest request) {
        try {
            User user = getUserFromToken(authToken);
            fileOperationService.renameFile(filename, request.getFilename(), user);

            Map<String, Object> details = new HashMap<>();
            details.put("oldFilename", filename);
            details.put("newFilename", request.getFilename());

            return ResponseEntity.ok(new FileResponse("File renamed successfully", details));

        } catch (Exception e) {
            logger.error("Error renaming file: {}", filename, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}