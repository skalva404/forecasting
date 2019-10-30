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
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.util.SampleDataFactory;
import com.forecasting.models.preprocess.OutlierDetector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OutlierDetectorTest extends TestCase {

    public OutlierDetectorTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(OutlierDetectorTest.class);
    }

    public void testOutlierDetector() throws Exception {

        SampleDataFactory dataFactory=new SampleDataFactory();
        DataSet dataSet=dataFactory.getDataSetFromFile();

        System.out.println("Original Data Set...........");

        for (DataPoint dp : dataSet.getDataPoints())
            System.out.println(String.format("Key = %s    forecast = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue()));

        OutlierDetector detector=new OutlierDetector(400);
        dataSet=detector.removeOutlier(dataSet,7);

        System.out.println("After Removing Outlier.........");

        for (DataPoint dp : dataSet.getDataPoints())
            System.out.println(String.format("Key = %s    forecast = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue()));
    }

}
