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
package com.forecasting.models.models.tests;

import com.forecasting.models.Forecaster;
import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Random;

public class BestForecastExample extends TestCase {

    public BestForecastExample(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BestForecastExample.class);
    }

    @org.junit.Test
    public void testGetBestForecast() {

        try {
            //Creating Time Series

            DataSet timeSeries = new DataSet();
            DataPoint observation;
            Random gaussian = new Random();
            double MEAN = 100.0f;
            double VARIANCE = 5.0f;
            double value;
            for (int idx = 0; idx <= 42; ++idx) {
                value = MEAN + gaussian.nextGaussian() * VARIANCE;
                observation = new Observation();
                observation.setIndependentValue(IndependentVariable.SLICE, idx);
                observation.setDependentValue(value);
                timeSeries.add(observation);
            }


            System.out.println("*********************************************");
            System.out.println("         Actual Time Series");
            System.out.println("*********************************************");

            for (DataPoint dp : timeSeries.getDataPoints())
                System.out.println(String.format("Key = %s    forecast = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue()));

            // Model data parameters
            int trainPoints = 35;
            int seasonalPeriod = 7;
            int validationPoints =1 ;
            int futurePoints = 1;

            // Forecaster - getting best forecast by running all models
            Forecaster forecaster = new Forecaster();
            forecaster.init(timeSeries);
            DataSet forecastTimeSeries = forecaster.forecast(trainPoints, validationPoints, futurePoints, seasonalPeriod);


            // Forecasted Series

            System.out.println("*********************************************");
            System.out.println("      Forecasted Time Series");
            System.out.println("*********************************************");

            for (DataPoint dp : forecastTimeSeries.getDataPoints())
                System.out.println(String.format("Key = %s    forecast = %s   LB = %s  UB = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue(), dp.getLowerDependentValue(), dp.getUpperDependentValue()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}