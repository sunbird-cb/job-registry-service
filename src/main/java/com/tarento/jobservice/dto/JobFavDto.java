package com.tarento.jobservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JobFavDto {
  private String jobTransientId;
  private Boolean isFavorite;
}
