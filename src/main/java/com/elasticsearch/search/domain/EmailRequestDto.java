package com.elasticsearch.search.domain;

import com.elasticsearch.search.api.model.Result;

import java.util.List;

public record EmailRequestDto(String receiver, List<Result> results) {
}
