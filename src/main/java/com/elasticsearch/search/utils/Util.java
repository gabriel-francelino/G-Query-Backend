package com.elasticsearch.search.utils;

import com.elasticsearch.search.api.model.QueryParameter;
import com.elasticsearch.search.api.model.Result;
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

        return filter1 || filter2;
    }

    public static Filter createFilterClass(QueryParameter query) {
        List<String> fields = new ArrayList<>();
        if (query.getFilterReadingTime()) {
            fields.add("reading_time");
        }
        if (query.getFilterDateCreation()) {
            fields.add("dt_creation");
        }

        if (!validateFilterValues(query.getFilterMinValue()))
            query.setFilterMinValue(0);

        if (!validateFilterValues(query.getFilterMaxValue()))
            query.setFilterMaxValue(Integer.MAX_VALUE);

        if (!validateFilterValues(query.getFilterMinDate()))
            query.setFilterMinDate("0000-00-00");

        if (!validateFilterValues(query.getFilterMaxDate())) {
            LocalDate date = LocalDate.now();
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
        if (date == null || date.isBlank())
            return false;

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

    public static boolean isBooleanSearch(String query) {
        if (query == null || query.isBlank())
            return false;

        return query.contains("AND") || query.contains("OR") || query.contains("NOT");
    }

    public static boolean hasOrdering(QueryParameter parameter) {
        String readingTimeOrder = parameter.getSortByReadingTime();
        String dateCreationOrder = parameter.getSortByDateCreation();
        boolean condition1 = readingTimeOrder != null && (readingTimeOrder.equals("asc") || readingTimeOrder.equals("desc"));
        boolean condition2 = dateCreationOrder != null && (dateCreationOrder.equals("asc") || dateCreationOrder.equals("desc"));

        return condition1 || condition2;
    }

    // Operações de email a seguir
    public static boolean isValidEmail(String email) {
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(email).matches();
    }

    public static String generateEmailBody(List<Result> results) {
        StringBuilder body = new StringBuilder();
        body.append("Hello!\n\n");
        body.append("We want to thank you for using our search system! 🎉\n");
        body.append("We are excited to share the results we found for you:\n\n");

        for (Result result : results) {
            body.append("------------------------------------------------\n\n");
            body.append("🔍 **Title:** ").append(result.getTitle()).append("\n");
            body.append("🔗 **URL:** ").append(result.getUrl()).append("\n");
            body.append("📝 **Summary:** ").append(result.getAbs()).append("\n");
            body.append("⏳ **Reading time:** ").append(result.getReadingTime()).append(" minutes\n");
            body.append("📅 **Creation date:** ").append(result.getDateCreation()).append("\n\n");
//        body.append("⭐ **Favorite:** ").append(result.isFavorite() ? "Yes" : "No").append("\n\n");
        }

        body.append("------------------------------------------------\n\n");
        body.append("We hope you find this information useful!\n");
        body.append("If you need anything else, don't hesitate to contact us.\n\n");
        body.append("Thank you and see you next time! 🙌\n");
        body.append("G-Query Support Team\n");

        return body.toString();
    }

    public static String generateEmailBodyHtml(List<Result> results) {
        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html>");
        body.append("<html lang='en'>");
        body.append("<head>");
        body.append("<meta charset='UTF-8'>");
        body.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        body.append("<style>");
        body.append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f9; }");
        body.append(".container { padding: 20px; max-width: 800px; margin: 20px auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); }");
        body.append(".header { font-size: 28px; font-weight: bold; color: #333; text-align: center; margin-bottom: 20px; }");
        body.append(".result { border: 1px solid #ddd; padding: 20px; margin-bottom: 20px; border-radius: 8px; background-color: #f9f9f9; transition: transform 0.2s; }");
        body.append(".result:hover { transform: scale(1.02); }");
        body.append(".result .title { font-size: 22px; font-weight: bold; color: #333; margin-bottom: 10px; }");
        body.append(".result .url { color: #0066cc; margin-bottom: 10px; display: block; text-decoration: none; }");
        body.append(".result .url:hover { text-decoration: underline; }");
        body.append(".result .summary { margin: 10px 0; font-size: 18px; color: #555; }");
        body.append(".result .meta { font-size: 16px; color: #777; }");
        body.append("p { font-size: 18px; line-height: 1.6; color: #555; }");
        body.append("@media (max-width: 600px) {");
        body.append(".container { padding: 10px; }");
        body.append(".result { padding: 15px; }");
        body.append(".result .title { font-size: 20px; }");
        body.append(".result .summary { font-size: 16px; }");
        body.append("}");
        body.append("</style>");
        body.append("<title>Search Results</title>");
        body.append("</head>");
        body.append("<body>");
        body.append("<div class='container'>");
        body.append("<div class='header'>");
        body.append("Hello!");
        body.append("</div>");
        body.append("<p>We want to thank you for using our search system! 🎉</p>");
        body.append("<p>We are excited to share the results we found for you:</p>");

        for (Result result : results) {
            body.append("<div class='result'>");
            body.append("<div class='title'>🔍 <strong>Title:</strong> ").append(result.getTitle()).append("</div>");
            body.append("<a class='url' href='").append(result.getUrl()).append("'>🔗 <strong>URL:</strong> ").append(result.getUrl()).append("</a>");
            body.append("<div class='summary'>📝 <strong>Summary:</strong> ").append(result.getAbs()).append("</div>");
            body.append("<div class='meta'>⏳ <strong>Reading time:</strong> ").append(result.getReadingTime()).append(" minutes</div>");
            body.append("<div class='meta'>📅 <strong>Creation date:</strong> ").append(result.getDateCreation()).append("</div>");
            // body.append("<div class='meta'>⭐ <strong>Favorite:</strong> ").append(result.isFavorite() ? "Yes" : "No").append("</div>");
            body.append("</div>");
        }

        body.append("<p>We hope you find this information useful!</p>");
        body.append("<p>If you need anything else, don't hesitate to contact us.</p>");
        body.append("<p>Thank you and see you next time! 🙌</p>");
        body.append("<p>G-Query Support Team</p>");

        body.append("</div>");
        body.append("</body>");
        body.append("</html>");

        return body.toString();
    }




}
