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
import lombok.Data;

import java.io.Serializable;
import java.util.*;

@Data
public class DataSet implements Serializable {
    private SortedSet<DataPoint> dataPoints = new TreeSet<DataPoint>();
    public boolean add(DataPoint obj) {
        if (obj == null)
            throw new NullPointerException("DataSet does not support addition of null DataPoints");
        return dataPoints.add(obj);
    }

    public boolean addAll(Collection<? extends DataPoint> c) {
        // iterate through all elements in Collection
        //  adding a copy of each DataPoint to this DataSet
        if (c == null)
            throw new NullPointerException("DataSet does not support addition of null DataPoints");

        Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            add((DataPoint) it.next());
        }
        return true;
    }

    public Iterator<DataPoint> iterator() {
        return dataPoints.iterator();
    }

    public int size() {
        return dataPoints.size();
    }

    /**
     * Checks if a datapoint exists
     * @param obj
     * @return
     */
    public boolean contains(Object obj) {
        if (obj == null) {
            throw new NullPointerException("DataSet does not support null DataPoint objects");
        }
        DataPoint dataPoint = (DataPoint) obj;
        Iterator<DataPoint> it = this.iterator();
        while (it.hasNext()) {
            DataPoint dp = it.next();
            if (dataPoint.equals(dp))
                return true;
        }
        return false;
    }

    public boolean equals(DataSet dataSet) {
        if (dataSet == null) {
            return false;
        }
        if (this.size() != dataSet.size()) {
            return false;
        }
        Iterator<DataPoint> it = dataSet.iterator();
        while (it.hasNext()) {
            DataPoint dataPoint = it.next();
            if (!this.contains(dataPoint))
                return false;
        }
        return true;
    }

    public boolean remove(DataPoint obj) {
        if (obj == null) {
            throw new NullPointerException("DataSet does not support null DataPoint objects");
        }
        return dataPoints.remove(obj);
    }

    public double[] toArray() {
        int i = 0;
        double[] array = new double[dataPoints.size()];
        for (DataPoint point : dataPoints)
            array[i++] = point.getDependentValue();
        return array;
    }
}
