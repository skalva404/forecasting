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
package examples;

import com.forecasting.models.Forecaster;
import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;

import java.io.*;

public class ForecastExample {
    public static void main(String[] args) {

        try {
            //Creating Time Series

            String inputFile  = "/tmp/ts.txt";
            String validFile  = "/tmp/validTS.txt";
            String outputFile = "/tmp/outTS.txt";
            // Model data parameters
            int trainPoints = 35;
            int seasonalPeriod = 7;
            int validationPoints = 7;
            int futurePoints = 3;

            /*Read the file and create time series*/
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            BufferedWriter wr = new BufferedWriter(new FileWriter(outputFile));
            BufferedWriter vr = new BufferedWriter(new FileWriter(validFile));

            String line;
            double value;

            while ((line = br.readLine()) != null) {
                DataSet timeSeries = new DataSet();
                DataPoint observation;

                String [] str = line.split(" ");
                for (int idx = 0; idx <= 41; ++idx) {
                      value =  Integer.parseInt(str[idx]);
                      observation = new Observation();
                      observation.setIndependentValue(IndependentVariable.SLICE, idx);
                      observation.setDependentValue(value);
                      timeSeries.add(observation);
                }
                for (int idx = 42; idx <= 44; ++idx) {
                       vr.write(str[idx]);
                       vr.write("\t");
                }
                vr.newLine();

            System.out.println("*********************************************");
            System.out.println("         Actual Time Series");
            System.out.println("*********************************************");

            for (DataPoint dp : timeSeries.getDataPoints())
                System.out.println(String.format("Key = %s  Request = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue()));



            // Forecaster - getting best forecast by running all models
            Forecaster forecaster = new Forecaster();
            forecaster.init(timeSeries);
            DataSet forecastTimeSeries = forecaster.forecast(trainPoints, validationPoints, futurePoints, seasonalPeriod);

            // Forecasted Series
            System.out.println("*********************************************");
            System.out.println("      Forecasted Time Series");
            System.out.println("*********************************************");

            for (DataPoint dp : forecastTimeSeries.getDataPoints()){
                System.out.println(String.format("Key = %s    forecast = %s   LB = %s  UB = %s", dp.getIndependentValue(IndependentVariable.SLICE), dp.getDependentValue(), dp.getLowerDependentValue(), dp.getUpperDependentValue()));
                wr.write(String.valueOf(dp.getDependentValue()));
                wr.write("\t");
            }
                wr.newLine();


            }
            br.close();
            wr.close();
            vr.close();
            }catch (Exception e) {
            e.printStackTrace();

        }



    }

}
