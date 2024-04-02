package com.tarento.jobservice.elasticsearch.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.tarento.jobservice.constant.Constants;
import com.tarento.jobservice.elasticsearch.dto.FacetDTO;
import com.tarento.jobservice.elasticsearch.dto.SearchCriteria;
import com.tarento.jobservice.elasticsearch.dto.SearchResult;
import com.tarento.jobservice.elasticsearch.esconfig.EsConnection;
import com.tarento.jobservice.exception.JobException;
import com.tarento.jobservice.elasticsearch.service.EsUtilService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@Slf4j
public class EsUtilServiceImpl implements EsUtilService {

  @Autowired
  private EsConnection esConnection;

  @Autowired
  private ObjectMapper objectMapper;


  @Override
  public void addDocument(String esIndexName, String id, JsonNode documentToBeIndex, String esRequiredFieldsJsonFilePath) {
    log.info("inside addDocument method");
    Map<String, Object> map = objectMapper.convertValue(documentToBeIndex, Map.class);
    addDocument(esIndexName, Constants.INDEX_TYPE, id,
            map, esRequiredFieldsJsonFilePath
    );
  }

  @Override
  public void deleteDocument(String documentId, String esIndexName) {
    try{
      // Create a delete request
      DeleteRequest request = new DeleteRequest(esIndexName, Constants.INDEX_TYPE, documentId);

      // Execute the delete request
      DeleteResponse response = esConnection.getRestHighLevelClient().delete(request, RequestOptions.DEFAULT);

      // Check if the document was successfully deleted
      if (response.getResult() == DeleteResponse.Result.DELETED) {
        log.info("Document deleted successfully from elasticsearch.");
      } else {
        throw new JobException("ERROR01", "Document not found or failed to delete from elasticsearch.");
      }
    } catch (Exception e) {
      e.getStackTrace();
      throw new JobException("ERROR02", "Error occured during deleting document in elasticsearch");
    }
  }

  @Override
  public void deleteDocumentsByCriteria(String esIndexName, SearchSourceBuilder sourceBuilder) {
    try{
      // Create a search request with your criteria
      SearchRequest searchRequest = new SearchRequest(esIndexName);
      searchRequest.source(sourceBuilder);

      // Execute the search request
      SearchResponse searchResponse = esConnection.getRestHighLevelClient().search(searchRequest, RequestOptions.DEFAULT);

      // Check the search response for matching documents
      if (searchResponse.getHits().getTotalHits().value > 0) {
        // Create a bulk request for deleting matching documents
        BulkRequest bulkRequest = new BulkRequest();
        searchResponse.getHits().forEach(hit -> {
          bulkRequest.add(new DeleteRequest(esIndexName, Constants.INDEX_TYPE, hit.getId()));
        });

        // Execute the bulk delete request
        BulkResponse bulkResponse = esConnection.getRestHighLevelClient().bulk(bulkRequest, RequestOptions.DEFAULT);

        // Check if the documents were successfully deleted
        if (!bulkResponse.hasFailures()) {
          log.info("Documents matching the criteria deleted successfully from elasticsearch.");
        } else {
          throw new JobException("ERROR03", "Some documents failed to delete from elasticsearch.");
        }
      } else {
        log.info("No documents match the criteria.");
      }
    } catch (Exception e) {
      e.getStackTrace();
      throw new JobException("ERROR04", "Error occured during deleting documents by criteria from elasticsearch.");
    }
  }

