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
import com.forecasting.models.dto.Model;
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.utils.AccuracyIndicators;
import com.forecasting.models.exception.ModelInitializationException;

public abstract class AbstractForecastModel implements ForecastModel {

    protected boolean initialized = false;
    protected static double TOLERANCE = 0.00000001;

    protected int validationPoints;
    protected int trainPoints;
    protected int seasonalPeriod;


    protected DataSet observations;
    protected DataSet forecastDataSet;
    protected Model model;
    protected double weight = 0d;
    protected double errorBound;
    protected AccuracyIndicators accuracyIndicators=new AccuracyIndicators();

    /**
     * Intialize with observations
     * @param observations
     * @throws ModelInitializationException
     */
    public void init(DataSet observations) throws ModelInitializationException {
        if (observations == null)
            throw new ModelInitializationException("Time series data is null");
        this.observations = observations;
        initialized = true;
    }

    public abstract void train();

    public abstract void forecast(int points);

    public int windowSize() {
        return observations.size();
    }

    public DataSet getForecastDataSet() {
        return forecastDataSet;
    }

    public void setForecastDataSet(DataSet dataSet) {
       this.forecastDataSet=dataSet;
    }
    public String getModelName() {
        return model.toString();
    }

    public AccuracyIndicators getAccuracyIndicators() {
        return accuracyIndicators;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public DataSet getTimeSeriesDataSet() {
        return  this.observations;
    }


}
