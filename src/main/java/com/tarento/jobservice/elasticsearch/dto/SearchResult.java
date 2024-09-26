package com.tarento.jobservice.elasticsearch.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult implements Serializable {
  private JsonNode data;
  private Map<String, List<FacetDTO>> facets;
  private long totalCount;
}
