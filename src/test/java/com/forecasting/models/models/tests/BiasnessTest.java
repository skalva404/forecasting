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

import com.forecasting.models.models.util.SampleDataFactory;
import com.forecasting.models.postprocess.BiasnessHandler;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BiasnessTest extends TestCase {

    public BiasnessTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BiasnessTest.class);
    }

    public void testPositiveBiasness() {
        SampleDataFactory dataFactory = new SampleDataFactory();
        double[][] dataMatrix = dataFactory.getBiasedDataMatrix(100d, 6, 20);
        double adjustmentFactor;

        System.out.println("**************** Positive Biasness *****************");

        for(int i=0;i<dataMatrix.length;i++)
            System.out.println(String.format(" Actual = %s     Forecast = %s",dataMatrix[i][0],dataMatrix[i][1]));

        adjustmentFactor= BiasnessHandler.handle(dataMatrix);

        System.out.println("adjustmentFactor = " + adjustmentFactor);
        System.out.println("After handling Biasness....");

        for(int i=0;i<dataMatrix.length;i++)
            System.out.println(String.format(" Actual = %s     Forecast = %s",dataMatrix[i][0],dataMatrix[i][1]));
    }

    public void testNegativeBiasness() {


        System.out.println("**************** Negative Biasness *****************");

        SampleDataFactory dataFactory = new SampleDataFactory();
        double[][] dataMatrix = dataFactory.getBiasedDataMatrix(100d, -6, 20);
        double adjustmentFactor;

        for(int i=0;i<dataMatrix.length;i++)
            System.out.println(String.format(" Actual = %s     Forecast = %s",dataMatrix[i][0],dataMatrix[i][1]));

        BiasnessHandler.handle(dataMatrix);
        adjustmentFactor=BiasnessHandler.handle(dataMatrix);

        System.out.println("adjustmentFactor = " + adjustmentFactor);
        System.out.println("After handling Biasness....");

        for(int i=0;i<dataMatrix.length;i++)
            System.out.println(String.format(" Actual = %s     Forecast = %s",dataMatrix[i][0],dataMatrix[i][1]));
    }

    public void testZeroBiasness() {


        System.out.println("**************** Zero Biasness *****************");
        SampleDataFactory dataFactory = new SampleDataFactory();
        double[][] dataMatrix = dataFactory.getBiasedDataMatrix(100d, 100, 20);
        double adjustmentFactor;

        for(int i=0;i<dataMatrix.length;i++)
            System.out.println(String.format(" Actual = %s     Forecast = %s",dataMatrix[i][0],dataMatrix[i][1]));

        BiasnessHandler.handle(dataMatrix);
        adjustmentFactor=BiasnessHandler.handle(dataMatrix);
        System.out.println("adjustmentFactor = " + adjustmentFactor);

        System.out.println("After handling Biasness....");

        for(int i=0;i<dataMatrix.length;i++)
            System.out.println(String.format(" Actual = %s     Forecast = %s",dataMatrix[i][0],dataMatrix[i][1]));
    }
}
