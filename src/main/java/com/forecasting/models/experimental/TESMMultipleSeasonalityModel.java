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
package com.forecasting.models.experimental;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Model;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.postprocess.BiasnessHandler;
import com.forecasting.models.utils.ModelUtil;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.models.impl.AbstractForecastModel;
import com.forecasting.models.postprocess.ErrorBoundsHandler;
import lombok.Data;

@Data
public class TESMMultipleSeasonalityModel extends AbstractForecastModel {

    //Data parameters
    double[] actual;

    //Model Parameters
    private float optAlpha;
    private float optBeta;
    private float optGamma;
    private float optDelta;
    private boolean findDecayConstants;
    private final int dof = 3;

    //Accuracy parameters
    private double min_mse_error;
    private double train_error;
    private int seasonality1, seasonality2;

    /**
     * Constructor with  initializeCoefficients flag
     *
     * @param trainPoints
     * @param validationPoints
     */
    public TESMMultipleSeasonalityModel(int trainPoints, int validationPoints, int seasonality1, int seasonality2) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.seasonality1 = seasonality1;
        this.seasonality2 = seasonality2;
        this.findDecayConstants = true;
        this.model = Model.MSTESM;

    }

    /**
     * Constructor with provided decay constants, initializeCoefficients
     *
     * @param trainPoints
     * @param validationPoints
     * @param alpha
     * @param beta
     * @param gamma
     */
    public TESMMultipleSeasonalityModel(int trainPoints, int validationPoints, int seasonality1, int seasonality2, float alpha, float beta, float gamma, float delta) {
        this(trainPoints, validationPoints, seasonality1, seasonality2);
        this.optAlpha = alpha;
        this.optBeta = beta;
        this.optGamma = gamma;
        this.optDelta = delta;
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
            throw new ModelInitializationException("TESM :Invalid decay constants");
        if (trainPoints <= 0 || validationPoints <= 0 || seasonality1 <= 0 || seasonality2 <= 0)
            throw new ModelInitializationException("TESM :Invalid  model arguments");
    }

    /**
     * compute accuracy indicator and find optimum decay constants
     */
    public void train() {
        double[][] valMatrix = new double[validationPoints][2];
        actual = observations.toArray();
        for (int point = 0; point < validationPoints; point++)
            populateForecastMatrix(point, valMatrix);

        double biasness = BiasnessHandler.handle(valMatrix);
        accuracyIndicators.setBias(biasness);
        ModelUtil.computeAccuracyIndicators(accuracyIndicators, null, valMatrix, dof);
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
            for (float alpha = 0f; alpha <= 1f; alpha += 0.1f)
                for (float beta = 0f; beta <= 1f; beta += 0.1f)
                    for (float gamma = 0f; gamma <= 1f; gamma += 0.1f)
                        for (float delta = 0f; delta <= 1f; delta += 0.1f)
                            initializeAndTrainModel(alpha, beta, gamma, delta, startPoint, endPoint, valMatrix);
        else
            initializeAndTrainModel(optAlpha, optBeta, optGamma, optDelta, startPoint, endPoint, valMatrix);
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
    public void initializeAndTrainModel(float alpha, float beta, float gamma, float delta, int startPoint, int endPoint, double[][] valMatrix) {

        double[][] trainMatrix = new double[endPoint - startPoint][2];

        double trend = initializeTrend(startPoint);
        double permanent = initializePermanentComponent(startPoint);
//        double[] seasonalComponent1 = new double[seasonality1];
//        double[] seasonalComponent2 = new double[seasonality2];

//        for (int i = 0; i < seasonalComponent1.length; i++)
//            seasonalComponent1[i] = 1;
//        for (int i = 0; i < seasonalComponent2.length; i++)
//            seasonalComponent2[i] = 1;

        double[] seasonalComponent1 = initializeSeasonality1(startPoint, endPoint);
        double[] seasonalComponent2 = initializeSeasonality2(startPoint, endPoint);

        double lastPermanent;
        double forecast;

        trainMatrix[0][0] = actual[startPoint];
        trainMatrix[0][1] = permanent;
        int idx1, idx2;

        double actualValue = 0d, forecastValue = 0d;

        for (int i = startPoint + 1; i < endPoint + 1; i++) {
            idx1 = (i - startPoint) % seasonality1;
            idx2 = (i - startPoint) % seasonality2;

            forecast = seasonalComponent1[idx1] * seasonalComponent2[idx2] * (permanent + trend);

            if (i < endPoint) {
                trainMatrix[i - startPoint][0] = actual[i];
                trainMatrix[i - startPoint][1] = forecast;
            }

            lastPermanent = permanent;
            permanent = (alpha * (actual[i] / (seasonalComponent1[idx1] * seasonalComponent2[idx2]))) + (1 - alpha) * (lastPermanent + trend);
            trend = (beta * (permanent - lastPermanent)) + ((1 - beta) * trend);
            seasonalComponent1[idx1] = (gamma * (actual[i] / (permanent * seasonalComponent2[idx2]))) + ((1 - gamma) * seasonalComponent1[idx1]);
            seasonalComponent2[idx2] = (delta * (actual[i] / (permanent * seasonalComponent1[idx1]))) + ((1 - delta) * seasonalComponent2[idx2]);

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
            optGamma=gamma;
            optDelta = delta;
            valMatrix[endPoint-trainPoints][0] = actualValue;
            valMatrix[endPoint-trainPoints][1] = forecastValue;
            errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);
        }
    }

    /**
     * generates forecast dataset with optimum/provided decay constants
     *
     * @param numFuturePoints
     */
    public void forecast(int numFuturePoints) {

        int startPoint = 0, endPoint = actual.length, idx1, idx2;
        double trend = initializeTrend(startPoint);
        double lastPermanent, permanent = initializePermanentComponent(startPoint);

        double[] seasonalComponent1 = initializeSeasonality1(startPoint, endPoint);
        double[] seasonalComponent2 = initializeSeasonality2(startPoint, endPoint);

        for (int i = startPoint + 1; i < endPoint; i++) {

            idx1 = (i - startPoint) % seasonality1;
            idx2 = (i - startPoint) % seasonality2;

            lastPermanent = permanent;
            permanent = (optAlpha * (actual[i] / (seasonalComponent1[idx1] * seasonalComponent2[idx2]))) + (1 - optAlpha) * (lastPermanent + trend);
            trend = (optBeta * (permanent - lastPermanent)) + ((1 - optBeta) * trend);
            seasonalComponent1[idx1] = (optGamma * (actual[i] / (permanent * seasonalComponent2[idx2]))) + ((1 - optGamma) * seasonalComponent1[idx1]);
            seasonalComponent2[idx2] = (optDelta * (actual[i] / (permanent * seasonalComponent1[idx1]))) + ((1 - optDelta) * seasonalComponent2[idx2]);
        }

        forecastDataSet = new DataSet();
        startPoint = actual.length;
        double seasonalElement1, seasonalElement2;
        Observation observation;
        double forecastValue, lowerBound, upperBound;

        for (int i = startPoint; i < startPoint + numFuturePoints; i++) {
            seasonalElement1 = seasonalComponent1[i % seasonality1];
            seasonalElement2 = seasonalComponent2[i % seasonality2];

            forecastValue = seasonalElement1 * seasonalElement2 * (permanent + ((i - startPoint + 1) * trend));
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
            observation.setIndependentValue(IndependentVariable.SLICE, i - startPoint);
            observation.setDependentValue(forecastValue);
            observation.setLowerDependentValue(lowerBound > 0 ? lowerBound : 0);
            observation.setUpperDependentValue(upperBound);
            forecastDataSet.add(observation);
        }
    }

    /**
     * Initialize seasonalities, set to Zero if initializeCoefficients=false
     *
     * @param startPoint
     * @param endPoint
     * @return
     */
    private double[] initializeSeasonality1(int startPoint, int endPoint) {
        double[] seasonalComponent = new double[seasonality1];
        double[] cycle = new double[(endPoint - startPoint) / seasonality1];
        for (int i = 0; i < cycle.length; i++) {
            for (int j = 0; j < seasonality1; j++)
                cycle[i] += actual[i * seasonality1 + j];
            cycle[i] = cycle[i] / seasonality1;
        }
        for (int index = 0; index < seasonality1; index++) {
            Double value = 0d;
            for (int iter = 0; iter < cycle.length; iter++)
                value += actual[startPoint + index + iter * seasonality1] / cycle[iter];
            seasonalComponent[index] = value / ((endPoint - startPoint) / seasonality1);
        }
        return seasonalComponent;
    }

    /**
     * Initialize seasonalities, set to Zero if initializeCoefficients=false
     *
     * @param startPoint
     * @param endPoint
     * @return
     */
    private double[] initializeSeasonality2(int startPoint, int endPoint) {
        double[] seasonalComponent = new double[seasonality2];
        double[] cycle = new double[(endPoint - startPoint) / seasonality2];
        for (int i = 0; i < cycle.length; i++) {
            for (int j = 0; j < seasonality1; j++)
                cycle[i] += actual[i * seasonality2 + j];
            cycle[i] = cycle[i] / seasonality2;
        }
        for (int index = 0; index < seasonality2; index++) {
            Double value = 0d;
            for (int iter = 0; iter < cycle.length; iter++)
                value += actual[startPoint + index + iter * seasonality2] / cycle[iter];
            seasonalComponent[index] = value / ((endPoint - startPoint) / seasonality2);
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
        for (int i = startPoint; i < startPoint + seasonality1; i++)
            sum = sum + ((actual[seasonality1 + i] - actual[i]) / seasonality1);
        return sum / seasonality1;
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
