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
package com.forecasting.models.models.impl2;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Model;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.impl.AbstractForecastModel;
import com.forecasting.models.postprocess.BiasnessHandler;
import com.forecasting.models.postprocess.ErrorBoundsHandler;
import com.forecasting.models.utils.AccuracyIndicators;
import com.forecasting.models.utils.ModelUtil;
import com.forecasting.models.exception.ModelInitializationException;
import lombok.Data;

/**
 * Substitute for Moving Average Weekly and TESA with Zero initialization
 */
@Data
public class SeasonalSingleExponentialSmoothingModel_2 extends AbstractForecastModel {

    private double min_val_error;
    private double[] actual;
    private float optGamma;
    private final int dof = 1;
    private boolean findDecayConstants;


    public void init(DataSet observations) throws ModelInitializationException {
        super.init(observations);
    }

    public SeasonalSingleExponentialSmoothingModel_2(int trainPoints, int validationPoints, int seasonalPeriod, float gamma) {

        this(trainPoints, validationPoints, seasonalPeriod);
        this.findDecayConstants = false;
        this.optGamma = gamma;
    }

    public SeasonalSingleExponentialSmoothingModel_2(int trainPoints, int validationPoints, int seasonalPeriod) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.seasonalPeriod = seasonalPeriod;
        this.model = Model.SSES2;
        this.findDecayConstants = true;
    }

    @Override
    public void train() {
        actual = observations.toArray();
        min_val_error = Double.MAX_VALUE;
        if (findDecayConstants)
            for (float gamma = 0f; gamma <= 1f; gamma += 0.01f)
                initializeAndTrainModel(gamma);
        else
            initializeAndTrainModel(optGamma);
    }

    /**
     * Curve fitting on provided gamma by minimizing MSE
     *
     * @param gamma
     */
    private void initializeAndTrainModel(float gamma) {
        int index;
        double forecast;
        double[][] trainMatrix = new double[trainPoints][2];
        double[][] valMatrix = new double[validationPoints][2];

//        double[] seasonalComponent = new double[seasonalPeriod];
        double[] seasonalComponent = initializeSeasonality(trainPoints);
        for (int i = 0; i < trainPoints; i++) {

            index = i % seasonalPeriod;
            forecast = seasonalComponent[index];
            trainMatrix[i][0] = actual[i];
            trainMatrix[i][1] = forecast;

            seasonalComponent[index] = (gamma * actual[i]) + ((1 - gamma) * seasonalComponent[index]);
        }

        for (int t = 1; t <= validationPoints; t++) {
            valMatrix[t - 1][1] = seasonalComponent[(trainPoints + t - 1) % seasonalPeriod];
            valMatrix[t - 1][0] = actual[trainPoints + t - 1];
        }

        double biasness = BiasnessHandler.handle(valMatrix);
        AccuracyIndicators AI = new AccuracyIndicators();
        ModelUtil.computeAccuracyIndicators(AI, trainMatrix, valMatrix, dof);
        AI.setBias(biasness);
        if (min_val_error > AI.getMAPE()) {
            min_val_error = AI.getMAPE();
            optGamma = gamma;
            accuracyIndicators = AI;
        }
    }

    /**
     * Forecasts using optimum gamma value
     *
     * @param numFuturePoints
     */
    @Override
    public void forecast(int numFuturePoints) {

        int index;
//        double[] seasonalComponent = new double[seasonalPeriod];
        double[] seasonalComponent = initializeSeasonality(trainPoints + validationPoints);
        double forecast;

        double[][] trainMatrix = new double[trainPoints+validationPoints][2];

        for (int i = 0; i < actual.length; i++) {
            index = (i) % seasonalPeriod;
            forecast = seasonalComponent[index];
            trainMatrix[i][0] = actual[i];
            trainMatrix[i][1] = forecast;
            seasonalComponent[index] = (optGamma * actual[i]) + ((1 - optGamma) * seasonalComponent[index]);
        }

        errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);
        forecastDataSet = new DataSet();
        Observation observation;
        double forecastValue, lowerBound, upperBound;

        for (int i = actual.length; i < actual.length + numFuturePoints; i++) {
            forecastValue = seasonalComponent[i % seasonalPeriod];

            if (forecastValue < 0) {
                forecastValue = 0l;
                lowerBound = 0l;
                upperBound = 0l;
            } else {
                forecastValue = BiasnessHandler.adjustBiasness(forecastValue, accuracyIndicators.getBias());
                lowerBound = forecastValue - errorBound;
                upperBound = forecastValue + errorBound;
            }
            observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i - actual.length);
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
