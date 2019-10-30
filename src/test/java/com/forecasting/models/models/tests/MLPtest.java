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
import com.forecasting.models.experimental.MultiLayerPerceptronModel;
import com.forecasting.models.models.util.SampleDataFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MLPtest extends TestCase {

    public MLPtest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(MLPtest.class);
    }

    public void testMLP() {

        System.out.println("Entered test");
        int trainPoints = 200, validationPoints = 7;
        SampleDataFactory dataFactory = new SampleDataFactory();
        DataSet dataSet = dataFactory.getSeasonalDummyDataSet(trainPoints, 7);

        System.out.println("Started");

        MultiLayerPerceptronModel model = new MultiLayerPerceptronModel(2, 7, 1);

        double[] points = dataSet.toArray();
        double[] output = new double[2];

//        for (int i = 0; i < validationPoints; i++) {
//            double[] input = new double[trainPoints];
//            for (int j = i; j < i + trainPoints; j++) {
//                input[j - i] = points[j];
//                output[1] = points[j];
//            }
//            System.out.println("Training now");
//            model.train(input, output);
//        }
//
        double[] input = new double[3];
        for (int i = 0; i < trainPoints-1; i++) {
            input[1] = points[0];
            input[2] = i % 7;
            output[0] = points[i + 1];
            System.out.println("Training now");
            model.train(input, output);
        }

        input = new double[3];

        System.out.println("Forecasting now..............");
        output[1] = points[trainPoints - 1];
        for (int i = trainPoints; i < trainPoints + validationPoints + 7; i++) {
            input[1] = output[1];
            input[2] = i % 7;
            output = model.passNet(input);
            System.out.println("\n\n\noutput = " + output[1]);
        }
    }
}
