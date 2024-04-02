package com.tarento.jobservice.elasticsearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.jobservice.elasticsearch.dto.SearchCriteria;
import com.tarento.jobservice.elasticsearch.dto.SearchResult;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public interface EsUtilService {

  void addDocument(String esIndexName, String id, JsonNode documentToBeIndex,
      String esRequiredFieldsJsonFilePath);

  void deleteDocument(String documentId, String esIndexName);

  void deleteDocumentsByCriteria(String esIndexName, SearchSourceBuilder sourceBuilder);

  SearchResult searchDocuments(String esIndexName, SearchCriteria searchCriteria) throws Exception;

}
