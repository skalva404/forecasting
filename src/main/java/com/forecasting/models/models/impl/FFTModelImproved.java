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
package com.forecasting.models.models.impl;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Model;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.utils.ModelUtil;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.postprocess.BiasnessHandler;
import com.forecasting.models.preprocess.SeasonalityCalculatorUsingAutoCorrelation;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

import java.util.*;

/**
 * FFT  model improvised implementation
 */
public class FFTModelImproved extends AbstractForecastModel {

    //Actual and forecast series
    private double[] ts;
    private double[] forecast;

    //Model Attributes
    private Integer threshold;

    //post processing attributes
    private double mean;
    private int dof;

    /**
     * @param trainPoints
     * @param validationPoints
     * @param seasonalPeriod
     * @param threshold
     */
    public FFTModelImproved(int trainPoints, int validationPoints, final int seasonalPeriod, final Integer threshold) {
        this.trainPoints = trainPoints;
        this.validationPoints = validationPoints;
        this.seasonalPeriod = seasonalPeriod;
        this.threshold = threshold;
        this.dof = threshold;
        this.model = Model.FFT;

    }

    /**
     * @param observations
     */
    public void init(final DataSet observations) throws ModelInitializationException {
        super.init(observations);
        if (threshold == null)
            threshold = 5;
        if (trainPoints <= 0 || validationPoints <= 0 || seasonalPeriod <= 0)
            throw new ModelInitializationException("FFT:Invalid model arugments");
    }

    // Accuracy Indicators are computed on validation points only, could be extend to train points too later
    @Override
    public void train() {

        int startPoint;
        int endPoint;
        double[] timeSeries;
        double actualValue, forecastValue;
        double[][] valMatrix = new double[validationPoints][2];
        ts = observations.toArray();
        for (int i = 0; i < validationPoints; i++) {
            startPoint = 0;
            endPoint = trainPoints + i;
            timeSeries = new double[endPoint];
            for (int j = startPoint; j < endPoint; j++)
                timeSeries[j] = ts[j];

            actualValue = ts[endPoint];
            forecastValue = getForecast(timeSeries);
            valMatrix[i][0] = actualValue;
            valMatrix[i][1] = forecastValue;
        }

        double biasness = BiasnessHandler.handleOffset(valMatrix);
        accuracyIndicators.setBias(biasness);
        ModelUtil.computeAccuracyIndicators(accuracyIndicators, null, valMatrix, dof);
    }

