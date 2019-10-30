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
package com.forecasting.models.postprocess;

import com.forecasting.models.utils.ModelConstants;

public class ErrorBoundsHandler {

    /**
     * Computes the delta/error bound to get forecast confidence intervals
     * @param inputMatrix
     * @return
     */
    public static double computeErrorBoundInterval(double[][] inputMatrix) {

        double delta;
        double SE = 0d;

        for (int i = 0; i < inputMatrix.length; i++)
            SE += Math.pow((inputMatrix[i][0] - inputMatrix[i][1]), 2);
            delta= ModelConstants.DEFAULT_CONFIDENCE_INTERVAL_COEFFICIENT*Math.sqrt(SE/inputMatrix.length);
        return delta;
    }
}
