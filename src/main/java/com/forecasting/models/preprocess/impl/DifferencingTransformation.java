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
package com.forecasting.models.preprocess.impl;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;

/**
 * Only 1st Order Differencing , good for now.
 */
public class DifferencingTransformation extends AbstractPreprocessModel {

    public DifferencingTransformation() {
    }

    public void init(DataSet observations) {
        super.init(observations);
    }

    /**
     * Only 1st Order differencing.
     * @return
     */
    @Override
    public DataSet transform() {
        DataSet output = new DataSet();
        double[] points = observations.toArray();
        Observation observation;

        for (int i = 1; i < points.length; i++) {
            observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i - 1);
            observation.setDependentValue(points[i] -  points[i - 1]);
            output.add(observation);
        }
        return output;
    }

    /**
     * Reverse tranforms by picking last value of actual time series
     * @return
     */
    @Override
    public DataSet reverseTransform(DataSet input) {

        DataSet output = new DataSet();
        Observation observation;

        double[] actualPoints =  observations.toArray();
        double difference = actualPoints[actualPoints.length - 1];

        for (DataPoint point : input.getDataPoints()) {
            observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, point.getIndependentValue(IndependentVariable.SLICE));
            observation.setDependentValue(point.getDependentValue()+difference);
            observation.setLowerDependentValue(point.getLowerDependentValue()+difference);
            observation.setUpperDependentValue(point.getUpperDependentValue()+difference);
            output.add(observation);
            difference = point.getDependentValue();

        }

        return output;
    }
}
