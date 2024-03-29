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

public interface ModelConstants {
    float DEFAULT_FFT_OFFSET_DECAY_FACTOR=0.95f;        // Single exponential decay on seasonality on FFT model
    float DEFAULT_BOX_COX_COEFFICIENT = 0f;                // For box cox tranformation, controls the degree of skewness
    float DEFAULT_CONFIDENCE_INTERVAL_COEFFICIENT=1.645f;   //For Error bounds
    float FFT_LOW_PASS_ENERGY_CONSTRAINT=0.9f;             // For FFT low pass filter - preprocesssing
}
