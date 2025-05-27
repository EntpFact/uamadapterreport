package com.hdfcbank.uamadapterreport.service;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSVReportGeneratorTest {

    // Provide the delimiter directly for testing
    private final CSVReportGenerator generator = new CSVReportGenerator(",");

    @Test
    void testGenerateCSVReport() throws Exception {
        List<Map<String, Object>> data = List.of(
                Map.of("name", "Alice", "age", 30),
                Map.of("name", "Bob", "age", 25)
        );

        String filePath = "test-output.csv";
        generator.generateCSVReport(data, filePath);

        File file = new File(filePath);
        assertTrue(file.exists(), "CSV file was not created");

        List<String> lines = Files.readAllLines(file.toPath());

        assertTrue(lines.size() > 1, "CSV should contain header and at least one row");

        // Remove wrapping quotes for validation
        String header = lines.get(0).replace("\"", "");
        assertTrue(header.contains("name") && header.contains("age"), "Header does not contain expected columns");

        String row1 = lines.get(1).replace("\"", "");
        assertTrue(row1.contains("Alice") && row1.contains("30"), "First row does not contain expected data");

        String row2 = lines.get(2).replace("\"", "");
        assertTrue(row2.contains("Bob") && row2.contains("25"), "Second row does not contain expected data");

        file.delete(); // cleanup
    }

    @Test
    void testGenerateCSVReport_whenDataIsNull_writesNoDataAvailable() throws Exception {
        String filePath = "test-empty-null.csv";
        generator.generateCSVReport(null, filePath);

        File file = new File(filePath);
        assertTrue(file.exists(), "CSV file was not created for null data");

        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals(1, lines.size(), "CSV for null data should have exactly one line");
        assertEquals("\"No data available\"", lines.get(0), "File should contain no-data message");

        file.delete(); // cleanup
    }

    @Test
    void testGenerateCSVReport_whenDataIsEmpty_writesNoDataAvailable() throws Exception {
        String filePath = "test-empty-list.csv";
        generator.generateCSVReport(Collections.emptyList(), filePath);

        File file = new File(filePath);
        assertTrue(file.exists(), "CSV file was not created for empty list");

        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals(1, lines.size(), "CSV for empty list should have exactly one line");
        assertEquals("\"No data available\"", lines.get(0), "File should contain no-data message");

        file.delete(); // cleanup
    }
}
