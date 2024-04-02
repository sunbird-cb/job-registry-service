package com.tarento.jobservice.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sid_jobs")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class JobEntity implements Serializable {

  @Id
  private String id;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode data;

  private String jobTransientId;

  private String sourceSystem;

  private Timestamp createdDate;

  private Timestamp lastUpdatedDate;
}
