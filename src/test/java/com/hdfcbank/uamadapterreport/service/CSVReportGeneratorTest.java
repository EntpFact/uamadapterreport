package com.hdfcbank.uamadapterreport.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CSVReportGeneratorTest {

    private final String delimiter = ",";
    private final CSVReportGenerator csvReportGenerator = new CSVReportGenerator(delimiter);
    private File tempFile;

    @AfterEach
    void cleanUp() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void testGenerateCSVReport_withData_shouldGenerateCSVWithBOM() throws IOException {
        // Given
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("Name", "John, Doe");
        row1.put("Age", 30);
        row1.put("Note", "Line\nBreak");
        data.add(row1);

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("Name", "Jane \"JJ\" Smith");
        row2.put("Age", 28);
        row2.put("Note", "Normal");
        data.add(row2);

        tempFile = File.createTempFile("test-report", ".csv");

        // When
        csvReportGenerator.generateCSVReport(data, tempFile.getAbsolutePath());

        // Then
        byte[] fileBytes = Files.readAllBytes(tempFile.toPath());

        // Check BOM
        assertEquals((byte)0xEF, fileBytes[0]);
        assertEquals((byte)0xBB, fileBytes[1]);
        assertEquals((byte)0xBF, fileBytes[2]);

        String content = new String(fileBytes, StandardCharsets.UTF_8);
        System.out.println("Generated CSV:\n" + content);

        assertTrue(content.contains("Name,Age,Note"));
        assertTrue(content.contains("\"John, Doe\",30,\"Line\nBreak\""));
        assertTrue(content.contains("\"Jane \"\"JJ\"\" Smith\",28,Normal"));
    }

    @Test
    void testGenerateCSVReport_emptyData_shouldWriteNoDataLine() throws IOException {
        // Given
        List<Map<String, Object>> emptyData = Collections.emptyList();
        tempFile = File.createTempFile("test-empty-report", ".csv");

        // When
        csvReportGenerator.generateCSVReport(emptyData, tempFile.getAbsolutePath());

        // Then
        String content = Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(content.startsWith("\uFEFF"));
        assertTrue(content.contains("No data available"));
    }
}
