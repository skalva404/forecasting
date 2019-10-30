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
package com.forecasting.models.models.impl;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Model;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.postprocess.BiasnessHandler;
import com.forecasting.models.postprocess.ErrorBoundsHandler;
import com.forecasting.models.utils.ModelUtil;
import com.forecasting.models.exception.ModelInitializationException;
import lombok.Data;

/**
 * Substitute for Moving Average Weekly and TESA with Zero initialization
 */
@Data
public class SeasonalSingleExponentialSmoothingModel extends AbstractForecastModel {

    private double min_mse_error;
    private double[] actual;
    private float optGamma;
    private final int dof=1;


    public void init(DataSet observations) throws ModelInitializationException {
        super.init(observations);
    }

    public SeasonalSingleExponentialSmoothingModel(int trainPoints, int validationPoints, int seasonalPeriod) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.seasonalPeriod = seasonalPeriod;
        this.model= Model.SSES;
    }

    /**
     * Computes best coefficient for different validation points
     */
    @Override
    public void train() {

        double[][] valMatrix = new double[validationPoints][2];
        actual = observations.toArray();

        for (int point = 0; point < validationPoints; point++)
            populateForecastMatrix(point, valMatrix);

        double biasness = BiasnessHandler.handleOffset(valMatrix);
        accuracyIndicators.setBias(biasness);
        ModelUtil.computeAccuracyIndicators(accuracyIndicators,null,valMatrix,dof);

    }

    /**
     * Learns best decay coeffcient for seasonality  on a specific point
      * @param point
     * @param valMatrix
     */
    private void populateForecastMatrix(int point, double[][] valMatrix) {
        min_mse_error = Double.MAX_VALUE;
        int startPoint = point;
        int endPoint = trainPoints + point;
        for (float gamma = 0f; gamma <= 1.00001f; gamma += 0.01f)
            initializeAndtrain(gamma, startPoint, endPoint, valMatrix);
    }

    /**
     * Curve fitting on provided gamma by minimizing MSE
     * @param gamma
     * @param startPoint
     * @param endPoint
     * @param valMatrix
     */
    private void initializeAndtrain(float gamma, int startPoint, int endPoint, double[][] valMatrix) {
        int index;
        double forecast,train_error,forecastValue=0,actualValue=0;
        double[][] trainMatrix = new double[trainPoints][2];
//        double[] seasonalComponent = new double[actual.length];

        double[] seasonalComponent = initializeSeasonality(trainPoints);

        for (int i = startPoint + 1; i < endPoint + 1; i++) {
            index = (i - startPoint) % seasonalPeriod;
            forecast = seasonalComponent[index];
            if (i> startPoint+validationPoints+1 && i < endPoint) {
                trainMatrix[i - startPoint][0] = actual[i];
                trainMatrix[i - startPoint][1] = forecast;
            }
            seasonalComponent[index] = (gamma * actual[i]) + ((1 - gamma) * seasonalComponent[index]);

            if (i == endPoint) {
                actualValue = actual[i];
                forecastValue = forecast;
            }
        }

        train_error = ModelUtil.computeMSE(trainMatrix);
        if (min_mse_error > train_error) {
            min_mse_error = train_error;
            optGamma = gamma;
            valMatrix[startPoint][0] = actualValue;
            valMatrix[startPoint][1] = forecastValue;
            errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);
            ModelUtil.computeAccuracyIndicators(accuracyIndicators,trainMatrix,null,dof);
        }
    }

    /**
     * Forecasts using optimum gamma value
     * @param numFuturePoints
     */
    @Override
    public void forecast(int numFuturePoints) {

        int startPoint = 0, endPoint = actual.length, index;
//        double[] seasonalComponent = new double[validationPoints];
        double[] seasonalComponent = initializeSeasonality(trainPoints+validationPoints);

        for (int i = startPoint + 1; i < endPoint; i++) {
            index = (i - startPoint) % seasonalPeriod;
            seasonalComponent[index] = (optGamma * actual[i]) + ((1 - optGamma) * seasonalComponent[index]);
        }

        forecastDataSet = new DataSet();
        startPoint = actual.length;
        Observation observation;
        double forecastValue, lowerBound, upperBound,adjustedValue;

        for (int i = startPoint; i < startPoint + numFuturePoints; i++) {
            forecastValue = seasonalComponent[i % seasonalPeriod];

            adjustedValue = BiasnessHandler.addOffset(forecastValue, accuracyIndicators.getBias());

            if (adjustedValue > 0)
                forecastValue = adjustedValue;

            if (forecastValue < 0) {
                forecastValue = 0l;
                lowerBound = 0l;
                upperBound = 0l;
            } else {
                lowerBound = forecastValue - errorBound;
                upperBound = forecastValue + errorBound;
            }
            observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i - startPoint);
            observation.setDependentValue(forecastValue);
            observation.setLowerDependentValue(lowerBound > 0 ? lowerBound : 0);
            observation.setUpperDependentValue(upperBound);
            forecastDataSet.add(observation);
        }
    }

    private double[] initializeSeasonality(int points) {
        double[] seasonalComponent = new double[seasonalPeriod];
        double[] cycle = new double[(points) / seasonalPeriod];

        for (int index = 0; index < seasonalPeriod; index++) {
            Double value = 0d;
            for (int iter = 0; iter < cycle.length; iter++)
                value += actual[index + iter * seasonalPeriod];
            seasonalComponent[index] = value / cycle.length;
        }
        return seasonalComponent;
    }
}
