package com.hdfcbank.uamadapterreport.service;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
@RequiredArgsConstructor
public class SFTPService {

    private final SftpRemoteFileTemplate sftpRemoteFileTemplate;

    public void uploadFile(String localFilePath, String remoteFilePath) {
        try {
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("Local file does not exist: " + localFilePath);
            }

            sftpRemoteFileTemplate.execute(session -> {
                try (var inputStream = new java.io.FileInputStream(file)) {
                    session.write(inputStream, remoteFilePath);
                    log.info("File uploaded to SFTP at path: {}", remoteFilePath);
                    return null;
                }
            });

        } catch (Exception e) {
            log.error("SFTP upload failed for file {}: {}", localFilePath, e.getMessage(), e);
            throw new CustomException("SFTP upload failed for file: " + localFilePath, e);
        }
    }

}
