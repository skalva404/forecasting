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
import com.forecasting.models.dto.Model;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.ForecastModel;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ForecastUsingSpecificModelsExample extends TestCase {

    public ForecastUsingSpecificModelsExample(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ForecastUsingSpecificModelsExample.class);
    }

    @org.junit.Test
    public void testForecastWithSpecificModels() {

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
            int validationPoints = 7;
            int futurePoints = 7;

            // Forecaster - getting best forecast by running all models
            List<Model> modelList=new ArrayList<Model>();
            modelList.add(Model.BOXCOX);
            modelList.add(Model.TESA);
            modelList.add(Model.TESM);
            modelList.add(Model.FFT);
            modelList.add(Model.ENSEMBLE);

            Forecaster forecaster = new Forecaster(modelList);
            forecaster.init(timeSeries);
            DataSet forecastTimeSeries = forecaster.forecast(trainPoints, validationPoints, futurePoints, seasonalPeriod);

            // Forecasted Series
            System.out.println("*********************************************");
            System.out.println("      Forecasted Time Series");
            System.out.println("*********************************************");

            for (DataPoint dp : forecastTimeSeries.getDataPoints())
                System.out.println(String.format("Key = %s    forecast = %s   LB = %s  UB = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue(), dp.getLowerDependentValue(), dp.getUpperDependentValue()));

            System.out.println("\n\nForecast Execution Time (ms)  =  "+forecaster.getExecutionTime()+"\n\n");


         // getAllmodels and Get their Accuracy Indicators
            System.out.println("Accuracy Indicators - ");
            List<ForecastModel> forecastModels=forecaster.getAllModels();
            for(ForecastModel model:forecastModels)
                System.out.println(String.format(" Model  =  %s    Accuracy Indicator  =  %s\n",model.getModelName(),model.getAccuracyIndicators().toString()));

           // getFinalModel
            ForecastModel finalModel=forecaster.getFinalModel();
            System.out.println("\n\nfinalModel = " + finalModel.getModelName()+"\n\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}