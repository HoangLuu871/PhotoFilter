package com.example.photofilterapp;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.Random;

public class PictureEffects {
    public Bitmap applyHighlightEffect(Bitmap src) {
        Bitmap rs = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(rs);
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        Paint ptBlur = new Paint();
        ptBlur.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));
        int[] offsetXY = new int[2];

        Bitmap bmAlpha = src.extractAlpha(ptBlur, offsetXY);

        Paint ptAlphaColor = new Paint();
        ptAlphaColor.setColor(0xFFFFFFFF);

        canvas.drawBitmap(bmAlpha, offsetXY[0], offsetXY[1], ptAlphaColor);

        bmAlpha.recycle();

        canvas.drawBitmap(src, 0, 0, null);

        return rs;
    }

    public Bitmap applyInvertEffect(Bitmap src) {
        Bitmap rs = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());

        int A, R, G, B;
        int pixelColor;

        int height = src.getHeight();
        int width = src.getWidth();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelColor = src.getPixel(x, y);
                A = Color.alpha(pixelColor);
                R = 255 - Color.red(pixelColor);
                G = 255 - Color.green(pixelColor);
                B = 255 - Color.blue(pixelColor);

                rs.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return rs;
    }

    public Bitmap applyGreyscaleEffect(Bitmap src) {
        final double GS_R = 0.299;
        final double GS_G = 0.587;
        final double GS_B = 0.114;

        Bitmap rs = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());

        int A, R, G, B;
        int pixel;

        int height = src.getHeight();
        int width = src.getWidth();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                R = G = B = (int) (GS_R * R + GS_G * G + GS_B * B);
                rs.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return rs;
    }

    public Bitmap applyGammaEffect(Bitmap src, double red, double green, double blue) {
        Bitmap rs = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());

        int A, R, G, B;
        int pixel;

        int height = src.getHeight();
        int width = src.getWidth();

        final int MAX_SIZE = 256;
        final double MAX_VALUE_DBL = 255.0;
        final int MAX_VALUE_INT = 255;
        final double REVERSE = 1.0;

        int[] gammaR = new int[MAX_SIZE];
        int[] gammaG = new int[MAX_SIZE];
        int[] gammaB = new int[MAX_SIZE];

        for (int i = 0; i < MAX_SIZE; ++i) {
            gammaR[i] = Math.min(MAX_VALUE_INT, (int) ((MAX_VALUE_DBL * Math.pow(i / MAX_VALUE_DBL, REVERSE / red)) + 0.5));
            gammaG[i] = Math.min(MAX_VALUE_INT, (int) ((MAX_VALUE_DBL * Math.pow(i / MAX_VALUE_DBL, REVERSE / green)) + 0.5));
            gammaB[i] = Math.min(MAX_VALUE_INT, (int) ((MAX_VALUE_DBL * Math.pow(i / MAX_VALUE_DBL, REVERSE / blue)) + 0.5));
        }

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = gammaR[Color.red(pixel)];
                G = gammaG[Color.green(pixel)];
                B = gammaB[Color.blue(pixel)];

                rs.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return rs;
    }

    public Bitmap applyContrastEffect(Bitmap src, double value) {
        int width = src.getWidth();
        int height = src.getHeight();

        Bitmap rs = Bitmap.createBitmap(width, height, src.getConfig());
        int A, R, G, B;
        int pixel;
        double contrast = Math.pow((100 + value) / 100, 2);

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                R = (int) (((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if (R < 0) {
                    R = 0;
                } else if (R > 255) {
                    R = 255;
                }

                G = Color.red(pixel);
                G = (int) (((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if (G < 0) {
                    G = 0;
                } else if (G > 255) {
                    G = 255;
                }

                B = Color.red(pixel);
                B = (int) (((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if (B < 0) {
                    B = 0;
                } else if (B > 255) {
                    B = 255;
                }

                rs.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        return rs;
    }


    public Bitmap applyEmbossEffect(Bitmap src) {
        double[][] EmbossConfig = new double[][]{
                {-1, 0, -1},
                {0, 4, 0},
                {-1, 0, -1}
        };
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        convMatrix.applyConfig(EmbossConfig);
        convMatrix.Factor = 1;
        convMatrix.Offset = 127;
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }

    public Bitmap applyEngraveEffect(Bitmap src) {
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        convMatrix.setAll(0);
        convMatrix.Matrix[0][0] = -2;
        convMatrix.Matrix[1][1] = 2;
        convMatrix.Factor = 1;
        convMatrix.Offset = 95;
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }

    public static final int COLOR_MIN = 0x00;
    public static final int COLOR_MAX = 0xFF;

    public Bitmap applyFleaEffect(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        Random random = new Random();

        int index = 0;

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                index = y * width + x;
                int randColor = Color.rgb(random.nextInt(COLOR_MAX),
                        random.nextInt(COLOR_MAX), random.nextInt(COLOR_MAX));
                pixels[index] |= randColor;
            }
        }

        Bitmap rs = Bitmap.createBitmap(width, height, source.getConfig());
        rs.setPixels(pixels, 0, width, 0, 0, width, height);
        return rs;
    }

    public Bitmap applyGaussianBlurEffect(Bitmap src) {
        double[][] GaussianBlurConfig = new double[][]{
                {1, 2, 1},
                {2, 4, 2},
                {1, 2, 1}
        };
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        convMatrix.applyConfig(GaussianBlurConfig);
        convMatrix.Factor = 16;
        convMatrix.Offset = 0;
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }

    public Bitmap applySharpenEffect(Bitmap src, double weight) {
        double[][] SharpConfig = new double[][]{
                {0, -2, 0},
                {-2, weight, -2},
                {0, -2, 0}
        };
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        convMatrix.applyConfig(SharpConfig);
        convMatrix.Factor = weight - 8;
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }

    public Bitmap applyMeanRemovalEffect(Bitmap src) {
        double[][] MeanRemovalConfig = new double[][]{
                {-1, -1, -1},
                {-1, 9, -1},
                {-1, -1, -1}
        };
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        convMatrix.applyConfig(MeanRemovalConfig);
        convMatrix.Factor = 1;
        convMatrix.Offset = 0;
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }

    public Bitmap applySmoothEffect(Bitmap src, double value) {
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        convMatrix.setAll(1);
        convMatrix.Matrix[1][1] = value;
        convMatrix.Factor = value + 8;
        convMatrix.Offset = 1;
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }

    public Bitmap applyRoundCornerEffect(Bitmap src, float round) {

        int width = src.getWidth();
        int height = src.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(result);
        canvas.drawARGB(0, 0, 0, 0);


        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);


        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);

        canvas.drawRoundRect(rectF, round, round, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, rect, rect, paint);

        return result;
    }

    public static final double PI = 3.14159d;
    public static final double FULL_CIRCLE_DEGREE = 360d;
    public static final double HALF_CIRCLE_DEGREE = 180d;
    public static final double RANGE = 256d;

    public Bitmap applyTintEffect(Bitmap src, int degree) {

        int width = src.getWidth();
        int height = src.getHeight();

        int[] pix = new int[width * height];
        src.getPixels(pix, 0, width, 0, 0, width, height);

        int RY, GY, BY, RYY, GYY, BYY, R, G, B, Y;
        double angle = (PI * (double) degree) / HALF_CIRCLE_DEGREE;

        int S = (int) (RANGE * Math.sin(angle));
        int C = (int) (RANGE * Math.cos(angle));

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int r = (pix[index] >> 16) & 0xff;
                int g = (pix[index] >> 8) & 0xff;
                int b = pix[index] & 0xff;
                RY = (70 * r - 59 * g - 11 * b) / 100;
                GY = (-30 * r + 41 * g - 11 * b) / 100;
                BY = (-30 * r - 59 * g + 89 * b) / 100;
                Y = (30 * r + 59 * g + 11 * b) / 100;
                RYY = (S * BY + C * RY) / 256;
                BYY = (C * BY - S * RY) / 256;
                GYY = (-51 * RYY - 19 * BYY) / 100;
                R = Y + RYY;
                R = (R < 0) ? 0 : ((R > 255) ? 255 : R);
                G = Y + GYY;
                G = (G < 0) ? 0 : ((G > 255) ? 255 : G);
                B = Y + BYY;
                B = (B < 0) ? 0 : ((B > 255) ? 255 : B);
                pix[index] = 0xff000000 | (R << 16) | (G << 8) | B;
            }

        Bitmap outBitmap = Bitmap.createBitmap(width, height, src.getConfig());
        outBitmap.setPixels(pix, 0, width, 0, 0, width, height);

        pix = null;

        return outBitmap;
    }


    public  Bitmap applySepiaToningEffect(Bitmap src, int depth, double red, double green, double blue) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap rs = Bitmap.createBitmap(width, height, src.getConfig());
        final double GS_RED = 0.3;
        final double GS_GREEN = 0.59;
        final double GS_BLUE = 0.11;
        int A, R, G, B;
        int pixel;

        for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                B = G = R = (int)(GS_RED * R + GS_GREEN * G + GS_BLUE * B);
                R += (depth * red);
                if(R > 255) { R = 255; }
                G += (depth * green);
                if(G > 255) { G = 255; }
                B += (depth * blue);
                if(B > 255) { B = 255; }

                rs.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        return rs;
    }

}
