package com.example.photofilterapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Handler;
import android.widget.ImageView;

public class PictureThread{
    private Bitmap bitmap;
    private Bitmap tmp_bmp;
    private final Canvas canvas;
    private final Paint paint;
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorMatrixColorFilter;

    public PictureThread(Bitmap bitmap) {
        this.bitmap = bitmap;
        tmp_bmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        canvas = new Canvas(tmp_bmp);
        paint = new Paint();
    }

    Bitmap adjustBrightness(int amount) {
        colorMatrix = new ColorMatrix(new float[]{
                1, 0, 0, 0, amount,
                0, 1f, 0, 0, amount,
                0, 0, 1f, 0, amount,
                0, 0, 0, 1f, 0,
        });
        colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixColorFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return tmp_bmp;
    }

    Bitmap adjustContrast(int amount) {
        float scale = (float) Math.pow((amount/100.f + 1.f), 2);
        float translate = (float) (((((amount / 255.0) - 0.5) * scale) + 0.5) * 255.0);
        colorMatrix = new ColorMatrix(new float[]{
                scale, 0, 0, 0, translate,
                0, scale, 0, 0, translate,
                0, 0, scale, 0, translate,
                0, 0, 0, 1, 0,
        });
        colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixColorFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return tmp_bmp;
    }

}
