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

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.experimental.TESMMultipleSeasonalityModel;
import com.forecasting.models.models.util.ConfigManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TESMwithMultipleSeasonalityTest extends TestCase {

    public TESMwithMultipleSeasonalityTest(String testName) {
        super(testName);
        try {
            ConfigManager.loadConfigFile("test.properties", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Test suite() {
        return new TestSuite(TESMwithMultipleSeasonalityTest.class);

    }

    public void testMultipleSeasonality() throws IOException, ModelInitializationException {



        try {

        String fileName = this.getClass().getResource(ConfigManager.get("input.weekly")).getPath();
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String[] dataPts = br.readLine().split(",");

        DataSet timeSeries=new DataSet();

        for (int i =1;i<dataPts.length;i++) {
            DataPoint observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, timeSeries.size());
            observation.setDependentValue(Double.parseDouble(dataPts[i]));
            timeSeries.add(observation);
        }


        System.out.println("timeSeries = " + timeSeries.size());

        System.out.println("*********************************************");
        System.out.println("         Actual Time Series");
        System.out.println("*********************************************");

        for (DataPoint dp : timeSeries.getDataPoints())
            System.out.println(String.format("Key = %s    forecast = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue()));


        ForecastModel model = new TESMMultipleSeasonalityModel(65,13,4,13);
        model.init(timeSeries);
        model.train();
        model.forecast(13);
        DataSet forecastDataSet = model.getForecastDataSet();


        System.out.println("*********************************************");
        System.out.println("         Forecast Time Series");
        System.out.println("*********************************************");

        for (DataPoint dp : forecastDataSet.getDataPoints())
            System.out.println(String.format("Key = %s    forecast = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue()));
    }

        catch (Exception e){
            e.printStackTrace();
        }
    }

}