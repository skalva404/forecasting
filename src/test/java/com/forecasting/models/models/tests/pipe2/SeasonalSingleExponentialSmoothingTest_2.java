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
import com.forecasting.models.models.ForecastModel;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.models.impl2.SeasonalSingleExponentialSmoothingModel_2;
import com.forecasting.models.models.util.SampleDataFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SeasonalSingleExponentialSmoothingTest_2 extends TestCase {


    public SeasonalSingleExponentialSmoothingTest_2(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SeasonalSingleExponentialSmoothingTest_2.class);
    }

    public void testSES() throws ModelInitializationException {

        SampleDataFactory dataFactory = new SampleDataFactory();
        DataSet timeSeries = dataFactory.getSeasonalDummyDataSet(42, 7);


        System.out.println("*********************************************");
        System.out.println("         Actual Time Series");
        System.out.println("*********************************************");

        for (DataPoint point : timeSeries.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());
        ForecastModel ses = new SeasonalSingleExponentialSmoothingModel_2(35, 7, 7);
        ses.init(timeSeries);
        ses.train();
        ses.forecast(7);

        DataSet forecastDS = ses.getForecastDataSet();


        System.out.println("*********************************************");
        System.out.println("         Forecasted Time Series");
        System.out.println("*********************************************");


        for (DataPoint point : forecastDS.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());
    }

}
