package com.tarento.jobservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.jobservice.elasticsearch.dto.SearchCriteria;
import com.tarento.jobservice.service.EntityLoaderService;
import com.tarento.jobservice.service.TargetJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/Job")
@Slf4j
public class JobController {
  // Controller

  @Autowired
  TargetJobService targetJobService;

  @Autowired
  EntityLoaderService entityLoaderService;

  @PostMapping(value = "/loadJobsFromExcel", consumes = "multipart/form-data")
  public ResponseEntity<String> loadJobsFromExcel(@RequestParam("file") MultipartFile file) {
    try {
      entityLoaderService.loadJobsFromExcel(file);
      return ResponseEntity.ok("Loading of jobs from excel is successful.");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error during loading of jobs from excel: " + e.getMessage());
    }
  }

  @PutMapping(value = "/consumeJobFromProvider")
  public ResponseEntity<String> consumeJobFromProvider(@RequestBody JsonNode jobPushedFromProvider) {
    try {
      entityLoaderService.consumeJobFromProvider(jobPushedFromProvider);
      return ResponseEntity.ok("Successfully consumed the job from provider");
    }catch (Exception e){
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error during consuming the job from provider: " + e.getMessage());
    }
  }
  @GetMapping("/{id}")
  public JsonNode getJobById(@PathVariable String id) {
    return targetJobService.getJobById(id);
  }

  @PostMapping("/searchJobs")
  public Object searchJobs(@RequestBody SearchCriteria searchCriteria) throws Exception {
    return targetJobService.searchJobs(searchCriteria);
  }

  @GetMapping("/count")
  public Long getJobCount() {
    return targetJobService.getJobCount();
  }

  @PostMapping("/loadESFromSecondarySource")
  public void loadESFromSecondarySource() {
    targetJobService.loadESFromSecondarySource();
  }


  @GetMapping("/health")
  public String healthCheck() {
    log.info("sid-job-service::healthCheck");
    return "success";
  }

}
