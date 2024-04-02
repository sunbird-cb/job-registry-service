package com.tarento.jobservice.service.impl;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.tarento.jobservice.constant.Constants;
import com.tarento.jobservice.entity.JobSourceEntity;
import com.tarento.jobservice.exception.JobException;
import com.tarento.jobservice.repository.JobSourceRepository;
import com.tarento.jobservice.service.EntityLoaderService;
import com.tarento.jobservice.service.TargetJobService;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class EntityLoadServiceImpl implements EntityLoaderService {

  @Autowired
  private JobSourceRepository jobXRepository;

  @Autowired
  ObjectMapper objectMapper;

  @Value("${transformation.source-to-target.spec.path}")
  private String pathOfTragetFile;

  @Autowired
  private TargetJobService jobService;

  public void consumeJobFromProvider(JsonNode jobPushedFromProvider)
      throws JsonProcessingException {
    log.info("EntityLoadServiceImpl::consumeJobFromProvider ");
    validatePayload(Constants.JOB_DATA_PAYLOAD_VALIDATION_FILE, jobPushedFromProvider);
    formatDate(jobPushedFromProvider);
    saveRawJobDataFromProvider(jobPushedFromProvider);
    transformAndPersist(jobPushedFromProvider);
  }

  public void loadJobsFromExcel(MultipartFile file) throws IOException {
    log.info("EntityLoadServiceImpl::loadJobsFromExcel");
    List<Map<String, String>> processedData = processExcelFile(file);
    log.info("No.of processedData from excel: " + processedData.size());
    JsonNode jobDataJson = objectMapper.valueToTree(processedData);
    jobDataJson.forEach(
        eachJobXData -> {
          saveRawJobDataFromProvider(eachJobXData);
          try {
            transformAndPersist(eachJobXData);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }
  private List<Map<String, String>> processExcelFile(MultipartFile incomingFile) {
    log.info("EntityLoadServiceImpl::processExcelFile");
    try {
      return validateFileAndProcessRows(incomingFile);
    } catch (Exception e) {
      log.error("Error occurred during file processing: {}", e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }

  private List<Map<String, String>> validateFileAndProcessRows(MultipartFile file) {
    log.info("EntityLoadServiceImpl::validateFileAndProcessRows");
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      return processSheetAndSendMessage(sheet);
    } catch (IOException e) {
      log.error("Error while processing Excel file: {}", e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }

  private List<Map<String, String>> processSheetAndSendMessage(Sheet sheet) {
    log.info("EntityLoadServiceImpl::processSheetAndSendMessage");
    DataFormatter formatter = new DataFormatter();
    Row headerRow = sheet.getRow(0);
    List<Map<String, String>> dataRows = new ArrayList<>();

    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row dataRow = sheet.getRow(rowIndex);

      if (dataRow == null) {
        break; // No more data rows, exit the loop
      }

      boolean allBlank = true;
      Map<String, String> rowData = new HashMap<>();

      for (int colIndex = 0; colIndex < headerRow.getLastCellNum(); colIndex++) {
        Cell headerCell = headerRow.getCell(colIndex);
        Cell valueCell = dataRow.getCell(colIndex);

        if (headerCell != null && headerCell.getCellType() != CellType.BLANK) {
          String excelHeader =
              formatter.formatCellValue(headerCell).replaceAll("[\\n*]", "").trim();
          String cellValue = "";

          if (valueCell != null && valueCell.getCellType() != CellType.BLANK) {
            if (valueCell.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(valueCell)) {
              // Handle date format
              Date date = valueCell.getDateCellValue();
              SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
              cellValue = dateFormat.format(date);
            } else {
              cellValue = formatter.formatCellValue(valueCell).replace("\n", ",").trim();
            }
            allBlank = false;
          }

          rowData.put(excelHeader, cellValue);
        }
      }
      log.info("Data Rows: " + rowData);

      if (allBlank) {
        break; // If all cells are blank in the current row, stop processing
      }

      dataRows.add(rowData);
    }

    log.info("Number of Data Rows Processed: " + dataRows.size());
    return dataRows;
  }

  public void validatePayload(String fileName, JsonNode payload) {
    try {
      JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
      InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(fileName);
      JsonSchema schema = schemaFactory.getSchema(schemaStream);

      Set<ValidationMessage> validationMessages = schema.validate(payload);
      if (!validationMessages.isEmpty()) {
        StringBuilder errorMessage = new StringBuilder("Validation error(s): \n");
        for (ValidationMessage message : validationMessages) {
          errorMessage.append(message.getMessage()).append("\n");
        }
        throw new JobException(Constants.ERROR, errorMessage.toString());
      }
    } catch (Exception e) {
      throw new JobException(Constants.ERROR, "Failed to validate payload: " + e.getMessage());
    }
  }

  private JsonNode formatDate(JsonNode jobPushedFromProvider) {
    SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    if(jobPushedFromProvider.get("posted_on")!= null) {
      if (isValidDateFormat(jobPushedFromProvider.get("posted_on").asText(), "dd/MM/yyyy")){
        String inputPostedOnDate = jobPushedFromProvider.get("posted_on").asText();
        try {
          Date postedOnDate = inputFormat.parse(inputPostedOnDate);
          ((ObjectNode) jobPushedFromProvider).put("posted_on", outputFormat.format(postedOnDate));
        } catch (ParseException e) {
          e.getStackTrace();
          throw new JobException(Constants.ERROR, "Error parsing date: " + inputPostedOnDate);
        }
      }
    }

    if(jobPushedFromProvider.get("valid_upto") != null) {
      if (isValidDateFormat(jobPushedFromProvider.get("valid_upto").asText(), "dd/MM/yyyy")){
        String inputValidUptoDate = jobPushedFromProvider.get("valid_upto").asText();
        try {
          Date validUptoDate = inputFormat.parse(inputValidUptoDate);
          ((ObjectNode) jobPushedFromProvider).put("valid_upto", outputFormat.format(validUptoDate));
        } catch (ParseException e) {
          e.getStackTrace();
          throw new JobException(Constants.ERROR, "Error parsing date: " + inputValidUptoDate);
        }
      }
    }
    return jobPushedFromProvider;
  }

  private void saveRawJobDataFromProvider(JsonNode rawJobData) {
    log.info("EntityLoadServiceImpl::saveRawJobDataFromProvider");
    JobSourceEntity jobEntity = new JobSourceEntity();
    jobEntity.setJobId(rawJobData.get("JobId").asText());
    jobEntity.setData(rawJobData);
    jobXRepository.save(jobEntity);
  }
  private boolean isValidDateFormat(String date, String format) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    sdf.setLenient(false);
    try {
      sdf.parse(date);
      return true;
    } catch (ParseException e) {
      return false;
    }
  }

  private void transformAndPersist(JsonNode jobPushedFromProvider) throws JsonProcessingException{
    log.info("EntityLoadServiceImpl::transformAndSendJobData:");
    Object transforemedTargetFormat = transformData(jobPushedFromProvider, pathOfTragetFile);
    transforemedTargetFormat = addSearchTags((ObjectNode) transforemedTargetFormat);
    log.info(
        "EntityLoadServiceImpl::transformAndSendJobData: Sending object: "
            + transforemedTargetFormat);
    JsonNode transformedData = (JsonNode) transforemedTargetFormat;
    jobService.createOrUpdateJob(transformedData);
//      kafkaProducer.sendJobData(transforemedTargetFormat);
  }

  public JsonNode transformData(Object sourceObject, String destinationPath) {
    log.info("EntityLoadServiceImpl::transformData");
    String inputJson;
    try {
      inputJson = objectMapper.writeValueAsString(sourceObject);
    } catch (JsonProcessingException e) {
      return null;
    }
    List<Object> specJson = JsonUtils.classpathToList(destinationPath);
    Chainr chainr = Chainr.fromSpec(specJson);
    Object transformedOutput = chainr.transform(JsonUtils.jsonToObject(inputJson));
    return objectMapper.convertValue(transformedOutput, JsonNode.class);
  }

  private ObjectNode addSearchTags(ObjectNode formattedData) {
    List<String> searchTags = new ArrayList<>();
    searchTags.add(formattedData.get("jobTitle").textValue().toLowerCase());
    searchTags.add(formattedData.get("industryName").textValue().toLowerCase());
    searchTags.add(formattedData.get("companyName").textValue().toLowerCase());
    searchTags.add(formattedData.get("jobLocationState").textValue().toLowerCase());
    searchTags.add(formattedData.get("jobLocationDistrict").textValue().toLowerCase());
    searchTags.add(formattedData.get("jobLocationCountry").textValue().toLowerCase());
    ArrayNode searchTagsArray = objectMapper.valueToTree(searchTags);
    ((ObjectNode) formattedData).putArray("searchTags").add(searchTagsArray);
    return formattedData;
  }
}
