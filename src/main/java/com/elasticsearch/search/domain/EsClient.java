package com.elasticsearch.search.domain;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.elasticsearch.search.api.model.QueryParameter;
import com.elasticsearch.search.utils.Util;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.altindag.ssl.SSLFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EsClient {
    private ElasticsearchClient elasticsearchClient;
    public final static int PAGE_SIZE = 10;

    public EsClient() {
        createConnection();
    }

    private void createConnection() {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        String USER = "elastic";
        String PWD = "user123";
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(USER, PWD));

        SSLFactory sslFactory = SSLFactory.builder()
                .withUnsafeTrustMaterial()
                .withUnsafeHostnameVerifier()
                .build();

        RestClient restClient = RestClient.builder(
                        new HttpHost("localhost", 9200, "https"))
                .setHttpClientConfigCallback((HttpAsyncClientBuilder httpClientBuilder) -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLContext(sslFactory.getSslContext())
                        .setSSLHostnameVerifier(sslFactory.getHostnameVerifier())
                ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        elasticsearchClient = new co.elastic.clients.elasticsearch.ElasticsearchClient(transport);
    }

    public void favoriteDocument(String id) {
        try {
            Query matchQuery = MatchQuery.of(q -> q.field("_id").query(id))._toQuery();

            elasticsearchClient.reindex(r -> r
                    .source(s -> s
                            .index("wikipedia")
                            .query(matchQuery))
                    .dest(d -> d
                            .index("wikipedia_fav")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unfavoriteDocument(String id) {
        try {
            Query matchQuery = MatchQuery.of(q -> q.field("_id").query(id))._toQuery();

            elasticsearchClient.deleteByQuery(d -> d
                    .index("wikipedia_fav")
                    .query(matchQuery));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SearchResponse searchFavorites(Integer pageNumber) {
        pageNumber = Util.validatePageNumber(pageNumber);
        Integer currentPage = (PAGE_SIZE * (pageNumber - 1));

        try {
            return elasticsearchClient.search(s -> s
                    .index("wikipedia_fav")
                    .from(currentPage), ObjectNode.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SearchResponse searchFavorites() {
        try {
            return elasticsearchClient.search(s -> s
                    .index("wikipedia_fav"), ObjectNode.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SearchResponse search(QueryParameter queryParameter) {
        String query = queryParameter.getQuery();
        Integer pageNumber = Util.validatePageNumber(queryParameter.getPageNumber());

        SearchResponse<ObjectNode> response;
        Integer currentPage = (PAGE_SIZE * (pageNumber - 1));

//        Suggester phraseSuggestion = getPhraseSuggestion(query);
        Suggester phraseSuggestion = generateTermSuggestion(query);

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (!Util.isBooleanSearch(query)) {
            generateSimpleQuery(boolQueryBuilder, query);
        } else {
            generateBooleanSearchQuery(boolQueryBuilder, query);
        }

        if (Util.hasFilter(queryParameter)) {
            Filter filter = Util.createFilterClass(queryParameter);
            generateFilterQuery(boolQueryBuilder, filter);
        }

        Query queryCompleted = boolQueryBuilder.build()._toQuery();

        Highlight.Builder highlightBuilder = generateHighlight();

        SearchRequest.Builder searchRequest = new SearchRequest.Builder()
                .index("wikipedia")
                .from(currentPage)
                .size(PAGE_SIZE)
                .query(queryCompleted)
                .highlight(highlightBuilder.build())
                .suggest(phraseSuggestion);

        if (Util.hasOrdering(queryParameter)) {
            List<SortOptions> sortOptions = generateSortOptions(queryParameter);
            searchRequest.sort(sortOptions);
        }

        try {
            response = elasticsearchClient.search(searchRequest.build(), ObjectNode.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    private Query generateMatchQuery(String query, String field){
        return new MatchQuery.Builder()
                .field(field)
                .query(query)
                .build()
                ._toQuery();
    }

    private Query generateMultiMatchQuery(String query, String... fields) {
        return new MultiMatchQuery.Builder()
                .fields(List.of(fields))
                .query(query)
                .build()
                ._toQuery();
    }

    private List<SortOptions> generateSortOptions(QueryParameter queryParameter) {
        List<SortOptions> sortOptionsList =  new ArrayList<>();

        if (queryParameter.getSortByReadingTime() != null) {
            SortOrder order = queryParameter.getSortByReadingTime().equals("asc") ? SortOrder.Asc: SortOrder.Desc;
            FieldSort fieldSort1 = FieldSort.of(s -> s
                    .field("reading_time")
                    .order(order));

            SortOptions sort1 = SortOptions.of(s -> s.field(fieldSort1));
            sortOptionsList.add(sort1);
        }

        if (queryParameter.getSortByDateCreation() != null) {
            SortOrder order = queryParameter.getSortByDateCreation().equals("asc") ? SortOrder.Asc: SortOrder.Desc;
            FieldSort fieldSort2 = FieldSort.of(s -> s
                    .field("dt_creation")
                    .order(order));

            SortOptions sort2 = SortOptions.of(s -> s.field(fieldSort2));
            sortOptionsList.add(sort2);
        }

        return sortOptionsList;
    }

    private Highlight.Builder generateHighlight() {
        Highlight.Builder highlightBuilder = new Highlight.Builder();
        HighlightField.Builder highlightField = new HighlightField.Builder();
        Map<String, HighlightField> map = new HashMap<>();
        map.put("content", highlightField.build());
        return highlightBuilder
                .preTags("<strong>")
                .postTags("</strong>")
                .numberOfFragments(1)
                .fragmentSize(500)
                .fields(map);
    }

    private void generateFilterQuery(BoolQuery.Builder boolQuery, Filter filter) {
        List<Query> filters = new ArrayList<>();

        if (filter.fields().contains("reading_time")) {
            Query filter1 = RangeQuery.of(r -> r
                    .field("reading_time")
                    .gte(JsonData.of(filter.minValue()))
                    .lte(JsonData.of(filter.maxValue()))
            )._toQuery();
            filters.add(filter1);
        }

        if (filter.fields().contains("dt_creation")) {
            Query filter2 = RangeQuery.of(r -> r
                    .field("dt_creation")
                    .gte(JsonData.of(filter.minDate()))
                    .lte(JsonData.of(filter.maxDate()))
            )._toQuery();
            filters.add(filter2);
        }

        boolQuery.filter(filters);
    }

    private void generateSimpleQuery(BoolQuery.Builder boolQuery, String query) {
//        Query matchQuery = MatchQuery.of(q -> q.field("content").query(query))._toQuery();
        Query multiMatchQuery = generateMultiMatchQuery(query, "title", "content");
        List<String> phrases = Util.containsMatchPhrase(query);

        if (!phrases.isEmpty()) {
            List<Query> queries = new ArrayList<>();

            for (String phrase : phrases) {
                Query matchPhraseQuery = MatchPhraseQuery.of(
                        m -> m.field("content").query(phrase).slop(1)
                )._toQuery();

                queries.add(matchPhraseQuery);
            }

            boolQuery.must(queries);
            boolQuery.should(multiMatchQuery);
        } else {
            boolQuery.must(multiMatchQuery);
        }
    }

    private void generateBooleanSearchQuery(BoolQuery.Builder boolQuery, String query) {
        if (query.contains("NOT")) {
            String[] queryList = query.split("NOT");
            query = queryList[0].trim();
            String queryNot = queryList[1].trim();

            Query mustNotQuery = generateMultiMatchQuery(queryNot, "title", "content");
            boolQuery.mustNot(mustNotQuery);
        }

        if (Util.isBooleanSearch(query)) {
            if (query.contains("AND")) {
                List<String> arrayWithAND = new ArrayList<>(List.of(query.split("AND")));

                if (arrayWithAND.contains("OR")) {
                    List<String> auxOR = new ArrayList<>();
                    List<String> arrOR = arrayWithAND.stream()
                            .filter(s -> s.contains("OR"))
                            .toList();

                    for (String string : arrOR) {
                        String[] terms = string.split("OR");
                        for (String term : terms) {
                            auxOR.add(term.trim());
                        }
                    }

                    List<Query> shouldQueriesOR = generateQueryOR(auxOR);

                    boolQuery.should(shouldQueriesOR);
                    arrayWithAND.removeIf(s -> s.contains("OR"));
                }

                List<Query> mustQueriesAND = new ArrayList<>();
                for (String string : arrayWithAND) {
                    Query matchPhraseQuery = generateMultiMatchQuery(string, "title", "content");

                    mustQueriesAND.add(matchPhraseQuery);
                }

                boolQuery.must(mustQueriesAND);
            }

            if (query.contains("OR") && !query.contains("AND")) {
                List<String> arrayWithOR = new ArrayList<>(List.of(query.split("OR")));

                List<Query> shouldQueriesOR = generateQueryOR(arrayWithOR);

                boolQuery.should(shouldQueriesOR);
            }
        } else {
            Query matchQuery = generateMultiMatchQuery(query, "title", "content");
            boolQuery.must(matchQuery);
        }

    }

    private List<Query> generateQueryOR(List<String> queryOR) {
        List<Query> shouldQueriesOR = new ArrayList<>();

        for (String term : queryOR) {
            Query matchQuery = generateMultiMatchQuery(term.trim(), "title", "content");

            shouldQueriesOR.add(matchQuery);
        }

        return shouldQueriesOR;
    }

    private Suggester getPhraseSuggestion(String query) {
        return Suggester.of(s -> s
                .suggesters("suggest_phrase", ts -> ts
                    .text(query)
                    .phrase(p -> p
                            .field("content")
                            .size(1)
                            .gramSize(3)
                            .confidence(0.5)
                            .highlight(h -> h
                                    .preTag("<strong><em>")
                                    .postTag("</strong></em>")))));
    }

    private Suggester generateTermSuggestion(String query) {
        return Suggester.of(s -> s
                .suggesters("suggest_term", ts -> ts
                        .text(query)
                        .term(t -> t
                                .field("content")
                                .size(1)
                                .minWordLength(2))));
    }
}
