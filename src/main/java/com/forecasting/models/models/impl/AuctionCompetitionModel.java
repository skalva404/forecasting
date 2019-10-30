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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.forecasting.models.dto.Model;
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.exception.ModelNotFoundException;
import lombok.Getter;

public class AuctionCompetitionModel extends AbstractCompetitionModel {

    private static Logger logger = Logger.getLogger(AuctionCompetitionModel.class.getName());
    private final String modelName = Model.ENSEMBLE.toString();
    List<ForecastModel> models = new ArrayList<ForecastModel>();
    @Getter
    ForecastModel finalModel;

    /**
     * Add model for competition
     *
     * @param model
     */
    public void addModel(ForecastModel model) {
        models.add(model);
    }

    /**
     * Add list of models for competition
     *
     * @param models
     */
    public void addModels(List<ForecastModel> models) {
        this.models = models;
    }

    /**
     * Run competition , find best model on the basis of MAPE
     */
    public void run() throws  ModelNotFoundException,ModelInitializationException {
        double min_error = 1000000000000000d;
        double modelError;

        if (null == models || models.size() == 0) {
            logger.log(Level.SEVERE, "Error occured while running the model");
            throw new ModelNotFoundException("No Model Provided for competition");
        }
            for (ForecastModel model : models) {
                modelError = model.getAccuracyIndicators().getMAPE();
                if (modelError < min_error) {
                    min_error = modelError;
                    finalModel = model;
                }
            }
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
