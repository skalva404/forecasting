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
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.regression.SimpleRegression;

// Incomplete -  Works well for coefficient=0, for other, values, feature scaling problem exists - Found a solution, need to implement it
public class BoxCoxTransformation extends AbstractPreprocessModel {

    private Model model = Model.BOXCOX;
    private Double coefficient;
    private boolean findCoefficient;

    public BoxCoxTransformation() {
        findCoefficient = true;
    }

    public BoxCoxTransformation(double coefficient) {
        this.coefficient = coefficient;
        findCoefficient = false;
    }

    public void init(DataSet observations) {
        super.init(observations);
    }

    /**
     * Computes the Box-Cox Tranformation of the time series
     * Link - http://www-stat.stanford.edu/~olshen/manuscripts/selenite/node6.html
     * @return
     */
    @Override
    public DataSet transform() {
        double[] points = observations.toArray();
        double value;

        if (findCoefficient)
            coefficient = computeBestCoefficient(points);

        if (coefficient == Double.MIN_VALUE)
            return observations;

        for (DataPoint point : observations.getDataPoints()) {
            value = point.getDependentValue();

            if (coefficient == 0)
                point.setDependentValue(Math.log(value));
            else
                point.setDependentValue((Math.pow(value, coefficient) - 1) / coefficient);
        }
        return observations;
    }

    /**
     * Computes best Coefficient based on linear regression - Not in use
     * @param points
     * @return
     */
    private double computeBestCoefficient(double[] points) {

        double coeff, error, value;
        double[] transformedPoints = new double[points.length];

        sort(points);

        double[] probabilities = computeOrderStatistics(points);

        double min_error = regressionAnalysis(points, probabilities);
        coefficient = Double.MIN_VALUE;
        for (coeff = -2; coeff <= 2; coeff += 0.2) {

            for (int i = 0; i < points.length; i++) {
                value = points[i];

                if (coeff == 0)
                    transformedPoints[i] = Math.log(value);
                else
                    transformedPoints[i] = (Math.pow(points[i], coeff) - 1) / coeff;
            }

            error = regressionAnalysis(transformedPoints, probabilities);

            if (min_error > error) {
                coefficient = coeff;
                min_error = error;
            }
        }
        return coefficient;
    }

    private double[] computeOrderStatistics(double[] points) {

        double[] probabilities = new double[points.length];
        double quantile;
        NormalDistribution gaussian = new NormalDistribution();

        for (int i = 0; i < points.length; i++) {
            quantile = ((i + 1) - 0.5) / points.length;
            probabilities[i] = gaussian.inverseCumulativeProbability(quantile);
        }

        return probabilities;
    }

    private void sort(double[] points) {

        for (int i = 0; i < points.length; i++) {
            int j = i;
            double temp = points[i];

            while ((j > 0) && (points[j - 1] < temp)) {
                points[j] = points[j - 1];
                j--;
            }
            points[j] = temp;
        }
    }

    /**
     * Does linear regression and returns SSE
     * @param points
     * @param probabilities
     * @return
     */
    private double regressionAnalysis(double[] points, double[] probabilities) {

        SimpleRegression linearRegression = new SimpleRegression();

        for (int i = 0; i < points.length; i++)
            linearRegression.addData(points[i], probabilities[i]);

        return linearRegression.getSumSquaredErrors();
    }

    /**
     * Reverse Transforms if coeffficient = 0
     * @param input
     * @return
     */
    @Override
    public DataSet reverseTransform(DataSet input) {

        DataSet output=new DataSet();
        DataPoint outputDP;
        if (coefficient == 0)
            for (DataPoint dp : input.getDataPoints()) {

                outputDP=new Observation();
                outputDP.setIndependentValue(IndependentVariable.SLICE,dp.getIndependentValue(IndependentVariable.SLICE));
                outputDP.setDependentValue(Math.pow(Math.E, dp.getDependentValue()));
                outputDP.setLowerDependentValue(Math.pow(Math.E, dp.getLowerDependentValue()));
                outputDP.setUpperDependentValue(Math.pow(Math.E, dp.getUpperDependentValue()));
                output.add(outputDP);
            }
        return output;
    }
}
