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
package com.forecasting.models.models.impl2;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Model;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.exception.ModelInitializationException;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.models.impl.AbstractForecastModel;
import com.forecasting.models.postprocess.BiasnessHandler;
import com.forecasting.models.postprocess.ErrorBoundsHandler;
import com.forecasting.models.utils.ModelConstants;
import com.forecasting.models.utils.ModelUtil;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

import java.util.*;

/**
 * FFT  model implementation
 */
public class FFTModel_2 extends AbstractForecastModel {


    //Model Attributes
    private Integer threshold;

    //post processing attributes
    private double mean;
    private double offsetDecayFactor;
    private int dof;

    /**
     * @param trainPoints
     * @param validationPoints
     * @param seasonalPeriod
     * @param threshold
     */
    public FFTModel_2(int trainPoints, int validationPoints, final int seasonalPeriod, final Integer threshold) {
        this(trainPoints, validationPoints, seasonalPeriod, threshold, -1);
    }

    /**
     * @param trainPoints
     * @param validationPoints
     * @param seasonalPeriod
     * @param threshold
     * @param offsetDecayFactor
     */
    public FFTModel_2(int trainPoints, int validationPoints, final int seasonalPeriod, final Integer threshold, int offsetDecayFactor) {
        this.trainPoints = trainPoints;
        this.validationPoints = validationPoints;
        this.seasonalPeriod = seasonalPeriod;
        this.threshold = threshold;
        this.offsetDecayFactor = offsetDecayFactor;
        this.dof = threshold;
        this.model = Model.FFT2;
    }

    /**
     * @param observations
     */
    public void init(final DataSet observations) throws ModelInitializationException {
        super.init(observations);
        if (trainPoints <= 0 || validationPoints <= 0 || seasonalPeriod <= 0 || threshold <= 0 || offsetDecayFactor > 1)
            throw new ModelInitializationException("FFT:Invalid model arugments");
        if (offsetDecayFactor == -1)
            offsetDecayFactor = ModelConstants.DEFAULT_FFT_OFFSET_DECAY_FACTOR;
    }

    // Accuracy Indicators are computed on validation points only, could be extend to train points too later
    @Override
    public void train() {

        double[][] trainMatrix = new double[trainPoints][2];
        double[][] valMatrix = new double[validationPoints][2];

        double[] ts = observations.toArray();

        for (int j = 0; j < trainPoints; j++)

            ts = Arrays.copyOf(ts, ts.length + validationPoints);

        for (int j = trainPoints; j < trainPoints + validationPoints; j++)
            ts[j] = ts[j - validationPoints];

        double[] approxSignal = getApproximatedSignal(ts);

        for (int i = 0; i < trainPoints; i++) {
            trainMatrix[i][0] = ts[i];
            trainMatrix[i][1] = approxSignal[i];
        }

        for (int i = trainPoints; i < trainPoints + validationPoints; i++) {
            valMatrix[i - trainPoints][0] = ts[i];
            valMatrix[i - trainPoints][1] = approxSignal[i];
        }

        double biasness = BiasnessHandler.handle(valMatrix);
        accuracyIndicators.setBias(biasness);
        ModelUtil.computeAccuracyIndicators(accuracyIndicators, trainMatrix, valMatrix, dof);
    }

    private double[] getApproximatedSignal(double[] timeSeries) {

        double[] tsArray = transform(timeSeries);
        double[] outputArray = doFFTanalysis(tsArray);
        reverseTransform(outputArray);
        return outputArray;
    }

    /**
     * @param futurePoints
     */
    public void forecast(int futurePoints) {

        DataPoint observation;
        double forecastValue, lowerbound, upperbound;
        forecastDataSet = new DataSet();
        double[] ts = observations.toArray();
        double[][] trainMatrix = new double[trainPoints+validationPoints][2];

        ts = Arrays.copyOf(ts, ts.length + futurePoints);

        for (int i = observations.size(); i < observations.size() + futurePoints; i++)
            ts[i] = ts[i - futurePoints];

        double[] approximatedSignal = getApproximatedSignal(ts);

        for (int i = 0; i < trainPoints + validationPoints; i++) {
            trainMatrix[i][0] = ts[i];
            trainMatrix[i][1] = approximatedSignal[i];
        }

        errorBound = ErrorBoundsHandler.computeErrorBoundInterval(trainMatrix);

        for (int i = 0; i < futurePoints; i++) {

            forecastValue = approximatedSignal[approximatedSignal.length - futurePoints + i];
            forecastValue = BiasnessHandler.adjustBiasness(forecastValue, accuracyIndicators.getBias());
            lowerbound = forecastValue - errorBound;
            upperbound = forecastValue + errorBound;
            observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i);
            observation.setDependentValue(forecastValue);
            observation.setLowerDependentValue(lowerbound > 0 ? lowerbound : 0);
            observation.setUpperDependentValue(upperbound);
            forecastDataSet.add(observation);
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
        double maxDiff = -1;
        double diff;

        for (int i = 0; i < magnitudeList.size() - 1; i++) {
            diff = magnitudeList.get(i).getValue() - magnitudeList.get(i + 1).getValue();
            if (diff > maxDiff) {
                maxDiff = diff;
                index = i;
            }
        }

        if (threshold == null) {
            if (index < 5)
                index = 5;
        } else index = threshold;
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
