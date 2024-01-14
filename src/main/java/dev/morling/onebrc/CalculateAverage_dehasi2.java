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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CalculateAverage_dehasi2 {

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
        private final String city;
        private double min;
        private double max;
        private double sum;
        private long count = 1;

        public MeasurementAggregator(String city, double temperature) {
            this.city = city;
            min = max = sum = temperature;
        }
    }

    /*
     * Baseline:
     * real 4m11.364s
     * user 3m51.048s
     * sys 0m11.471s
     *
     * real 5m13.501s
     * user 4m55.923s
     * sys 0m11.772s
     *
     * Try delete string.intern
     *
     * real 3m28.715s
     * user 3m13.798s
     * sys 0m10.607s
     * 
     * Use hashmap
     *
     * real 3m18.573s
     * user 3m2.236s
     * sys 0m10.491s
     *
     * Add constructor
     * real    3m11.652s
     * user    2m59.161s
     * sys     0m10.387s
     *
     *

     * 
     */
    public static void main(String[] args) throws IOException {

        Map<String, MeasurementAggregator> agggregationMap = new HashMap<>();
        Files.lines(Path.of(FILE)).forEach(line -> {
            int split = line.indexOf(';');
            String city = line.substring(0, split);
            double temperature = Double.parseDouble(line.substring(split + 1));
            agggregationMap.compute(city, (__, oldValue) -> {
                MeasurementAggregator aggregator = new MeasurementAggregator(city, temperature);
                if (oldValue == null) {
                    return aggregator;
                }
                aggregator.max = Math.max(oldValue.max, aggregator.max);
                aggregator.min = Math.min(oldValue.min, aggregator.min);
                aggregator.count += oldValue.count;
                aggregator.sum += oldValue.sum;
                return aggregator;
            });
        });

        Map<String, ResultRow> measurements = new TreeMap<>();
        agggregationMap.values().forEach(agg -> {
            measurements.put(agg.city, new ResultRow(agg.min, agg.sum / agg.count, agg.max));
        });

        System.out.println(measurements);
    }
}
