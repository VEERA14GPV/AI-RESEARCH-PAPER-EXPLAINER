package com.paperexplainer.repository;

import com.paperexplainer.entity.Paper;
import com.paperexplainer.entity.PaperStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    List<Paper> findByStatus(PaperStatus status);

    List<Paper> findByTitleContainingIgnoreCase(String title);
}