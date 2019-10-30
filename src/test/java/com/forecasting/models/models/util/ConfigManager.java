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
package com.forecasting.models.models.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ConfigManager {

    private static final Properties _properties;
    private static String envronment;

    private static Logger logger = null;

    static {
        _properties = new Properties();
    }

    public static void loadLogger() {
        if (null == logger) {
            logger = Logger.getLogger(ConfigManager.class.getName());
        }
    }

    public static void loadConfigFile(String fileName, boolean envSpecific) {
        try {
            fileName = (envSpecific ? envronment : "") + fileName;
            InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            _properties.load(resourceAsStream);
            loadLogger();
//            logger.debug("Loading file < " + fileName + " >");
            logger.debug("loading resource " + Thread.currentThread().getContextClassLoader().getResource(fileName).getPath());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void loadProperties(Properties properties) {
        try {
            Enumeration<?> enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                Object o = enumeration.nextElement();
                _properties.put(o, properties.get(o));
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void set(String key, String value) {
        _properties.setProperty(key, value);
    }

    public static String get(String key, String defaultVal) {
        String val = _properties.getProperty(key);
        return val == null ? defaultVal : val;
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static Integer getInt(String key) {
        return Integer.valueOf(get(key));
    }

    public static Long getLong(String key) {
        return Long.valueOf(get(key));
    }

    public static Boolean getBoolean(String key) {
        return Boolean.valueOf(get(key));
    }

    public static Map<String, String> getAllLoadedProperties() {
        Set<Map.Entry<Object, Object>> entries = _properties.entrySet();
        Map<String, String> propMap = new TreeMap<String, String>();
        for (Map.Entry<Object, Object> entry : entries) {
            propMap.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return propMap;
    }
}
