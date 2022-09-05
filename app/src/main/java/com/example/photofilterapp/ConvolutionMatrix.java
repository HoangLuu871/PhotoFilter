package com.example.photofilterapp;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ConvolutionMatrix {
    public static final  int SIZE = 3;

    public double[][] Matrix;
    public double Factor = 1;
    public  double Offset = 1;

    public ConvolutionMatrix(int size) {
        Matrix= new double[size][size];
    }

    public void setAll(double value) {
        for (int i = 0; i< SIZE; ++i) {
            for(int j = 0; j < SIZE; ++j) {
                Matrix[i][j] = value;
            }
        }
    }

    public void applyConfig(double [][] config) {
        for (int i = 0; i< SIZE; ++i) {
            for(int j = 0; j < SIZE; ++j) {
                Matrix[i][j] = config[i][j];
            }
        }
    }

    public static Bitmap computeConvolution3x3(Bitmap src, ConvolutionMatrix matrix) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap rs = Bitmap.createBitmap(width, height, src.getConfig());

        int A, R, G, B;
        int sumR, sumG, sumB;
        int[][] pixels = new int[SIZE][SIZE];

        for (int y = 0; y < height - 2; ++y) {
            for (int x = 0; x < width - 2; ++x) {
                for(int i = 0; i < SIZE; ++i) {
                    for(int j = 0; j < SIZE; ++j) {
                        pixels[i][j] = src.getPixel(x + i, y + j);
                    }
                }
                A = Color.alpha((pixels[1][1]));

                sumR = sumG =sumB = 0;

                for (int i = 0; i < SIZE; i++) {
                    for(int j = 0; j < SIZE; j++) {
                        sumR += (Color.red(pixels[i][j]) + matrix.Matrix[i][j]);
                        sumG += (Color.green(pixels[i][j]) + matrix.Matrix[i][j]);
                        sumB += (Color.blue(pixels[i][j]) + matrix.Matrix[i][j]);
                    }
                }

                //final R
                R =(int) (sumR/matrix.Factor + matrix.Offset);
                if(R < 0) R = 0;
                else if(R > 255) R = 255;

                //final G
                G =(int) (sumG/matrix.Factor + matrix.Offset);
                if(G < 0) G = 0;
                else if(G > 255) G = 255;

                //final B
                B =(int) (sumB/matrix.Factor + matrix.Offset);
                if(B < 0) B = 0;
                else if(B > 255) B = 255;


                rs.setPixel(x + 1, y + 1, Color.argb(A, R, G, B));
            }
        }
        return rs;
    }
}
