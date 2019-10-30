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
package com.forecasting.models.models;

import com.forecasting.models.PeriodType;
import com.forecasting.models.exception.ObjectNotCreatedException;
import com.forecasting.models.utils.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/*
TS class represent time series.
*/

// Not needed
public class TS {
    private Date startDate;
    private Date endDate;
    private PeriodType periodType;
    private ArrayList<Long> timeseries; //timeseries contains


    public TS(Date startDate, Date endDate, ArrayList<Long> timeseries) throws ObjectNotCreatedException {
        this.startDate = startDate;
        this.endDate = endDate;

        try {
            Calendar start = Calendar.getInstance();
            start.setTime(startDate);
            Calendar end = Calendar.getInstance();
            end.setTime(endDate);

            if (DateUtils.daysBetween(start, end) != timeseries.size()) {
                throw new ObjectNotCreatedException("Not able to create TS object");
            }
            this.timeseries = timeseries;
        } catch (Exception e) {
            throw new ObjectNotCreatedException(
                    "Not able to create TS object ", e);
        }

    }

    public TS(Date startDate, Date endDate, HashMap<java.util.Date, Long> pastData) throws ObjectNotCreatedException {
        super();

        this.startDate = startDate;
        this.endDate = endDate;
        this.periodType = PeriodType.Days;
        try {
            Calendar start = Calendar.getInstance();
            start.setTime(startDate);
            Calendar end = Calendar.getInstance();
            end.setTime(endDate);
            timeseries = new ArrayList<Long>();

            for (; !start.after(end); start.add(Calendar.DATE, 1)) {
                Date current = start.getTime();
                if (pastData.containsKey(current)) {
                    timeseries.add(pastData.get(current));
                } else {
                    timeseries.add(0L);
                }
            }

        } catch (Exception e) {
            throw new ObjectNotCreatedException(
                    "Not able to create TS object ", e);
        }
    }

    public TS(HashMap<String, Long> pastData, String startDate2, String endDate2) throws ObjectNotCreatedException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            this.startDate = dateFormat.parse(startDate2);
            this.endDate = dateFormat.parse(endDate2);

            Calendar start = Calendar.getInstance();
            start.setTime(startDate);
            Calendar end = Calendar.getInstance();
            end.setTime(endDate);
            this.timeseries = new ArrayList<Long>();
            String key;

            for (; !start.after(end); start.add(Calendar.DATE, 1)) {
                Date current = start.getTime();
                key = dateFormat.format(current).toString();
                // Long key =  dateFormat.format(current).toString();
                if (pastData.containsKey(key)) {
                    timeseries.add(pastData.get(key));
                } else {
                    timeseries.add(0L);
                }
            }
        } catch (ParseException e) {
            throw new ObjectNotCreatedException(
                    "Not able to create TS object ", e);
        }
    }


    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public ArrayList<Long> getTimeseries() {
        return timeseries;
    }

    public void setTimeseries(ArrayList<Long> timeseries) {
        this.timeseries = timeseries;
    }

    public int size() {
        return this.timeseries.size();
    }

    public TS subTS(int startIdx, int endIdx) {

        if (startIdx < 0 || startIdx > timeseries.size() || startIdx > endIdx || endIdx > timeseries.size() || endIdx < 0) {
            System.err.println("Invalid Index : " + startIdx + ":" + endIdx + ":" + timeseries.size());
            return null;
        }
        ArrayList<Long> subTS = new ArrayList<Long>();
        Calendar start = Calendar.getInstance();
        for (int i = startIdx; i <= endIdx; i++) {
            subTS.add(timeseries.get(i));
        }

        start.setTime(startDate);
        start.add(Calendar.DATE, startIdx);
        Date startDate1 = start.getTime();

        start.add(Calendar.DATE, endIdx - startIdx);
        Date endDate1 = start.getTime();

        try {
            return new TS(startDate1, endDate1, subTS);
        } catch (ObjectNotCreatedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;

        }
    }


}
