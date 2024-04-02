package com.tarento.jobservice.constant;

public class Constants {

  private Constants() {

  }

  public static final String JOB_ENGINE_KEY = "jobs_";

  public static final String CREATED_DATE = "createDate";

  public static final String UPDATED_DATE = "lastUpdateDate";

  public static final Boolean JOB_ACTIVE_STATUS = true;

  public static final String CREATED_BY = "createdBy";

  public static final String CREATED_BY_VALUE = "JOBX";

  public static final String UPDATED_BY = "updatedBy";
  public static final String CONTACT_PERSON_DETAILS = "contactPersonDetails";

  public static final String KEYWORD =".keyword";

  public static final String ASC ="asc";

  public static final String INDEX_TYPE ="_doc";
  public static final String ES_REQUIRED_FIELDS_JSON_FILE = "/EsFieldsmapping/esRequiredFieldsJsonFilePath.json";
  public static final String ES_INDEX_NAME ="job_data_tranformed";

  public static final String IS_ACTIVE ="isActive";

  public static final String DATE_FORMAT ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  public static final String SALARY_RANGE ="salaryRange";

  public static final String ID ="id";

  public static final String JOB_TRANSIENT_ID ="jobTransientId";

  public static final String REQUEST_PAYLOAD ="requestPayload";

  public static final String DISTRICTS ="districts";

  public static final String STATES ="states";

  public static final String COUNTRY ="country";

  public static final String JOB_LOCATION_DISTRICT ="jobLocation.district";

  public static final String JOB_LOCATION_COUNTRY ="jobLocation.country";

  public static final String JOB_LOCATION_STATES ="jobLocation.state";

  public static final String CANDIDATE_ID ="candidateId";

  public static final String JOB_COUNT_CACHE_KEY ="jobCount";

  public static final String MAX_CTC_MONTHLY ="maxCtcMonthly";

  public static final String MIN_CTC_MONTHLY ="minCtcMonthly";
  public static final String VALID_UPTO ="validUpto";
  public static final String POSTED_ON ="postedOn";

  public static final String JOB_DATA_PAYLOAD_VALIDATION_FILE = "/payloadValidation/jobValidationData.json";

  public static final String ERROR = "ERROR";
}
