package com.getyourguide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

class Solution {

    private static class ParsedPhotoName implements Comparable<ParsedPhotoName> {

        private final String fileExtension;
        private final String city;
        private final long time;

        private ParsedPhotoName(String fileExtension, String city, long time) {
            this.fileExtension = fileExtension;
            this.city = city;
            this.time = time;
        }

        public long getTime() {
            return time;
        }

        public String getCity() {
            return city;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        @Override
        public int compareTo(ParsedPhotoName o) {
            return Long.compare(this.time, o.time);
        }

        @Override
        public String toString() {
            return "ParsedPhotoName{" +
                    "fileExtension='" + fileExtension + '\'' +
                    ", city='" + city + '\'' +
                    ", time=" + time +
                    '}';
        }
    }

    private static interface PhotoNameParser {

        ParsedPhotoName parseName(String name);
    }

    private static interface PhotoNameGenerator {

        void addName(ParsedPhotoName name);

        String generate();
    }

    private static class RegxPhotoNameParser implements PhotoNameParser {

        private final Pattern FILE_NAME_PATTERN = Pattern.compile("[A-Za-z]*\\.(jpeg|jpg|png)");
        private final Pattern CITY_NAME_PATTERN = Pattern.compile("[A-Za-z]*");
        private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        private String validateAndParseExtension(String fileName) {
            if (!FILE_NAME_PATTERN.matcher(fileName).matches()) {
                throw new IllegalStateException("invalid file name");
            }
            return fileName.split("\\.")[1];
        }

        private String validateCityNameAndGetIt(String cityPart) {
            if (!CITY_NAME_PATTERN.matcher(cityPart).matches()) {
                throw new IllegalStateException("invalid city name");
            }
            return cityPart;
        }

        private long validateDateAndTime(String datePart) {
            try {
                Date date = DATE_FORMAT.parse(datePart);
                return date.toInstant().toEpochMilli();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ParsedPhotoName parseName(String name) {
            String[] parts = name.split(",");
            return new ParsedPhotoName(
                    validateAndParseExtension(parts[0].trim()),
                    validateCityNameAndGetIt(parts[1].trim()),
                    validateDateAndTime(parts[2].trim())
            );
        }
    }

    private static class SimplePhotoNameGenerator implements PhotoNameGenerator {

        private static class ParseNameHolder implements Comparable<ParseNameHolder> {

            private final ParsedPhotoName name;
            private final StringBuilder stringBuilder;

            private ParseNameHolder(ParsedPhotoName name) {
                this.name = name;
                this.stringBuilder = new StringBuilder();
            }

            public ParsedPhotoName getName() {
                return name;
            }

            public StringBuilder getStringBuilder() {
                return stringBuilder;
            }

            @Override
            public int compareTo(ParseNameHolder o) {
                return this.name.compareTo(o.name);
            }
        }

        private final List<ParseNameHolder> names = new ArrayList<>();
        private final Map<String, List<ParseNameHolder>> namesByCity = new HashMap<>();

        @Override
        public void addName(ParsedPhotoName name) {
            ParseNameHolder holder = new ParseNameHolder(name);
            names.add(holder);
            List<ParseNameHolder> sorted = namesByCity.computeIfAbsent(name.city, s -> new ArrayList<>());
            int index = Collections.binarySearch(sorted, holder);
            if (index < 0) {
                index = -index - 1;
            }
            sorted.add(index, holder);
        }

        private int lenOfNumber(int i) {
            int len = 0;
            while (i > 0) {
                i /= 10;
                ++len;
            }
            return len;
        }

        private String fixedLenStringFormat(int i) {
            return "%0"+lenOfNumber(i)+"d";
        }

        @Override
        public String generate() {

            for (List<ParseNameHolder> sorted : namesByCity.values()) {
                String format = fixedLenStringFormat(sorted.size());
                for (int i = 0; i < sorted.size(); i++) {
                    ParseNameHolder holder = sorted.get(i);
                    holder.getStringBuilder()
                            .append(holder.getName().getCity())
                            .append(String.format(format, i+1))
                            .append(".")
                            .append(holder.getName().getFileExtension());
                }
            }

            StringBuilder result = new StringBuilder();
            for (ParseNameHolder holder : names) {
                result.append(holder.getStringBuilder().toString())
                        .append("\r\n");
            }
            return result.toString();
        }
    }

    private static String parse(String input, PhotoNameParser parser, Supplier<PhotoNameGenerator> generatorSupplier) {
        String[] lines = input.split("\r\n");
        PhotoNameGenerator generator = generatorSupplier.get();
        for (String line : lines) {
            generator.addName(parser.parseName(line));
        }
        return generator.generate();
    }

    public static String solution(String s) {
        return parse(s, new RegxPhotoNameParser(), SimplePhotoNameGenerator::new);
    }

    public static void main(String[] args) {
        String input = "photo.jpg, Warsaw, 2013-09-05 14:08:15\r\n" +
                "aa.jpg, Warsaw, 2013-09-05 14:08:15\r\n" +
                "sds.jpg, Warsaw, 2013-09-05 14:08:15\r\n" +
                "d.jpg, Warsaw, 2013-09-05 14:08:15\r\n" +
                "gghh.jpg, Warsaw, 2013-09-05 14:08:15\r\n" +
                "ssd.jpg, Warsaw, 2013-06-05 14:08:15\r\n" +
                "xsd.jpg, Warsaw, 2013-09-05 14:08:15\r\n" +
                "dsadvv.jpg, Warsaw, 2013-06-05 14:08:15\r\n" +
                "sdased.jpg, Warsaw, 2013-02-05 14:08:15\r\n" +
                "bgg.jpg, Warsaw, 2013-09-02 14:08:15\r\n" +
                "adas.jpg, Warsaw, 2013-09-05 14:08:15\r\n" +
                "a.jpg, London, 2013-09-05 14:08:15\r\n" +
                "b.jpg, Warsaw, 2013-09-04 14:08:15"; //and so on !
        System.out.println(solution(input));
    }
}