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
package com.forecasting.models.preprocess;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;

import java.util.logging.Logger;

public class OutlierDetector {

    private static Logger logger = Logger.getLogger(OutlierDetector.class.getName());

    double threshold;

    public OutlierDetector(double threshold) {
        this.threshold = (1 + threshold / 100);
    }

    public DataSet removeOutlier(DataSet dataSet, int cycle) {
        double[] points = dataSet.toArray();
        double sum, avg;
        int j;

        for (int i = cycle; i < points.length; i++) {
            sum = 0;
            for (j = i-cycle; j < i; j++)
                sum += points[j];
            avg = sum / cycle;
            if (avg * threshold < points[i] || avg >threshold*points[i]) {
//                logger.info(" Outlier Detected :  Average = " + avg + " and Observation  = " + points[j]);
                points[i] = (long)avg;
            }
        }

        DataSet output = new DataSet();
        for (int i = 0; i < points.length; i++) {
            DataPoint observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i);
            observation.setDependentValue(points[i]);
            output.add(observation);
        }
        return output;
    }
}
