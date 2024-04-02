package com.tarento.jobservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.jobservice.constant.Constants;
import com.tarento.jobservice.dto.JWTDetailsDTO;
import com.tarento.jobservice.dto.JobFavDto;
import com.tarento.jobservice.dto.JobFavResponseDTO;
import com.tarento.jobservice.elasticsearch.dto.SearchCriteria;
import com.tarento.jobservice.elasticsearch.dto.SearchResult;
import com.tarento.jobservice.exception.JobException;
import com.tarento.jobservice.elasticsearch.service.EsUtilService;
import com.tarento.jobservice.entity.JobEntity;
import com.tarento.jobservice.entity.UserJobSeekerIdsTransaction;
import com.tarento.jobservice.entity.UserJobTransaction;
import com.tarento.jobservice.repository.TargetJobRepository;
import com.tarento.jobservice.repository.UserJobSeekerIdsTransactionRepository;
import com.tarento.jobservice.repository.UserJobTransactionRepository;
import com.tarento.jobservice.service.TargetJobService;
import com.tarento.jobservice.util.service.UtilService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TargetJobServiceImpl implements TargetJobService {

  @Autowired
  TargetJobRepository targetJobRepository;

  @Autowired
  EsUtilService esUtilService;

  @Autowired
  private UtilService utilService;

  @Autowired
  private UserJobTransactionRepository userJobTransactionRepository;

  @Value("${job.entity.redis.ttl}")
  private long jobEntityRedisTtl;

  @Value("${search.result.redis.ttl}")
  private long searchResultRedisTtl;

  @Autowired
  private RedisTemplate<String, JobEntity> jobEntityRedisTemplate;

  @Autowired
  private RedisTemplate<String, SearchResult> searchResultRedisTemplate;

  @Autowired
  private RedisTemplate<String, Long> jobCountRedisTemplate;

  @Autowired
  private UserJobSeekerIdsTransactionRepository userJobSeekerIdsTransactionRepository;

  @Override
  public void createOrUpdateJob(JsonNode jobJson) throws JsonProcessingException {
    log.info("SidJobServiceImpl::createOrUpdateJob: create or update job");
    try{
      JsonNode jobJsonNode = jobJson;
      if (jobJsonNode != null) {
        String id = UUID.randomUUID().toString();
        String jobTransientId = jobJson.get("sourceSystem").asText() + "_" + jobJson.get("jobId").asText();
        JobEntity sidJobEntity = new JobEntity();
        sidJobEntity.setJobTransientId(jobTransientId);
        Optional<JobEntity> dataFetched = Optional.ofNullable(
            targetJobRepository.findByJobTransientId(jobTransientId));
        ObjectNode dataObj = (ObjectNode) jobJson;
        if (dataFetched.isPresent()){
          log.info("SidJobServiceImpl::createOrUpdateJob:updating the job "+dataFetched);
          sidJobEntity= dataFetched.get();
          Timestamp currentTime = new Timestamp(System.currentTimeMillis());
          sidJobEntity.setLastUpdatedDate(currentTime);
          dataObj = fetchDetailsFromDb(dataObj, sidJobEntity.getData());
          sidJobEntity.setData(jobJson);
        }else {
          log.info("SidJobServiceImpl::createOrUpdateJob:creating the job ");
          sidJobEntity.setId(id);
          Timestamp currentTime = new Timestamp(System.currentTimeMillis());
          sidJobEntity.setCreatedDate(currentTime);
          sidJobEntity.setLastUpdatedDate(currentTime);
          sidJobEntity.setSourceSystem(jobJson.get("sourceSystem").asText());
          dataObj= generateDateAndStatus(dataObj);
        }
        dataObj.put(Constants.ID, sidJobEntity.getId());
        dataObj.put(Constants.JOB_TRANSIENT_ID, sidJobEntity.getJobTransientId());
        sidJobEntity.setData(dataObj);
        targetJobRepository.save(sidJobEntity);
        log.info("Id of job created in SID: " + sidJobEntity.getId());
        jobEntityRedisTemplate.opsForValue()
            .set(Constants.JOB_ENGINE_KEY + sidJobEntity.getId(), sidJobEntity, jobEntityRedisTtl,
                TimeUnit.SECONDS);
        jobCountRedisTemplate.opsForValue().set(Constants.JOB_COUNT_CACHE_KEY, targetJobRepository.count());
        esUtilService.addDocument(Constants.ES_INDEX_NAME, sidJobEntity.getId(), sidJobEntity.getData(), Constants.ES_REQUIRED_FIELDS_JSON_FILE);
      }else {
        log.info("Job is missing in the JSON data.");
      }
    } catch (Exception e) {
      log.error("Error occured: ", e.getMessage());
    }
  }

  private ObjectNode generateDateAndStatus(ObjectNode dataObj) {
    Date currentDate = new Date();
    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
    String formattedDate = dateFormat.format(currentDate);
    dataObj.put(Constants.CREATED_DATE,formattedDate);
    dataObj.put(Constants.UPDATED_DATE,formattedDate);
    dataObj.put(Constants.IS_ACTIVE, Constants.JOB_ACTIVE_STATUS);
    dataObj.put(Constants.CREATED_BY, Constants.CREATED_BY_VALUE);
    dataObj.put(Constants.UPDATED_BY,Constants.CREATED_BY_VALUE);
    return dataObj;
  }

  private ObjectNode fetchDetailsFromDb(ObjectNode dataObj, JsonNode dataFetchedFromDb) {
    dataObj.set(Constants.CREATED_DATE,dataFetchedFromDb.get(Constants.CREATED_DATE));
    dataObj.put(Constants.CREATED_BY, Constants.CREATED_BY_VALUE);
    Date currentDate = new Date();
    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
    String formattedDate = dateFormat.format(currentDate);
    dataObj.put(Constants.UPDATED_DATE,formattedDate);
    dataObj.put(Constants.IS_ACTIVE, Constants.JOB_ACTIVE_STATUS);
    dataObj.put(Constants.UPDATED_BY,Constants.CREATED_BY_VALUE);
    return dataObj;
  }

  @Override
  public JsonNode getJobById(String id) {
    log.info("SidJobServiceImpl::getJobById");
    JobEntity sidJobEntity =  jobEntityRedisTemplate.opsForValue().get(Constants.JOB_ENGINE_KEY+id);
    if(sidJobEntity != null) {
      log.info("SidJobServiceImpl::getJobById: job fetched from redis");
      return sidJobEntity.getData();
    }
    Optional<JobEntity> optSidJobEntity = targetJobRepository.findById(id);
    if (optSidJobEntity.isPresent()) {
      sidJobEntity = optSidJobEntity.get();
      jobEntityRedisTemplate.opsForValue()
          .set(Constants.JOB_ENGINE_KEY + sidJobEntity.getId(), sidJobEntity, jobEntityRedisTtl,
              TimeUnit.SECONDS);
      return sidJobEntity.getData();
    }
    throw new JobException("ERROR", "job is not available for given ID:"+id);
  }

  @Override
  public SearchResult searchJobs(SearchCriteria searchCriteria) {
    log.info("SidJobServiceImpl::searchJobs");
    SearchResult searchResult =  searchResultRedisTemplate.opsForValue().get(utilService.generateRedisJwtTokenKey(searchCriteria));
    if(searchResult != null) {
      log.info("SidJobServiceImpl::searchJobs: job search result fetched from redis");
      return searchResult;
    }
    String searchString = searchCriteria.getSearchString();
    if (searchString!= null){
        if (searchString.length()  > 2){
          log.info("Validated SearchString");
           searchCriteria.setSearchString(searchString.toLowerCase());

        }else
          throw new JobException("ERROR10", "Mininum 3 characters are required to search");
    }
    try {
      log.info("SidJobServiceImpl::searchJobs: job search result fetching from es");
      searchResult = esUtilService.searchDocuments(Constants.ES_INDEX_NAME, searchCriteria);
      searchResultRedisTemplate.opsForValue()
          .set(utilService.generateRedisJwtTokenKey(searchCriteria), searchResult, searchResultRedisTtl,
              TimeUnit.SECONDS);
      return searchResult;
    } catch (Exception e) {
      e.getStackTrace();
      throw new JobException("ERROR", e.getMessage());
    }
  }

  @Override
  public Long getJobCount() {
    try {
      Long count = jobCountRedisTemplate.opsForValue().get(Constants.JOB_COUNT_CACHE_KEY);
      if (count == null || count == 0) {
        count = targetJobRepository.count();
        ValueOperations<String, Long> val = jobCountRedisTemplate.opsForValue();
        val.set(Constants.JOB_COUNT_CACHE_KEY, count);
        log.info("save to cache : {}", count);
      }
      return count;
    } catch (Exception e) {
      e.getStackTrace();
      log.error("error : {}", e.getMessage(), e);
    }
    return 0L;
  }

  @Override
  public void loadESFromSecondarySource() {
    log.info("SidJobServiceImpl::loadESFromSecondarySource");
    try {
      List<JobEntity> jobList = targetJobRepository.findAll();
      if(jobList != null && !jobList.isEmpty())
        for (JobEntity job : jobList) {
          esUtilService.addDocument(Constants.ES_INDEX_NAME, job.getId(), job.getData(), Constants.ES_REQUIRED_FIELDS_JSON_FILE);
        }
    } catch (Exception e) {
      e.getStackTrace();
      throw new JobException("ERROR", "Error while loading ES from secondary source: "+e.getMessage());
    }
  }

  public void saveOrRemoveFavouriteJob(String candidateId, String jobTransientId, boolean isFavourite) {
    UserJobTransaction userJobTransaction = userJobTransactionRepository.findByCandidateIdAndJobTransientId(candidateId, jobTransientId);

    if (isFavourite && userJobTransaction == null) {
      UserJobTransaction newUserJobTransaction = new UserJobTransaction();
      newUserJobTransaction.setCandidateId(candidateId);
      newUserJobTransaction.setJobTransientId(jobTransientId);
      newUserJobTransaction.setCreatedDate(new Timestamp(System.currentTimeMillis()));
      userJobTransactionRepository.save(newUserJobTransaction);
    } else if (!isFavourite && userJobTransaction != null) {
      userJobTransactionRepository.deleteById(userJobTransaction.getId());
    }
  }



  private void mapExternalPortalJobSeekerIdOfSameUserWithSID(String candidateId, String jobxUserId) {
    log.info("SidJobServiceImpl::mapExternalPortalJobSeekerIdOfSameUserWithSID");
    UserJobSeekerIdsTransaction userJobSeekerIdsTransaction = new UserJobSeekerIdsTransaction();
    userJobSeekerIdsTransaction.setUserCandidateId(candidateId);

    ObjectNode jobSeekerIdsJson = JsonNodeFactory.instance.objectNode();
    jobSeekerIdsJson.put("jobxUserId", Integer.parseInt(jobxUserId));

    userJobSeekerIdsTransaction.setJobSeekerIds(jobSeekerIdsJson);
    userJobSeekerIdsTransactionRepository.save(userJobSeekerIdsTransaction);
  }

  private ObjectNode setUserDetails(ObjectNode source) {
    log.info("SidJobServiceImpl::setUserDetails");
    ObjectNode target = JsonNodeFactory.instance.objectNode();
    target.put("first_Name", source.get("firstName"));
    target.put("mobile_Number", source.get("mobileNumber"));
    target.put("loginName", source.get("mobileNumber"));
    target.put("loginPassword", source.get("mobileNumber"));
    target.put("gender", source.get("gender"));
    target.put("resistration_Source", "Skill India Digital");
    target.put("pwdType", getValueOrDefault(source, "pwdType", "NA"));
    target.put("pwdCategory", getValueOrDefault(source, "pwdCategory", "NA"));
    target.put("qualification", getValueOrDefault(source, "qualification", "NA"));
    target.put("uG_Course_Name", getValueOrDefault(source, "uG_Course_Name", "NA"));
    target.put("specialization", getValueOrDefault(source, "specialization", "NA"));

    JsonNode addressDetails = source.get("addressDetails");
    if (addressDetails != null && addressDetails.isArray() && addressDetails.size() > 0) {
      JsonNode firstAddress = addressDetails.get(0);  // Assuming you want the first address
      if (firstAddress != null) {
        target.put("stateName", getValueOrDefault(firstAddress, "state", "NA"));
        target.put("districtName", getValueOrDefault(firstAddress, "district", "NA"));
      }
    }

    return target;
  }

  private String getValueOrDefault(JsonNode node, String fieldName, String defaultValue) {
    if (node != null && node.has(fieldName) && !node.get(fieldName).isNull() && node.get(fieldName).asText().trim().length() > 0) {
      return node.get(fieldName).asText();
    }
    return defaultValue;
  }

}