  @Override
  public SearchResult searchDocuments(String esIndexName, SearchCriteria searchCriteria)
          throws Exception {
    log.info("Searching scheme with filters");
    SearchSourceBuilder searchSourceBuilder = buildSearchSourceBuilder(searchCriteria);

    RestHighLevelClient restHighLevelClient = esConnection.getRestHighLevelClient();
    SearchRequest searchRequest = new SearchRequest(esIndexName);
    searchRequest.source(searchSourceBuilder);

    try {
      // Get the total count without retrieving hits
      searchSourceBuilder.size(0);
      SearchResponse totalHitsSearchResponse =
              restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
      long totalCount = totalHitsSearchResponse.getHits().getTotalHits().value;

      Map<String, List<FacetDTO>> fieldAggregations = new HashMap<>();
      List<String> testFacets= new ArrayList<>();
      testFacets = searchCriteria.getFacets();
      if (testFacets != null){
        // Extract and process the aggregation results for each field
        for (String field : searchCriteria.getFacets()) {
          Terms fieldAggregation = totalHitsSearchResponse.getAggregations().get(field + "_agg");

          // Create a list of the field's unique value and count
          List<FacetDTO> fieldValueList = new ArrayList<>();
          for (Terms.Bucket bucket : fieldAggregation.getBuckets()) {
            if(!bucket.getKeyAsString().isEmpty()) {
              FacetDTO facetDTO = new FacetDTO(bucket.getKeyAsString(), bucket.getDocCount());
              fieldValueList.add(facetDTO);
            }
          }

          // Store the field's list in map
          fieldAggregations.put(field, fieldValueList);
        }

      }

      // Configure pagination
      int startIndex;
      int endIndex;
      if (searchCriteria.getPageNumber() > 0 && searchCriteria.getPageSize() > 0) {
        int pageNumber = searchCriteria.getPageNumber();
        int pageSize = searchCriteria.getPageSize();

        startIndex = (pageNumber - 1) * pageSize;
        endIndex = (int) Math.min(startIndex + pageSize, totalCount);
      } else {
        startIndex = 0;
        endIndex = 10000;
      }

      if(startIndex > endIndex) {
        throw new JobException("ERROR", "Requested page is beyond the available data.");
      }

      // Perform the paginated search
      searchSourceBuilder.from(startIndex);
      searchSourceBuilder.size(endIndex - startIndex); // Adjust size

      SearchResponse paginatedSearchResponse =
              restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
      SearchHit[] hits = paginatedSearchResponse.getHits().getHits();

      List<Map<String, Object>> paginatedResult = new ArrayList<>();
      for (SearchHit hit : hits) {
        paginatedResult.add(hit.getSourceAsMap());
      }

      SearchResult searchResult = new SearchResult();
      searchResult.setData(objectMapper.valueToTree(paginatedResult));
      searchResult.setFacets(fieldAggregations);
      searchResult.setTotalCount(totalCount);

      return searchResult;
    } catch (IOException e) {
      e.getStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  private SearchSourceBuilder buildSearchSourceBuilder(SearchCriteria searchCriteria) {
    log.info("building search query");
    if (searchCriteria == null || searchCriteria.toString().isEmpty()) {
      throw new JobException("ERROR05", "Search criteria body is missing");
    }
    BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    if (searchCriteria.getFilterCriteriaMap() != null) {
      searchCriteria
              .getFilterCriteriaMap()
              .forEach(
                      (field, value) -> {
                        //configure specific use cases of filters
                        constructQueryForSpecificUseCases(
                                boolQueryBuilder, field, value);
                        if (value instanceof Boolean) {
                          boolQueryBuilder.must(QueryBuilders.termQuery(field, value));
                        } else if (value instanceof ArrayList) {
                          boolQueryBuilder.must(
                                QueryBuilders.termsQuery(
                                    field + Constants.KEYWORD, ((ArrayList<?>) value).toArray()));
                        } else if(value instanceof String){
                          boolQueryBuilder.must(QueryBuilders.termsQuery(field +Constants.KEYWORD, value));
                        }
                      });
    }

    addSorting(searchSourceBuilder, searchCriteria, boolQueryBuilder);

    if (searchCriteria.getRequestedFields() == null) {
      // to get all the fields in response
      searchSourceBuilder.fetchSource(null);
    } else {
      if (searchCriteria.getRequestedFields().isEmpty()) {
        throw new JobException(
                "INVALID_REQUEST", "Please specify at least one field to include in the results.");
      }
      searchSourceBuilder.fetchSource(
              searchCriteria.getRequestedFields().toArray(new String[0]), null);
    }

    // Add query string if present
    String searchString = searchCriteria.getSearchString();
    if (searchString != null) {
      boolQueryBuilder.must(QueryBuilders.boolQuery()
          .should(new WildcardQueryBuilder("searchTags.keyword", "*" + searchCriteria.getSearchString() + "*")));
    }

    List<String> testFacets= new ArrayList<>();
    testFacets = searchCriteria.getFacets();
    if (testFacets!= null){
      for (String field : searchCriteria.getFacets()) {
        searchSourceBuilder.aggregation(AggregationBuilders.terms(field + "_agg").field(field + ".keyword").size(50));
      }
    }

    searchSourceBuilder.query(boolQueryBuilder);
    return searchSourceBuilder;
  }

  private void addSorting(SearchSourceBuilder searchSourceBuilder, SearchCriteria searchCriteria, BoolQueryBuilder boolQueryBuilder) {
    if (searchCriteria.getSortCriteria() != null && !searchCriteria.getSortCriteria().isEmpty()) {

      for (SearchCriteria.SortCriteria sortCriteria : searchCriteria.getSortCriteria()) {
        if (isValidSortCriteria(sortCriteria)) {
          SortOrder sortOrder = sortCriteria.getOrder().equalsIgnoreCase(Constants.ASC) ? SortOrder.ASC : SortOrder.DESC;

          if (sortCriteria.getType().equalsIgnoreCase("keyword")) {
            searchSourceBuilder.sort(SortBuilders.fieldSort(sortCriteria.getField() + Constants.KEYWORD).order(sortOrder));
          } else if (sortCriteria.getType().equalsIgnoreCase("number")) {
            if (sortCriteria.getField().equalsIgnoreCase("maxCtcMonthly") || sortCriteria.getField().equalsIgnoreCase("minCtcMonthly")) {
              handleSalarySpecificSotingCriteria(searchSourceBuilder, boolQueryBuilder, sortCriteria, sortOrder);
            } else {
              searchSourceBuilder.sort(SortBuilders.fieldSort(sortCriteria.getField()).order(sortOrder));
            }
          } else if (sortCriteria.getType().equalsIgnoreCase("date")) {
            searchSourceBuilder.sort(SortBuilders.fieldSort(sortCriteria.getField()).order(sortOrder));
          }
        }
      }
    }
  }

  private void handleSalarySpecificSotingCriteria(SearchSourceBuilder searchSourceBuilder, BoolQueryBuilder boolQueryBuilder, SearchCriteria.SortCriteria sortCriteria, SortOrder sortOrder) {
    if(sortCriteria.getField().equalsIgnoreCase("maxCtcMonthly") && sortCriteria.getOrder().equalsIgnoreCase("desc")) {
      boolQueryBuilder.mustNot(QueryBuilders.termQuery(sortCriteria.getField(), 10000000));
      searchSourceBuilder.sort(SortBuilders.fieldSort(sortCriteria.getField()).order(sortOrder));
    }
    if(sortCriteria.getField().equalsIgnoreCase("minCtcMonthly") && sortCriteria.getOrder().equalsIgnoreCase("asc")) {
      boolQueryBuilder.mustNot(QueryBuilders.termQuery(sortCriteria.getField(), 0));
      searchSourceBuilder.sort(SortBuilders.fieldSort(sortCriteria.getField()).order(sortOrder));
    }
    searchSourceBuilder.sort(SortBuilders.fieldSort(sortCriteria.getField()).order(sortOrder));
  }


  private boolean isValidSortCriteria(SearchCriteria.SortCriteria sortCriteria) {
    return sortCriteria.getType() != null
            && !sortCriteria.getType().isEmpty()
            && sortCriteria.getField() != null
            && !sortCriteria.getField().isEmpty()
            && sortCriteria.getOrder() != null
            && !sortCriteria.getOrder().isEmpty();
  }

  private BoolQueryBuilder constructQueryForSpecificUseCases(BoolQueryBuilder boolQueryBuilder, String field, Object value) {
    if (field.equalsIgnoreCase(Constants.SALARY_RANGE) && value instanceof Map) {
      Map<String, Object> salaryRange = (Map<String, Object>) value;
      Object minSalary = salaryRange.get("minSalary");
      Object maxSalary = salaryRange.get("maxSalary");

      if (minSalary instanceof Number && maxSalary instanceof Number) {
        BoolQueryBuilder salarySubQuery = QueryBuilders.boolQuery();
        salarySubQuery.should(
                QueryBuilders.rangeQuery("minCtcMonthly")
                        .from(minSalary)
                        .to(maxSalary)
        );

        salarySubQuery.should(
                QueryBuilders.rangeQuery("maxCtcMonthly")
                        .from(minSalary)
                        .to(maxSalary)
        );

        boolQueryBuilder.must(salarySubQuery);
      }
    }
    return boolQueryBuilder;
  }

  private void addDocument(String esIndexName, String indexType, String id,
                           Map<String, Object> indexDocument1, String esRequiredFieldsJsonFilePath
  ) {
    try {
      JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
      InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(
          esRequiredFieldsJsonFilePath);
      Map<String, Object> map = objectMapper.readValue(schemaStream, new TypeReference<Map<String, Object>>() {});
      Iterator<Map.Entry<String, Object>> iterator = indexDocument1.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Object> entry = iterator.next();
        String key = entry.getKey();
        if (!map.containsKey(key)) {
          iterator.remove();
        }
      }
      IndexRequest indexRequest = new IndexRequest(esIndexName, indexType, id)
              .source(indexDocument1, XContentType.JSON);
      IndexResponse response = esConnection.getRestHighLevelClient()
              .index(indexRequest, RequestOptions.DEFAULT);

      String documentId = response.getId();
      log.info("SchemeV2ServiceImpl::addEntity: ");
      String indexName = response.getIndex();
      log.info("indexName::addDocument: ");
      long version = response.getVersion();
      if (response.getResult() == DocWriteResponse.Result.CREATED) {
        log.info("Document indexed successfully! Index: {}, ID: {}, Version: {}", indexName,
                documentId, version);
      } else if (response.getResult() == DocWriteResponse.Result.UPDATED) {
        log.info("Document updated successfully! Index: {}, ID: {}, Version: {}", indexName,
                documentId, version);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      log.error("Issue while Indexing to es: {}", e.getMessage());
    }
  }

}
