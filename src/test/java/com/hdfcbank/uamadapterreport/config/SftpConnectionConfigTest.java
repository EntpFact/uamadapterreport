package com.hdfcbank.uamadapterreport.config;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

class SftpConnectionConfigTest {

    /**
     * Reflection helper to get private/protected field values by name.
     */
    private Object getField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the wrapped sessionFactory inside CachingSessionFactory using reflection.
     */
    private Object getSessionFactoryFromCachingSessionFactory(CachingSessionFactory<?> cachingFactory) {
        try {
            Field field = CachingSessionFactory.class.getDeclaredField("sessionFactory");
            field.setAccessible(true);
            return field.get(cachingFactory);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testSftpSessionFactory_withPassword() {
        SftpServerProps props = new SftpServerProps();
        props.setHost("localhost");
        props.setPort("22");
        props.setUserName("user");
        props.setPassword("pass");
        props.setPrivateKey(null);

        SftpConnectionConfig config = new SftpConnectionConfig();
        var sessionFactory = config.sftpSessionFactory(props);

        assertNotNull(sessionFactory);
        assertTrue(sessionFactory instanceof CachingSessionFactory);

        var actualFactory = getSessionFactoryFromCachingSessionFactory((CachingSessionFactory<?>) sessionFactory);
        assertTrue(actualFactory instanceof DefaultSftpSessionFactory);

        DefaultSftpSessionFactory defaultFactory = (DefaultSftpSessionFactory) actualFactory;
        assertEquals("localhost", getField(defaultFactory, "host"));
        assertEquals(22, getField(defaultFactory, "port"));
        assertEquals("user", getField(defaultFactory, "user"));
        assertEquals("pass", getField(defaultFactory, "password"));
        assertTrue((Boolean) getField(defaultFactory, "allowUnknownKeys"));
    }

    @Test
    void testSftpSessionFactory_withPrivateKey() {
        SftpServerProps props = new SftpServerProps();
        props.setHost("localhost");
        props.setPort("22");
        props.setUserName("user");
        props.setPassword(null);
        props.setPrivateKey("private-key-content");

        SftpConnectionConfig config = new SftpConnectionConfig();
        var sessionFactory = config.sftpSessionFactory(props);

        assertNotNull(sessionFactory);

        var actualFactory = getSessionFactoryFromCachingSessionFactory((CachingSessionFactory<?>) sessionFactory);
        assertTrue(actualFactory instanceof DefaultSftpSessionFactory);

        DefaultSftpSessionFactory defaultFactory = (DefaultSftpSessionFactory) actualFactory;
        assertEquals("localhost", getField(defaultFactory, "host"));
        assertEquals(22, getField(defaultFactory, "port"));
        assertEquals("user", getField(defaultFactory, "user"));
        assertNotNull(getField(defaultFactory, "privateKey"));
        assertTrue((Boolean) getField(defaultFactory, "allowUnknownKeys"));
    }

    @Test
    void testSftpRemoteFileTemplateBean() {
        SftpServerProps props = new SftpServerProps();
        props.setHost("localhost");
        props.setPort("22");
        props.setUserName("user");
        props.setPassword("pass");

        SftpConnectionConfig config = new SftpConnectionConfig();
        var sessionFactory = config.sftpSessionFactory(props);
        SftpRemoteFileTemplate template = config.sftpRemoteFileTemplate(sessionFactory);

        assertNotNull(template);
        // Verify the session factory is set internally
        assertEquals(sessionFactory, template.getSessionFactory());
    }
}
