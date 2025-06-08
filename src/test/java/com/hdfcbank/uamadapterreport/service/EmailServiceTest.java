package com.hdfcbank.uamadapterreport.service;

import com.hdfcbank.ef.apiconnect.builder.GetRequest;
import com.hdfcbank.ef.apiconnect.builder.PostRequest;
import com.hdfcbank.ef.apiconnect.service.APIClient;
import com.hdfcbank.uamadapterreport.config.MultiPathProperties;
import com.hdfcbank.uamadapterreport.config.MultiPathProperties.AppEmailConfig;
import com.hdfcbank.uamadapterreport.config.MultiPathProperties.AppEmailConfig.EmailProps;
import com.hdfcbank.uamadapterreport.exception.CustomException;
import com.hdfcbank.uamadapterreport.model.Response;
import com.hdfcbank.uamadapterreport.util.TemplateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private Environment env;

    @Mock
    private MultiPathProperties multiPathProperties;

    @Mock
    private APIClient apiClient;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        Field urlField = EmailService.class.getDeclaredField("emailRestURL");
        urlField.setAccessible(true);
        urlField.set(emailService, "http://mock-url/email");

    }

    @Test
    void testSendEmailSuccess() {
        String app = "testapp";
        String appId = "1001";
        String path = "/test/path";
        List<String> files = List.of("file1.csv", "file2.csv");

        EmailProps emailProps = new EmailProps();
        emailProps.setTo("to@bank.com");
        emailProps.setFrom("from@bank.com");
        emailProps.setCc("cc@bank.com");
        emailProps.setBcc("bcc@bank.com");
        emailProps.setSubject("Test Subject");
        emailProps.setReachOutEmail("help@bank.com");
        AppEmailConfig config = new AppEmailConfig();
        config.setSuccess(emailProps);


        when(multiPathProperties.getApplications()).thenReturn(Map.of(app, config));

        try (MockedStatic<TemplateUtil> mockedStatic = mockStatic(TemplateUtil.class)) {
            mockedStatic.when(() ->
                    TemplateUtil.getEmailBodyFromEnvTemplate(any(), any(), any(), any(), any(), any(), any())
            ).thenReturn("Mock email body");

            Response response = new Response();
            response.setStatus("SUCCESS");
            response.setMessage("Sent");

            when(apiClient.execute(any(PostRequest.class))).thenReturn(Mono.just(response));

            assertDoesNotThrow(() -> emailService.sendEmail("SUCCESS", app, appId, path, files));
        }
    }




    @Test
    void testSendEmailInvalidStatus() {
        emailService.sendEmail("FAILURE", "testapp", "101", null, null);
        verify(apiClient, never()).execute((GetRequest) any());
    }

    @Test
    void testSendEmailInvalidAppConfig() {
        when(multiPathProperties.getApplications()).thenReturn(Map.of());
        assertThrows(CustomException.class, () ->
                emailService.sendEmail("SUCCESS", "invalidApp", "id", null, null));
    }


    @Test
    void testSendEmailMapWrapper() {
        EmailService spy = Mockito.spy(emailService);
        doNothing().when(spy).sendEmail(any(), any(), any(), any(), any());

        Map<String, Object> headers = Map.of(
                "status", "SUCCESS",
                "applicationName", "app",
                "applicationId", "id",
                "sftpPath", "/path",
                "fileNames", List.of("f1", "f2")
        );

        spy.sendEmail(headers);
        verify(spy).sendEmail("SUCCESS", "app", "id", "/path", List.of("f1", "f2"));
    }

    private void mockStaticTemplateUtil(String returnValue) {
        mockStatic(TemplateUtil.class);
        when(TemplateUtil.getEmailBodyFromEnvTemplate(
                any(), any(), any(), any(), any(), any(), any())

        ).thenReturn(returnValue);
    }
}
