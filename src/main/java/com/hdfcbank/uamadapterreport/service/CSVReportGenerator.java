package com.hdfcbank.uamadapterreport.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CSVReportGenerator {

    private final char delimiter;

    public CSVReportGenerator(@Value("${report.csv.delimiter:,}") String delimiter) {
        this.delimiter = delimiter.charAt(0);
    }

    public void generateCSVReport(List<Map<String, Object>> data, String filePath) throws IOException {
        if (data == null || data.isEmpty()) {
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write("No data available\n");
            }
            return;
        }

        List<String> headers = data.get(0).keySet().stream().toList();

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(filePath),
                CSVFormat.DEFAULT.withDelimiter(delimiter).withHeader(headers.toArray(new String[0])))) {

            for (Map<String, Object> row : data) {
                printer.printRecord(headers.stream()
                        .map(h -> row.getOrDefault(h, ""))
                        .toList());
            }
        }
    }
}
