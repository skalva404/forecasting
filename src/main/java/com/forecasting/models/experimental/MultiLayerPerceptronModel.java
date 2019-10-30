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
package com.forecasting.models.experimental;

public class MultiLayerPerceptronModel {

    private int nInputs, nHidden, nOutput;  // Number of neurons in each layer
    private double[] input, hidden, output;
    private double[][] weightL1,weigthL2;
    private double learningRate = 0.8;


    public MultiLayerPerceptronModel(int nInput, int nHidden, int nOutput) {

        this.nInputs = nInput;
        this.nHidden = nHidden;
        this.nOutput = nOutput;

        input = new double[nInput+1];
        hidden = new double[nHidden+1];
        output = new double[nOutput+1];

        weightL1 = new double[nHidden+1][nInput+1];
        weigthL2 = new double[nOutput+1][nHidden+1];

        // Initialize weigths
        generateRandomWeights();
    }

    private void generateRandomWeights() {

        for(int j=1; j<=nHidden; j++)
            for(int i=0; i<=nInputs; i++) {
                weightL1[j][i] = Math.random() - 0.5;
            }

        for(int j=1; j<=nOutput; j++)
            for(int i=0; i<=nHidden; i++) {
                weigthL2[j][i] = Math.random() - 0.5;
            }
    }


    public double[] train(double[] pattern, double[] desiredOutput) {
        double[] output = passNet(pattern);
        System.out.println("*****************output = " + output[1]);
        System.out.println("*********************desiredOutput = " + desiredOutput[0]);
        backpropagation(desiredOutput);
//        System.out.println("L1 Weights - ");
////        for(int j=1; j<=nOutput; j++)
////            for(int i=0; i<=nHidden; i++)
////                System.out.println(String.format(" i = %s j = %s  weight = %s",i,j,weigthL2[j][i]));
////
////        System.out.println("L2 Weights - ");
////        for(int j=1; j<=nHidden; j++)
////            for(int i=0; i<=nInputs; i++)
////                System.out.println(String.format(" i = %s j = %s  weight = %s",i,j,weightL1[j][i]));
        return output;
    }

    public double[] passNet(double[] pattern) {

        for(int i=0; i<nInputs; i++) {
            input[i+1] = pattern[i];
        }

        // Set bias
        input[0] = 1.0;
        hidden[0] = 1.0;

        // Passing through hidden layer
        for(int j=1; j<=nHidden; j++) {
            hidden[j] = 0.0;
            for(int i=0; i<=nInputs; i++) {
                hidden[j] += weightL1[j][i] * input[i];
            }
            hidden[j] = 1.0/(1.0+Math.exp(-hidden[j]));
        }

        // Passing through output layer
        for(int j=1; j<=nOutput; j++) {
            output[j] = 0.0;
            for(int i=0; i<=nHidden; i++) {
                output[j] += weigthL2[j][i] * hidden[i];
            }
//            output[j] = 1.0/(1+0+Math.exp(-output[j]));
        }

        return output;
    }

    private void backpropagation(double[] desiredOutput) {

        double[] errorL2 = new double[nOutput+1];
        double[] errorL1 = new double[nHidden+1];
        double Esum = 0.0;

        for(int i=1; i<=nOutput; i++)  // Layer 2 error gradient
//            errorL2[i] = output[i] * (1.0-output[i]) * (desiredOutput[i-1]-output[i]);

        errorL2[i] = (desiredOutput[i-1]-output[i]);

        for(int i=0; i<=nHidden; i++) {  // Layer 1 error gradient
            for(int j=1; j<=nOutput; j++)
                Esum += weigthL2[j][i] * errorL2[j];

            errorL1[i] = hidden[i] * (1.0-hidden[i]) * Esum;
            Esum = 0.0;
        }

        for(int j=1; j<=nOutput; j++)
            for(int i=0; i<=nHidden; i++)
                weigthL2[j][i] += learningRate * errorL2[j] * hidden[i];

        for(int j=1; j<=nHidden; j++)
            for(int i=0; i<=nInputs; i++)
                weightL1[j][i] += learningRate * errorL1[j] * input[i];
    }
}

