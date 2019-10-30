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
package com.forecasting.models.utils;

import java.util.logging.Logger;

public class ModelUtil {


    private static Logger logger = Logger.getLogger(ModelUtil.class.getName());

    /**
     * Compute Accuracy indicators , AIC and BIC are given constan degrees of freedom as 1, but every model
     * has its own degrees of freedom, TES has 3, DES has 2, SES has 1, FFT has 15, MOV has 7. Need to take
     * the input from each model for degrees of freedom and calculate AIC and BIC, but right now AIC and BIC are
     * not used in either curve fitting OR ensemble. Therefore, Good for now.
     *
     * @param trainMatrix
     * @param valMatrix
     * @param dof
     * @return
     */
    public static void computeAccuracyIndicators(AccuracyIndicators accuracyIndicators,double[][] trainMatrix, double[][] valMatrix, int dof) {
        if (valMatrix != null) {
            accuracyIndicators.setMAPE(calculateMAPE(valMatrix));
            accuracyIndicators.setValidationErrors(calculateValidationErrors(valMatrix));
            accuracyIndicators.setDirectionalError(calculateDirectionalError(valMatrix));
            accuracyIndicators.setDirectionalErrorMatrix(calculateDirectionalErrorMatrix(valMatrix));
        }

        if (trainMatrix != null) {
            accuracyIndicators.setKlic(calculateKLIC(trainMatrix));
            accuracyIndicators.setAIC(calculateAIC(trainMatrix, dof));
            accuracyIndicators.setBic(calculateBIC(trainMatrix, dof));
            accuracyIndicators.setMSE(computeMSE(trainMatrix));
        }
//        logger.info("accuracyIndicators.toString() = " + accuracyIndicators.toString());
    }

    private static double[] calculateValidationErrors(double[][] matrix) {
        double[] validationErrors = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++)
            validationErrors[i] = Math.abs(matrix[i][0] - matrix[i][1]) * 100 / matrix[i][0];

        return validationErrors;
    }

    /**
     * Computes KLIC
     *
     * @param matrix
     * @return
     */
    public static double calculateKLIC(double[][] matrix) {
        double klic = 0d;
        for (int i = 0; i < matrix.length; i++)
            klic += Math.log(matrix[i][0] / matrix[i][1]) * matrix[i][0];
        return klic;
    }

    /**
     * Computes Mean absolute percent error
     *
     * @param matrix
     * @return
     */
    private static double calculateMAPE(double[][] matrix) {
        double errorSum = 0;
        for (int i = 0; i < matrix.length; i++)
            errorSum += Math.abs(matrix[i][0] - matrix[i][1]) * 100 / matrix[i][0];

        return errorSum / matrix.length;
    }

    /**
     * Compute Mean squared error
     *
     * @param matrix
     * @return
     */
    public static double computeMSE(double[][] matrix) {
        double errorSum = 0d;
        for (int i = 0; i < matrix.length; i++)
            errorSum += Math.pow(matrix[i][0] - matrix[i][1], 2);
        return errorSum / matrix.length;
    }

    /**
     * Computes the directional error in the form of ratio of incorrect direction to all directions
     */
    public static double calculateDirectionalError(double[][] matrix) {
        double error;
        double incorrectDirection = 0;
        int totalDirections = matrix.length - 1;

        if(matrix.length==1)
            return -1;

        for (int i = 1; i < matrix.length; i++) {
            if ((matrix[i][0] - matrix[i - 1][0]) * (matrix[i][1] - matrix[i - 1][1]) < 0)
                incorrectDirection++;
        }
        error = incorrectDirection * 100 / totalDirections;
        return error;
    }

    /**
     * Computes the directional error matrix in the form of ratio of incorrect direction to all directions
     */
    public static double[] calculateDirectionalErrorMatrix(double[][] matrix) {

        double[] direction = new double[matrix.length];
        for (int i = 1; i < matrix.length; i++) {
            if ((matrix[i][0] - matrix[i - 1][0]) * (matrix[i][1] - matrix[i - 1][1]) >= 0)
                direction[i]=1;
        }
        return direction;
    }

    /**
     * Computes AIC with given expected and forecasted values and degrees of freedom
     *
     * @param matrix
     * @param k
     * @return
     */
    public static double calculateAIC(double[][] matrix, double k) {
        double MSE = computeMSE(matrix);
        return calculateAIC(MSE, k, matrix.length);
    }

    /**
     * Computest AIC with given MSE, degrees of freedom and number of observations
     * Ref - http://en.wikipedia.org/wiki/Akaike_information_criterion
     *
     * @param MSE
     * @param k
     * @param n
     * @return
     */
    public static double calculateAIC(double MSE, double k, double n) {
        double aic = 2 * k + n * Math.log(MSE);
        return aic;
    }

    /**
     * Computes BIC with given expected and forecasted values and degrees of freedom
     *
     * @param matrix
     * @param k
     * @return
     */
    public static double calculateBIC(double[][] matrix, double k) {
        double MSE = computeMSE(matrix);
        return calculateBIC(MSE, k, matrix.length);
    }


    /**
     * Computest AIC with given MSE, degrees of freedom and number of observations
     * Ref - http://en.wikipedia.org/wiki/Bayesian_information_criterion
     *
     * @param MSE
     * @param k
     * @param n
     * @return
     */
    public static double calculateBIC(double MSE, double k, double n) {
        double bic = 2 * Math.log(MSE) + k * Math.log(n);
        return bic;
    }

    /**
     * if return 0, no bias, return negative=underforecasted and return positive=overforecasted
     *
     * @param input
     * @return
     */
    public static double calculateBiasness(double[][] input) {
        double diff, min_diff = 10000000000000d;
        int underForecasted = 0, overForecasted = 0;
        for (int i = 0; i < input.length; i++) {

            if (input[i][0] > input[i][1])
                underForecasted++;
            else overForecasted++;

            diff = Math.abs(input[i][0] - input[i][1]) / input[i][0];

            if (min_diff > diff)
                min_diff = diff;
        }
        if (underForecasted == input.length)
            return (-1) * min_diff;
        else if (overForecasted == input.length)
            return min_diff;
        else return 0;
    }
}
