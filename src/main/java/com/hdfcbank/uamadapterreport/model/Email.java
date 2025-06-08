package com.hdfcbank.uamadapterreport.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Email {
    private String to;
    private String from;
    private String cc;
    private String bcc;
    private String subject;
    private String body;
    private String status;
    private String reconId;

}
