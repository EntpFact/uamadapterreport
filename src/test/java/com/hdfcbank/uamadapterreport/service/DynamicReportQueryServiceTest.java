package com.hdfcbank.uamadapterreport.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DynamicReportQueryServiceTest {

    @Test
    void testRunQueryReturnsData() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicReportQueryService service = new DynamicReportQueryService(jdbcTemplate);

        List<Map<String, Object>> mockData = List.of(Map.of("key", "value"));
        when(jdbcTemplate.queryForList("SELECT * FROM users")).thenReturn(mockData);

        List<Map<String, Object>> result = service.runQuery("SELECT * FROM users");

        assertEquals(mockData, result);
    }

}
