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
import com.forecasting.models.preprocess.impl.FFTlowPassFilter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Date;

public class FFTlowfilterTest extends TestCase {


    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public FFTlowfilterTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(FFTlowfilterTest.class);
    }

    /**
     * Rigourous Test :-)
     */


    public void testApp() {

        SampleDataFactory dataFactory = new SampleDataFactory();

        int points = 42;
        Long id = 7029631l;
        Date date = DateUtil.addDays(new Date(), -1);
        DataSet dataSet = dataFactory.getDataSet(points);
//        DataSet dataSet=dataFactory.getDummyDataSet(1200);


        if(dataSet.size()==points) {
        System.out.println("Original DataSet");
        for (DataPoint point : dataSet.getDataPoints()) {
            System.out.println(String.format(" key = %s     value  = %s ", point.getIndependentValue(IndependentVariable.SLICE), point.getDependentValue()));
        }

        int threshold = points/3;
        FFTlowPassFilter filter = new FFTlowPassFilter(threshold);

        filter.init(dataSet);
        DataSet transformedDataSet = filter.transform();

        System.out.println("Tranformed DataSet");
        for (DataPoint point : transformedDataSet.getDataPoints()) {
            System.out.println(String.format(" key = %s     value  = %s ", point.getIndependentValue(IndependentVariable.SLICE), point.getDependentValue()));
        }
        }

        assertTrue(true);
    }


//    public void testnewFFTlibrary() {
//
//        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
//
//        SampleDataFactory dataFactory = new SampleDataFactory();
//        double[] signal=dataFactory.getDummyArray(8);
//
//
//        for(double value:signal)
//            System.out.println("value = " + value);
//
//        Complex[] transformed = fft.transform(signal, TransformType.FORWARD);
//
//        System.out.println("FFT.....");
//
//        for(Complex comp:transformed)
//            System.out.println(String.format(" real = %s     img = %s ", comp.getReal(), comp.getImaginary()));
//
//        transformed = fft.transform(transformed, TransformType.INVERSE);
//
//        System.out.println("iFFT......");
//        for(Complex comp:transformed)
//            System.out.println(String.format(" real = %s     img = %s ", comp.getReal(), comp.getImaginary()));
//
//    }


}
