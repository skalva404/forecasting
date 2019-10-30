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

@Data
public class TripleExponentialSmoothingAdditiveModel extends AbstractForecastModel {

    //Data parameters
    double[] actual;

    //Model Parameters
    private float optAlpha;
    private float optBeta;
    private float optGamma;
    private boolean findDecayConstants;
    private final int dof=3;

    //Accuracy parameters
    private double min_mse_error;
    private double train_error;

    /**
     * Constructor with  initializeCoefficients flag
     *
     * @param trainPoints
     * @param validationPoints
     * @param seasonalPeriod
     */
    public TripleExponentialSmoothingAdditiveModel(int trainPoints, int validationPoints, int seasonalPeriod) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.seasonalPeriod = seasonalPeriod;
        this.findDecayConstants = true;
        this.model = Model.TESA;

    }

    /**
     * Constructor with provided decay constants, initializeCoefficients
     *
     * @param trainPoints
     * @param validationPoints
     * @param seasonalPeriod
     * @param alpha
     * @param beta
     * @param gamma
     */
    public TripleExponentialSmoothingAdditiveModel(int trainPoints, int validationPoints, int seasonalPeriod, float alpha, float beta, float gamma) {
        this(trainPoints, validationPoints, seasonalPeriod);
        this.optAlpha = alpha;
        this.optBeta = beta;
        this.optGamma = gamma;
        this.findDecayConstants = false;
    }

    /**
     * Initialize with time series data
     *
     * @param observations
     */
    public void init(final DataSet observations) throws ModelInitializationException {
        super.init(observations);
        if (!findDecayConstants && (optAlpha < 0 || optAlpha > 1 || optBeta < 0 || optBeta > 1 || optGamma < 0 || optGamma > 1))
            throw new ModelInitializationException("TESA :Invalid decay constants");
        if (trainPoints <= 0 || validationPoints <= 0 || seasonalPeriod <= 0)
            throw new ModelInitializationException("TESA :Invalid  model arguments");
    }

    /**
     * compute accuracy indicator and find optimum decay constants
     */
    public void train() {
        double[][] valMatrix = new double[validationPoints][2];
        actual = observations.toArray();
        for (int point = 0; point < validationPoints; point++)
            populateForecastMatrix(point, valMatrix);
        double biasness= BiasnessHandler.handleOffset(valMatrix);
        accuracyIndicators.setBias(biasness);
        ModelUtil.computeAccuracyIndicators(accuracyIndicators,null,valMatrix,dof);

    }

    /**
     * Populate forecast matrix on validation points to be used for accuracy indicator computation
     *
     * @param point
     * @param valMatrix
     */
    void populateForecastMatrix(int point, double[][] valMatrix) {
        min_mse_error = -1;
        int startPoint = 0;
        int endPoint = trainPoints + point;
        if (findDecayConstants)
            for (float alpha = 0f; alpha <= 1.01f; alpha += 0.1f)
                for (float beta = 0f; beta <= 1.01f; beta += 0.1f)
                    for (float gamma = 0f; gamma <= 1.01f; gamma += 0.1f)
                        initializeAndTrainModel(alpha, beta, gamma, startPoint, endPoint, valMatrix);
        else
            initializeAndTrainModel(optAlpha, optBeta, optGamma, startPoint, endPoint, valMatrix);

    }

    /**
     * Compute MSE error and evaluate the given decay constants
     *
     * @param alpha
     * @param beta
     * @param gamma
     * @param startPoint
     * @param endPoint
     * @param valMatrix
     */
    public void initializeAndTrainModel(float alpha, float beta, float gamma, int startPoint, int endPoint, double[][] valMatrix) {

        double[][] trainMatrix = new double[endPoint-startPoint][2];

        double trend = initializeTrend(startPoint);
        double permanent = initializePermanentComponent(startPoint);
        double[] seasonalComponent = initializeSeasonality(startPoint, endPoint);
        double lastPermanent;
        double forecast;

        trainMatrix[0][0] = actual[startPoint];
        trainMatrix[0][1] = permanent;
        int index;

        double actualValue = 0d, forecastValue = 0d;

        for (int i = startPoint + 1; i < endPoint + 1; i++) {
            index = (i - startPoint) % seasonalPeriod;

            forecast = seasonalComponent[index] + permanent + trend;

            if (i < endPoint) {
                trainMatrix[i - startPoint][0] = actual[i];
                trainMatrix[i - startPoint][1] = forecast;
            }

            lastPermanent = permanent;
            permanent = (alpha * (actual[i] - seasonalComponent[index])) + (1 - alpha) * (lastPermanent + trend);
            trend = (beta * (permanent - lastPermanent)) + ((1 - beta) * trend);
            seasonalComponent[index] = (gamma * (actual[i] - permanent)) + ((1 - gamma) * seasonalComponent[index]);

            if (i == endPoint) {
                actualValue = actual[i];
                forecastValue = forecast;
            }
        }

        train_error = ModelUtil.computeMSE(trainMatrix);
        if (min_mse_error > train_error || min_mse_error == -1) {
            min_mse_error = train_error;
            optAlpha = alpha;
            optBeta = beta;
            optGamma = gamma;
            valMatrix[endPoint-trainPoints][0] = actualValue;
            valMatrix[endPoint-trainPoints][1] = forecastValue;
            errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);
            ModelUtil.computeAccuracyIndicators(accuracyIndicators,trainMatrix,null,dof);
        }
    }


    public void forecast(int numFuturePoints) {

        int startPoint = 0, endPoint = actual.length, index;
        double trend = initializeTrend(startPoint);
        double permanent = initializePermanentComponent(startPoint);
        double[] seasonalComponent = initializeSeasonality(startPoint, endPoint);
        double lastPermanent;

        for (int i = startPoint + 1; i < endPoint; i++) {

            index = (i - startPoint) % seasonalPeriod;
            lastPermanent = permanent;
            permanent = (optAlpha * (actual[i] - seasonalComponent[index])) + (1 - optAlpha) * (lastPermanent + trend);
            trend = (optBeta * (permanent - lastPermanent)) + ((1 - optBeta) * trend);
            seasonalComponent[index] = (optGamma * (actual[i] - permanent)) + ((1 - optGamma) * seasonalComponent[index]);
        }

        forecastDataSet = new DataSet();
        startPoint = actual.length;
        double seasonalElement;
        Observation observation;
        double forecastValue, lowerBound, upperBound,adjustedValue;

        for (int i = startPoint; i < startPoint + numFuturePoints; i++) {
            seasonalElement = seasonalComponent[i % seasonalPeriod];

            forecastValue = seasonalElement + permanent + ((i - startPoint + 1) * trend);
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

    /**
     * Initialize seasonalities
     *
     * @param startPoint
     * @param endPoint
     * @return
     */
    private double[] initializeSeasonality(int startPoint, int endPoint) {

        double[] seasonalComponent = new double[seasonalPeriod];
            double[] cycle = new double[(endPoint - startPoint) / seasonalPeriod];
            for (int i = 0; i < cycle.length; i++) {
                for (int j = 0; j < seasonalPeriod; j++)
                    cycle[i] += actual[i * seasonalPeriod + j];
                cycle[i] = cycle[i] / seasonalPeriod;
            }
            for (int index = 0; index < seasonalPeriod; index++) {
                Double value = 0d;
                for (int iter = 0; iter < cycle.length; iter++)
                    value += actual[startPoint + index + iter * seasonalPeriod] - cycle[iter];
                seasonalComponent[index] = value / ((endPoint-startPoint) / seasonalPeriod);
            }
        return seasonalComponent;
    }

    /**
     * Initialize Trend, set to Zero if initializeCoefficients=false
     *
     * @param startPoint
     * @return
     */
    private double initializeTrend(int startPoint) {
            Double sum = 0d;
            for (int i = startPoint; i < startPoint + seasonalPeriod; i++)
                sum = sum + ((actual[seasonalPeriod + i] - actual[i]) / seasonalPeriod);
            return sum / seasonalPeriod;
    }

    /**
     * Initialize constant component, set to Zero if initializeCoefficients=false
     *
     * @param startPoint
     * @return
     */
    private double initializePermanentComponent(int startPoint) {
        return actual[startPoint];
    }
}
