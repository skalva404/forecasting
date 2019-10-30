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
package com.forecasting.models.models.tests.pipe2;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.models.impl2.DoubleExponentialSmoothingModel_2;
import com.forecasting.models.models.util.SampleDataFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DoubleExponentialSmoothingTest_2 extends TestCase {

    public DoubleExponentialSmoothingTest_2(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(DoubleExponentialSmoothingTest_2.class);
    }

    public void testDoubleExponentialSmoothing() throws ModelInitializationException {
        SampleDataFactory dataFactory = new SampleDataFactory();
        DataSet dataSet = dataFactory.getDummyDataSet(42);
        DoubleExponentialSmoothingModel_2 model=new DoubleExponentialSmoothingModel_2(35,7);

        System.out.println("*********************************************");
        System.out.println("         Actual Time Series");
        System.out.println("*********************************************");
        for (DataPoint point : dataSet.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());

        model.init(dataSet);
        model.train();
        model.forecast(8);

        System.out.println("*********************************************");
        System.out.println("         Actual Time Series");
        System.out.println("*********************************************");

        for (DataPoint point : model.getForecastDataSet().getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());


    }
}