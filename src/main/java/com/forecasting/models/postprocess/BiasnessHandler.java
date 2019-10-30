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
package com.forecasting.models.postprocess;

public class BiasnessHandler {

    /**
     * Checks the biasness in data and adjusts the forecast
     *
     * @param input
     */
    public static double handle(double[][] input) {

        double adjustmentFactor = 1, diff, min_diff = Double.MAX_VALUE;
        int underForecasted = 0, overForecasted = 0;
        for (int i = 0; i < input.length; i++) {

            if (input[i][0] > input[i][1])
                underForecasted++;
            else overForecasted++;

            if (input[i][1] != 0) {
                diff = Math.abs(input[i][0] - input[i][1]) / input[i][1];
                if (min_diff > diff)
                    min_diff = diff;
            }

        }

        if (min_diff == Double.MAX_VALUE)
            return 1;

        if (underForecasted == input.length)
            adjustmentFactor = adjustmentFactor + min_diff;
        else if (overForecasted == input.length)
            adjustmentFactor = adjustmentFactor - min_diff;


        if (adjustmentFactor != 1) {
            for (int i = 0; i < input.length; i++)
                input[i][1] = input[i][1] * adjustmentFactor;
        }

        return adjustmentFactor;
    }


    public static double handleOffset(double[][] input) {

        double avgDiff, sum = 0, offset = 0;
        int underForecasted = 0, overForecasted = 0;

        for (int i = 0; i < input.length; i++) {

            if (input[i][0] > input[i][1])
                underForecasted++;
            else overForecasted++;

            if (input[i][1] != 0) {
                sum += Math.abs(input[i][0] - input[i][1]);
            }
        }

        avgDiff = sum / input.length;

        if (underForecasted == input.length)
            offset = avgDiff;
        else if (overForecasted == input.length)
            offset = -1 * avgDiff;
        else return 0;

        if (offset != 0) {
            for (int i = 0; i < input.length; i++)
                input[i][1] = input[i][1] + offset;
        }
        return offset;
    }




    /**
     * Adjusts biasness on an array with provided bias factor
     *
     * @param points
     * @param bias
     */
    public static void adjustBiasness(double[] points, double bias) {
        for (double point : points)
            point = point * bias;
    }

    /**
     * Adjusts Biasness on given value
     *
     * @param value
     * @param bias
     * @return
     */
    public static double adjustBiasness(double value, double bias) {
        return value * bias;
    }

    public static double addOffset(double value, double offset) {
        return value + offset;
    }


}
