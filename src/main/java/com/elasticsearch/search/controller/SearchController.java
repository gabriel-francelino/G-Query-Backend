package com.elasticsearch.search.controller;

import com.elasticsearch.search.api.facade.SearchApi;
import com.elasticsearch.search.api.model.QueryParameter;
import com.elasticsearch.search.api.model.ResultList;
import com.elasticsearch.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@CrossOrigin
@RestController
public class SearchController implements SearchApi {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public CompletableFuture<ResponseEntity<ResultList>> search(QueryParameter queryParameter) {
        var result = searchService.submitQuery(queryParameter);
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(result));
    }

    @Override
    public CompletableFuture<ResponseEntity<String>> addToFavorites(String id) {
        searchService.addToFavorites(id);
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok("Document with id " + id + " has been added to favorites"));
    }

    @Override
    public CompletableFuture<ResponseEntity<String>> removeFromFavorites(String id) {
        searchService.removeFromFavorites(id);
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok("Document with id " + id + " has been removed from  favorites"));
    }

    @Override
    public CompletableFuture<ResponseEntity<ResultList>> searchFavorites() {
        var result = searchService.searchFavorites();
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(result));
    }
}
