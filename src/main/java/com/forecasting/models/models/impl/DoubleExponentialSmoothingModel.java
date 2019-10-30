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
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.postprocess.BiasnessHandler;
import com.forecasting.models.postprocess.ErrorBoundsHandler;
import com.forecasting.models.utils.ModelUtil;
import com.forecasting.models.exception.ModelInitializationException;
import lombok.Data;

@Data
public class DoubleExponentialSmoothingModel extends AbstractForecastModel {

    //Data parameters
    double[] actual;

    //Model Parameters
    private float optAlpha;
    private float optBeta;
    private final int dof = 2; // degrees of freedom for calculation of bic,aic
    private boolean findDecayConstants;

    //Accuracy parameters
    private double min_mse_error;
    private double train_error;

    /**
     * Constructor with  initializeCoefficients flag
     *
     * @param trainPoints
     * @param validationPoints
     */
    public DoubleExponentialSmoothingModel(int trainPoints, int validationPoints) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.findDecayConstants = true;
        this.model = Model.DES;
    }

    /**
     * Constructor with provided decay constants, initializeCoefficients
     *
     * @param trainPoints
     * @param validationPoints
     * @param alpha
     * @param beta
     */
    public DoubleExponentialSmoothingModel(int trainPoints, int validationPoints, float alpha, float beta) {
        this(trainPoints, validationPoints);
        this.optAlpha = alpha;
        this.optBeta = beta;
        this.findDecayConstants = false;
    }

    /**
     * Initialize with time series data
     *
     * @param observations
     */
    public void init(final DataSet observations) throws ModelInitializationException {
        super.init(observations);
        if (!findDecayConstants && (optAlpha < 0 || optAlpha > 1 || optBeta < 0 || optBeta > 1))
            throw new ModelInitializationException("DES :Invalid decay constants");
        if (trainPoints <= 0 || validationPoints <= 0)
            throw new ModelInitializationException("DESA :Invalid  model arguments");
    }

    /**
     * compute accuracy indicator and find optimum decay constants
     */
    public void train() {
        double[][] valMatrix = new double[validationPoints][2];
        actual = observations.toArray();
        for (int point = 0; point < validationPoints; point++)
            populateForecastMatrix(point, valMatrix);

        double biasness = BiasnessHandler.handleOffset(valMatrix);
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
        min_mse_error = Double.MAX_VALUE;
        int startPoint = 0;
        int endPoint = trainPoints + point;
        if (findDecayConstants)
            for (float alpha = 0f; alpha <= 1.01f; alpha += 0.1f)
                for (float beta = 0f; beta <= 1.01f; beta += 0.1f)
                    initializeAndTrainModel(alpha, beta, startPoint, endPoint, valMatrix);
        else
            initializeAndTrainModel(optAlpha, optBeta, startPoint, endPoint, valMatrix);
    }

    /**
     * Compute MSE error and evaluate the given decay constants
     *
     * @param alpha
     * @param beta
     * @param startPoint
     * @param endPoint
     * @param forecastMat
     */
    public void initializeAndTrainModel(float alpha, float beta, int startPoint, int endPoint, double[][] forecastMat) {

        double[][] trainMatrix = new double[endPoint - startPoint][2];

        double trend = initializeTrend(startPoint);
        double permanent = initializePermanentComponent(startPoint, trend);
        double lastPermanent;
        double forecast;

        double actualValue = 0d, forecastValue = 0d;

        for (int i = startPoint; i < endPoint + 1; i++) {
            forecast = permanent + trend;
            if (i < endPoint) {
                trainMatrix[i - startPoint][0] = actual[i];
                trainMatrix[i - startPoint][1] = forecast;
            }
            lastPermanent = permanent;
            permanent = (alpha * actual[i]) + (1 - alpha) * (lastPermanent + trend);
            trend = (beta * (permanent - lastPermanent)) + ((1 - beta) * trend);
            if (i == endPoint) {
                actualValue = actual[i];
                forecastValue = forecast;
            }
        }

        train_error = ModelUtil.computeMSE(trainMatrix);

        if (min_mse_error > train_error) {
            min_mse_error = train_error;
            optAlpha = alpha;
            optBeta = beta;
            forecastMat[endPoint - trainPoints][0] = actualValue;
            forecastMat[endPoint - trainPoints][1] = forecastValue;
            errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);
            ModelUtil.computeAccuracyIndicators(accuracyIndicators, trainMatrix, null, dof);
        }
    }

    public void forecast(int numFuturePoints) {

        int startPoint = 0, endPoint = actual.length;
        double trend = initializeTrend(startPoint);
        double permanent = initializePermanentComponent(startPoint, trend);
        double lastPermanent;

        for (int i = startPoint + 1; i < endPoint; i++) {
            lastPermanent = permanent;
            permanent = (optAlpha * actual[i]) + (1 - optAlpha) * (lastPermanent + trend);
            trend = (optBeta * (permanent - lastPermanent)) + ((1 - optBeta) * trend);
        }

        forecastDataSet = new DataSet();
        startPoint = actual.length;
        DataPoint observation;
        double forecastValue, adjustedValue, lowerBound, upperBound;

        for (int i = startPoint; i < startPoint + numFuturePoints; i++) {

            forecastValue = permanent + (i - startPoint + 1) * trend;
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
     * Initialize Trend
     *
     * @param startPoint
     * @return
     */
    private double initializeTrend(int startPoint) {
        Double sum = 0d;
        for (int i = startPoint + 1; i <= startPoint + 3; i++)
            sum = sum + ((actual[i] - actual[i - 1]));
        return sum / 3;
    }

    /**
     * Initialize constant component      beta
     *
     * @param startPoint
     * @param trend
     * @return
     */
    private double initializePermanentComponent(int startPoint, double trend) {
        double permanent = actual[startPoint] - trend;
        return permanent;
    }
}
