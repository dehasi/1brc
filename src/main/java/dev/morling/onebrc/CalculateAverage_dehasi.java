/*
 *  Copyright 2023 The original authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.morling.onebrc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.MIN_VALUE;

public class CalculateAverage_dehasi {

    private static final String FILE = "./measurements.txt";

    private static record Measurement(String station, double value) {
        private Measurement(String[] parts) {
            this(parts[0], Double.parseDouble(parts[1]));
        }
    }

    private static record ResultRow(double min, double mean, double max) {
        public String toString() {
            return round(min) + "/" + round(mean) + "/" + round(max);
        }

        private double round(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }

    private static class MeasurementAggregator {
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;
        private double sum;
        private long count;
    }

    /*
     * Baseline:
     * real 4m11.364s
     * user 3m51.048s
     * sys 0m11.471s
     *
     * My Code
     * real 5m41.593s
     * user 5m20.700s
     * sys 0m10.316s
     *
     * If make the buffer standard. Therefore, laading text is not a problem
     * real 5m56.632s
     * user 5m38.495s
     * sys 0m12.781s
     *
     */
    private static final Trie trie = new Trie();

    public static void main(String[] args) throws IOException {

        Files.lines(Path.of(FILE)).forEach(line -> {
            var sd = line.split(";");
            String city = sd[0];
            double temperature = Double.parseDouble(sd[1]);
            trie.add(city, temperature);
        });

        Map<String, ResultRow> measurements = new TreeMap<>();
        trie.fill(measurements);
        System.out.println(measurements);
    }

    static class Trie {
        private final Map<Character, Trie> trie = new TreeMap<>();
        private boolean isLeaf = false;
        private int count = 0;
        private double min = MAX_VALUE, max = -MAX_VALUE, sum = 0;
        private String city;

        void add(String city, double temperature) {
            var cityChars = city.toCharArray();
            var cur = this;
            for (final char ch : cityChars) {
                cur.trie.putIfAbsent(ch, new Trie());
                cur = cur.trie.get(ch);
            }
            cur.isLeaf = true;
            cur.city = city;
            cur.max = Math.max(cur.max, temperature);
            cur.min = Math.min(cur.min, temperature);
            cur.sum += temperature;
            cur.count += 1;
        }

        void fill(Map<String, ResultRow> measurements) {
            if (isLeaf) {
                measurements.put(city, new ResultRow(min, sum / count, max));
            }

            for (final var chileTrie : trie.values()) {
                chileTrie.fill(measurements);
            }
        }

        void printAll() {
        }
    }
}
