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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;

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
     * If make the buffer standard. Therefore, laading text  is not a problem
     * real    5m56.632s
       user    5m38.495s
       sys     0m12.781s

     */
    private static final Trie trie = new Trie();
    public static void main(String[] args) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(FILE));

        Map<String, Double> max = new HashMap<>(), min = new HashMap<>(), sum = new HashMap<>();
        Map<String, Integer> cnt = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            var sd = line.split(";");
            String city = sd[0];
            double temperature = Double.parseDouble(sd[1]);

            max.compute(city, ((k, v) -> (v == null) ? temperature : Math.max(v, temperature)));
            min.compute(city, ((k, v) -> (v == null) ? temperature : Math.min(v, temperature)));
            sum.compute(city, ((k, v) -> (v == null) ? temperature : temperature + v));
            cnt.compute(city, ((k, v) -> (v == null) ? 1 : v + 1));
        }

        Map<String, ResultRow> measurements = new TreeMap<>();
        cnt.keySet().forEach(city -> measurements.put(city,
                new ResultRow(min.get(city), sum.get(city) / cnt.get(city), max.get(city))));

        System.out.println(measurements);
    }

    static class Trie {
        private static final int ALPHABET_LENGTH = 1024;
        Trie[] trie = new Trie[ALPHABET_LENGTH];
        ResultRow resultRow = null;
        boolean isLeaf = false;
        int count = 0;
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE, sum = 0;
        String city;

        void add(String city, double measurement) {
            var cityChars = city.toCharArray();
            var cur = this;
            for (final char ch : cityChars) {
                if (cur.trie[ch] == null)
                    cur.trie[ch] = new Trie();
                cur = cur.trie[ch];
            }
            cur.isLeaf = true;
            cur.city = city;
            cur.max = Math.max(cur.max, measurement);
            cur.min = Math.min(cur.min, measurement);
            cur.sum += measurement;
            cur.count += 1;
        }

        void printAll(){}
    }
}
