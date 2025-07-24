package com.hdfcbank.uamadapterreport.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportRequest {
    private String reportDate; // Format: YYYY-MM-DD
}

