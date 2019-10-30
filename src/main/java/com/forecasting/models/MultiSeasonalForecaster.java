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
import com.forecasting.models.experimental.TESAMultipleSeasonalityModel;
import com.forecasting.models.experimental.TESMMultipleSeasonalityModel;
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.models.impl.*;
import com.forecasting.models.preprocess.OutlierDetector;
import lombok.Data;

import java.util.List;

@Data
public class MultiSeasonalForecaster {

    List<Model> modelList;
    DataSet timeSeries;
    Long executionTime;
    ForecastModel finalModel;
    List<ForecastModel> allModels;

    boolean runAllModels;

    public MultiSeasonalForecaster(List<Model> modelList) {
        this.modelList = modelList;
        runAllModels = false;
    }

    public MultiSeasonalForecaster() {
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
     * @param periodicity1
     * @param periodicity2
     * @return
     * @throws Exception
     */
    public DataSet forecast(int trainPoints, int validationPoints, int futurePoints, int periodicity1, int periodicity2) throws Exception {

        Long start = System.currentTimeMillis();

        ModelExecutor executor = new ModelExecutor();
        executor.setForecastPoints(futurePoints);
        OutlierDetector detector = new OutlierDetector(500);
        timeSeries = detector.removeOutlier(timeSeries, periodicity1);
        executor.setTimeSeries(timeSeries);


        if (runAllModels || modelList.contains(Model.MOV)) {
            int step = 1;
            ForecastModel model = new WeightedMovingAverageModel(null, periodicity1, step, trainPoints, validationPoints);
            executor.addModel(model);
        }

        if (runAllModels || modelList.contains(Model.TESA)) {
            ForecastModel model = new TripleExponentialSmoothingAdditiveModel(trainPoints, validationPoints, periodicity1);
            executor.addModel(model);
        }

        if (runAllModels || modelList.contains(Model.TESM)) {
            ForecastModel model = new TripleExponentialSmoothingMultiplicativeModel(trainPoints, validationPoints, periodicity1);
            executor.addModel(model);
        }

        if ((runAllModels || modelList.contains(Model.MSTESA)) && periodicity2 != -1) {
            ForecastModel model = new TESAMultipleSeasonalityModel(trainPoints, validationPoints, periodicity1, periodicity2);
            executor.addModel(model);
        }

        if ((runAllModels || modelList.contains(Model.MSTESM)) && periodicity2 != -1) {
            ForecastModel model = new TESMMultipleSeasonalityModel(trainPoints, validationPoints, periodicity1, periodicity2);
            executor.addModel(model);
        }

        if (runAllModels || modelList.contains(Model.SSES)) {
            ForecastModel model = new SeasonalSingleExponentialSmoothingModel(trainPoints, validationPoints, periodicity1);
            executor.addModel(model);
        }

        if (runAllModels || modelList.contains(Model.DES)) {
            ForecastModel model = new DoubleExponentialSmoothingModel(trainPoints, validationPoints);
            executor.addModel(model);
        }

        if (runAllModels || modelList.contains(Model.FFT)) {
            int threshold = 5;
            ForecastModel model = new FFTModelImproved(trainPoints, validationPoints, periodicity1, threshold);
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
