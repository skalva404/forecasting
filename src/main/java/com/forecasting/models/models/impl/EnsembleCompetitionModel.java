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
import com.forecasting.models.models.CompetitionModel;
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.exception.ModelNotFoundException;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EnsembleCompetitionModel implements CompetitionModel {
    private static Logger logger = Logger.getLogger(AuctionCompetitionModel.class.getName());
    private final String modelName = Model.ENSEMBLE.toString();
    @Setter
    private List<ForecastModel> models = new ArrayList<ForecastModel>();
    @Setter
    private int validationPoints;
    @Setter
    private int numfuturePoints;
    @Getter
    private ForecastModel finalModel;

    public EnsembleCompetitionModel(int validationPoints, int numfuturePoints) {
        this.validationPoints = validationPoints;
        this.numfuturePoints=numfuturePoints;
    }

    @Override
    public void addModel(ForecastModel model) {
        this.models.add(model);
    }

    @Override
    public void addModels(List<ForecastModel> models) {
        this.models = models;
    }

    /**
     * Runs Ensemble forecast model
     * @throws ModelNotFoundException
     * @throws ModelInitializationException
     */
    @Override
    public void run() throws ModelNotFoundException, ModelInitializationException {
            DataSet observations = models.get(0).getTimeSeriesDataSet();
            ForecastModel forecastModel = new EnsembleModel(models, validationPoints);
            forecastModel.init(observations);
            forecastModel.train();
            forecastModel.forecast(numfuturePoints);
            finalModel = forecastModel;
    }

    @Override
    public ForecastModel getFinalModel() {
        return finalModel;
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
