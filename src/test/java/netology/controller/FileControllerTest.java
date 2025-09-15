package netology.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.netology.controller.FileController;
import ru.netology.dto.FileInfoDto;
import ru.netology.dto.FileResponse;
import ru.netology.dto.RenameRequest;
import ru.netology.entity.FileEntity;
import ru.netology.entity.User;
import ru.netology.security.JwtTokenUtil;
import ru.netology.service.FileOperationService;
import ru.netology.repository.UserRepository;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileOperationService fileOperationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private FileController fileController;

    private User testUser;
    private final String testToken = "valid-token";
    private final String testFilename = "test.txt";
    private final String testNewFilename = "renamed.txt";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setLogin("testuser");
    }

    @Test
    void getFileList_ShouldReturnFiles_WhenTokenIsValid() {
        when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

        FileInfoDto fileInfo = new FileInfoDto(testFilename, 1024L);
        when(fileOperationService.getUserFiles(testUser, 10)).thenReturn(List.of(fileInfo));

        ResponseEntity<List<FileInfoDto>> response = fileController.getFileList("Bearer " + testToken, 10);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testFilename, response.getBody().get(0).getFilename());

        verify(jwtTokenUtil).getUsernameFromToken(testToken);
        verify(userRepository).findByLogin(testUser.getLogin());
        verify(fileOperationService).getUserFiles(testUser, 10);
    }

    @Test
    void getFileList_ShouldReturnEmptyList_WhenUserHasNoFiles() {
        when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
        when(fileOperationService.getUserFiles(testUser, 10)).thenReturn(List.of());

        ResponseEntity<List<FileInfoDto>> response = fileController.getFileList("Bearer " + testToken, 10);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
void uploadFile_ShouldReturnSuccess_WhenFileIsValid() throws IOException {
    when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
    when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

    MultipartFile mockFile = new MockMultipartFile(
        "file", testFilename, "text/plain", "test content".getBytes()
    );

    ResponseEntity<FileResponse> response = fileController.uploadFile(
        "Bearer " + testToken, testFilename, mockFile
    );

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("File uploaded successfully", response.getBody().getMessage());

    verify(fileOperationService).uploadFile(mockFile, testFilename, testUser);
}

@Test
void uploadFile_ShouldThrowBadRequest_WhenIOExceptionOccurs() throws IOException {
    when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
    when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

    MultipartFile mockFile = new MockMultipartFile(
        "file", testFilename, "text/plain", "test content".getBytes()
    );

    doThrow(new IOException("Storage error")).when(fileOperationService)
        .uploadFile(any(), anyString(), any());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        fileController.uploadFile("Bearer " + testToken, testFilename, mockFile);
    });

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertTrue(exception.getReason().contains("File upload failed"));
}

    @Test
    void downloadFile_ShouldReturnFile_WhenFileExists() throws Exception {
        when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

        Path tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, "тестовое содержимое".getBytes());

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(testFilename);
        fileEntity.setFilePath(tempFile.toString());

        when(fileOperationService.getFileForDownload(testFilename, testUser))
                .thenReturn(fileEntity);

        ResponseEntity<?> response = fileController.downloadFile(
                "Bearer " + testToken, testFilename
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Resource);

        Files.deleteIfExists(tempFile);
    }

    @Test
    void downloadFile_ShouldThrowInternalError_WhenFileNotReadable() throws Exception {
        when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(testFilename);
        fileEntity.setFilePath("/invalid/path.txt");

        when(fileOperationService.getFileForDownload(testFilename, testUser))
                .thenReturn(fileEntity);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            fileController.downloadFile("Bearer " + testToken, testFilename);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Error downloading file"));
    }

    @Test
void deleteFile_ShouldReturnSuccess_WhenFileExists() throws IOException {
    // Arrange
    when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
    when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

    // Act
    ResponseEntity<FileResponse> response = fileController.deleteFile(
        "Bearer " + testToken, testFilename
    );

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("File deleted successfully", response.getBody().getMessage());

    verify(fileOperationService).deleteFile(testFilename, testUser);
}

@Test
void deleteFile_ShouldThrowBadRequest_WhenIOExceptionOccurs() throws IOException {
    when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
    when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

    doThrow(new IOException("Delete error")).when(fileOperationService)
        .deleteFile(anyString(), any());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        fileController.deleteFile("Bearer " + testToken, testFilename);
    });

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
}

    @Test
void renameFile_ShouldReturnSuccess_WhenRenameIsValid() throws IOException {
    when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
    when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

    RenameRequest request = new RenameRequest();
    request.setFilename(testNewFilename);

    ResponseEntity<FileResponse> response = fileController.renameFile(
        "Bearer " + testToken, testFilename, request
    );

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("File renamed successfully", response.getBody().getMessage());
    assertNotNull(response.getBody().getDetails());

    verify(fileOperationService).renameFile(testFilename, testNewFilename, testUser);
}

@Test
void renameFile_ShouldThrowBadRequest_WhenServiceThrowsException() throws IOException {
    when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn(testUser.getLogin());
    when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

    RenameRequest request = new RenameRequest();
    request.setFilename(testNewFilename);

    doThrow(new RuntimeException("Rename error")).when(fileOperationService)
        .renameFile(anyString(), anyString(), any());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        fileController.renameFile("Bearer " + testToken, testFilename, request);
    });

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
}

    @Test
    void anyEndpoint_ShouldThrowUnauthorized_WhenUserNotFound() throws Exception {
        when(jwtTokenUtil.getUsernameFromToken(anyString())).thenReturn("nonexistent");
        when(userRepository.findByLogin("nonexistent")).thenReturn(Optional.empty());

        Method getUserFromToken = FileController.class.getDeclaredMethod("getUserFromToken", String.class);
        getUserFromToken.setAccessible(true);

        InvocationTargetException invocationException = assertThrows(InvocationTargetException.class, () -> {
            getUserFromToken.invoke(fileController, "Bearer " + testToken);
        });

        Throwable actualException = invocationException.getCause();
        assertInstanceOf(ResponseStatusException.class, actualException);

        ResponseStatusException responseException = (ResponseStatusException) actualException;
        assertEquals(HttpStatus.UNAUTHORIZED, responseException.getStatusCode());
        assertEquals("User not found", responseException.getReason());
    }
}