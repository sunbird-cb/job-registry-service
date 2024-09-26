package com.tarento.jobservice.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class JobSourceEntity {
  @Id
  private String jobId;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode data;
}
