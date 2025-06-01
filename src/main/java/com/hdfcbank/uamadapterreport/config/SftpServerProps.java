package com.hdfcbank.uamadapterreport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpServerProps {
    private String serverName;
    private String host;
    private String port;
    private String userName;
    private String password;
    private String privateKey; // PEM string content
}
