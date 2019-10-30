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
package com.forecasting.models.preprocess;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.preprocess.impl.DifferencingTransformation;

public class SeasonalityCalculatorUsingAutoCorrelation {


    public int computeSeasonality(DataSet dataSet) {

        DifferencingTransformation diff = new DifferencingTransformation();
        diff.init(dataSet);
        DataSet transformedDS = diff.transform();
//        DataSet transformedDS = dataSet;

        double[] points = transformedDS.toArray();
        double[][] correlation = new double[dataSet.size()][2];
        double product = 0;

        for (int shift = 1; shift < points.length; shift++) {
            product = 0;
            for (int index = 0; index < points.length - shift; index++)
                product = +points[index + shift] * points[index];

            correlation[shift - 1][0] = shift;
            correlation[shift - 1][1] = product / (points.length - shift);
        }

        insertionSort(correlation);

        int seasonality, computedSeasonality = -1;

        for (seasonality = 4; seasonality <= 7; seasonality++) {
            boolean seasonalityExists = checkSeasonality(correlation, seasonality);
            if (seasonalityExists) {
                computedSeasonality = seasonality;
                break;
            }
        }
        return computedSeasonality;
    }

    private boolean checkSeasonality(double[][] correlation, int seasonality) {

        int count = 0;
        for (int i = 0; i < correlation.length - 1; i++) {
            if (Math.abs(correlation[i + 1][0] - correlation[i][0]) == seasonality)
                count++;
        }

        int threshold = 2;
        if (count >= (correlation.length / seasonality) - threshold)
            return true;
        else return false;

    }

    public void insertionSort(double array[][]) {
        int size = array.length;
        for (int i = 1; i < size; i++) {
            int j = i;
            double index = array[i][0];
            double B = array[i][1];
            while ((j > 0) && (array[j - 1][1] < B)) {
                array[j][0] = array[j - 1][0];
                array[j][1] = array[j - 1][1];
                j--;
            }
            array[j][0] = index;
            array[j][1] = B;
        }
    }

    public int findCyclePeriod(double[] ts) {

        double[][] correlation = new double[ts.length/2][2];
        double product = 0;

        for (int shift = 0; shift < ts.length / 2; shift++) {
            product = 0;
            for (int index = 0; index < ts.length - shift; index++)
                product += ts[index + shift] * ts[index];
            correlation[shift][0] = shift;
            correlation[shift][1] = product / (ts.length - shift);
        }
        insertionSort(correlation);
//
        if(correlation[1][0] - correlation[0][0]==0)
        for (int i = 0; i < correlation.length; i++)
            System.out.println("correlation = " + correlation[i][0] + " second = " + correlation[i][1]);
//
//        System.out.println("(correlation[1][0] - correlation[0][0]) = " + (correlation[1][0] - correlation[0][0]));

        int period = new Double(Math.abs(correlation[1][0] - correlation[0][0])).intValue();
        return period;
    }
}
