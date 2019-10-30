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
package com.forecasting.models;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.Model;
import com.forecasting.models.exception.ModelNotFoundException;
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.models.impl.*;
import com.forecasting.models.preprocess.OutlierDetector;
import lombok.Data;

import java.util.List;

@Data
public class Forecaster {

    List<Model> modelList;
    DataSet timeSeries;
    Long executionTime;
    ForecastModel finalModel;
    List<ForecastModel> allModels;

    boolean runAllModels;

    public Forecaster(List<Model> modelList) {
        this.modelList = modelList;
        runAllModels = false;
    }

    public Forecaster() {
        runAllModels = true;
    }


    public void init(DataSet observations) {
        timeSeries = observations;
    }

    /**
     * Generates forecast on time series using either all models or specified models
     *
     * @param trainPoints
     * @param validationPoints
     * @param futurePoints
     * @param seasonPeriod
     * @return
     * @throws Exception
     */
    public DataSet forecast(int trainPoints, int validationPoints, int futurePoints, int seasonPeriod) throws Exception {

        Long start = System.currentTimeMillis();

        ModelExecutor executor = new ModelExecutor();
        executor.setForecastPoints(futurePoints);
        OutlierDetector detector = new OutlierDetector(400);
        timeSeries = detector.removeOutlier(timeSeries, 7);
        executor.setTimeSeries(timeSeries);


        if (runAllModels || modelList.contains(Model.MOV)) {
            int step = 1;
            ForecastModel model;
            if (seasonPeriod == -1)
                model = new WeightedMovingAverageModel(null, 7, step, trainPoints, validationPoints);
            else
                model = new WeightedMovingAverageModel(null, 7, step, trainPoints, validationPoints);
            executor.addModel(model);
        }

        if ((runAllModels || modelList.contains(Model.TESA)) && seasonPeriod != -1) {
            ForecastModel model = new TripleExponentialSmoothingAdditiveModel(trainPoints, validationPoints, seasonPeriod);
            executor.addModel(model);
        }

        if (runAllModels || modelList.contains(Model.SSES)) {
            ForecastModel model;
            if (seasonPeriod == -1)
                model = new SeasonalSingleExponentialSmoothingModel(trainPoints, validationPoints, 7);
            else
                model = new SeasonalSingleExponentialSmoothingModel(trainPoints, validationPoints, 7);
            executor.addModel(model);
        }

        if ((runAllModels || modelList.contains(Model.DES)) && seasonPeriod != -1) {
            ForecastModel model = new DoubleExponentialSmoothingModel(trainPoints, validationPoints);
            executor.addModel(model);
        }


        if ((runAllModels || modelList.contains(Model.TESM)) && seasonPeriod != -1) {
            ForecastModel model = new TripleExponentialSmoothingMultiplicativeModel(trainPoints, validationPoints, seasonPeriod);
            executor.addModel(model);
        }

        if (runAllModels || modelList.contains(Model.FFT)) {

            ForecastModel model;
            int threshold = 5;
            if (seasonPeriod == -1)
                model = new FFTModelImproved(trainPoints, validationPoints, 7, threshold);
            else
                model = new FFTModelImproved(trainPoints, validationPoints, seasonPeriod, threshold);
            executor.addModel(model);
        }

        if (runAllModels || modelList.contains(Model.ENSEMBLE))
            executor.setCompetitionModel(new EnsembleCompetitionModel(validationPoints, futurePoints));
        else if (modelList.contains(Model.AUCTION))
            executor.setCompetitionModel(new AuctionCompetitionModel());
        else throw new ModelNotFoundException(" No competition Model Found, Provide either Ensemble Or Auction");

        executor.runModels();
        finalModel = executor.getFinalModel();
        allModels = executor.getAllModels();
        Long end = System.currentTimeMillis();
        executionTime = end - start;
        return finalModel.getForecastDataSet();
    }
}
