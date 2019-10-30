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
package com.forecasting.models.preprocess;

import com.forecasting.models.dto.DataSet;
import com.forecasting.models.models.DataPoint;
import com.forecasting.models.preprocess.impl.DifferencingTransformation;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.List;

// Not complete - Spectral leakage issue - Still working on it
public class SeasonalityCalculator {


    /**TODO
     * Part of Preprocessing Step, but does not implement PreprocessModel interface
     * Still need to solve the problem of spectrum leakage - Not ready
     * @param observations
     * @return
     */
    public double[] computeSeasonality(DataSet observations) {

        DifferencingTransformation tranformation = new DifferencingTransformation();
        tranformation.init(observations);

//        DataSet transformedDataSet = tranformation.transform();

        DataSet transformedDataSet = observations;
        double[] ts = new double[transformedDataSet.getDataPoints().size()];
        int i = 0;

        for (DataPoint point : transformedDataSet.getDataPoints()) {
            ts[i++] = point.getDependentValue();
        }
        return null;
    }

    private int findSeasonality(double[] ts) {

        double max = -1d;
        Double seasonality = -1d;
        DoubleFFT_1D fft = new DoubleFFT_1D(ts.length);
        int index = -1;
        fft.realForward(ts);

        List<Complex> complexList = getComplexRepresentation(ts);

        for (int i = 1; i <complexList.size(); i++) {
            if (Math.round(complexList.get(i).abs()) > max) {
                index = i;
                max = complexList.get(i).abs();
            }
        }
        seasonality = new Double(ts.length)/index;
        return seasonality.intValue()==ts.length?-1:seasonality.intValue();
    }

    private List<Complex> getComplexRepresentation(double[] ts) {
        List<Complex> complexList = new ArrayList<Complex>();
        Complex complex = null;

        if (ts.length % 2 == 0) {
            for (int i = 0; i <= ts.length / 2; i++) {

                if (i == 0) {
                    complex = new Complex(ts[i], 0);
                    complexList.add(complex);
                } else if (i == ts.length / 2) {
                    complex = new Complex(ts[1], 0);
                    complexList.add(complex);
                } else {
                    complex = new Complex(ts[i * 2], ts[2 * i + 1]);
                    complexList.add(complex);
                }
            }

        } else {

            for (int i = 0; i < (ts.length + 1) / 2; i++) {
                if (i == 0) {
                    complex = new Complex(ts[i], 0);
                    complexList.add(complex);
                }
                else if (i == (ts.length - 1) / 2) {
                    complex = new Complex(ts[2 * i], ts[1]);
                    complexList.add(complex);
                } else {
                    complex = new Complex(ts[i * 2], ts[2 * i + 1]);
                    complexList.add(complex);
                }
            }
        }
        return complexList;
    }
}
