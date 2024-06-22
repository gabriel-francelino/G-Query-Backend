package com.elasticsearch.search.domain;

import lombok.Builder;
import lombok.ToString;

import java.util.List;

@Builder
public record Filter(
        List<String> fields,
        int minValue,
        int maxValue,
        String minDate,
        String maxDate) {
}
