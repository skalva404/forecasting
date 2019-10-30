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
package com.forecasting.models.postprocess;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.ForecastModel;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.List;

public class Evaluator {

    int validationPoints;
    List<ForecastModel> models;

    public Evaluator(List<ForecastModel> models, int validationPoints) {
        this.models = models;
        this.validationPoints = validationPoints;
    }

    public void evaluate() {

        double mape;
        double dir_error;
        double mase;
        double weight_sum = 0;
        double sigma = getStandardDeviation(models.get(0).getTimeSeriesDataSet());
        double unitWeight = new Double(1.0 / validationPoints);
        ForecastModel selectedModel;


        for (int point = 0; point < validationPoints; point++) {
            selectedModel = getBestModel(point);
            selectedModel.setWeight(selectedModel.getWeight() + unitWeight);
        }

        // Compute weight from directional error,MAPE,MASE
        for (ForecastModel model : models) {
            mape = model.getAccuracyIndicators().getMAPE();

            if(validationPoints>1) {
            dir_error = model.getAccuracyIndicators().getDirectionalError() / 100;
            model.setWeight(model.getWeight() * (1 - dir_error));
            }
            mase = mape / sigma;
            model.setWeight(model.getWeight() * Math.pow(Math.E, -1 * mape));
//            model.setWeight(model.getWeight() * Math.pow(Math.E, -1 * mase));
            weight_sum += model.getWeight();

        }

        //Normalize weights

        if(weight_sum!=0)
        for (ForecastModel model : models)
            model.setWeight(model.getWeight() / weight_sum);

    }

    private double getStandardDeviation(DataSet ts) {
        SummaryStatistics stats = new SummaryStatistics();

        for (DataPoint dp : ts.getDataPoints())
            stats.addValue(dp.getDependentValue());
        double sd = stats.getStandardDeviation();
        return sd;

    }

    private ForecastModel getBestModel(int point) {

        double min_error = Double.MAX_VALUE;
        ForecastModel selectedModel = null;
        for (ForecastModel model : models) {
            double[] error = model.getAccuracyIndicators().getValidationErrors();
            if (error[point] < min_error) {
                min_error = error[point];
                selectedModel = model;
            }
        }
        return selectedModel;
    }
}
