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
public class WeightedMovingAverageModel extends AbstractForecastModel {

    //Model Parameters
    private int dof;
    private float[] weights;
    private int slices;
    private int step;

    private double[] actual;
    private double errorBound;


    /**
     * Constructor, by default step is taken as one
     *
     * @param weights
     * @param slices
     * @param validationPoints
     */
    public WeightedMovingAverageModel(float[] weights, int slices, int trainPoints, int validationPoints) {
        this(weights, slices, 1, trainPoints, validationPoints);
    }

    /**
     * Constructor
     *
     * @param weights
     * @param slices
     * @param step
     * @param validationPoints
     */
    public WeightedMovingAverageModel(float[] weights, int slices, int step, int trainPoints, int validationPoints) {
        this.weights = weights;
        this.slices = slices;
        this.trainPoints = trainPoints;
        this.validationPoints = validationPoints;
        this.step = step;
        this.dof = slices;
        this.model = Model.MOV;
    }

    /**
     * Initialize weights and time series
     *
     * @param observations
     */
    public void init(final DataSet observations) throws ModelInitializationException {
        super.init(observations);
        if (weights != null) {
            if (weights.length != slices)
                throw new IllegalArgumentException(" Number of weights and Number of slices mismatch");
            if ((slices * step) > observations.getDataPoints().size())
                throw new IllegalArgumentException(" Insufficient data points for the given step size and number of slices");

            float sum = 0.0f;
            for (float weight : weights) sum += weight;
            boolean adjust = false;
            if (Math.abs(sum - 1.0) > TOLERANCE) {
                adjust = true;
            }
            int periods = weights.length;
            if (adjust) {
                for (int w = 0; w < periods; w++)
                    this.weights[w] = weights[w] / sum;
            }
        } else {
            weights = new float[slices];
            for (int i = 0; i < weights.length; i++)
                weights[i] = 1;
        }
    }

    /**
     * train method to compute accuracy indicators on the basis of 7 points validation points only as in MOV, there are no train points
     */
    public void train() {

        int w;
        double sum = 0, avg;
        double forecast;
        double[][] trainMatrix = new double[trainPoints][2];
        double[][] valMatrix = new double[validationPoints][2];
        actual = observations.toArray();

        for (int i = 0; i < step * slices; i++)
            sum += actual[i];

        avg = sum / slices;

        for (int pos = 0; pos < trainPoints; pos++) {
            sum = 0;
            w = 0;

            if (pos >= step * slices) {
                for (int k = pos - step * slices; k < pos; k += step)
                    sum += actual[k] * weights[w++];
                forecast = sum / slices;
            } else forecast = avg;

            trainMatrix[pos][0] = actual[pos];
            trainMatrix[pos][1] = forecast;
        }

        for (int pos = actual.length - validationPoints, j = 0; pos < actual.length; pos++) {
            sum = 0;
            w = 0;
            for (int k = pos - step * slices; k < pos; k += step)
                sum += actual[k] * weights[w++];
            forecast = sum / slices;
            valMatrix[j][0] = actual[pos];
            valMatrix[j][1] = forecast;
            j++;
        }
        double biasness = BiasnessHandler.handleOffset(valMatrix);
        accuracyIndicators.setBias(biasness);
        ModelUtil.computeAccuracyIndicators(accuracyIndicators, trainMatrix, valMatrix, dof);
        errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);
    }

    /**
     * Computes forecast
     *
     * @param numFuturePoints
     */
    public void forecast(int numFuturePoints) {
        double sum, adjustedValue, forecastValue, upperBound, lowerBound;
        int w = 0;
        double[] forecast = new double[numFuturePoints];
        for (int i = actual.length; i < actual.length + numFuturePoints; i++) {
            sum = 0;
            w = 0;
            for (int j = i - step * slices; j < i; j += step) {
                sum += weights[w] * (j < actual.length ? actual[j] : forecast[j - actual.length]);
            }
            forecast[i - actual.length] = sum / slices;
        }

        BiasnessHandler.adjustBiasness(forecast, accuracyIndicators.getBias());

        forecastDataSet = new DataSet();
        for (int i = 0; i < forecast.length; i++) {
            Observation observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i);

            adjustedValue = BiasnessHandler.addOffset(forecast[i], accuracyIndicators.getBias());

            if (adjustedValue > 0)
                forecastValue = adjustedValue;
            else forecastValue = forecast[i];

            if (forecastValue < 0) {
                forecastValue = 0l;
                upperBound = 0l;
                lowerBound = 0l;
            } else {
                lowerBound = forecastValue - errorBound;
                upperBound = forecastValue + errorBound;
            }

            observation.setDependentValue(forecastValue);
            observation.setLowerDependentValue(lowerBound > 0 ? lowerBound : 0);
            observation.setUpperDependentValue(upperBound);


            forecastDataSet.add(observation);
        }
    }

    public DataSet getForecastDataSet() {
        return forecastDataSet;
    }

    /**
     * Returns model name
     *
     * @return
     */
    public String getModelName() {
        return model.toString().concat("-step-" + step);
    }
}
