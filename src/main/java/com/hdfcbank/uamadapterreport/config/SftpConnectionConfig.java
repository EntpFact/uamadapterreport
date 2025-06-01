package com.hdfcbank.uamadapterreport.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.apache.sshd.sftp.client.SftpClient;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class SftpConnectionConfig {

    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory(SftpServerProps sftpProps) {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(sftpProps.getHost());
        factory.setPort(Integer.parseInt(sftpProps.getPort()));
        factory.setUser(sftpProps.getUserName());

        if (sftpProps.getPassword() != null && !sftpProps.getPassword().isBlank()) {
            factory.setPassword(sftpProps.getPassword());
            log.info("Password authentication enabled");
        }

        if (sftpProps.getPrivateKey() != null && !sftpProps.getPrivateKey().isBlank()) {
            Resource privateKeyResource = new ByteArrayResource(sftpProps.getPrivateKey().getBytes(StandardCharsets.UTF_8));
            factory.setPrivateKey(privateKeyResource);
            log.info("Private key authentication enabled");
        }

        factory.setAllowUnknownKeys(true); // disables strict host key checking

        return new CachingSessionFactory<>(factory);
    }

    @Bean
    public SftpRemoteFileTemplate sftpRemoteFileTemplate(SessionFactory<SftpClient.DirEntry> sftpSessionFactory) {
        return new SftpRemoteFileTemplate(sftpSessionFactory);
    }
}
