/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.forecasting.models.utils;

public class StatisticsUtil {

    /**
     * Find AutoCorrelation plot in a time series
     * Dropped - Was supposed to be used for seasonality calculation, but FFT method is better for that purpose.
     * Not Tested
     *
     * @param input
     * @return
     */
    public static double[] autoCorrelation(double[] input) {
        double[] output = new double[input.length / 2];
        for (int shift = 0; shift < input.length / 2; shift++) {
            for (int i = shift, j = 0; i < input.length; i++, j++)
                output[shift] += input[i] * input[j];
        }
        return output;
    }
}
