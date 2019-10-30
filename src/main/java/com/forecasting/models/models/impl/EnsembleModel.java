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
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.postprocess.Evaluator;
import com.forecasting.models.utils.AccuracyIndicators;
import com.forecasting.models.exception.ModelInitializationException;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.List;
import java.util.Set;

public class EnsembleModel extends AbstractForecastModel {

    int validationPoints;
    List<ForecastModel> models;

    /**
     * Constructor
     *
     * @param models
     * @param validationPoints
     */
    public EnsembleModel(List<ForecastModel> models, int validationPoints) {
        this.models = models;
        this.validationPoints = validationPoints;
        model = Model.ENSEMBLE;
    }

    public void init(final DataSet observations) throws ModelInitializationException {
        super.init(observations);
    }

    @Override
    public void train()
    {
        new Evaluator(models,validationPoints).evaluate();
    }


    /**
     * train method to set weights of different forecast models
     */
//    @Override
//    public void train() {
//        ForecastModel selectedModel;
//        double unitWeight = new Double(1.0 / validationPoints);
//        for (int point = 0; point < validationPoints; point++) {
//            selectedModel = getBestModel2(point);
//            selectedModel.setWeight(selectedModel.getWeight() + unitWeight);
//        }
//
//    }

//   This method learns weights for different models, here , the directional errors are used to learn weights

//    @Override
//    public void train() {
//        double total_dir_error = 0;
//
//        for (ForecastModel model : models) {
//            total_dir_error += (100 - model.getAccuracyIndicators().getDirectionalError());
//        }
//
//        for (ForecastModel model : models)
//            model.setWeight((100 - model.getAccuracyIndicators().getDirectionalError()) / total_dir_error);
//    }

//
//    @Override
//    public void train() {
//
//        ForecastModel selectedModel;
//        double unitWeight = new Double(1.0 / validationPoints);
//        Map<Model, double[]> weights=new HashMap<Model,double[]>();
//
//        for (int point = 0; point < validationPoints; point++) {
//            selectedModel = getBestModel(point);
//
//            if(weights.get(selectedModel.getModelName()))
//            selectedModel.setWeight(selectedModel.getWeight() + unitWeight);
//        }
//    }

    //Not in use
    private double computeDistributionProbability(ForecastModel model) {

        DataSet ts = model.getTimeSeriesDataSet();
        DataSet forecastDataSet = model.getForecastDataSet();
        SummaryStatistics stats = new SummaryStatistics();

        for (DataPoint dp : ts.getDataPoints())
            stats.addValue(dp.getDependentValue());

        double mean = stats.getMean();
        double sd = stats.getStandardDeviation();
        double probability = 0;

        NormalDistribution gaussian = new NormalDistribution(mean, sd);

        for (DataPoint dp : forecastDataSet.getDataPoints())
            probability += gaussian.density(dp.getDependentValue());
        return probability;
    }


    /**
     * Calculate weighted sum of forecast from forecast models
     *
     * @param numfuturePoints
     */
    @Override
    public void forecast(int numfuturePoints) {

        double[][] forecast = new double[numfuturePoints][3];
        double[][] wtforecast;
        double wtMape = 0d;

        for (ForecastModel model : models) {
            wtforecast = getWeightedForecast(model.getForecastDataSet().getDataPoints(), model.getWeight());
            wtMape += model.getAccuracyIndicators().getMAPE() * model.getWeight();
            for (int i = 0; i < numfuturePoints; i++) {
                forecast[i][0] += wtforecast[i][0];
                forecast[i][1] += wtforecast[i][1];
                forecast[i][2] += wtforecast[i][2];
            }
        }

        forecastDataSet = new DataSet();
        Observation point;
        for (int i = 0; i < forecast.length; i++) {
            point = new Observation();
            point.setIndependentValue(IndependentVariable.SLICE, i);
            point.setDependentValue(forecast[i][0]);
            point.setLowerDependentValue(forecast[i][1]);
            point.setUpperDependentValue(forecast[i][2]);
            forecastDataSet.add(point);
        }
        accuracyIndicators = new AccuracyIndicators();
        accuracyIndicators.setMAPE(wtMape);
    }


    private double[][] getWeightedForecast(Set<DataPoint> dataPoints, double weight) {

        int i = 0;
        double[][] output = new double[dataPoints.size()][3];
        for (DataPoint dataPoint : dataPoints) {
            output[i][0] = dataPoint.getDependentValue() * weight;
            output[i][1] = dataPoint.getLowerDependentValue() * weight;
            output[i][2] = dataPoint.getUpperDependentValue() * weight;
            i++;
        }
        return output;
    }

    //Not in use in case of directional error weightage
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

    private ForecastModel getModelWithBestDirection(int point) {


        //TO DO : if val points =1, dir error wont work
        double min_error = Double.MAX_VALUE;
        ForecastModel selectedModel = null;
        for (ForecastModel model : models) {
            double error = model.getAccuracyIndicators().getDirectionalError();
            if (error < min_error) {
                min_error = error;
                selectedModel = model;
            }
        }
        return selectedModel;
    }

    //Not in use in case of directional error weightage
    private ForecastModel getBestModel2(int point) {

        double min_error = Double.MAX_VALUE;
        ForecastModel selectedModel = null;
        for (ForecastModel model : models) {
            double[] error = model.getAccuracyIndicators().getValidationErrors();
            double[] directionalMatrix = model.getAccuracyIndicators().getDirectionalErrorMatrix();
            if (directionalMatrix[point] == 1 && (error[point] < min_error)) {
                min_error = error[point];
                selectedModel = model;
            }
        }
        if (selectedModel == null)
            selectedModel = getBestModel(point);

        return selectedModel;
    }
}