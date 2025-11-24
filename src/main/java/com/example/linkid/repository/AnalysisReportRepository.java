package com.example.linkid.repository;

import com.example.linkid.domain.AnalysisReport;
import com.example.linkid.domain.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {
    Optional<AnalysisReport> findByVideo(Video video);
}