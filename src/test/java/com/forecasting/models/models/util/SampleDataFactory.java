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
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SampleDataFactory {

    Logger logger = Logger.getLogger(SampleDataFactory.class.getName());

    Connection connection;

    public SampleDataFactory() {
        try {
            ReflectionUtil.addURL(new URL("resources"));
            ConfigManager.loadConfigFile("test.properties", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SampleDataFactory(Connection con) {
        connection = con;
    }

    public void close() {
        try {
            if (null != connection) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public DataSet getDataSet(int points) {

        DataSet dataSet = new DataSet();
        try {

            String fileName = this.getClass().getResource(ConfigManager.get("input.daily")).getPath();
            FileInputStream in = new FileInputStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String str = br.readLine();
            String[] token = str.split(",");

            for (int i = 1; i < token.length; i++) {
                DataPoint dp = new Observation();
                dp.setIndependentValue(IndependentVariable.SLICE, dataSet.size());
                dp.setDependentValue(Double.parseDouble(token[i]));
                dataSet.add(dp);
            }

            logger.info("Size of Data Set  : " + dataSet.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred while generating data set");
            e.printStackTrace();
        } finally {

            if (dataSet.size() == 0)
                dataSet = getDummyDataSet(points);
            return dataSet;
        }
    }

    public DataSet getDataSetFromDB(Long id, Date date, int points) {

        String query = "select total_requests from " + ConfigManager.get("table.forecast_base_inventory_history", "forecast_base_inventory_history") + " where segment_id in (?) and date(day)>date('%s')-? and date(day)<=date('%s') order by day";
        logger.info(query);
        DataSet dataSet = null;
        Observation observation;


        System.out.println("Table = " + ConfigManager.get("table.forecast_base_inventory_history", "forecast_base_inventory_history"));

        try {

            query=String.format(query,"2013-07-10","2013-07-10");
            System.out.println("date.getTime() = " + new java.sql.Date(date.getTime()));
            System.out.println("points = " + points);
            PreparedStatement ps = connection.prepareStatement(query);
            System.out.println("id = " + id);
            ps.setLong(1, id);
//            ps.setDate(2, new java.sql.Date(date.getTime()));
            ps.setInt(2, points);
//            ps.setDate(4, new java.sql.Date(date.getTime()));
            ResultSet rs = ps.executeQuery();

            dataSet = new DataSet();
            int i = 0;

            while (rs.next()) {
                observation = new Observation();
                System.out.println("rs.getLong() = " + rs.getLong("total_requests"));
                observation.setDependentValue(rs.getLong("total_requests"));
                observation.setIndependentValue(IndependentVariable.SLICE, i++);
                dataSet.add(observation);
            }
            logger.info("Size of Data Set  : " + dataSet.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred while generating data set");

            e.printStackTrace();
        } finally {
            return dataSet;
        }
    }

    public DataSet getWeeklyDataSetFromDB(Long id, Date date, int points) {


        String query = "select total_requests from " + ConfigManager.get("table.forecast_base_inventory_history_weekly", "forecast_base_inventory_history_weekly") + " where segment_id in (?) and date(week_start)>date(?)-? and date(week_start)<=date(?) order by week_start";
        logger.info(query);
        DataSet dataSet = null;
        Observation observation;

        try {

            PreparedStatement ps = connection.prepareStatement(query);
            ps.setLong(1, id);
            ps.setDate(2, new java.sql.Date(date.getTime()));
            ps.setInt(3, points * 7);
            ps.setDate(4, new java.sql.Date(date.getTime()));
            ResultSet rs = ps.executeQuery();

            dataSet = new DataSet();
            int i = 0;

            while (rs.next()) {
                observation = new Observation();
                observation.setDependentValue(rs.getLong("total_requests"));
                observation.setIndependentValue(IndependentVariable.SLICE, i++);
                dataSet.add(observation);
            }
//            System.out.println("id = " + id);
            if (dataSet.size() != points)
                logger.info("Size of Data Set  : " + dataSet.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred while generating data set");

            e.printStackTrace();
        } finally {
            return dataSet;
        }
    }

    public DataSet getDummyDataSet(int points) {

        DataSet dataSet = new DataSet();
        Observation observation;

        while (dataSet.size() != points) {
            observation = new Observation();
            observation.setDependentValue(dataSet.size() * 5 + 1);
            observation.setIndependentValue(IndependentVariable.SLICE, dataSet.size());
            dataSet.add(observation);
        }
        return dataSet;
    }



    public double[] getDummyArray(int points) {
        double[] array = new double[points];
        for (int i = 0; i < array.length; i++)
            array[i] = i * 5 + 1;
        return array;
    }

    public DataSet getSeasonalDummyDataSet(int points, int seasonality) {

        DataSet dataSet = new DataSet();
        Observation observation;

        while (dataSet.size() != points) {
            observation = new Observation();

            if (dataSet.size() % seasonality == 0)
                observation.setDependentValue(100);
            else observation.setDependentValue(5);

            observation.setIndependentValue(IndependentVariable.SLICE, dataSet.size());
            dataSet.add(observation);
        }
        return dataSet;
    }

    public DataSet buildDataSet(int value, int size) {

        DataSet dataSet = new DataSet();
        Observation observation;

        for (int i = 0; i < size; i++) {
            observation = new Observation();
            observation.setDependentValue(value);
            observation.setIndependentValue(IndependentVariable.SLICE, dataSet.size());
            dataSet.add(observation);
        }
        return dataSet;
    }

    public double[][] getBiasedDataMatrix(double value, int biasness, int size) {

        double[][] dataMatrix = new double[size][2];

        for (int i = 0; i < dataMatrix.length; i++) {
            dataMatrix[i][0] = value;
            dataMatrix[i][1] = value * (1 - 0.01 * biasness);
        }
        return dataMatrix;
    }

    public void fetchPrimeDataSet(Map<String, DataSet> goldenDataSetMap, Date date, int points) throws Exception {

        try {

//            List<Long> segmentIds = getPrimeSegmentIds();

            List<Long> segmentIds = getSegmentsFromFile();

            System.out.println("segmentIds = " + segmentIds.size());
            for (Long segId : segmentIds) {
                DataSet dataSet = getDataSetFromDB(segId, date, points);
                goldenDataSetMap.put("TS-" + (goldenDataSetMap.size() + 1), dataSet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Long> getSegmentsFromFile() {
        List<Long> segs = new LinkedList<Long>();

        try {
            String fileName = this.getClass().getResource(ConfigManager.get("segFile.higherror")).getPath();
            FileInputStream in = new FileInputStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                segs.add(Long.parseLong(strLine.trim()));
            }

            System.out.println("sseg = " + segs.get(0));
            br.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return segs;
        }
    }

    public void fetchWeeklyPrimeDataSet(Map<String, DataSet> goldenDataSetMap, Date date, int points) {

        try {
            List<Long> segmentIds = getWeeklyPrimeSegmentIds();
            System.out.println("segmentIds = " + segmentIds.size());
            for (Long segId : segmentIds) {
                DataSet dataSet = getWeeklyDataSetFromDB(segId, date, points);
                goldenDataSetMap.put("TS-" + (goldenDataSetMap.size() + 1), dataSet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Map<String, DataSet> fetchGoldenDataSet() {
        Map<String, DataSet> goldenDataSetMap = new HashMap<String, DataSet>();
        try {
            String strLine;
            String fileName = this.getClass().getResource(ConfigManager.get("input.daily")).getPath();
            FileInputStream in = new FileInputStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            DataSet dataSet;
            DataPoint dp;

            while ((strLine = br.readLine()) != null) {
                String[] token = strLine.split(",");
                dataSet = new DataSet();
                goldenDataSetMap.put(token[0], dataSet);

                for (int i = 1; i < token.length; i++) {
                    dp = new Observation();
                    dp.setIndependentValue(IndependentVariable.SLICE, dataSet.size());
                    dp.setDependentValue(Double.parseDouble(token[i]));
                    dataSet.add(dp);
                }
            }
            return goldenDataSetMap;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return goldenDataSetMap;
        }
    }

    private List<Long> getPrimeSegmentIds() throws SQLException {
        List<Long> res = new ArrayList<Long>();
        String query = "select id from " + ConfigManager.get("table.forecast_base_segment", "forecast_base_segment") + " where above_threshold = true and lower(country) in (" + ConfigManager.get("prime.countries") + ") and lower(os) in (" + ConfigManager.get("prime.os") + ") and lower(slotsize) in (" + ConfigManager.get("prime.slot") + ") and lower(device) in (" + ConfigManager.get("prime.device") + ") and uid_type in (" + ConfigManager.get("prime.uid") + ")";
        try {
            PreparedStatement sql = connection.prepareStatement(query);
            ResultSet rs = sql.executeQuery();

            logger.info("query = " + query);
            while (rs.next())
                res.add(rs.getLong("id"));

        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Exception occured in getPrimeSegmentIds", e);

        } finally {
            return res;
        }
    }

    private List<Long> getWeeklyPrimeSegmentIds() throws SQLException {
        List<Long> res = new ArrayList<Long>();
//        String query = "select id from " + ConfigManager.get("table.forecast_base_segment_weekly", "forecast_base_segment_weekly") + " where (above_threshold = true and lower(country) in (" + ConfigManager.get("prime.countries") + ") and lower(os) in (" + ConfigManager.get("prime.os") + ")  and lower(device) in (" + ConfigManager.get("prime.device") + ")) or ( above_threshold = true and id in ( select segment_id from " + ConfigManager.get("table.forecast_base_inventory_history_weekly", "forecast_base_inventory_history_weekly_1") + " group by segment_id having avg(total_requests)>=10000 order by avg(total_requests) desc)) limit 600";

        String query = "select id from " + ConfigManager.get("table.forecast_base_segment_weekly", "forecast_base_segment_weekly") + " where above_threshold = true and lower(country) in (" + ConfigManager.get("prime.countries") + ") and lower(os) in (" + ConfigManager.get("prime.os") + ")  and lower(device) in (" + ConfigManager.get("prime.device") + ")";

        try {
            PreparedStatement sql = connection.prepareStatement(query);
            ResultSet rs = sql.executeQuery();

            logger.info("query = " + query);
            while (rs.next())
                res.add(rs.getLong("id"));

//            System.out.println("res.size() = " + res.size());
//            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Exception occured in getPrimeSegmentIds", e);

        } finally {
            return res;
        }
    }

    public DataSet getOutlierData(int n, int k) {

        DataSet dataSet = new DataSet();
        Observation observation;

        while (dataSet.size() != n) {
            observation = new Observation();

            if (dataSet.size() == k)
                observation.setDependentValue(dataSet.size() * 500 + 1);
            else observation.setDependentValue(dataSet.size() * 5 + 1);

            observation.setIndependentValue(IndependentVariable.SLICE, dataSet.size());
            dataSet.add(observation);
        }
        return dataSet;


    }


    public DataSet getDataSetFromFile() throws Exception
    {
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
