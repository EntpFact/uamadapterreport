package com.hdfcbank.uamadapterreport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object used by the email sender service to return email status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response {

    private String status;
    private String message;
}
