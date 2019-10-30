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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

//Not needed
public class IO {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public static void writeItinFileAppendedMode(String record, String filePath) {
		    createDataDirectory(new File(filePath).getParent());
		    try {
		      BufferedWriter out = new BufferedWriter(new FileWriter(filePath, true));
		      out.write(record);
		      out.newLine();
		      out.close();
		    } catch (IOException e) {
		     System.err.println("Exception in writing in "+filePath);
		    }
		  }
	 
	 public static void createDataDirectory(String dirPath) {
		    File dir = new File(dirPath);
		    if (!dir.exists())
		      dir.mkdirs();
		  }
}
