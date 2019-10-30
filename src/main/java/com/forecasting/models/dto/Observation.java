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
package com.forecasting.models.dto;

import com.forecasting.models.models.DataPoint;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Set;

// Implementation of DataPoint, Limitation - The implementation of DataPoint should be in model.jar only, which does not give flexibility to user
public class Observation implements DataPoint, Comparable, Serializable {

    private double dependentValue;
    private double lowerDependentValue;     // Stores the lower bound of the forecasted value, not significant for input time series
    private double upperDependentValue;     // Stores the upper bound of the forecasted value, not significant for input time series
    private Hashtable<IndependentVariable, Integer> independentValues;

    /**
     * Constructor
     */
    public Observation() {
        independentValues = new Hashtable<IndependentVariable, Integer>();
    }

    public void setDependentValue(double value) {
        this.dependentValue = value;
    }

    public double getDependentValue() {
        return dependentValue;
    }

    public void setIndependentValue(IndependentVariable name, Integer value) {
        independentValues.put(name, value);
    }

    public int getIndependentValue(IndependentVariable name) {
        return independentValues.get(name);
    }


    public double getLowerDependentValue() {
        return lowerDependentValue;
    }

    public void setLowerDependentValue(double lowerDependentValue) {
        this.lowerDependentValue = lowerDependentValue;
    }

    public double getUpperDependentValue() {
        return upperDependentValue;
    }

    public void setUpperDependentValue(double upperDependentValue) {
        this.upperDependentValue = upperDependentValue;
    }


    @Override
    public boolean equals(DataPoint dp) {
        if (dependentValue != dp.getDependentValue())
            return false;

        // Get a list of independent variable names
        Set<IndependentVariable> vars = getIndependentVariableNames();

        // Check that the given DataPoint has the same number of variables
        Set<IndependentVariable> dpVars = getIndependentVariableNames();
        if (vars.size() != dpVars.size())
            return false;

        // Check the values of each variable match
        for (IndependentVariable next : vars) {
            double thisValue = ((Number) independentValues.get(next)).doubleValue();
            double dpValue = dp.getIndependentValue(next);
            if (thisValue != dpValue)
                return false;
        }

        // All variable values match, so the given DataPoint represents the
        // same data point as this Observation
        return true;
    }

    public Set<IndependentVariable> getIndependentVariableNames() {
        return independentValues.keySet();
    }

    @Override
    public int compareTo(Object o) {
        Observation that = (Observation) o;
        double thisTime = this.getIndependentValue(IndependentVariable.SLICE);
        double thatTime = that.getIndependentValue(IndependentVariable.SLICE);
        if (thisTime > thatTime) {
            return 1;
        } else if (thisTime < thatTime) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("DataPoint(");
        builder.append("Independent = ").append(getIndependentValue(IndependentVariable.SLICE));
        builder.append("ABS_Dependent = " + getUpperDependentValue());
        builder.append("LB_dependent = ").append(getLowerDependentValue());
        builder.append("UB_dependent = " + getUpperDependentValue());
        return builder.toString();
    }

}
