package com.hdfcbank.uamadapterreport.service;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

import java.io.File;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SFTPServiceTest {

    @Mock
    private SftpRemoteFileTemplate sftpRemoteFileTemplate;

    @InjectMocks
    private SFTPService sftpService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUploadFile_success() {
        String localFilePath = "src/test/resources/test-upload.txt";
        String remoteFilePath = "remote/dir/test-upload.txt";

        // Ensure the test file exists
        File testFile = new File(localFilePath);
        testFile.getParentFile().mkdirs();
        try {
            if (!testFile.exists()) {
                assertTrue(testFile.createNewFile());
                testFile.deleteOnExit();
            }
        } catch (Exception e) {
            fail("Failed to create test file: " + e.getMessage());
        }

        // Mock the execute method, no explicit SessionCallback class needed
        when(sftpRemoteFileTemplate.execute(any())).thenReturn(null);

        // Verify no exception is thrown during upload
        assertDoesNotThrow(() -> sftpService.uploadFile(localFilePath, remoteFilePath));

        // Verify execute was called exactly once
        verify(sftpRemoteFileTemplate, times(1)).execute(any());
    }

    @Test
    void testUploadFile_fileNotFound_shouldThrowException() {
        String nonExistentPath = "non/existent/file.txt";
        String remoteFilePath = "remote/dir/file.txt";

        CustomException exception = assertThrows(CustomException.class, () ->
                sftpService.uploadFile(nonExistentPath, remoteFilePath)
        );

        assertTrue(exception.getMessage().contains("SFTP upload failed for file"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("Local file does not exist"));

        verify(sftpRemoteFileTemplate, never()).execute(any());
    }

    @Test
    void testUploadFile_sftpExecuteFails_shouldThrowCustomException() {
        String localFilePath = "src/test/resources/test-fail.txt";
        String remoteFilePath = "remote/dir/test-fail.txt";

        // Create dummy test file if it doesn't exist
        File testFile = new File(localFilePath);
        testFile.getParentFile().mkdirs();
        try {
            if (!testFile.exists()) {
                assertTrue(testFile.createNewFile());
                testFile.deleteOnExit();
            }
        } catch (Exception e) {
            fail("Failed to create test file: " + e.getMessage());
        }

        // Make execute throw an exception to simulate SFTP failure
        doThrow(new RuntimeException("Simulated SFTP failure"))
                .when(sftpRemoteFileTemplate).execute(any());

        CustomException ex = assertThrows(CustomException.class, () ->
                sftpService.uploadFile(localFilePath, remoteFilePath)
        );

        assertTrue(ex.getMessage().contains("SFTP upload failed"));
        verify(sftpRemoteFileTemplate, times(1)).execute(any());
    }

}
