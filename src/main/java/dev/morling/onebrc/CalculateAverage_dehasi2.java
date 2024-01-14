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

    private static String FILE = "./measurements.txt";

   private  record ResultRow(double min, double mean, double max) {
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

        MeasurementAggregator(String city, double temperature) {
            this.city = city;
            min = max = sum = temperature;
        }

        void updateWith(double temperature) {
            max = Math.max(max, temperature);
            min = Math.min(min, temperature);
            sum += temperature;
            ++count;
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
     * real 3m11.652s
     * user 2m59.161s
     * sys 0m10.387s
     *
     * Use var inMap = agggregationMap.get(city);
     *
     * real 3m4.090s
     * user 2m51.752s
     * sys 0m9.748s
     *
     * after bugfix
     * real 3m4.475s
     * user 2m50.257s
     * sys 0m9.914s
     *
     * Inline :agggregationMap.put(city, new MeasurementAggregator(city, temperature));
     * real 3m11.652s
     * user 2m49.760s
     * sys 0m10.443s
     *
     * ---- ????
     * real 3m5.269s
     * user 2m52.463s
     * sys 0m10.176s
     *
     * Inline agggregationMap.put(city, new MeasurementAggregator(city, temperature));
     * use (inMap == null)
     * real 2m48.554s
     * user 2m36.512s
     * sys 0m9.545s
     *
     *
     * All possible finals
     *
     * real 2m52.403s
     * user 2m39.459s
     * sys 0m9.784s
     *
     *
     * delete all finals
     * real 2m46.605s
     * user 2m34.479s
     * sys 0m9.545s
     *
     * Make ResultRow a normal class (continu deleteing finals)
     * real 2m52.804s
     * user 2m38.238s
     * sys 0m9.528s
     *
     * make ResultRow just final
     * real 2m55.696s
     * user 2m39.700s
     * sys 0m9.803s
     * 
     * Revert ResultRow to record
     * real 2m59.620s
     * user 2m43.421s
     * sys 0m10.226s
     * 
     * Use: private final String city;
     * real 2m47.701s
     * user 2m34.227s
     * sys 0m9.430s
     * 
     * Replace string concatenating with template
     * real 2m54.723s
     * user 2m38.123s
     * sys 0m9.716s
     * 
     * Add method updateWith(temperature) to MeasurementAggregator, no big difference
     * real 2m54.069s
     * user 2m39.321s
     * sys 0m9.670s
     * 
     * Add big HashMap capacity, no difference
     * real 2m58.629s
     * user 2m39.208s
     * sys 0m10.783s
     *
     * Add big HashMap capacity 5k
     *
     * real 2m52.683s
     * user 2m36.693s
     * sys 0m10.230s
     *
     * Add big HashMap capacity 4096, just use pow of two
     * real 2m48.328s
     * user 2m35.097s
     * sys 0m9.504s
     * 
     * Use Graal
     */
    public static void main(String[] args) throws IOException {

        Map<String, MeasurementAggregator> aggregatorMap = new HashMap<>(4096);
        Files.lines(Path.of(FILE)).forEach(line -> {
            int split = line.indexOf(';');
            String city = line.substring(0, split);
            double temperature = Double.parseDouble(line.substring(split + 1));
            var inMap = aggregatorMap.get(city);
            if (inMap == null) {
                aggregatorMap.put(city, new MeasurementAggregator(city, temperature));
            }
            else {
                inMap.updateWith(temperature);
            }
        });

        // try system.gc()
        Map<String, ResultRow> measurements = new TreeMap<>();
        aggregatorMap.values().forEach(agg -> {
            measurements.put(agg.city, new ResultRow(agg.min, agg.sum / agg.count, agg.max));
        });

        System.out.println(measurements);
    }
}
