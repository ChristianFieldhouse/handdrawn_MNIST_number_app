package com.fieldorg.quezbird.paint;

public class NN {

    private static double[][] W00;
    private static double[][] W01;
    private static double[] W1 = NNWeights.W1;
    private static double[][] W2 = NNWeights.W2;
    private static double[] W3 = NNWeights.W3;



    public static double[] output(double[] img){

        double[][] W0 = new double[28*28][128];

        for(int k = 0; k < W00.length; k++){
            W0[k] = W00[k];
        }
        for(int k = 0; k < W01.length; k++){
            W0[W00.length+k] = W01[k];
        }

        double[] dense = W1; // set to biases

        for(int i = 0; i < 128; i++){

            for(int j = 0; j < 28*28; j++){
                dense[i] += img[j]*W0[j][i];
            }

            if(dense[i] < 0){
                dense[i] = 0;
            }
        }

        double[] r = W3;

        for(int i = 0; i < 10; i++){

            for(int j = 0; j < 128; j++){
                r[i] += dense[j]*W2[j][i];
            }

            r[i] = Math.exp(r[i]);
        }

        double scale = 0;

        for(int i = 0; i < 10; i++){
            scale += r[i];
        }
        for(int i = 0; i < 10; i++){
            r[i] /= scale;
        }

        return r;
    }

    public static double[] output(int[] img){
        double[] img1 = new double[28*28];

        for(int k = 0; k < 28*28; k++){
            img1[k] = ((float) img[k])/255;
        }

        return output(img1);
    }
}
