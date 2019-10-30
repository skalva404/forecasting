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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

//Not needed
public class DateUtil {

    public static Date addDays(Date date, int days) {
        Calendar cl = Calendar.getInstance();
        cl.setTime(date);
        cl.set(Calendar.DATE, cl.get(Calendar.DATE) + days);
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        return cl.getTime();
    }

    public static java.util.Date resetDate(java.util.Date date) {
        Calendar cl = Calendar.getInstance();
        cl.setTime(date);
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        return cl.getTime();
    }

    /**
     * @param date
     * @return
     */
    public static Date getRestDate(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.DATE, instance.get(Calendar.DATE));
        instance.set(Calendar.HOUR, 1);
        instance.set(Calendar.MINUTE, 1);
        instance.set(Calendar.SECOND, 1);
        instance.set(Calendar.MILLISECOND, 1);
        instance.set(Calendar.AM_PM, Calendar.AM);
        return instance.getTime();
    }

    public static Date getRestDateHour(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.DATE, instance.get(Calendar.DATE));
        instance.set(Calendar.HOUR_OF_DAY, instance.get(Calendar.HOUR_OF_DAY));
        instance.set(Calendar.MINUTE, 1);
        instance.set(Calendar.SECOND, 1);
        instance.set(Calendar.MILLISECOND, 1);
        return instance.getTime();
    }

    /**
     * Get Date object corresponding to start time of the given date
     *
     * @param date
     * @return start time for the given day
     */
    public static Date getSODDate(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.DATE, instance.get(Calendar.DATE));
        instance.set(Calendar.HOUR, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 1);
        instance.set(Calendar.AM_PM, Calendar.AM);
        return instance.getTime();
    }

    /**
     * Get Date object corresponding to end time of the given date
     *
     * @param date
     * @return end time for the given day
     */
    public static Date getEODDate(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.DATE, instance.get(Calendar.DATE));
        instance.set(Calendar.HOUR, 11);
        instance.set(Calendar.MINUTE, 59);
        instance.set(Calendar.SECOND, 59);
        instance.set(Calendar.MILLISECOND, 999);
        instance.set(Calendar.AM_PM, Calendar.PM);
        return instance.getTime();
    }

    /**
     * Get Date object corresponding to start time of the given date+hour
     *
     * @param date
     * @return start time for the given day+hour
     */
    public static Date getSOHDate(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.DATE, instance.get(Calendar.DATE));
        instance.set(Calendar.HOUR_OF_DAY, instance.get(Calendar.HOUR_OF_DAY));
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 1);
        return instance.getTime();
    }

    /**
     * Get Date object corresponding to end time of the given date+hour
     *
     * @param date
     * @return end time for the given day+hour
     */
    public static Date getEOHDate(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.DATE, instance.get(Calendar.DATE));
        instance.set(Calendar.HOUR_OF_DAY, instance.get(Calendar.HOUR_OF_DAY));
        instance.set(Calendar.MINUTE, 59);
        instance.set(Calendar.SECOND, 59);
        instance.set(Calendar.MILLISECOND, 999);
        return instance.getTime();
    }

    public static String formatDate(Date date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    public static Date parseDate(String stringDate, String format) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.parse(stringDate);
    }

    public static int daysBetween(Date sd, Date ed) {

        Calendar startDate = Calendar.getInstance();
        startDate.setTime(sd);

        Calendar endDate = Calendar.getInstance();
        endDate.setTime(ed);

        Calendar date = (Calendar) startDate.clone();
        int daysBetween = 0;
        while (date.before(endDate)) {
            date.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }
        return daysBetween;
    }
}
