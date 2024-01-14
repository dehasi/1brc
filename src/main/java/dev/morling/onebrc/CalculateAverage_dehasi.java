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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Double.MAX_VALUE;

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
     *
     * with Trie on Tree Map
     * real 7m24.268s
     * user 7m2.932s
     * sys 0m16.625s
     *
     * with Trie on HashMap
     * real 5m36.547s
     * user 5m26.247s
     * sys 0m12.309s
     *
     * On direct priting, no treemap
     * real 5m26.768s
     * user 5m16.908s
     * sys 0m12.379s
     *
     *
     * event loner id hash = 256
     *
     * real 6m58.572s
     * user 6m45.140s
     * sys 0m13.356s
     *
     * HashMap with default size, deleted sp
     * real 5m28.171s
     * user 5m14.669s
     * sys 0m12.979s
     *
     * Fill TreeMap then print, no keys sorting
     * real    5m30.433s
       user    5m15.379s
       sys     0m12.684s

     */
    private static final Trie trie = new Trie();

    public static void main(String[] args) throws IOException {

        Files.lines(Path.of(FILE)).forEach(line -> {
            int split = line.indexOf(';');
            String city = line.substring(0, split);
            double temperature = Double.parseDouble(line.substring(split + 1));
            trie.add(city, temperature);
        });

        // start = true;
        // trie.printAll();
        // System.out.println("}");
        Map<String, ResultRow> measurements = new TreeMap<>();
        trie.fill(measurements);
        System.out.println(measurements);
    }

    private static boolean start = true;

    static class Trie {
        public static final String START = "{";
        public static final String MID_SEP = ", ";

        private final Map<Character, Trie> trie = new HashMap<>();

        private int count = 0;
        private double min = MAX_VALUE, max = -MAX_VALUE, sum = 0;
        private String city;

        void add(String city, double temperature) {
            var cur = this;
            for (int i = 0; i < city.length(); ++i) {
                char ch = city.charAt(i);
                cur.trie.putIfAbsent(ch, new Trie());
                cur = cur.trie.get(ch);
            }
            cur.city = city;
            cur.max = Math.max(cur.max, temperature);
            cur.min = Math.min(cur.min, temperature);
            cur.sum += temperature;
            cur.count += 1;
        }

        void fill(Map<String, ResultRow> measurements) {
            if (city != null) {
                measurements.put(city, new ResultRow(min, sum / count, max));
            }
            // ArrayList<Character> keys = new ArrayList<>(trie.keySet());
            // keys.sort(Character::compareTo);
            for (final var child : trie.values()) {
                child.fill(measurements);
                // trie.get(key).fill(measurements);
            }
        }

        private double round(double value) {
            return Math.round(value * 10.0) / 10.0;
        }

        // ", "
        void printAll() {
            if (city != null) {
                String prefix = start ? START : MID_SEP;
                System.out.print(prefix + city + "=" + round(min) + "/" + round(sum / count) + "/" + round(max));
                start = false;
            }
            ArrayList<Character> keys = new ArrayList<>(trie.keySet());
            keys.sort(Character::compareTo);
            for (final var key : keys) {
                trie.get(key).printAll();
            }
        }
    }
}
