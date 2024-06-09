package com.elasticsearch.search.utils;

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
