package com.elasticsearch.search.controller;

import com.elasticsearch.search.api.facade.SearchApi;
import com.elasticsearch.search.api.model.ResultList;
import com.elasticsearch.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@CrossOrigin
@RestController
public class SearchController implements SearchApi {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public CompletableFuture<ResponseEntity<ResultList>> search(String query, Integer pageNumber) {
        var result = searchService.submitQuery(query, pageNumber);
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(result));
    }
}
