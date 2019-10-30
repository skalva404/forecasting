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
package com.forecasting.models.models.util;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.utils.DateUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class WeeklyDataGenerator extends TestCase {

    private static final String DELIM = ",";
    private static final int BATCH_SIZE = 200;

    public WeeklyDataGenerator(String testName) {
        super(testName);
        try {
            ReflectionUtil.addURL(new URL("resources"));
            ConfigManager.loadConfigFile("test.properties", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Test suite() {

        return new TestSuite(WeeklyDataGenerator.class);
    }

    public void populateGoldenDataSet() throws Exception {

        DecimalFormat decimalFormatter = new DecimalFormat("0.0000");
        Date endDate = DateUtil.addDays(new Date(), -2);
        Date startDate = DateUtil.addDays(endDate, -13*7);
        int timeSeriesSize = 65;
        int testDataSize = 13;
        Connection con=Connections.getStagingConnection();
        SampleDataFactory dataFactory = new SampleDataFactory(con);
        Map<String, DataSet> goldenDataSetMap = new TreeMap<String, DataSet>();
//        while (startDate.compareTo(endDate) <= 0) {
            System.out.println(" Data populated for " + startDate);
            dataFactory.fetchWeeklyPrimeDataSet(goldenDataSetMap, endDate, timeSeriesSize + testDataSize);
            startDate = DateUtil.addDays(startDate, 7);
//        }
        dataFactory.close();
        String timeSeriesfile = ConfigManager.get("input");
        String testDatafile = ConfigManager.get("testDataSet");

        BufferedWriter timeSeriesDataWriter = new BufferedWriter(new FileWriter(new File(timeSeriesfile)));
        BufferedWriter testDataWriter = new BufferedWriter(new FileWriter(new File(testDatafile)));
        StringBuilder timeSeriesLine;
        StringBuilder testDataLine;
        int counter = 0;
        for (String key : goldenDataSetMap.keySet()) {
            timeSeriesLine = new StringBuilder();
            testDataLine = new StringBuilder();
            timeSeriesLine.append(key);
            timeSeriesLine.append(DELIM);
            testDataLine.append(key);
            testDataLine.append(DELIM);

            double[] points = goldenDataSetMap.get(key).toArray();

//            System.out.println("key = " + key);
            boolean flag=true;

            if(points.length !=91)
                System.out.println("********************key*");

            for (int i = 0; i < points.length; i++) {

//                System.out.println("points = " + points[i]);

                if(points[i]<10000)
                    flag=false;

                if (i < timeSeriesSize) {
                    timeSeriesLine.append(decimalFormatter.format(points[i]));
                    timeSeriesLine.append(DELIM);
                } else {
                    testDataLine.append(decimalFormatter.format(points[i]));
                    testDataLine.append(DELIM);
                }
            }

            timeSeriesLine.append("\n");
            testDataLine.append("\n");

            if(flag)     {
            timeSeriesDataWriter.write(timeSeriesLine.toString());
            testDataWriter.write(testDataLine.toString());

            counter++;

            if (BATCH_SIZE == counter) {
                counter = 0;
                timeSeriesDataWriter.flush();
                testDataWriter.flush();
            }
            }
        }
        timeSeriesDataWriter.flush();
        testDataWriter.flush();
        timeSeriesDataWriter.close();
        testDataWriter.close();
    }

    public void testDataGenerator() throws Exception {
        populateGoldenDataSet();
    }
}
