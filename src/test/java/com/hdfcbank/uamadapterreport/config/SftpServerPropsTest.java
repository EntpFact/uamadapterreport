package com.hdfcbank.uamadapterreport.config;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;


class SftpServerPropsTest {

    @Test
    void testBinding() {
        Map<String, String> props = new HashMap<>();
        props.put("sftp.serverName", "myServer");
        props.put("sftp.host", "localhost");
        props.put("sftp.port", "22");
        props.put("sftp.userName", "user");
        props.put("sftp.password", "pass");
        props.put("sftp.privateKey", "private-key-content");

        MapConfigurationPropertySource source = new MapConfigurationPropertySource(props);
        Binder binder = new Binder(source);

        BindResult<SftpServerProps> bindResult = binder.bind("sftp", SftpServerProps.class);

        assertTrue(bindResult.isBound());
        SftpServerProps sftpProps = bindResult.get();

        assertEquals("myServer", sftpProps.getServerName());
        assertEquals("localhost", sftpProps.getHost());
        assertEquals("22", sftpProps.getPort());
        assertEquals("user", sftpProps.getUserName());
        assertEquals("pass", sftpProps.getPassword());
        assertEquals("private-key-content", sftpProps.getPrivateKey());
    }
}
