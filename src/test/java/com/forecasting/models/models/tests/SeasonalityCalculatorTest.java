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
import com.forecasting.models.models.util.SampleDataFactory;
import com.forecasting.models.utils.DateUtil;
import com.forecasting.models.preprocess.SeasonalityCalculator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Date;

public class SeasonalityCalculatorTest extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SeasonalityCalculatorTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(SeasonalityCalculatorTest.class);
    }

    public void testSeasonalityCalculator() {

        SampleDataFactory dataFactory = new SampleDataFactory();

        int points = 42;
        Long id = 7029631l;
        Date date = DateUtil.addDays(new Date(), -1);

        DataSet dataSet=dataFactory.getSeasonalDummyDataSet(178,7);


//        DataSet dataSet=dataFactory.getDataSet(id,date,points);

//
//        System.out.println("Original DataSet");
//        for (DataPoint point : dataSet.getDataPoints()) {
//            System.out.println(String.format(" key = %s     value  = %s ", point.getIndependentValue(IndependentVariable.SLICE), point.getDependentValue()));
//        }

        SeasonalityCalculator seasonalityCalculator = new SeasonalityCalculator();
        seasonalityCalculator.computeSeasonality(dataSet);
    }

}
