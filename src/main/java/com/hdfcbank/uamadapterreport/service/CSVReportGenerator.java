package com.hdfcbank.uamadapterreport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CSVReportGenerator {

    private final String delimiter;

    public CSVReportGenerator(@Value("${report.csv.delimiter:,}") String delimiter) {
        this.delimiter = delimiter;
    }

    public void generateCSVReport(List<Map<String, Object>> data, String filePath) throws IOException {
        try (
                OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(filePath), StandardCharsets.UTF_8)
        ) {
            // Write UTF-8 BOM
            writer.write('\uFEFF');

            if (data == null || data.isEmpty()) {
                writer.append("No data available\n");
                log.info("No data available for this report");
                return;
            }

            List<String> headers = data.get(0).keySet().stream().toList();
            writer.append(String.join(delimiter, headers)).append("\n");

            for (Map<String, Object> row : data) {
                StringBuilder rowBuilder = new StringBuilder();
                for (int i = 0; i < headers.size(); i++) {
                    Object value = row.get(headers.get(i));
                    String cell = value != null ? value.toString() : "";
                    cell = escapeCell(cell);
                    rowBuilder.append(cell);
                    if (i < headers.size() - 1) rowBuilder.append(delimiter);
                }
                writer.append(rowBuilder.toString()).append("\n");
            }

            writer.flush();
        }
    }

    private String escapeCell(String value) {
        // Properly quote if needed (for Excel + CSV compliance)
        if (value.contains(delimiter) || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
