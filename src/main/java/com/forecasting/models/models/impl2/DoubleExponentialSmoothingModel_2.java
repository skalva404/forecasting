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
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.models.impl.AbstractForecastModel;
import com.forecasting.models.postprocess.BiasnessHandler;
import com.forecasting.models.postprocess.ErrorBoundsHandler;
import com.forecasting.models.utils.AccuracyIndicators;
import com.forecasting.models.utils.ModelUtil;

// Both TESA and TESA Zero initialization, TESA Zero Initialization is deprecated to need to deprecate initalizeCoefficients
public class DoubleExponentialSmoothingModel_2 extends AbstractForecastModel {

    //Data parameters
    int trainPoints;
    int validationPoints;
    double[] actual;

    //Model Parameters
    private float optAlpha;
    private float optBeta;
    private final int dof = 2;
    private boolean findDecayConstants;

    //Accuracy parameters
    private double min_val_error;

    private double errorBound;


    /**
     * Constructor with  initializeCoefficients flag
     *
     * @param trainPoints
     * @param validationPoints
     */
    public DoubleExponentialSmoothingModel_2(int trainPoints, int validationPoints) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.findDecayConstants = true;
        this.model = Model.DES2;

    }

    /**
     * Constructor with provided decay constants, initializeCoefficients
     *
     * @param trainPoints
     * @param validationPoints
     * @param alpha
     * @param beta
     */
    public DoubleExponentialSmoothingModel_2(int trainPoints, int validationPoints, float alpha, float beta) {
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
    @Override
    public void train() {
        actual = observations.toArray();
        min_val_error = Double.MAX_VALUE;
        if (findDecayConstants)
            for (float alpha = 0f; alpha <= 1f; alpha += 0.1f)
                for (float beta = 0f; beta <= 1f; beta += 0.1f)
                    initializeAndTrainModel(alpha, beta);
        else
            initializeAndTrainModel(optAlpha, optBeta);
    }

    /**
     * Compute MSE error and evaluate the given decay constants
     *
     * @param alpha
     * @param beta
     */
    public void initializeAndTrainModel(float alpha, float beta) {

        double[][] trainMatrix = new double[trainPoints][2];
        double[][] valMatrix = new double[validationPoints][2];
        double trend = initializeTrend();
        double permanent = initializePermanentComponent(trend);
        double lastPermanent;
        trainMatrix[0][0] = actual[0];
        trainMatrix[0][1] = permanent;

        for (int i = 1; i < trainPoints; i++) {

            trainMatrix[i][0] = actual[i];
            trainMatrix[i][1] = permanent + trend;

            lastPermanent = permanent;
            permanent = (alpha * actual[i]) + (1 - alpha) * (lastPermanent + trend);
            trend = (beta * (permanent - lastPermanent)) + ((1 - beta) * trend);
        }

        for (int t = 1; t <= validationPoints; t++) {
            valMatrix[t - 1][1] = permanent + trend * t;
            valMatrix[t - 1][0] = actual[trainPoints + t - 1];
        }

        double biasness = BiasnessHandler.handle(valMatrix);
        AccuracyIndicators AI = new AccuracyIndicators();
        ModelUtil.computeAccuracyIndicators(AI, trainMatrix, valMatrix, dof);
        AI.setBias(biasness);
        if (min_val_error >= AI.getMAPE()) {
            min_val_error = AI.getMAPE();
            optAlpha = alpha;
            optBeta = beta;
            accuracyIndicators = AI;
        }
    }

    /**
     * Forecast the points in horizon
     *
     * @param numFuturePoints
     */
    public void forecast(int numFuturePoints) {

        double trend = initializeTrend();
        double permanent = initializePermanentComponent(trend);
        double lastPermanent;
        double[][] trainMatrix = new double[trainPoints + validationPoints][2];
        trainMatrix[0][0] = actual[0];
        trainMatrix[0][1] = permanent;


        for (int i = 1; i < actual.length; i++) {

            trainMatrix[i][0] = actual[i];
            trainMatrix[i][1] = permanent + trend;

            lastPermanent = permanent;
            permanent = (optAlpha * actual[i]) + (1 - optAlpha) * (lastPermanent + trend);
            trend = (optBeta * (permanent - lastPermanent)) + ((1 - optBeta) * trend);
        }

        errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);

        forecastDataSet = new DataSet();
        Observation observation;
        double forecastValue, lowerBound, upperBound;

        for (int t = 1; t <= numFuturePoints; t++) {
            forecastValue = permanent + (t) * trend;
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
            observation.setIndependentValue(IndependentVariable.SLICE, t - 1);
            observation.setDependentValue(forecastValue);
            observation.setLowerDependentValue(lowerBound > 0 ? lowerBound : 0);
            observation.setUpperDependentValue(upperBound);
            forecastDataSet.add(observation);
        }
    }

    /**
     * Initialize Trend
     *
     * @return
     */
    private double initializeTrend() {
        Double sum = 0d;
        for (int i = 1; i <= 3; i++)
            sum = sum + ((actual[i] - actual[i - 1]));
        return sum / 3;
    }

    /**
     * Initialize constant component
     *
     * @param trend
     * @return
     */
    private double initializePermanentComponent(double trend) {
        double permanent = actual[0] - trend;
        return permanent;
    }

    /**
     * Returns model name appended with initialization flag
     *
     * @return
     */
    @Override
    public String getModelName() {
        return model.toString();
    }
}
