package com.tarento.jobservice.exception;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ErrorResponse {

  private String code;
  private String message;
  private Map<String, String> errors;
  private int httpStatusCode;
}