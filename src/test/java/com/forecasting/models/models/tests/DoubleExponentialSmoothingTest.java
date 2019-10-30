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

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.util.SampleDataFactory;
import com.forecasting.models.models.impl.DoubleExponentialSmoothingModel;
import com.forecasting.models.preprocess.OutlierDetector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DoubleExponentialSmoothingTest extends TestCase {

    public DoubleExponentialSmoothingTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(DoubleExponentialSmoothingTest.class);
    }

    public void testDoubleExponentialSmoothing() throws Exception {
        SampleDataFactory dataFactory = new SampleDataFactory();
        DataSet dataSet = dataFactory.getDataSetFromFile();

        OutlierDetector outlierDetector = new OutlierDetector(400);
        dataSet = outlierDetector.removeOutlier(dataSet, 7);

        DoubleExponentialSmoothingModel model=new DoubleExponentialSmoothingModel(35,7);

        for (DataPoint point : dataSet.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());
        model.init(dataSet);
        model.train();
        model.forecast(10);
        System.out.println("Forecast -");

        for (DataPoint point : model.getForecastDataSet().getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());
    }
}