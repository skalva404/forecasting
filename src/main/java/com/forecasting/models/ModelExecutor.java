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

import com.forecasting.models.dto.Model;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.exception.ModelNotFoundException;
import com.forecasting.models.dto.DataSet;
import com.forecasting.models.models.CompetitionModel;
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.preprocess.PreprocessModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
public class ModelExecutor {

    private List<ForecastModel> models = new ArrayList<ForecastModel>();
    private List<PreprocessModel> preprocessModels = new ArrayList<PreprocessModel>();
    private ForecastModel finalModel;
    private int forecastPoints;
    private DataSet timeSeries;
    private CompetitionModel competitionModel;
    private static Logger logger = Logger.getLogger(ModelExecutor.class.getName());

    public ModelExecutor() {
    }

    /**
     * Add forecast model to executor
     *
     * @param model
     */

    public void addModel(ForecastModel model) {
        models.add(model);
    }

    /**
     * Add preprocess models to the executor
     *
     * @param model
     */
    public void addPreprocessModel(PreprocessModel model) {
        preprocessModels.add(model);
    }

    /**
     * Run all models - pre-processing, forecast models and auction model
     *
     * @throws Exception
     */
    public void runModels() throws ModelNotFoundException, ModelInitializationException {

        if (null == models || models.size() == 0) {
            logger.log(Level.SEVERE, "Error occured while running the model");
            throw new ModelNotFoundException("No Model Provided to ModelExecutor");
        }

        if (preprocessModels.size() > 0)
            for (PreprocessModel model : preprocessModels) {
                model.init(timeSeries);
                timeSeries = model.transform();
            }

        for (ForecastModel model : models) {
            model.init(timeSeries);
            model.train();
            model.forecast(forecastPoints);
        }
        if (preprocessModels.size() > 0) {
            Collections.reverse(preprocessModels);

            for (PreprocessModel preprocessModel : preprocessModels) {

                DataSet dataSet;
                for (ForecastModel model : models) {
                    dataSet = preprocessModel.reverseTransform(model.getForecastDataSet());
                    model.setForecastDataSet(dataSet);
                }
            }
        }

        if (models.size() > 1) {
            competitionModel.addModels(models);
            competitionModel.run();
            finalModel = competitionModel.getFinalModel();
            if (finalModel.getModelName().equals(Model.ENSEMBLE.toString()))
                models.add(finalModel);
        } else finalModel = models.get(0);
    }

    /**
     * @return
     */
    public List<ForecastModel> getAllModels() {
        return models;
    }
}
