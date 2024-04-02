package com.tarento.jobservice.elasticsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {

  private HashMap<String, Object> filterCriteriaMap;

  private List<String> requestedFields;

  private int PageNumber;

  private int PageSize;

  private List<SortCriteria> sortCriteria;

  private String SearchString;

  private List<String> facets;

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SortCriteria {
    private String type;
    private String field;
    private String order;
  }
}
