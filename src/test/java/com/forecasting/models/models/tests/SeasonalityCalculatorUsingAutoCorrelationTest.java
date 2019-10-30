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
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.util.ConfigManager;
import com.forecasting.models.models.util.SampleDataFactory;
import com.forecasting.models.utils.DateUtil;
import com.forecasting.models.preprocess.SeasonalityCalculatorUsingAutoCorrelation;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.util.Date;
import java.util.Map;

public class SeasonalityCalculatorUsingAutoCorrelationTest extends TestCase {


    public SeasonalityCalculatorUsingAutoCorrelationTest(String testName) {
        super(testName);
        try {
//            ReflectionUtil.addURL(new URL("resources"));
            ConfigManager.loadConfigFile("test.properties", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Test suite() {
        return new TestSuite(SeasonalityCalculatorUsingAutoCorrelationTest.class);
    }

    public void testSeasonalityCalculator() throws Exception {

        Date date = DateUtil.addDays(new Date(), -3);
        SampleDataFactory dataFactory = new SampleDataFactory();
        Map<String, DataSet> goldenDataSetMap = dataFactory.fetchGoldenDataSet();
        DataSet dataSet = new DataSet();

        for (int i = 0; i < 42; i++) {
            Observation obs = new Observation();
            obs.setDependentValue(i % 7);
            obs.setIndependentValue(IndependentVariable.SLICE, i + 1);
            dataSet.add(obs);
        }

        dataSet = readDataFile();


//        for (String key : goldenDataSetMap.keySet()) {
//            DataSet dataSet = goldenDataSetMap.get(key);
        SeasonalityCalculatorUsingAutoCorrelation seasonalityCalculator = new SeasonalityCalculatorUsingAutoCorrelation();
        int seasonality = seasonalityCalculator.findCyclePeriod(dataSet.toArray());
        System.out.println(String.format(" Id =   TimeSeries = %s   Seasonality = %s", dataSet.size(), seasonality));
//        }
    }

    private DataSet readDataFile() throws IOException {

        String fileName = this.getClass().getResource("/ts.csv").getPath();
        FileInputStream in = new FileInputStream(fileName);

        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String strLine;

        DataSet dataSet = new DataSet();


        while ((strLine = br.readLine()) != null) {
            DataPoint dp = new Observation();
            dp.setIndependentValue(IndependentVariable.SLICE, dataSet.size());
            dp.setDependentValue(Double.parseDouble(strLine.trim()));
            dataSet.add(dp);
        }

        System.out.println("dataSet = " + dataSet.size());
        return dataSet;
    }
}
