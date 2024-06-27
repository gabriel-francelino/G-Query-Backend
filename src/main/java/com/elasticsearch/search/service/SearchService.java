package com.elasticsearch.search.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.elasticsearch.search.api.model.QueryParameter;
import com.elasticsearch.search.api.model.Result;
import com.elasticsearch.search.api.model.ResultList;
import com.elasticsearch.search.domain.EsClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final EsClient esClient;


    public SearchService(EsClient esClient) {
        this.esClient = esClient;
    }

    public void addToFavorites(String id) {
        esClient.favoriteDocument(id);
    }

    public void removeFromFavorites(String id) {
        esClient.unfavoriteDocument(id);
    }

    public ResultList searchFavorites() {
        var searchResponse = esClient.searchFavorites();

        var hitsList = searchResponse.hits();
        int totalHits = (int) (hitsList.total() != null ? hitsList.total().value() : 0);
        int totalPages = (int) Math.ceil((double) totalHits / EsClient.PAGE_SIZE);
        var searchTime = (int) searchResponse.took();
        List<Hit<ObjectNode>> hits = hitsList.hits();

        var results = hits.stream().map(h -> {
                    if (h.source() != null) {
                        return new Result()
                                .id(h.id())
                                .abs(treatContent(h.source().get("content").asText()))
                                .title(h.source().get("title").asText())
                                .url(h.source().get("url").asText())
                                .readingTime(h.source().get("reading_time").asInt())
                                .dateCreation(h.source().get("dt_creation").asText());
                    }
                    return new Result();
                }
        ).collect(Collectors.toList());

        return new ResultList()
                .searchTime(searchTime)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .results(results);
    }

    public ResultList submitQuery(QueryParameter queryParameter) {
        var searchResponse = esClient.search(queryParameter);

        var hitsList = searchResponse.hits();
        int totalHits = (int) (hitsList.total() != null ? hitsList.total().value() : 0);
        int totalPages = (int) Math.ceil((double) totalHits / EsClient.PAGE_SIZE);
        var searchTime = (int) searchResponse.took();
        List<Hit<ObjectNode>> hits = hitsList.hits();

        String suggest = getSuggestion(searchResponse);

        var results = hits.stream().map(h -> {
                    if (h.source() != null) {
                        return new Result()
                                .id(h.id())
                                .abs(treatContent(h.source().get("content").asText()))
                                .title(h.source().get("title").asText())
                                .url(h.source().get("url").asText())
                                .readingTime(h.source().get("reading_time").asInt())
                                .dateCreation(h.source().get("dt_creation").asText())
                                .highlight(h.highlight().get("content").get(0));
                    }
                    return new Result();
                }
        ).collect(Collectors.toList());

        int currentPage = queryParameter.getPageNumber() != null ? queryParameter.getPageNumber() : 1;
        return new ResultList()
                .searchTime(searchTime)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .suggestion(suggest)
                .results(results)
                .currentPage(currentPage);
    }

    private String getSuggestion(SearchResponse searchResponse) {
        Map<String, List<Suggestion>> suggestion = searchResponse.suggest();

        var suggestionList = suggestion
                .get("suggest_phrase")
                .get(0)
                .phrase()
                .options();

        if (suggestionList.isEmpty())
            return "";

        return suggestionList.get(0).highlighted();
    }

    private String treatContent(String content) {
        content = content.replaceAll("</?(som|math)\\d*>", "");
        content = content.replaceAll("[^A-Za-z\\s]+", "");
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll("^\\s+", "");
        return content;
    }
}