    /**
     * @param futurePoints
     */
    public void forecast(int futurePoints) {

        double[] timeSeries = ts.clone();
        DataPoint observation;
        double forecastValue, lowerBound, upperBound, adjustedValue;
        forecastDataSet = new DataSet();

        double[] tsArray = transform(timeSeries);
        double[] outputArray = doFFTanalysis(tsArray);
        int cyclePeriod = new SeasonalityCalculatorUsingAutoCorrelation().findCyclePeriod(outputArray);
        reverseTransform(outputArray);

        if (cyclePeriod == 1)
            cyclePeriod = seasonalPeriod;

        for (int i = 0; i < futurePoints; i++) {

            forecastValue = outputArray[outputArray.length - cyclePeriod + i % cyclePeriod];

            adjustedValue = BiasnessHandler.addOffset(forecastValue, accuracyIndicators.getBias());


            if (adjustedValue > 0)
                forecastValue = adjustedValue;

            if (forecastValue < 0) {
                forecastValue = 0l;
                lowerBound = 0l;
                upperBound = 0l;
            } else {
                lowerBound = forecastValue - errorBound;
                upperBound = forecastValue + errorBound;
            }

            observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i);
            observation.setDependentValue(forecastValue);
            observation.setLowerDependentValue(lowerBound > 0 ? lowerBound : 0);
            observation.setUpperDependentValue(upperBound);
            forecastDataSet.add(observation);
        }
    }

    /**
     * @param timeSeries
     * @return
     */

    public double getForecast(double[] timeSeries) {

        double[] tsArray = transform(timeSeries);
        double[] outputArray = doFFTanalysis(tsArray);

        int cyclePeriod = new SeasonalityCalculatorUsingAutoCorrelation().findCyclePeriod(outputArray);

        if (cyclePeriod == 1)
            cyclePeriod = seasonalPeriod;

        reverseTransform(outputArray);
        return outputArray[outputArray.length - cyclePeriod];
    }

    public void sort(double array[][]) {
        int size = array.length;
        for (int i = 1; i < size; i++) {
            int j = i;
            double index = array[i][0];
            double B = array[i][1];
            while ((j > 0) && (array[j - 1][1] < B)) {
                array[j][0] = array[j - 1][0];
                array[j][1] = array[j - 1][1];
                j--;
            }
            array[j][0] = index;
            array[j][1] = B;
        }
    }

    /**
     * @param data
     * @return
     */
    private double[] transform(double[] data) {
        double[] meanCenteredTS = new double[data.length * 2];
        double sum = 0l;
        for (int i = 0; i < data.length; i++)
            sum += data[i];
        mean = sum / data.length;
        for (int i = 0; i < data.length; i++)
            meanCenteredTS[i] = data[i] - mean;

        return meanCenteredTS;
    }

    /**
     * @param data
     */
    private void reverseTransform(double[] data) {
        for (int i = 0; i < data.length; i++)
            data[i] = data[i] + mean;
    }

    /**
     * @param points
     * @return
     */
    public double[] doFFTanalysis(double[] points) {

        DoubleFFT_1D fft = new DoubleFFT_1D(points.length / 2);
        fft.realForwardFull(points);
        List<MagnitudeObj> magnitudeList = new ArrayList<MagnitudeObj>();
        MagnitudeObj magnitude;

        for (int i = 0; i < points.length / 2; i++) {
            if (i == 0) {
                magnitude = new MagnitudeObj();
                magnitude.setIndex(i);
                magnitude.setRealIndex(i);
                magnitude.setImgIndex(i);
                magnitude.setValue(points[i]);
            } else {
                magnitude = new MagnitudeObj();
                magnitude.setIndex(i);
                magnitude.setRealIndex(2 * i + 1);
                magnitude.setImgIndex(2 * i);
                magnitude.setValue(Math.sqrt(Math.pow(points[2 * i + 1], 2) + Math.pow(points[2 * i], 2)));
            }
            magnitudeList.add(magnitude);
        }
        insertionSort(magnitudeList);

        int index = -1;
        double diff;

        for (int i = 0; i < magnitudeList.size() - 1; i++) {

            double maxDiff = -1;
            diff = magnitudeList.get(i).getValue() - magnitudeList.get(i + 1).getValue();
            if (diff > maxDiff) {
                maxDiff = diff;
                index = i;
            }
        }

        if (index < 5)
            index = 5;

        magnitudeList = magnitudeList.subList(index + 1, magnitudeList.size());

        for (int i = 0; i < magnitudeList.size(); i++) {
            magnitude = magnitudeList.get(i);

            points[magnitude.getRealIndex()] = 0;
            points[magnitude.getImgIndex()] = 0;
        }
        fft.realInverse(points, true);

        double[] outputArray = new double[points.length / 2];
        for (int i = 0; i < points.length / 2; i++) {
            outputArray[i] = points[i];
        }
        return outputArray;
    }

    public void insertionSort(List<MagnitudeObj> objList) {
        for (int i = 0; i < objList.size(); i++) {
            int j = i;
            MagnitudeObj B = objList.get(i);
            while ((j > 0) && (objList.get(j - 1).getValue() < B.getValue())) {
                objList.set(j, objList.get(j - 1));
                j--;
            }
            objList.set(j, B);
        }
    }

    public class MagnitudeObj {
        int index;
        int realIndex;
        int imgIndex;
        double value;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getRealIndex() {
            return realIndex;
        }

        public void setRealIndex(int realIndex) {
            this.realIndex = realIndex;
        }

        public int getImgIndex() {
            return imgIndex;
        }

        public void setImgIndex(int imgIndex) {
            this.imgIndex = imgIndex;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }
    }
}
