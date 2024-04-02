package com.tarento.jobservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JWTDetailsDTO {
  private String OSID;
  private List<String> roles;
  private String kcUserId;
  private String jwtHeader;
  private String preferredUserName;

}
