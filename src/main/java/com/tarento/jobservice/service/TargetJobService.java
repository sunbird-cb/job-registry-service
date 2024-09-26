package com.tarento.jobservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.jobservice.dto.JobFavDto;
import com.tarento.jobservice.dto.JobFavResponseDTO;
import com.tarento.jobservice.elasticsearch.dto.SearchCriteria;
import com.tarento.jobservice.elasticsearch.dto.SearchResult;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public interface TargetJobService {

  void createOrUpdateJob(JsonNode jobJson) throws JsonProcessingException;


  JsonNode getJobById(String jobId);

  SearchResult searchJobs(SearchCriteria searchCriteria);

  Long getJobCount();

  void loadESFromSecondarySource();


}
