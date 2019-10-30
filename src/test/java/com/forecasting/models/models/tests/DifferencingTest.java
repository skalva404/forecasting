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
import com.forecasting.models.utils.DateUtil;
import com.forecasting.models.models.util.SampleDataFactory;
import com.forecasting.models.preprocess.impl.DifferencingTransformation;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Date;

public class DifferencingTest extends TestCase {


    public DifferencingTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(DifferencingTest.class);
    }

    public void testDifferencing()
    {

        SampleDataFactory dataFactory = new SampleDataFactory();

        int points = 42;
        Long id = 7029631l;
        Date date = DateUtil.addDays(new Date(), -1);
        DataSet dataSet = dataFactory.getDataSet(points);

//        DataSet dataSet=dataFactory.getDummyDataSet(20);

       if(dataSet.size()==points) {
        for(DataPoint point:dataSet.getDataPoints())
            System.out.println("key = "+point.getIndependentValue(IndependentVariable.SLICE)+"     point = " + point.getDependentValue());

        DifferencingTransformation tranformation=new DifferencingTransformation();

        DataSet forecastDataSet=dataFactory.buildDataSet(5,8);

        tranformation.init(dataSet);
        DataSet output=tranformation.transform();

        System.out.println("After..........");

        for(DataPoint point:output.getDataPoints())
            System.out.println("key = "+point.getIndependentValue(IndependentVariable.SLICE)+"     point = " + point.getDependentValue());

        System.out.println("forecast..........");

        for(DataPoint point:tranformation.reverseTransform(forecastDataSet).getDataPoints())
            System.out.println("key = "+point.getIndependentValue(IndependentVariable.SLICE)+"     point = " + point.getDependentValue());

       }
        assertTrue( true );
    }

}
