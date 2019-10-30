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
package com.forecasting.models.models.tests;

import com.forecasting.models.ModelExecutor;
import com.forecasting.models.dto.DataSet;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.models.impl.TripleExponentialSmoothingMultiplicativeModel;
import com.forecasting.models.models.util.SampleDataFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ModelExecutorTest extends TestCase {

    public ModelExecutorTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ModelExecutorTest.class);
    }

    public void testModelExectuor() throws Exception {

        SampleDataFactory dataFactory = new SampleDataFactory();


        DataSet timeSeries = dataFactory.getSeasonalDummyDataSet(42, 7);
        ModelExecutor executor = new ModelExecutor();
        executor.setTimeSeries(timeSeries);
        for (DataPoint point : timeSeries.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());

        executor.setForecastPoints(7);
//        executor.addPreprocessModel(new BoxCoxTransformation(0));
//        executor.addPreprocessModel(new FFTlowPassFilter(timeSeries.size() / 3));
//        executor.addModel(new TripleExponentialSmoothingAdditiveModel(35, 7, 7));
        executor.addModel(new TripleExponentialSmoothingMultiplicativeModel(35, 7, 7));
//        executor.setCompetitionModel(new EnsembleCompetitionModel(7,7));

//
        executor.runModels();
        ForecastModel model = executor.getFinalModel();
        DataSet forecast = model.getForecastDataSet();

        System.out.println("Forecast -");

        for (DataPoint point : forecast.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());
    }
}
