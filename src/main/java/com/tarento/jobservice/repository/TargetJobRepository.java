package com.tarento.jobservice.repository;

import com.tarento.jobservice.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TargetJobRepository extends JpaRepository<JobEntity, String> {
  JobEntity findByJobTransientId (String jobTransientId);

}
