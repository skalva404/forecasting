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

/**
 * A simple Java Bean that gathers together in one class all current "accuracy
 * indicators" for a given forecasting model. Currently, the supported
 * measurements of accuracy of a forecasting model supported by this class
 * include:
 * <ul>
 * <li>AIC - or the Akaike Information Criteria value</li>
 * <li>bias - or the mean error</li>
 * <li>MAD - or the Mean Absolute Deviation</li>
 * <li>MAPE - or the Mean Absolute Percentage Error</li>
 * <li>MSE - or the Mean Square of the Error</li>
 * <li>SAE - or the Sum of the Absolute Errors</li>
 * </ul>
 *
 * @author Steven R. Gould
 * @since 0.3
 */
public class AccuracyIndicators {

    /**
     * Kullback–Leibler Information Criteria measure.
     */
    private double klic;

    /**
     * Akaike Information Criteria measure.
     */
    private double bic;

    /**
     * Bayesian Information Criteria measure.
     */
    private double aic;

    /**
     * Arithmetic mean of the errors.
     */
    private double bias;

    /**
     * Mean Absolute Deviation.
     */
    private double mad;

    /**
     * Mean Absolute Percentage Error.
     */
    private double mape;

    /**
     * Mean Square of the Error.
     */
    private double mse;

    /**
     * Sum of the Absolute Errors.
     */
    private double sae;

    /**
     * array of errors on validaiton points
     */
    private double[] validationErrors;

    /**
     * directionalError
     */
    private double directionalError;

    /**
     * directionalError Matrix

     */
    private double[] directionalErrorMatrix;

    /**
     * Default constructor. Initializes all accuracy indicators to their
     * "worst" possible values - generally this means some large number,
     * indicating "very" inaccurate.
     */
    public AccuracyIndicators() {
        aic = bias = mad = mape = mse = sae = Double.MAX_VALUE;
    }

    /**
     * Returns the AIC for the associated forecasting model.
     *
     * @return the AIC.
     */
    public double getAIC() {
        return aic;
    }

    /**
     * Sets the AIC for the associated forecasting model to the given value.
     *
     * @param aic the new value for the AIC.
     */
    public void setAIC(double aic) {
        this.aic = aic;
    }

    /**
     * Returns the bias for the associated forecasting model.
     *
     * @return the bias.
     */
    public double getBias() {
        return bias;
    }

    /**
     * Sets the bias for the associated forecasting model to the given value.
     *
     * @param bias the new value for the bias.
     */
    public void setBias(double bias) {
        this.bias = bias;
    }

    public double[] getDirectionalErrorMatrix() {
        return directionalErrorMatrix;
    }

    public void setDirectionalErrorMatrix(double[] directionalErrorMatrix) {
        this.directionalErrorMatrix = directionalErrorMatrix;
    }

    /**
     * Returns the Mean Absolute Deviation (MAD) for the associated
     * forecasting model to the given value.
     *
     * @return the Mean Absolute Deviation.
     */
    public double getMAD() {
        return mad;
    }

    /**
     * Sets the Mean Absolute Deviation (MAD) for the associated forecasting
     * model. That is, the <code>SUM( abs(actual-forecast) ) / n</code> for
     * the initial data set.
     *
     * @param mad the new value for the Mean Absolute Deviation.
     */
    public void setMAD(double mad) {
        this.mad = mad;
    }

    /**
     * Returns the Mean Absolute Percentage Error (MAPE) for the associated
     * forecasting model. That is, the
     * <code>SUM( 100% . abs(actual-forecast)/actual ) / n</code> for the
     * initial data set.
     *
     * @return the Mean Absolute Percentage Error.
     */
    public double getMAPE() {
        return mape;
    }

    /**
     * Sets the Mean Absolute Percentage Error (MAPE) for the associated
     * forecasting model to the given value.
     *
     * @param mape the new value for the Mean Absolute Percentage Error.
     */
    public void setMAPE(double mape) {
        this.mape = mape;
    }

    /**
     * Returns the Mean Square of the Errors (MSE) for the associated
     * forecasting model. That is, the
     * <code>SUM( (actual-forecast)^2 ) / n</code> for the initial data set.
     *
     * @return the Mean Square of the Errors.
     */
    public double getMSE() {
        return mse;
    }

    /**
     * Sets the Mean Square of the Errors (MSE) for the associated
     * forecasting model to the new value.
     *
     * @param mse the new value for the Mean Square of the Errors.
     */
    public void setMSE(double mse) {
        this.mse = mse;
    }

    /**
     * Returns the Sum of the Absolute Errors (SAE) for the associated
     * forecasting model. That is, the
     * <code>SUM( abs(actual-forecast) )</code> for the initial data set.
     *
     * @return the Mean Absolute Deviation.
     */
    public double getSAE() {
        return sae;
    }

    /**
     * Sets the Sum of the Absolute Errors (SAE) for the associated
     * forecasting model to the new value.
     *
     * @return the new value for the Mean Absolute Deviation.
     */
    public void setSAE(double sae) {
        this.sae = sae;
    }

    /**
     * Returns a string containing the accuracy indicators and their values.
     * Overridden to provide some useful output for debugging.
     *
     * @return a string containing the accuracy indicators and their values.
     */
    public String toString() {
        return "  MAPE = " + mape
                + " , MSE = " + mse
                + " , AIC = " + aic
                + " , BIC = " + bic
                + " , Directional Error = " + directionalError
                + " , Biasness = " + bias;
    }

    public double getKlic() {
        return klic;
    }

    public void setKlic(double klic) {
        this.klic = klic;
    }

    public double[] getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(double[] validationErrors) {
        this.validationErrors = validationErrors;
    }

    public double getDirectionalError() {
        return directionalError;
    }

    public void setDirectionalError(double directionalError) {
        this.directionalError = directionalError;
    }

    public double getBic() {
        return bic;
    }

    public void setBic(double bic) {
        this.bic = bic;
    }
}