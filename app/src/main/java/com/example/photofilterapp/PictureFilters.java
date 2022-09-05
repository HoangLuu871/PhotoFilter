package com.example.photofilterapp;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.Random;

public class PictureFilters {

    public static final int COLOR_MIN = 0x00;
    public static final int COLOR_MAX = 0xFF;

    public  Bitmap applyBlackFilter(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        Random random = new Random();

        int R, G, B, index = 0, thresHold = 0;
        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width; ++x) {
                index = y * width + x;
                R = Color.red(pixels[index]);
                G = Color.green(pixels[index]);
                B = Color.blue(pixels[index]);
                thresHold = random.nextInt(COLOR_MAX);
                if(R < thresHold && G < thresHold && B < thresHold) {
                    pixels[index] = Color.rgb(COLOR_MIN, COLOR_MIN, COLOR_MIN);
                }
            }
        }
        Bitmap rs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        rs.setPixels(pixels, 0, width, 0, 0, width, height);
        return rs;
    }

    public Bitmap applyColorFilterEffect(Bitmap src, double red, double green, double blue) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap rs = Bitmap.createBitmap(width, height, src.getConfig());
        int A, R, G, B;
        int pixel;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = (int) (Color.red(pixel) * red);
                G = (int) (Color.green(pixel) * green);
                B = (int) (Color.blue(pixel) * blue);
                rs.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        return rs;
    }

    public  Bitmap applyHueFilter(Bitmap source, int level) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        float[] HSV = new float[3];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        int index = 0;
        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width; ++x) {
                index = y * width + x;
                Color.colorToHSV(pixels[index], HSV);
                HSV[0] *= level;
                HSV[0] = (float) Math.max(0.0, Math.min(HSV[0], 360.0));
                pixels[index] |= Color.HSVToColor(HSV);
            }
        }
        Bitmap rs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        rs.setPixels(pixels, 0, width, 0, 0, width, height);
        return rs;
    }

    public  Bitmap applySaturationFilter(Bitmap source, int level) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        float[] HSV = new float[3];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        int index = 0;
        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width; ++x) {
                index = y * width + x;
                Color.colorToHSV(pixels[index], HSV);
                HSV[1] *= level;
                HSV[1] = (float) Math.max(0.0, Math.min(HSV[1], 1.0));
                pixels[index] |= Color.HSVToColor(HSV);
            }
        }
        Bitmap rs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        rs.setPixels(pixels, 0, width, 0, 0, width, height);
        return rs;
    }

    public Bitmap applyShadingFilter(Bitmap source, int shadingColor) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        int index = 0;
        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width; ++x) {
                index = y * width + x;
                pixels[index] &= shadingColor;
            }
        }
        Bitmap rs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        rs.setPixels(pixels, 0, width, 0, 0, width, height);
        return rs;
    }


}
