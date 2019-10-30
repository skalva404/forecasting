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

@Data
public class TripleExponentialSmoothingMultiplicativeModel_2 extends AbstractForecastModel {

    //Data parameters
    double[] actual;

    //Model Parameters
    private float optAlpha;
    private float optBeta;
    private float optGamma;
    private boolean findDecayConstants;
    private final int dof = 3;

    //Accuracy parameters
    private double min_val_error;

    /**
     * Constructor with  initializeCoefficients flag
     *
     * @param trainPoints
     * @param validationPoints
     * @param seasonalPeriod
     */
    public TripleExponentialSmoothingMultiplicativeModel_2(int trainPoints, int validationPoints, int seasonalPeriod) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.seasonalPeriod = seasonalPeriod;
        this.findDecayConstants = true;
        this.model = Model.TESM2;

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
    public TripleExponentialSmoothingMultiplicativeModel_2(int trainPoints, int validationPoints, int seasonalPeriod, float alpha, float beta, float gamma) {
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
            throw new ModelInitializationException("TESM :Invalid decay constants");
        if (trainPoints <= 0 || validationPoints <= 0 || seasonalPeriod <= 0)
            throw new ModelInitializationException("TESM :Invalid  model arguments");
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
                    for (float gamma = 0f; gamma <= 1f; gamma += 0.1f)
                        initializeAndTrainModel(alpha, beta, gamma);
        else
            initializeAndTrainModel(optAlpha, optBeta, optGamma);
    }


    /**
     * Compute MSE error and evaluate the given decay constants
     *
     * @param alpha
     * @param beta
     * @param gamma
     */
    public void initializeAndTrainModel(float alpha, float beta, float gamma) {

        double[][] trainMatrix = new double[trainPoints][2];
        double[][] valMatrix = new double[validationPoints][2];

        double trend = initializeTrend();
        double permanent = initializePermanentComponent();
        double[] seasonalComponent = initializeSeasonality(trainPoints);
        double lastPermanent;
        double forecast;

        trainMatrix[0][0] = actual[0];
        trainMatrix[0][1] = permanent;
        int index;

        double actualValue = 0d, forecastValue = 0d;

        for (int i = 1; i < trainPoints; i++) {

            index = i % seasonalPeriod;
            forecast = seasonalComponent[index] * (permanent + trend);

            trainMatrix[i][0] = actual[i];
            trainMatrix[i][1] = forecast;

            lastPermanent = permanent;
            permanent = (alpha * (actual[i] / seasonalComponent[index])) + (1 - alpha) * (lastPermanent + trend);
            trend = (beta * (permanent - lastPermanent)) + ((1 - beta) * trend);
            seasonalComponent[index] = (gamma * (actual[i] / permanent)) + ((1 - gamma) * seasonalComponent[index]);
        }

        for (int t = 1; t <= validationPoints; t++) {
            valMatrix[t - 1][1] = (permanent + trend * t) * seasonalComponent[(trainPoints + t - 1) % seasonalPeriod];
            valMatrix[t - 1][0] = actual[trainPoints + t - 1];
        }

        double biasness = BiasnessHandler.handle(valMatrix);
        AccuracyIndicators AI = new AccuracyIndicators();
        ModelUtil.computeAccuracyIndicators(AI, trainMatrix, valMatrix, dof);
        AI.setBias(biasness);

        if (min_val_error > AI.getMAPE()) {
            min_val_error = AI.getMAPE();
            optAlpha = alpha;
            optBeta = beta;
            optGamma = gamma;
            accuracyIndicators = AI;
        }
    }

    /**
     * generates forecast dataset with optimum/provided decay constants
     *
     * @param numFuturePoints
     */
    public void forecast(int numFuturePoints) {

        int startPoint = 0, endPoint = actual.length, index;
        double trend = initializeTrend();
        double permanent = initializePermanentComponent();
        double[] seasonalComponent = initializeSeasonality(trainPoints + validationPoints);
        double lastPermanent, forecast;
        double[][] trainMatrix = new double[trainPoints + validationPoints][2];
        trainMatrix[0][0] = actual[0];
        trainMatrix[0][1] = permanent;

        for (int i = startPoint + 1; i < endPoint; i++) {

            index = (i - startPoint) % seasonalPeriod;
            forecast = seasonalComponent[index] * (permanent + trend);

            trainMatrix[i][0] = actual[i];
            trainMatrix[i][1] = forecast;

            lastPermanent = permanent;
            permanent = (optAlpha * (actual[i] / seasonalComponent[index])) + (1 - optAlpha) * (lastPermanent + trend);
            trend = (optBeta * (permanent - lastPermanent)) + ((1 - optBeta) * trend);
            seasonalComponent[index] = (optGamma * (actual[i] / permanent)) + ((1 - optGamma) * seasonalComponent[index]);

        }

        errorBound= ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);

        forecastDataSet = new DataSet();
        startPoint = actual.length;
        double seasonalElement;
        Observation observation;
        double forecastValue, lowerBound, upperBound;

        for (int i = startPoint; i < startPoint + numFuturePoints; i++) {
            seasonalElement = seasonalComponent[i % seasonalPeriod];

            forecastValue = seasonalElement * (permanent + ((i - startPoint + 1) * trend));
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
     * @param trainPoints
     * @return
     */
    private double[] initializeSeasonality(int trainPoints) {
        double[] seasonalComponent = new double[seasonalPeriod];
        double[] cycle = new double[trainPoints / seasonalPeriod];
        for (int i = 0; i < cycle.length; i++) {
            for (int j = 0; j < seasonalPeriod; j++)
                cycle[i] += actual[i * seasonalPeriod + j];
            cycle[i] = cycle[i] / seasonalPeriod;
        }
        for (int index = 0; index < seasonalPeriod; index++) {
            Double value = 0d;
            for (int iter = 0; iter < cycle.length; iter++)
                value += actual[index + iter * seasonalPeriod] / cycle[iter];
            seasonalComponent[index] = value / (trainPoints / seasonalPeriod);
        }
        return seasonalComponent;
    }

    /**
     * Initialize Trend, set to Zero if initializeCoefficients=false
     *
     * @return
     */
    private double initializeTrend() {
        Double sum = 0d;
        for (int i = 0; i < seasonalPeriod; i++)
            sum = sum + ((actual[seasonalPeriod + i] - actual[i]) / seasonalPeriod);
        return sum / seasonalPeriod;
    }

    /**
     * Initialize constant component, set to Zero if initializeCoefficients=false
     *
     * @return
     */
    private double initializePermanentComponent() {
        return actual[0];
    }
}
