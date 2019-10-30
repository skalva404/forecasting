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
package com.forecasting.models.models.benchmark;

import com.forecasting.models.Forecaster;
import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Model;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.util.ConfigManager;
import com.forecasting.models.models.util.ReflectionUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;

public class BenchmarkTest extends TestCase {


    private static Logger logger = Logger.getLogger(BenchmarkTest.class.getName());
    private static final String DELIM1 = ",";
    private static final String DELIM2 = "|";
    private static final int BATCH_SIZE = 200;


    public BenchmarkTest(String testName) {
        super(testName);
        try {
            ReflectionUtil.addURL(new URL("resources"));
            ConfigManager.loadConfigFile("test.properties", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Test suite() {
        return new TestSuite(BenchmarkTest.class);
    }

    public void testDummy() {
        logger.info("Dummy test ");
    }


    public void testGoldenDataSet() {
        try {

            String strLine;
            String fileName = this.getClass().getResource(ConfigManager.get("input.daily")).getPath();
            FileInputStream in = new FileInputStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            Map<String, DataSet> timeSeriesMap = new HashMap<String, DataSet>();
            Map<String, DataSet> forecastTimeSeriesMap = new HashMap<String, DataSet>();

            DataSet dataSet;
            DataPoint dp;

            while ((strLine = br.readLine()) != null) {
                String[] token = strLine.split(",");
                dataSet = new DataSet();

                timeSeriesMap.put(token[0], dataSet);

                for (int i = 1; i < token.length; i++) {
                    dp = new Observation();
                    dp.setIndependentValue(IndependentVariable.SLICE, dataSet.size());
                    dp.setDependentValue(Double.parseDouble(token[i]));
                    dataSet.add(dp);
                }
            }


            logger.info(timeSeriesMap.size() + " Time Series Loaded");
            logger.info("Running Forecast");


            // Forecast parameters
            int trainPoints = 35;
            int seasonalPeriod = 7;
            int validationPoints = 7;
            int futurePoints = 7;

            List<Model> models = new LinkedList<Model>();
            models.add(Model.TESM);
            models.add(Model.ENSEMBLE);

            Forecaster forecaster = new Forecaster();
            for (String key : timeSeriesMap.keySet()) {
                dataSet = timeSeriesMap.get(key);
                forecaster.init(dataSet);
                DataSet forecastTimeSeries = forecaster.forecast(trainPoints, validationPoints, futurePoints, seasonalPeriod);
                forecastTimeSeriesMap.put(key, forecastTimeSeries);
            }

            logger.info("Forecast Complete, Writing to File now");
            writeToFile(forecastTimeSeriesMap);
            writeMetricsToFile(forecastTimeSeriesMap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeMetricsToFile(Map<String, DataSet> dataSetMap) {

        try {
            Map<String, double[]> actualMap = loadActualData();
            logger.info(String.format("Test Data Loaded for %s Time Series", actualMap.size()));
            double error;
            int segments = actualMap.size();
            int testPoints = Integer.parseInt(ConfigManager.get("points.test", "7"));
            int[] _15pc = new int[testPoints];
            int[] _20pc = new int[testPoints];
            int[] _25pc = new int[testPoints];
            int[] withinBounds = new int[testPoints];
            double[] avg_error = new double[testPoints];
            double[] wt_avg_error = new double[testPoints];
            double[] wt_sum = new double[testPoints];
            double[] error_sum = new double[testPoints];
            double[] sum = new double[testPoints];
            double boundSum = 0;


            double[] min_error = new double[testPoints];

            for (int i = 0; i < min_error.length; i++)
                min_error[i] = Double.MAX_VALUE;

            double[] max_error = new double[testPoints];


            for (int i = 0; i < max_error.length; i++)
                max_error[i] = Double.MIN_VALUE;

            for (String key : dataSetMap.keySet()) {
                if (null == actualMap.get(key))
                    logger.info("No Test Data Found for " + key);
                else {

                    Set<DataPoint> dataPoints = dataSetMap.get(key).getDataPoints();
                    double[] actual = actualMap.get(key);
                    int i = 0;
                    for (DataPoint dp : dataPoints) {

                        error = Math.abs(actual[i] - dp.getDependentValue()) * 100 / actual[i];

                        error_sum[i] += error;
                        wt_sum[i] += error * actual[i];
                        sum[i] += actual[i];

                        if (min_error[i] > error)
                            min_error[i] = error;
                        if (max_error[i] < error) {
                            System.out.println(String.format("Key = %s    I = %s Error  = %s", key, i, error));
                            max_error[i] = error;
                        }
                        if (error > 15)
                            _15pc[i]++;
                        if (error > 20)
                            _20pc[i]++;
                        if (error > 25)
                            _25pc[i]++;
                        if (actual[i] >= dp.getLowerDependentValue() && actual[i] <= dp.getUpperDependentValue()) {

                            boundSum += Math.log(Math.abs(dp.getDependentValue() - dp.getLowerDependentValue()));

                            withinBounds[i]++;
                        }
                        i++;
                    }
                }
            }

            for (int i = 0; i < testPoints; i++) {
                withinBounds[i] = withinBounds[i] * 100 / segments;
                _15pc[i] = _15pc[i] * 100 / segments;
                _20pc[i] = _20pc[i] * 100 / segments;
                _25pc[i] = _25pc[i] * 100 / segments;
                avg_error[i] = error_sum[i] / segments;
                wt_avg_error[i] = wt_sum[i] / sum[i];
            }

            StringBuilder sb = new StringBuilder();
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(2);


            for (int i = 0; i < testPoints; i++) {

                sb.append("           Forecast Metrics     ");
                sb.append("\n");
                sb.append("-----------------------------------------------");
                sb.append("\n");
                sb.append(" Time Series Count            = " + segments);
                sb.append("\n");
                sb.append(" Avg Error                    = " + nf.format(avg_error[i]));
                sb.append("%\n");
                sb.append(" Weighted Avg Error           = " + nf.format(wt_avg_error[i]));
                sb.append("%\n");
                sb.append(" Minimum Error                = " + nf.format(min_error[i]));
                sb.append("%\n");
                sb.append(" Maximum Error                = " + nf.format(max_error[i]));
                sb.append("%\n");
                sb.append(" With in Confidence Interval  = " + nf.format(withinBounds[i]));
                sb.append("%\n");
                sb.append(" Series with > 15% Error      = " + nf.format(_15pc[i]));
                sb.append("%\n");
                sb.append(" Series with > 20% Error      = " + nf.format(_20pc[i]));
                sb.append("%\n");
                sb.append(" Series with > 25% Error      = " + nf.format(_25pc[i]) + "%");
                sb.append("%\n");
                sb.append(" Bound Sum      = " + nf.format(boundSum) + "%");
                sb.append("\n\n\n\n");
            }

            String fileName = this.getClass().getResource(ConfigManager.get("metrics.daily")).getPath();

            System.out.println("fileName = " + fileName);
            File file=new File(fileName);
            if(!file.exists())
                file.createNewFile();

            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));
            fileWriter.write(sb.toString());
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, double[]> loadActualData() {
        try {


            String strLine;

            int testDataSize = Integer.parseInt(ConfigManager.get("points.test", "7"));
            String fileName = this.getClass().getResource(ConfigManager.get("testDataSet.daily")).getPath();

            FileInputStream in = new FileInputStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            Map<String, double[]> testData = new HashMap<String, double[]>();
            double[] testPoints;

            while ((strLine = br.readLine()) != null) {
                String[] token = strLine.split(",");
                testPoints = new double[testDataSize];

                for (int i = 1; i < token.length; i++)
                    testPoints[i - 1] = Double.parseDouble(token[i]);
                testData.put(token[0], testPoints);
            }

            return testData;
        } catch (Exception e) {
            logger.info("Exception occured while loading test data");
            e.printStackTrace();
            return null;
        }
    }

    private void writeToFile(Map<String, DataSet> dataSetMap) {

        try {
            String fileName = this.getClass().getResource(ConfigManager.get("output.daily")).getPath();
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(fileName)));
            Set<DataPoint> dataPoints;
            String forecastLine;
            int count = 0;

            for (String key : dataSetMap.keySet()) {
                count++;
                dataPoints = dataSetMap.get(key).getDataPoints();
                forecastLine = toCSVLine(key, dataPoints);
                fileWriter.write(forecastLine);
                if (count == BATCH_SIZE) {
                    fileWriter.flush();
                    count = 0;
                }
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            logger.info("Error while writing Output file");
            e.printStackTrace();
        }
    }

    private String toCSVLine(String key, Set<DataPoint> dataPoints) {

        StringBuilder sb = new StringBuilder();
        sb.append(key);
        sb.append(DELIM1);

        for (DataPoint dp : dataPoints) {
            sb.append("[");
            sb.append(dp.getLowerDependentValue());
            sb.append(DELIM2);
            sb.append(dp.getDependentValue());
            sb.append(DELIM2);
            sb.append(dp.getUpperDependentValue());
            sb.append("]");
            sb.append(DELIM1);
        }

        sb.append("\n");
        return sb.toString();
    }
}
