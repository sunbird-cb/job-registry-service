package com.tarento.jobservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandling {

  @ExceptionHandler(Exception.class)
  public ResponseEntity handleException(Exception ex) {
    log.debug("RestExceptionHandler::handleException::" + ex);
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    ErrorResponse errorResponse = null;
    if (ex instanceof JobException) {
      JobException jobException = (JobException) ex;
      status = HttpStatus.BAD_REQUEST;
      // Check if the JobException provides an HTTP status code
      if (jobException.getHttpStatusCode() != null) {
        try {
          status = jobException.getHttpStatusCode();
        } catch (IllegalArgumentException e) {
          log.warn("Invalid HTTP status code provided in JobException: " + jobException.getHttpStatusCode());
        }
      }
      errorResponse = ErrorResponse.builder()
          .code(jobException.getCode())
          .message(jobException.getMessage())
          .httpStatusCode(jobException.getHttpStatusCode() != null
              ? jobException.getHttpStatusCode().value()
              : status.value())
          .build();
      if (StringUtils.isNotBlank(jobException.getMessage())) {
        log.error(jobException.getMessage());
      }

      return new ResponseEntity<>(errorResponse, status);
    }
    errorResponse = ErrorResponse.builder()
        .code(ex.getMessage()).build();
    return new ResponseEntity<>(errorResponse, status);
  }

}