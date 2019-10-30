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
import com.forecasting.models.utils.DateUtil;
import com.forecasting.models.models.util.SampleDataFactory;
import com.forecasting.models.preprocess.impl.BoxCoxTransformation;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Date;

public class BoxCoxTransformationTest extends TestCase {

    public BoxCoxTransformationTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BoxCoxTransformationTest.class);
    }

    public void testBoxCoxTransformation() {

        System.out.println("Entered test");

        SampleDataFactory dataFactory = new SampleDataFactory();

        int points = 42;
        Long id = 7029631l;
        Date date = DateUtil.addDays(new Date(), -1);

        NormalDistribution gaussian=new NormalDistribution();

        DataSet dataSet=dataFactory.getDummyDataSet(30);
        BoxCoxTransformation transformation=new BoxCoxTransformation(0);
        transformation.init(dataSet);

        for (DataPoint point : dataSet.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());
        transformation.transform();

        System.out.println("Tran");
        for (DataPoint point : dataSet.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());

        DataSet out=transformation.reverseTransform(dataSet);

        System.out.println("Rev");
        for (DataPoint point : dataSet.getDataPoints())
            System.out.println("point.getDependentValue() = " + point.getDependentValue());
    }
}
