package com.hdfcbank.uamadapterreport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
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
        try (FileWriter writer = new FileWriter(filePath)) {
            if (data == null || data.isEmpty()) {
                writer.append("\"No data available\"\n");
                log.info("No data available for this report");
                return;
            }

            List<String> headers = data.get(0).keySet().stream().toList();
            String headerLine = String.join(delimiter, headers);
            writer.append("\"").append(headerLine).append("\"\n");

            for (Map<String, Object> row : data) {
                StringBuilder rowBuilder = new StringBuilder();
                for (int i = 0; i < headers.size(); i++) {
                    Object value = row.get(headers.get(i));
                    String cell = value != null ? value.toString() : "";
                    cell = cell.replace("\"", "\"\""); // Escape quotes
                    rowBuilder.append(cell);
                    if (i < headers.size() - 1) rowBuilder.append(delimiter);
                }
                writer.append("\"").append(rowBuilder.toString()).append("\"\n");
            }
        }
    }
}
