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
        body.append("Olá!\n\n");
        body.append("Queremos agradecer por utilizar nosso sistema de busca! 🎉\n");
        body.append("Estamos animados para compartilhar os resultados que encontramos para você:\n\n");

        for (Result result : results) {
            body.append("------------------------------------------------\n");
            body.append("🔍 **Título:** ").append(result.getTitle()).append("\n");
            body.append("🔗 **URL:** ").append(result.getUrl()).append("\n");
            body.append("📝 **Resumo:** ").append(result.getAbs()).append("\n");
            body.append("⏳ **Tempo de leitura:** ").append(result.getReadingTime()).append(" minutos\n");
            body.append("📅 **Data de criação:** ").append(result.getDateCreation()).append("\n");
//            body.append("⭐ **Favorito:** ").append(result.isFavorite() ? "Sim" : "Não").append("\n\n");
        }

        body.append("------------------------------------------------\n\n");
        body.append("Esperamos que você encontre essas informações úteis!\n");
        body.append("Se precisar de mais alguma coisa, não hesite em nos contatar.\n\n");
        body.append("Obrigado e até a próxima! 🙌\n");
        body.append("Equipe de Suporte\n");

        return body.toString();
    }

}
