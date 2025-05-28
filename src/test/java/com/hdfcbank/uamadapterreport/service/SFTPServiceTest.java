package com.hdfcbank.uamadapterreport.service;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import com.jcraft.jsch.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SFTPServiceTest {

    private SFTPService sftpService;

    @BeforeEach
    void setUp() {
        sftpService = new SFTPService();

        setPrivateField(sftpService, "sftpHost", "localhost");
        setPrivateField(sftpService, "sftpPort", 22);
        setPrivateField(sftpService, "sftpUsername", "user");
        setPrivateField(sftpService, "sftpPassword", "pass");
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "'", e);
        }
    }

    @Test
    void testUploadFile_success() throws Exception {
        File tempFile = File.createTempFile("test", ".csv");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("sample,data\n");
        }

        try (
                MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (jsch, context) -> {
                    Session session = mock(Session.class);
                    ChannelSftp channelSftp = mock(ChannelSftp.class);

                    when(jsch.getSession("user", "localhost", 22)).thenReturn(session);
                    doNothing().when(session).setPassword("pass");
                    doNothing().when(session).setConfig(any(Properties.class));
                    doNothing().when(session).connect();
                    when(session.openChannel("sftp")).thenReturn(channelSftp);
                    doNothing().when(channelSftp).connect();
                    doNothing().when(channelSftp).put(any(java.io.InputStream.class), eq("remote/test.csv"));
                    doNothing().when(channelSftp).disconnect();
                    doNothing().when(session).disconnect();
                })
        ) {
            sftpService.uploadFile(tempFile.getAbsolutePath(), "remote/test.csv");
        }

        tempFile.delete();
    }

    @Test
    void testUploadFile_connectionFailure_throwsCustomException() {
        File dummyFile = new File("nonexistent.csv");

        try (
                MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (jsch, context) -> {
                    when(jsch.getSession("user", "localhost", 22))
                            .thenThrow(new RuntimeException("Simulated connection failure"));
                })
        ) {
            assertThrows(CustomException.class,
                    () -> sftpService.uploadFile(dummyFile.getAbsolutePath(), "remote/test.csv"));
        }
    }

    @Test
    void testUploadFile_privateKeyAuthentication() throws Exception {
        File tempFile = File.createTempFile("test-key", ".csv");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("private,key,test\n");
        }

        setPrivateField(sftpService, "sftpPassword", null); // Simulate no password

        try (
                MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (jsch, context) -> {
                    Session session = mock(Session.class);
                    ChannelSftp channelSftp = mock(ChannelSftp.class);

                    doNothing().when(jsch).addIdentity(anyString(), anyString());
                    when(jsch.getSession("user", "localhost", 22)).thenReturn(session);
                    doNothing().when(session).setConfig(any(Properties.class));
                    doNothing().when(session).connect();
                    when(session.openChannel("sftp")).thenReturn(channelSftp);
                    doNothing().when(channelSftp).connect();
                    doNothing().when(channelSftp).put((String) any(), eq("remote/keytest.csv"));
                    doNothing().when(channelSftp).disconnect();
                    doNothing().when(session).disconnect();
                })
        ) {
            sftpService.uploadFile(tempFile.getAbsolutePath(), "remote/keytest.csv");
        }

        tempFile.delete();
    }

}
