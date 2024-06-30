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

    public ResultList searchFavorites(Integer pageNumber) {
        var searchResponse = esClient.searchFavorites(pageNumber);

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
                                .dateCreation(h.source().get("dt_creation").asText())
                                .isFavorite(true);
                    }
                    return new Result();
                }
        ).collect(Collectors.toList());

        int currentPage = pageNumber != null ? pageNumber : 1;

        return new ResultList()
                .searchTime(searchTime)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .results(results);
    }

    public ResultList submitQuery(QueryParameter queryParameter) {
        var searchResponse = esClient.search(queryParameter);

        var hitsList = searchResponse.hits();
        int totalHits = (int) (hitsList.total() != null ? hitsList.total().value() : 0);
        int totalPages = (int) Math.ceil((double) totalHits / EsClient.PAGE_SIZE);
        var searchTime = (int) searchResponse.took();
        String suggest = getSuggestion(searchResponse);
        List<Hit<ObjectNode>> hits = hitsList.hits();

        var results = generateResultsList(hits);

        int currentPage = queryParameter.getPageNumber() != null ? queryParameter.getPageNumber() : 1;

        return new ResultList()
                .searchTime(searchTime)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .suggestion(suggest)
                .results(results)
                .currentPage(currentPage);
    }

    private List<Result> generateResultsList(List<Hit<ObjectNode>> hits) {
        return hits.stream().map(h -> {
                    if (h.source() != null) {
                        return new Result()
                                .id(h.id())
                                .abs(treatContent(h.source().get("content").asText()))
                                .title(h.source().get("title").asText())
                                .url(h.source().get("url").asText())
                                .readingTime(h.source().get("reading_time").asInt())
                                .dateCreation(h.source().get("dt_creation").asText())
                                .highlight(h.highlight().get("content").get(0))
                                .isFavorite(isFavoriteHit(h.id()));
                    }
                    return new Result();
                }
        ).collect(Collectors.toList());
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

    private boolean isFavoriteHit(String hitId) {
        List<Hit<ObjectNode>> favoriteHits = esClient.searchFavorites().hits().hits();

        for (Hit<ObjectNode> favoriteHit : favoriteHits) {
            if (favoriteHit.id().equals(hitId)) {
                return true;
            }
        }

        return false;
    }

    private String treatContent(String content) {
        content = content.replaceAll("</?(som|math)\\d*>", "");
        content = content.replaceAll("[^A-Za-z\\s]+", "");
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll("^\\s+", "");
        return content;
    }
}
