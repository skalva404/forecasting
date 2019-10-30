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
import lombok.Data;

/**
 * Substitute for Moving Average
 */
@Data
public class SingleExponentialSmoothingModel_2 extends AbstractForecastModel {

    private double min_val_error;
    private double[] actual;
    private float optAlpha;
    private final int dof = 1;
    private boolean findDecayConstant;


    public void init(DataSet observations) throws ModelInitializationException {
        super.init(observations);
    }

    public SingleExponentialSmoothingModel_2(int trainPoints, int validationPoints, float alpha) {
        this(trainPoints, validationPoints);
        this.optAlpha = alpha;
        this.findDecayConstant = false;
    }

    public SingleExponentialSmoothingModel_2(int trainPoints, int validationPoints) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.model = Model.SES2;
        this.findDecayConstant = true;
    }

    /**
     * Computes best coefficient for different validation points
     */
    @Override
    public void train() {
        actual = observations.toArray();
        min_val_error = Double.MAX_VALUE;
        if (findDecayConstant)
            for (float alpha = 0f; alpha <= 1f; alpha += 0.01f)
                initializeAndTrainModel(alpha);
        else
            initializeAndTrainModel(optAlpha);
    }


    /**
     * Curve fitting on provided alpha by minimizing MSE
     *
     * @param alpha
     */
    private void initializeAndTrainModel(float alpha) {

        double forecast;
        double lastValue = actual[0];
        double[][] trainMatrix = new double[trainPoints][2];
        double[][] valMatrix = new double[validationPoints][2];

        for (int i = 1; i < trainPoints; i++) {
            forecast = lastValue;

            trainMatrix[i][0] = actual[i];
            trainMatrix[i][1] = forecast;
            lastValue = (alpha * actual[i]) + ((1 - alpha) * lastValue);
        }

        for (int t = 1; t <= validationPoints; t++) {
            valMatrix[t - 1][1] = lastValue;
            valMatrix[t - 1][0] = actual[trainPoints + t - 1];
        }


        double biasness = BiasnessHandler.handle(valMatrix);
        AccuracyIndicators AI = new AccuracyIndicators();
        ModelUtil.computeAccuracyIndicators(AI, trainMatrix, valMatrix, dof);
        AI.setBias(biasness);
        if (min_val_error >= AI.getMAPE()) {
            min_val_error = AI.getMAPE();
            optAlpha = alpha;
            accuracyIndicators = AI;
        }
    }

    /**
     * Forecasts using optimum alpha value
     *
     * @param numFuturePoints
     */
    @Override
    public void forecast(int numFuturePoints) {

        int startPoint = 0, endPoint = actual.length;
        double lastValue = actual[startPoint],forecast;
        double[][] trainMatrix = new double[trainPoints + validationPoints][2];
        trainMatrix[0][0] = actual[startPoint];
        trainMatrix[0][1] = lastValue;

        for (int i = startPoint + 1; i < endPoint; i++) {
            forecast = lastValue;

            trainMatrix[i][0] = actual[i];
            trainMatrix[i][1] = forecast;

            lastValue = optAlpha * actual[i] + (1 - optAlpha) * lastValue;
        }

        errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);

        forecastDataSet = new DataSet();
        startPoint = actual.length;
        Observation observation;
        double forecastValue, lowerBound, upperBound;

        for (int i = startPoint; i < startPoint + numFuturePoints; i++) {
            forecastValue = lastValue;

            lastValue = optAlpha * forecastValue + (1 - optAlpha) * lastValue;

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
}
