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
import com.forecasting.models.dto.Model;
import com.forecasting.models.preprocess.PreprocessModel;

public abstract class AbstractPreprocessModel implements PreprocessModel{

    protected DataSet observations;
    protected Model model;

    @Override
    public void init(DataSet data) {
        observations=data;
    }

    @Override
    public DataSet transform() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DataSet reverseTransform(DataSet input) {
        return input;
    }
}
