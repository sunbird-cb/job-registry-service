package com.tarento.jobservice.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TypeDef;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_job_transaction")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class UserJobTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "candidate_id")
  private String candidateId;

  @Column(name = "job_transient_id")
  private String jobTransientId;

  @Column(name = "created_date")
  private Timestamp createdDate;

}
