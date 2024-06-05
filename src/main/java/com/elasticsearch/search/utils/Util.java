package com.elasticsearch.search.utils;

public class Util {
    public static Integer validatePageNumber(Integer pageNumber) {
        if (pageNumber == null || pageNumber <= 0) {
            return 1;
        }

        return pageNumber;
    }
}
