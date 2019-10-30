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
package com.forecasting.models.preprocess.impl;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.dto.IndependentVariable;
import com.forecasting.models.dto.Model;
import com.forecasting.models.dto.Observation;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.utils.ModelConstants;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math3.complex.Complex;

import java.util.HashMap;
import java.util.Map;

public class FFTlowPassFilter extends AbstractPreprocessModel {

    private Model model= Model.FFTLowPass;

    private int threshold;
    private double mean;

    public FFTlowPassFilter(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void init(DataSet observations) {
        super.init(observations);
    }

    /**
     * FFT low pass filter transformation
     * @return returns filtered DataSet if energy constraint holds true, returns the original dataset if it violates
     */
    @Override
    public DataSet transform() {

        DataSet tranformedDataset;
        double[] ts = new double[observations.getDataPoints().size()];
        int i = 0;
        for (DataPoint point : observations.getDataPoints()) {
            ts[i++] = point.getDependentValue();
        }
        double[] filteredTS = FFTfilter(ts);

        if (filteredTS == null)
            return observations;

        tranformedDataset = new DataSet();
        Observation observation = null;
        for (i = 0; i < filteredTS.length; i++) {
            observation = new Observation();
            observation.setIndependentValue(IndependentVariable.SLICE, i);
            observation.setDependentValue(filteredTS[i]);
            tranformedDataset.add(observation);
        }
        return tranformedDataset;
    }

    /**
     * Computes FFT of the signal and filter the high frequencies from it
     * @param ts
     * @return  returns filtered signal if the filtered signal has satisfies the energy constraint, Null if it violates the constraint
     */
    private double[] FFTfilter(double[] ts) {
        double[] meanCentricTS = getMeanCentricSeries(ts);
        DoubleFFT_1D fft = new DoubleFFT_1D(meanCentricTS.length);
        fft.realForward(meanCentricTS);
        double energyRatio = filterAndComputeEnergyRatio(meanCentricTS);

        if (energyRatio < ModelConstants.FFT_LOW_PASS_ENERGY_CONSTRAINT)
            return null;

        fft.realInverse(meanCentricTS, true);
        double[] outputTS = addMean(meanCentricTS);
        return outputTS;
    }


    /**
     * Filter the high frequencies with the provided threshold, and computes energy ratio of filtered signal with original
     * @param fft
     * @return
     */
    private double filterAndComputeEnergyRatio(double[] fft) {

        double energyBeforeFilter = computeEnergyForSignal(fft);

        int totalFreq = fft.length % 2 == 0 ? fft.length / 2 : (fft.length + 1) / 2;
        int removeFreq = totalFreq - threshold;
        for (int i = 0; i < removeFreq; i++) {
            if (i == 0)
                fft[0] = 0;
            else
                fft[2 * i] = fft[2 * i + 1] = 0;
        }
        double energyAfterFilter = computeEnergyForSignal(fft);
        double energyRatio = energyBeforeFilter / energyAfterFilter;
        return energyRatio;
    }


//
//        a[2*k] = Re[k], 0<=k<n/2
//        a[2*k+1] = Im[k], 0<k<n/2
//        a[1] = Re[n/2]
//
//        if n is odd then
//        a[2*k] = Re[k], 0<=k<(n+1)/2
//        a[2*k+1] = Im[k], 0<k<(n-1)/2
//        a[1] = Im[(n-1)/2]

    /**
     * Show complex representation of FFT output, only for debugging
     * @param fft
     */
    private void showComplexRepresentation(double[] fft) {
        Map<Integer, Complex> fftComplexMap = new HashMap<Integer, Complex>();
        Complex complex = null;

        if (fft.length % 2 == 0) {
            for (int i = 0; i <= fft.length / 2; i++) {

                if (i == 0) {
                    complex = new Complex(fft[i], 0);
                    fftComplexMap.put(i, complex);
                } else if (i == fft.length / 2) {
                    complex = new Complex(fft[1], 0);
                    fftComplexMap.put(fft.length / 2, complex);
                } else {
                    complex = new Complex(fft[i * 2], fft[2 * i + 1]);
                    fftComplexMap.put(i, complex);
                }
            }
        } else {

            for (int i = 0; i < (fft.length + 1) / 2; i++) {

                if (i == 0) {
                    complex = new Complex(fft[i], 0);
                    fftComplexMap.put(i, complex);
                }
                if (i == (fft.length - 1) / 2) {
                    complex = new Complex(fft[2 * i], fft[1]);
                    fftComplexMap.put(i, complex);
                } else {
                    complex = new Complex(fft[i * 2], fft[2 * i + 1]);
                    fftComplexMap.put(i, complex);
                }
            }
        }
    }

    /**
     * Computes energy of the signal
      * @param fft
     * @return
     */
    private double computeEnergyForSignal(double[] fft) {

        double energy = 0d;

        if (fft.length % 2 == 0) {
            for (int i = 0; i <= fft.length / 2; i++) {

                if (i == 0)
                    energy += Math.abs(fft[i]);
                else if (i == fft.length / 2)
                    energy += Math.abs(fft[1]);
                else
                    energy += Math.sqrt(Math.pow(fft[2 * i], 2) + Math.pow(fft[2 * i + 1], 2));
            }
        } else {

            for (int i = 0; i < (fft.length + 1) / 2; i++) {

                if (i == 0)
                    energy += Math.abs(fft[i]);
                if (i == (fft.length - 1) / 2)
                    energy += Math.sqrt(Math.pow(fft[2 * i], 2) + Math.pow(fft[1], 2));
                else
                    energy += Math.sqrt(Math.pow(fft[2 * i], 2) + Math.pow(fft[2 * i + 1], 2));
            }
        }

        return energy;
    }


    private double[] addMean(double[] meanCentricTS) {
        double[] ts = new double[meanCentricTS.length / 2];
        for (int i = 0; i < meanCentricTS.length / 2; i++)
            ts[i] = meanCentricTS[i] + mean;
        return ts;
    }

    /**
     * Remove the mean from the time series data
     *
     * @param data
     * @return
     */
    private double[] getMeanCentricSeries(double[] data) {
        double[] meanCenteredTS = new double[data.length];
        double sum = 0l;
        for (int i = 0; i < data.length; i++)
            sum += data[i];
        mean = sum / data.length;
        for (int i = 0; i < data.length; i++)
            meanCenteredTS[i] = data[i] - mean;
        return meanCenteredTS;
    }

}