package com.hdfcbank.uamadapterreport.service;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
@Service
@Slf4j
public class SFTPService {

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port:22}")
    private int sftpPort;

    @Value("${sftp.username}")
    private String sftpUsername;

    @Value("${sftp.password}")
    private String sftpPassword;

    @Value("${sftp.private-key-path}")
    private String privateKeyPath;

    @Value("${sftp.private-key-passphrase}")
    private String privateKeyPassphrase;

    public void uploadFile(String localFilePath, String remoteFilePath) {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();

            // Load private key with or without passphrase
            if (privateKeyPassphrase != null && !privateKeyPassphrase.isBlank()) {
                jsch.addIdentity(privateKeyPath, privateKeyPassphrase);
            } else {
                jsch.addIdentity(privateKeyPath);
            }

            session = jsch.getSession(sftpUsername, sftpHost, sftpPort);

            // Set password as well (JSch may ignore it if key is accepted)
            session.setPassword(sftpPassword);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            try (InputStream inputStream = new FileInputStream(localFilePath)) {
                channelSftp.put(inputStream, remoteFilePath);
            }

            log.info("File uploaded successfully to SFTP: {}", remoteFilePath);

        } catch (Exception e) {
            log.error("SFTP upload failed: {}", e.getMessage(), e);
            throw new CustomException("SFTP upload failed for file: " + localFilePath, e);
        } finally {
            if (channelSftp != null) {
                channelSftp.exit();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
