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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;

import static java.lang.Integer.MAX_VALUE;
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
       Baseline:
       real 4m11.364s
       user 3m51.048s
       sys 0m11.471s

       My Code
       real    5m41.593s
       user    5m20.700s
       sys     0m10.316s

     */
    public static void main(String[] args) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(FILE), MAX_VALUE / 10);

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
        // Map<String, Double> measurements1 = Files.lines(Paths.get(FILE))
        // .map(l -> l.split(";"))
        // .collect(groupingBy(m -> m[0], averagingDouble(m -> Double.parseDouble(m[1]))));
        //
        // measurements1 = new TreeMap<>(measurements1.entrySet()
        // .stream()
        // .collect(toMap(e -> e.getKey(), e -> Math.round(e.getValue() * 10.0) / 10.0)));
        // System.out.println(measurements1);

        // Collector<Measurement, MeasurementAggregator, ResultRow> collector = Collector.of(
        // MeasurementAggregator::new,
        // (a, m) -> {
        // a.min = Math.min(a.min, m.value);
        // a.max = Math.max(a.max, m.value);
        // a.sum += m.value;
        // a.count++;
        // },
        // (agg1, agg2) -> {
        // var res = new MeasurementAggregator();
        // res.min = Math.min(agg1.min, agg2.min);
        // res.max = Math.max(agg1.max, agg2.max);
        // res.sum = agg1.sum + agg2.sum;
        // res.count = agg1.count + agg2.count;
        //
        // return res;
        // },
        // agg -> {
        // return new ResultRow(agg.min, agg.sum / agg.count, agg.max);
        // });
        //
        // Map<String, ResultRow> measurements = new TreeMap<>(Files.lines(Paths.get(FILE))
        // .map(l -> new Measurement(l.split(";")))
        // .collect(groupingBy(m -> m.station(), collector)));
        //
        // System.out.println(measurements);
    }
}
