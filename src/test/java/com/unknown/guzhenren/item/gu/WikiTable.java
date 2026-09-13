package com.unknown.guzhenren.item.gu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

/**
 * Reads manually maintained wiki [设定库] tables without changing their contents.
 */

final class WikiTable {

    private final String source;
    private final String text;
    private final List<Row> rows;
    private WikiTable(String source, String text, List<Row> rows) {
        this.source = source;
        this.text = text;
        this.rows = List.copyOf(rows);
    }
    static WikiTable parse(String source, String text) {
        List<Row> rows = new ArrayList<>();
        List<String> headers = List.of();
        List<String> previous = List.of();
        int headerLine = 0;
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (!line.startsWith("|")) {
                headers = List.of();
                previous = List.of();
                continue;
            }
            List<String> cells = split(line);
            if (cells.stream().allMatch(c -> c.matches(":?-+:?"))) {
                if (previous.size() != cells.size() || previous.isEmpty()) {
                    throw new AssertionError(source + ":" + (i + 1) + " invalid table header");
                }
                headers = previous;
                headerLine = i;
            } else if (!headers.isEmpty()) {
                if (cells.size() != headers.size()) {
                    throw new AssertionError(source + ":" + (i + 1) + " expected " + headers.size()
                            + " columns, found " + cells.size());
                }
                rows.add(new Row(source + ":" + (i + 1), source + ":" + headerLine, headers, cells));
            }
            previous = cells;
        }
        return new WikiTable(source, text, rows);
    }
    Row row(String table, String label) {
        List<Row> found = rows.stream().filter(r -> r.headers().getFirst().equals(table)
                && r.cells().getFirst().equals(label)).toList();
        if (found.size() != 1) {
            throw new AssertionError(source + ": expected one row [" + table + "/" + label
                    + "], found " + found.stream().map(Row::location).toList());
        }
        return found.getFirst();
    }
    Row row(List<String> headers, String label) {
        List<Row> found = rows.stream().filter(r -> r.headers().equals(headers)
                && r.cells().getFirst().equals(label)).toList();
        if (found.size() != 1) {
            throw new AssertionError(source + ": expected one row [" + headers + "/" + label
                    + "], found " + found.stream().map(Row::location).toList());
        }
        return found.getFirst();
    }
    void requireExactRows(List<String> headers, List<String> expectedLabels) {
        List<Row> tableRows = rows.stream().filter(r -> r.headers().equals(headers)).toList();
        if (tableRows.isEmpty()) {
            throw new AssertionError(source + ": missing table [" + headers + "]");
        }
        List<String> duplicateExpected = expectedLabels.stream()
                .filter(label -> expectedLabels.indexOf(label) != expectedLabels.lastIndexOf(label))
                .distinct().toList();
        List<String> duplicateRows = tableRows.stream().map(r -> r.cells().getFirst())
                .filter(label -> tableRows.stream().filter(r -> r.cells().getFirst().equals(label)).count() > 1)
                .distinct().toList();
        List<String> missing = expectedLabels.stream()
                .filter(label -> tableRows.stream().noneMatch(r -> r.cells().getFirst().equals(label))).toList();
        List<String> stale = tableRows.stream().filter(r -> !expectedLabels.contains(r.cells().getFirst()))
                .map(Row::location).toList();
        if (!duplicateExpected.isEmpty() || !duplicateRows.isEmpty() || !missing.isEmpty() || !stale.isEmpty()) {
            String headerLocation = tableRows.getFirst().headerLocation();
            throw new AssertionError(source + ": row coverage for [" + headers + "] at " + headerLocation
                    + " duplicateMapping=" + duplicateExpected + ", duplicateRows=" + duplicateRows
                    + ", missing=" + missing + ", stale=" + stale + ", rows="
                    + tableRows.stream().map(Row::location).toList());
        }
    }
    void expectMatch(String expression, List<Long> expected) {
        Matcher matcher = Pattern.compile(expression).matcher(text);
        if (!matcher.find()) throw new AssertionError(source + ": missing numeric mapping " + expression);
        long line = text.substring(0, matcher.start()).chars().filter(c -> c == '\n').count() + 1;
        String location = source + ":" + line;
        List<Long> actual = new ArrayList<>();
        for (int i = 1; i <= matcher.groupCount(); i++) actual.add(number(matcher.group(i), location));
        compare(location, expression, expected, actual);
        if (matcher.find()) throw new AssertionError(source + ": duplicate numeric mapping " + expression);
    }
    static void requireCoverage(List<String> mapped, List<String> registered) {
        if (new HashSet<>(mapped).size() != mapped.size()) throw new AssertionError("Duplicate item mapping");
        if (!new HashSet<>(mapped).equals(new HashSet<>(registered))) {
            List<String> missing = registered.stream().filter(id -> !mapped.contains(id)).toList();
            List<String> stale = mapped.stream().filter(id -> !registered.contains(id)).toList();
            throw new AssertionError("Item mapping missing=" + missing + ", stale=" + stale);
        }
    }
    private static List<String> split(String line) {
        List<String> result = new ArrayList<>();
        int start = 1;
        boolean wiki = false;
        for (int i = 1; i < line.length(); i++) {
            if (line.startsWith("[[", i)) wiki = true;
            if (line.startsWith("]]", i)) wiki = false;
            if (line.charAt(i) == '|' && !wiki && line.charAt(i - 1) != '\\') {
                result.add(line.substring(start, i).strip());
                start = i + 1;
            }
        }
        if (start < line.length()) result.add(line.substring(start).strip());
        return List.copyOf(result);
    }
    private static long number(String value, String location) {
        String normalized = value.strip().replaceAll("^[×+]", "").replaceAll("%$", "");
        int rank = "一二三四五六七八九".indexOf(normalized);
        if (normalized.length() == 1 && rank >= 0) return rank + 1L;
        if (!normalized.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)")) {
            throw new AssertionError(location + ": invalid integer '" + value + "'");
        }
        try {
            return Long.parseLong(normalized.replace(",", ""));
        } catch (NumberFormatException error) {
            throw new AssertionError(location + ": integer out of range '" + value + "'", error);
        }
    }
    private static void compare(String location, String field, List<Long> expected, List<Long> actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(location + " [" + field + "] code=" + expected + ", wiki=" + actual);
        }
    }
    record Row(String location, String headerLocation, List<String> headers, List<String> cells) {

        String cell(String column) {
            int index = headers.indexOf(column);
            if (index < 0 || index != headers.lastIndexOf(column)) {
                throw new AssertionError(location + ": missing or duplicate column " + column);
            }
            return cells.get(index);
        }
        void expect(String column, List<Long> expected) {expectValue(column, cell(column), expected);}
        void expectValue(String field, String value, List<Long> expected) {
            List<Long> actual;
            if (value.matches("[一二三四五六七八九]\\.\\.[一二三四五六七八九]")) {
                long from = number(value.substring(0, 1), location);
                long to = number(value.substring(3), location);
                actual = LongStream.rangeClosed(from, to).boxed().toList();
            } else {
                actual = Arrays.stream(value.split("/|\\.\\.", -1))
                        .map(v -> number(v, location)).toList();
            }
            compare(location, field, expected, actual);
        }
    }
}
