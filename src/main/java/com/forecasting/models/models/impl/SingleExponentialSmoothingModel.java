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
 * Substitute for Moving Average
 */
@Data
public class SingleExponentialSmoothingModel extends AbstractForecastModel {

    private double min_mse_error;
    private double[] actual;
    private float optAlpha;
    private final int dof=1;


    public void init(DataSet observations) throws ModelInitializationException {
        super.init(observations);
    }

    public SingleExponentialSmoothingModel(int trainPoints, int validationPoints) {
        this.validationPoints = validationPoints;
        this.trainPoints = trainPoints;
        this.model = Model.SES;
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

        double biasness = BiasnessHandler.handle(valMatrix);
        accuracyIndicators.setBias(biasness);
        ModelUtil.computeAccuracyIndicators(accuracyIndicators,null,valMatrix,dof);


    }

    /**
     * Learns best decay coeffcient  on a specific point
     *
     * @param point
     * @param valMatrix
     */
    private void populateForecastMatrix(int point, double[][] valMatrix) {

        min_mse_error = Double.MAX_VALUE;
        int startPoint = 0;
        int endPoint = trainPoints + point;
        for (float alpha = 0f; alpha <= 1f; alpha += 0.01f)
            initializeAndtrain(alpha, startPoint, endPoint, valMatrix);
    }

    /**
     * Curve fitting on provided alpha by minimizing MSE
     *
     * @param alpha
     * @param startPoint
     * @param endPoint
     * @param valMatrix
     */
    private void initializeAndtrain(float alpha, int startPoint, int endPoint, double[][] valMatrix) {

        double forecast, train_error, forecastValue = 0, actualValue = 0;
        double lastValue = actual[startPoint];
        double[][] trainMatrix = new double[endPoint - startPoint][2];

        for (int i = startPoint + 1; i < endPoint + 1; i++) {
            forecast = lastValue;

            if (i > startPoint + 1 && i < endPoint) {
                trainMatrix[i - startPoint][0] = actual[i];
                trainMatrix[i - startPoint][1] = forecast;
            }
            lastValue = (alpha * actual[i]) + ((1 - alpha) * lastValue);
            if (i == endPoint) {
                actualValue = actual[i];
                forecastValue = forecast;
            }
        }

        train_error = ModelUtil.computeMSE(trainMatrix);
        if (min_mse_error > train_error) {
            min_mse_error = train_error;
            optAlpha = alpha;
            valMatrix[startPoint][0] = actualValue;
            valMatrix[startPoint][1] = forecastValue;
            errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);
            ModelUtil.computeAccuracyIndicators(accuracyIndicators,trainMatrix,null,dof);
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
        double lastValue=actual[startPoint];

        for (int i = startPoint + 1; i < endPoint; i++) {
            lastValue = optAlpha * actual[i] + (1 - optAlpha) * lastValue;
        }

        forecastDataSet = new DataSet();
        startPoint = actual.length;
        Observation observation;
        double forecastValue, lowerBound, upperBound;

        for (int i = startPoint; i < startPoint + numFuturePoints; i++) {
            forecastValue = lastValue;

            lastValue=optAlpha*forecastValue+(1-optAlpha)*lastValue;

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
