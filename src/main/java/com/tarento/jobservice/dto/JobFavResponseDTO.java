package com.tarento.jobservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JobFavResponseDTO {
  private int statusCode;
  private Boolean isSuccess;
}
