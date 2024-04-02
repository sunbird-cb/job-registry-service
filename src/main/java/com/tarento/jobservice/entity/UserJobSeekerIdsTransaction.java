package com.tarento.jobservice.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_seeker_ids_transaction")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class UserJobSeekerIdsTransaction {

    @Id
    private String userCandidateId;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private JsonNode jobSeekerIds;
}
