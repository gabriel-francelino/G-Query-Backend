package com.elasticsearch.search.domain;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
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

//    public SearchResponse search(QueryParameter queryParameter) {
//        String query = queryParameter.getQuery();
//        Integer pageNumber = queryParameter.getPageNumber();
//        Query matchQuery = MatchQuery.of(q -> q.field("content").query(query))._toQuery();
//
//        Suggester phraseSuggestion = getPhraseSuggestion(query);
//
//        SearchResponse<ObjectNode> response;
//        Integer currencyPage = (PAGE_SIZE * (pageNumber - 1));
//
//        try {
//            response = elasticsearchClient.search(s -> s
//                    .index("wikipedia")
//                    .from(currencyPage)
//                    .size(PAGE_SIZE)
//                    .query(matchQuery)
//                    .suggest(phraseSuggestion), ObjectNode.class
//            );
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        return response;
//    }

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

        Suggester phraseSuggestion = getPhraseSuggestion(query);

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
//        highlightBuilder.highlightQuery(queryCompleted);

        try {
            response = elasticsearchClient.search(s -> s
                    .index("wikipedia")
                    .from(currentPage)
                    .size(PAGE_SIZE)
                    .query(queryCompleted)
                    .highlight(highlightBuilder.build())
                    .suggest(phraseSuggestion), ObjectNode.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return response;
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
        Query matchQuery = MatchQuery.of(q -> q.field("content").query(query))._toQuery();
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
            boolQuery.should(matchQuery);
        } else {
            boolQuery.must(matchQuery);
        }
    }

    private void generateBooleanSearchQuery(BoolQuery.Builder boolQuery, String query) {
        if (query.contains("NOT")) {
            String[] queryList = query.split("NOT");
            query = queryList[0].trim();
            String queryNot = queryList[1].trim();
            Query mustNotQuery = MatchQuery.of(q -> q.field("content").query(queryNot))._toQuery();
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
                    Query matchPhraseQuery = MatchQuery.of(
                            m -> m.field("content").query(string)
                    )._toQuery();

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
            String finalQuery = query;
            Query matchQuery = MatchQuery.of(q -> q.field("content").query(finalQuery))._toQuery();
            boolQuery.must(matchQuery);
        }

    }

    private List<Query> generateQueryOR(List<String> queryOR) {
        List<Query> shouldQueriesOR = new ArrayList<>();

        for (String term : queryOR) {
            Query matchQuery = MatchQuery.of(
                    m -> m.field("content").query(term.trim())
            )._toQuery();

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
                        .highlight(h -> h
                                .preTag("<strong><em>")
                                .postTag("</em></strong>")))));
    }
}
