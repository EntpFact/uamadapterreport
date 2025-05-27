package com.hdfcbank.uamadapterreport.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;



import java.util.List;
import java.util.Map;

@Service
public class DynamicReportQueryService {

    private final JdbcTemplate jdbcTemplate;

    public DynamicReportQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> runQuery(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (DataAccessException e) {
            return List.of();
        }
    }
}
