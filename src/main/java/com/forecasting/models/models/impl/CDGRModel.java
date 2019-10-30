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
import com.forecasting.models.utils.ModelUtil;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.postprocess.BiasnessHandler;
import com.forecasting.models.postprocess.ErrorBoundsHandler;
import lombok.Data;

/**
 * Compounded daily growth model implementation
 */
@Data
public class CDGRModel extends AbstractForecastModel {

    private int growthPeriod;
    private double[] actual;
    private final int dof = 1;

    /**
     * Constructor
     *
     * @param growthPeriod
     * @param validationPoints
     */
    public CDGRModel(int growthPeriod, int validationPoints) {
        this.growthPeriod = growthPeriod;
        this.validationPoints = validationPoints;
        this.model = Model.CDGR;
    }

    /**
     * initialize the model
     *
     * @param observations
     */
    public void init(final DataSet observations) throws ModelInitializationException {
        super.init(observations);
        if (growthPeriod <= 0 || validationPoints <= 0)
            throw new ModelInitializationException("CDGR : Invalid Model initialization arguments");
    }

    /**
     * train method to compute accuracy indicators on the basis of 7 points validation points only as in CDGR, there are no train points
     */
    public void train() {

        int i = 0;
        double value1, value2, growth, forecast;
        double[][] valMatrix = new double[validationPoints][2];
        actual = observations.toArray();

        for (int pos = actual.length - validationPoints, j = 0; pos < actual.length; pos++) {
            value1 = actual[pos - 1];
            value2 = actual[pos - 1 - growthPeriod];
            growth = Math.pow(value1 / value2, 1.0d / growthPeriod) - 1;
            forecast = value1 * (1 + growth);
            valMatrix[j][0] = actual[pos];
            valMatrix[j][1] = forecast;
            j++;
        }
        BiasnessHandler.handle(valMatrix);
        double biasness = BiasnessHandler.handle(valMatrix);
        accuracyIndicators.setBias(biasness);
        ModelUtil.computeAccuracyIndicators(accuracyIndicators, valMatrix, valMatrix, dof);
        errorBound = ErrorBoundsHandler.computeErrorBoundInterval(valMatrix);
    }

    /**
     * This method does forecasting on the basis of compounded daily growth
     *
     * @param numFuturePoints
     */
    public void forecast(int numFuturePoints) {
        double value1;
        double value2;
        double growth;
        double[] forecast = new double[numFuturePoints];
        for (int i = actual.length; i < actual.length + numFuturePoints; i++) {
            value1 = (i - 1) < actual.length ? actual[i - 1] : forecast[i - 1 - actual.length];
            value2 = (i - 1 - growthPeriod) < actual.length ? actual[i - 1 - growthPeriod] : forecast[i - 1 - growthPeriod - actual.length];
            growth = Math.pow(value1 / value2, 1.0d / growthPeriod) - 1;
            forecast[i - actual.length] = value1 * (1 + growth);
        }

        BiasnessHandler.adjustBiasness(forecast, accuracyIndicators.getBias());
        forecastDataSet = new DataSet();
        DataPoint observation;
        for (int i = 0; i < forecast.length; i++) {
            observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i);
            observation.setDependentValue(forecast[i]);
            observation.setLowerDependentValue(forecast[i] - errorBound);
            observation.setUpperDependentValue(forecast[i] + errorBound);
            forecastDataSet.add(observation);
        }
    }
}
