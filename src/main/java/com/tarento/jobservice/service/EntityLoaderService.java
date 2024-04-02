package com.tarento.jobservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface EntityLoaderService {

  public void consumeJobFromProvider(JsonNode jobPushedFromProvider) throws JsonProcessingException;

  public void loadJobsFromExcel(MultipartFile file) throws IOException;

}
