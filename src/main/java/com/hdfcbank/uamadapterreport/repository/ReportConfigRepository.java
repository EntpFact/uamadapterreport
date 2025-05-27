package com.hdfcbank.uamadapterreport.repository;

import com.hdfcbank.uamadapterreport.model.ReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportConfigRepository extends JpaRepository<ReportConfig, Long> {
    List<ReportConfig> findByActiveTrue();
}
