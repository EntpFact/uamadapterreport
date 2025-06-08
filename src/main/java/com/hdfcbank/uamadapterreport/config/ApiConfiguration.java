package com.hdfcbank.uamadapterreport.config;
import com.hdfcbank.ef.apiconnect.config.ApiConnectConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
@Configuration
@Import({ApiConnectConfig.class})
public class ApiConfiguration {

}
