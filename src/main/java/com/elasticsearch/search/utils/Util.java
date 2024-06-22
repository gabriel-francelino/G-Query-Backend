package com.elasticsearch.search.utils;

import com.elasticsearch.search.api.model.QueryParameter;
import com.elasticsearch.search.domain.Filter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static Integer validatePageNumber(Integer pageNumber) {
        if (pageNumber == null || pageNumber <= 0) {
            return 1;
        }

        return pageNumber;
    }

    public static boolean hasFilter(QueryParameter query) {
        boolean filter1 = query.getFilterReadingTime();
        boolean filter2 = query.getFilterDateCreation();

        return !filter1 || !filter2;
    }

    public static Filter createFilterClass(QueryParameter query) {
        List<String> fields = new ArrayList<>();
        if (query.getFilterReadingTime()) {
            fields.add("reading_time");
        }
        if (query.getFilterDateCreation()) {
            fields.add("date_creation");
        }

        if (!validateFilterValues(query.getFilterMinValue()))
            query.setFilterMinValue(0);

        if (!validateFilterValues(query.getFilterMaxValue()))
            query.setFilterMaxValue(Integer.MAX_VALUE);

        if (!validateFilterValues(query.getFilterMinDate()))
            query.setFilterMinDate("0000-00-00");

        if (!validateFilterValues(query.getFilterMaxDate())) {
            // Pegar data atual no formato aaaa-mm-dd
            LocalDate date = LocalDate.now();
            // salvar no formato aaaa-mm-dd
            query.setFilterMaxDate(date.toString());
        }


        return Filter.builder()
                .fields(fields)
                .minValue(query.getFilterMinValue())
                .maxValue(query.getFilterMaxValue())
                .minDate(query.getFilterMinDate())
                .maxDate(query.getFilterMaxDate())
                .build();
    }

    private static boolean validateFilterValues(int value) {
        return value >= 0;
    }

    private static boolean validateFilterValues(String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    public static List<String> containsMatchPhrase(String string) {
        List<String> phrases = new ArrayList<>();

        String regex = "\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);

        while (matcher.find()) {
            phrases.add(matcher.group(1));
        }

        return phrases;
    }
}
