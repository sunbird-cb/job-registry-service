package com.tarento.jobservice.repository;

import com.tarento.jobservice.entity.UserJobSeekerIdsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJobSeekerIdsTransactionRepository extends JpaRepository<UserJobSeekerIdsTransaction, String> {
}
