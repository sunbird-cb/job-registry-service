package com.tarento.jobservice.repository;

import com.tarento.jobservice.entity.JobSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobSourceRepository extends JpaRepository<JobSourceEntity, String> {

}
