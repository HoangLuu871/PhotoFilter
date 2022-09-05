package com.example.photofilterapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ImageView imageView, imageCropView, nonImageView;

    TextView flipBtn, rotateLeftBtn, addPhotoBtn, brightnessBtn, brightnessCloseBtn, brightnessAcceptBtn, brightnessValue,
            contrastBtn, saveAllBtn, contrastCloseBtn, contrastAcceptBtn, contrastValue, filterCloseBtn, filterBtn, filterAcceptBtn,
            effectCloseBtn, effectBtn, effectAcceptBtn, undoBtn, redoBtn, cropBtn, cropCloseBtn, cropSquareBtn, cropLandscapeBtn, cropPortraitBtn, cropSaveBtn;
    private MaterialCardView default_effect, invert_effect, greyscale_effect, gamma_effect,
            contrast_effect, flea_effect, gaussian_blur_effect, round_corner_effect, sepia_effect,
            sepiaG_effect, sepiaB_effect, smooth_effect, tint_effect;
    private MaterialCardView default_filter, black_filter, red_filter, green_filter, blue_filter,
            saturation_filter, hue_filter, cyan_filter, yellow_filter, green2_filter;

    private Uri imageUri;
    private Bitmap bitmap;
    private Bitmap thumbnail;
    private Bitmap effect;
    private Bitmap filter;
    private SeekBar seekBarContrast, seekBarBrightness;
    private HorizontalScrollView svMenu;
    private ConstraintLayout svEffect, svFilter;
    private LinearLayout llContrast, llBrightness, cropTool;
    private PictureThread pictureThread;

    //Kích thước max
    private int height = 0;
    private int width = 0;
    private int heightThumbnail = 0;
    private int widthThumbnail = 0;
    private int effectKey = 10000;
    private int filterKey = 20000;
    private static final int MAX_PIXEL_COUNT = 2048;
    private static final int MAX_PIXEL_COUNT_THUMBNAIL = 400;
    private boolean haveImage;
    public ArrayList<Bitmap> bitmap_history;
    public ArrayList<Bitmap> thumbnail_history;
    private int index;

    private int cropX, cropY, cropSizeX, cropSizeY;


    //private static final int key = 100;


    @SuppressLint({"NonConstantResourceId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        haveImage = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init
        initVariables();

        if (!haveImage) {
            //disable editing when starting app
            saveAllBtn.setEnabled(false);
            flipBtn.setEnabled(false);
            rotateLeftBtn.setEnabled(false);
            contrastBtn.setEnabled(false);
            filterBtn.setEnabled(false);
            brightnessBtn.setEnabled(false);
            effectBtn.setEnabled(false);
            undoBtn.setEnabled(false);
            redoBtn.setEnabled(false);
            cropBtn.setEnabled(false);
        }

        final int REQUEST_PICK_IMAGE = 1234;

        addPhotoBtn.setOnClickListener(view -> {
            if (bitmap != null) bitmap = null;
            haveImage = true;
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            final Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            final Intent chooserIntent = Intent.createChooser(intent, "Select Image");
            startActivityForResult(chooserIntent, REQUEST_PICK_IMAGE);

        });

        nonImageView.setOnClickListener(view -> {
            if (bitmap != null) bitmap = null;
            haveImage = true;
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            final Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            final Intent chooserIntent = Intent.createChooser(intent, "Select Image");
            startActivityForResult(chooserIntent, REQUEST_PICK_IMAGE);
        });

        saveAllBtn.setOnClickListener(view -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Save image?")
                    .setPositiveButton(Html.fromHtml("<font color='#FF7F27'>Yes</font>"), (dialogInterface, i) -> {
                        final File outFile = createImageFile();
                        try (FileOutputStream out = new FileOutputStream(outFile)) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            imageUri = Uri.parse("file://" + outFile.getAbsolutePath());
                            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
                            Toast.makeText(MainActivity.this, "SAVED", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton(Html.fromHtml("<font color='#FF7F27'>No</font>"), (dialogInterface, i) -> Log.e("Save", "NO"))
                    .show();
        });

        undoBtn.setOnClickListener(view -> {
            if (index - 1 >= 0) {
                index--;
                thumbnail = thumbnail_history.get(index);
                bitmap = bitmap_history.get(index);
                imageView.setImageBitmap(thumbnail);
            }
        });

        redoBtn.setOnClickListener(view -> {
            if (index + 1 < thumbnail_history.size()) {
                index++;
                thumbnail = thumbnail_history.get(index);
                bitmap = bitmap_history.get(index);
                imageView.setImageBitmap(thumbnail);
            }
        });

        //crop image
        cropBtn.setOnClickListener(view -> {
            imageCropView.setImageBitmap(thumbnail);
            imageCropView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            cropSizeX = 60;
            cropSizeY = 60;
            svMenu.setVisibility(View.GONE);
            cropTool.setVisibility(View.VISIBLE);

            double scale = (double) thumbnail.getHeight() / (double) thumbnail.getWidth();
            if (imageCropView.getHeight() > imageCropView.getWidth()) {
                imageCropView.getLayoutParams().width = (int) (imageCropView.getHeight() / scale);
                imageCropView.requestLayout();
            }

            imageCropView.setOnTouchListener((view1, motionEvent) -> {
                double scale1 = (double) thumbnail.getHeight() / (double) imageView.getHeight();
                cropX = (int) (motionEvent.getX() * scale1);
                cropY = (int) (motionEvent.getY() * scale1);
                System.out.println("image: " + imageCropView.getWidth() + " - " + imageCropView.getHeight());
                System.out.println("bit: " + bitmap.getWidth() + " - " + bitmap.getHeight());
                System.out.println("thumb: " + thumbnail.getWidth() + " - " + thumbnail.getHeight());
                System.out.println("click: " + cropX + " - " + cropY);
                Bitmap rs = Bitmap.createBitmap(thumbnail.getWidth(), thumbnail.getHeight(), thumbnail.getConfig());
                Canvas canvas = new Canvas(rs);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(89, 240, 34));
                paint.setStrokeWidth(3);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawBitmap(thumbnail, 0, 0, null);
                canvas.drawRect(cropX - (cropSizeX / 2), cropY - (cropSizeY / 2), cropX + cropSizeX, cropY + cropSizeY, paint);
                imageCropView.setImageBitmap(rs);
                return true;
            });


        });

        cropCloseBtn.setOnClickListener(view -> {
            imageCropView.getLayoutParams().width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            imageCropView.requestLayout();
            cropTool.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageCropView.setVisibility(View.GONE);
            imageView.setImageBitmap(thumbnail);
            imageView.setVisibility(View.VISIBLE);
        });

        cropSquareBtn.setOnClickListener(view -> {
            cropSizeX = cropSizeY = 60;
        });

        cropLandscapeBtn.setOnClickListener(view -> {
            cropSizeX = 60;
            cropSizeY = 60;
            cropSizeX = cropSizeX * 2;
        });

        cropPortraitBtn.setOnClickListener(view -> {
            cropSizeX = 60;
            cropSizeY = 60;
            cropSizeY = cropSizeY * 2;
        });

        cropSaveBtn.setOnClickListener(view -> {
            double scale = (double) bitmap.getWidth() / (double) thumbnail.getWidth();
            thumbnail = Bitmap.createBitmap(thumbnail, cropX - (cropSizeX / 2), cropY - (cropSizeY / 2), cropSizeX, cropSizeY);
            cropTool.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageCropView.setVisibility(View.GONE);
            imageView.setImageBitmap(thumbnail);
            imageView.setVisibility(View.VISIBLE);
            new Thread(() -> {
                System.out.println((int) (cropSizeX * scale));
                int bit_cropX = (int) (cropX * scale);
                int bit_cropY = (int) (cropY * scale);
                int bit_cropSizeX = (int) (cropSizeX * scale);
                int bit_cropSizeY = (int) (cropSizeY * scale);
                System.out.println(bitmap.getWidth() + " : " + bitmap.getHeight());
                System.out.println(bit_cropX + " ^ " + bit_cropY + " * " + bit_cropSizeX);
                bitmap = Bitmap.createBitmap(bitmap, bit_cropX - (bit_cropSizeX / 2), bit_cropY - (bit_cropSizeY / 2), bit_cropSizeX, bit_cropSizeY);
                System.out.println(bitmap.getWidth() + " : " + bitmap.getHeight());
                new Thread(() -> {
                    while (index + 1 < thumbnail_history.size()) {
                        thumbnail_history.remove(index + 1);
                        bitmap_history.remove(index + 1);
                    }
                    thumbnail_history.add(thumbnail);
                    bitmap_history.add(bitmap);
                    index++;
                }).start();
            }).start();

        });

        //flip image
        flipBtn.setOnClickListener(view -> {
            Matrix matrix = new Matrix();
            matrix.setScale(-1, 1);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);
            imageView.setImageBitmap(thumbnail);
            new Thread(() -> {
                while (index + 1 < thumbnail_history.size()) {
                    thumbnail_history.remove(index + 1);
                    bitmap_history.remove(index + 1);
                }
                thumbnail_history.add(thumbnail);
                bitmap_history.add(bitmap);
                index++;
            }).start();
        });

        //rotate image
        rotateLeftBtn.setOnClickListener(view -> {
            Matrix matrix = new Matrix();
            matrix.postRotate(-90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);
            imageView.setImageBitmap(thumbnail);
            new Thread(() -> {
                while (index + 1 < thumbnail_history.size()) {
                    thumbnail_history.remove(index + 1);
                    bitmap_history.remove(index + 1);
                }
                thumbnail_history.add(thumbnail);
                bitmap_history.add(bitmap);
                index++;
            }).start();
        });

        //change brightness
        brightnessBtn.setOnClickListener(view -> {
            seekBarBrightness.setProgress(0);
            svMenu.setVisibility(View.GONE);
            llBrightness.setVisibility(View.VISIBLE);
        });

        seekBarBrightness.setMin(-100);
        seekBarBrightness.setMax(100);
        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                pictureThread = new PictureThread(thumbnail);
                brightnessValue.setText(Integer.toString(seekBar.getProgress()));
                Bitmap bmp = pictureThread.adjustBrightness(seekBar.getProgress());
                imageView.setImageBitmap(bmp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        brightnessCloseBtn.setOnClickListener(view -> {
            llBrightness.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(thumbnail);
        });

        brightnessAcceptBtn.setOnClickListener(view -> {
            pictureThread = new PictureThread(bitmap);
            bitmap = pictureThread.adjustBrightness(seekBarBrightness.getProgress());
            pictureThread = new PictureThread(thumbnail);
            thumbnail = pictureThread.adjustBrightness(seekBarBrightness.getProgress());
            llBrightness.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(thumbnail);
            new Thread(() -> {
                while (index + 1 < thumbnail_history.size()) {
                    thumbnail_history.remove(index + 1);
                    bitmap_history.remove(index + 1);
                }
                thumbnail_history.add(thumbnail);
                bitmap_history.add(bitmap);
                index++;
            }).start();
        });

        // change contrast
        contrastBtn.setOnClickListener(view -> {
            seekBarContrast.setProgress(0);
            svMenu.setVisibility(View.GONE);
            llContrast.setVisibility(View.VISIBLE);

        });

        seekBarContrast.setMin(-100);
        seekBarContrast.setMax(100);
        seekBarContrast.setProgress(0);
        seekBarContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                contrastValue.setText(Integer.toString(seekBar.getProgress()));
                pictureThread = new PictureThread(thumbnail);
                Bitmap bmp = pictureThread.adjustContrast(seekBar.getProgress());
                imageView.setImageBitmap(bmp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        contrastCloseBtn.setOnClickListener(view -> {
            llContrast.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(thumbnail);
        });

        contrastAcceptBtn.setOnClickListener(view -> {
            pictureThread = new PictureThread(bitmap);
            bitmap = pictureThread.adjustContrast(seekBarContrast.getProgress());
            pictureThread = new PictureThread(thumbnail);
            thumbnail = pictureThread.adjustContrast(seekBarContrast.getProgress());
            llContrast.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(thumbnail);
            new Thread(() -> {
                while (index + 1 < thumbnail_history.size()) {
                    thumbnail_history.remove(index + 1);
                    bitmap_history.remove(index + 1);
                }
                thumbnail_history.add(thumbnail);
                bitmap_history.add(bitmap);
                index++;
            }).start();
        });

        //filter
        filterBtn.setOnClickListener(view -> {
            svMenu.setVisibility(View.GONE);
            svFilter.setVisibility(View.VISIBLE);
        });

        filterCloseBtn.setOnClickListener(view -> {
            svFilter.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(thumbnail);
        });

        View.OnClickListener filterClickListener = this::filterClicked;

        new Thread(() -> {
            default_filter.setOnClickListener(filterClickListener);
            black_filter.setOnClickListener(filterClickListener);
            red_filter.setOnClickListener(filterClickListener);
            green_filter.setOnClickListener(filterClickListener);
            blue_filter.setOnClickListener(filterClickListener);
            hue_filter.setOnClickListener(filterClickListener);
            saturation_filter.setOnClickListener(filterClickListener);
            cyan_filter.setOnClickListener(filterClickListener);
            yellow_filter.setOnClickListener(filterClickListener);
            green2_filter.setOnClickListener(filterClickListener);
        }).start();

        filterAcceptBtn.setOnClickListener(view -> {
            svFilter.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            thumbnail = filter;
            imageView.setImageBitmap(thumbnail);
            new Thread(() -> {
                PictureFilters pictureFilters = new PictureFilters();
                switch (filterKey) {
                    case R.id.filter_default:
                        break;
                    case R.id.filter_black:
                        bitmap = pictureFilters.applyBlackFilter(bitmap);
                        System.out.println(bitmap + "key" + filterKey);
                        break;
                    case R.id.filter_red:
                        bitmap = pictureFilters.applyColorFilterEffect(bitmap, 255, 0, 0);
                        break;
                    case R.id.filter_green:
                        bitmap = pictureFilters.applyColorFilterEffect(bitmap, 0, 255, 0);
                        break;
                    case R.id.filter_blue:
                        bitmap = pictureFilters.applyColorFilterEffect(bitmap, 0, 0, 255);
                        break;
                    case R.id.filter_hue:
                        bitmap = pictureFilters.applyHueFilter(bitmap, 2);
                        break;
                    case R.id.filter_saturation:
                        bitmap = pictureFilters.applySaturationFilter(bitmap, 1);
                        break;
                    case R.id.filter_shading_cyan:
                        bitmap = pictureFilters.applyShadingFilter(bitmap, Color.CYAN);
                        break;
                    case R.id.filter_shading_yellow:
                        bitmap = pictureFilters.applyShadingFilter(bitmap, Color.YELLOW);
                        break;
                    case R.id.filter_shading_green:
                        bitmap = pictureFilters.applyShadingFilter(bitmap, Color.GREEN);
                        break;
                }
                new Thread(() -> {
                    while (index + 1 < thumbnail_history.size()) {
                        thumbnail_history.remove(index + 1);
                        bitmap_history.remove(index + 1);
                    }
                    thumbnail_history.add(thumbnail);
                    bitmap_history.add(bitmap);
                    index++;
                }).start();
            }).start();
        });

        effectBtn.setOnClickListener(view -> {
            svMenu.setVisibility(View.GONE);
            svEffect.setVisibility(View.VISIBLE);
        });

        effectCloseBtn.setOnClickListener(view -> {
            svEffect.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(thumbnail);
        });

        View.OnClickListener effectClickListener = this::effectClicked;

        new Thread(() -> {
            default_effect.setOnClickListener(effectClickListener);
            invert_effect.setOnClickListener(effectClickListener);
            greyscale_effect.setOnClickListener(effectClickListener);
            gamma_effect.setOnClickListener(effectClickListener);
            contrast_effect.setOnClickListener(effectClickListener);
            flea_effect.setOnClickListener(effectClickListener);
            gaussian_blur_effect.setOnClickListener(effectClickListener);
            round_corner_effect.setOnClickListener(effectClickListener);
            sepia_effect.setOnClickListener(effectClickListener);
            sepiaG_effect.setOnClickListener(effectClickListener);
            sepiaB_effect.setOnClickListener(effectClickListener);
            smooth_effect.setOnClickListener(effectClickListener);
            tint_effect.setOnClickListener(effectClickListener);
        }).start();

        effectAcceptBtn.setOnClickListener(view -> {
            svEffect.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            thumbnail = effect;
            imageView.setImageBitmap(thumbnail);
            PictureEffects pictureEffects = new PictureEffects();
            new Thread(() -> {
                switch (effectKey) {
                    case R.id.effect_default:
                        break;
                    case R.id.effect_invert:
                        bitmap = pictureEffects.applyInvertEffect(bitmap);
                        break;
                    case R.id.effect_greyscale:
                        bitmap = pictureEffects.applyGreyscaleEffect(bitmap);
                        break;
                    case R.id.effect_gamma:
                        bitmap = pictureEffects.applyGammaEffect(bitmap, 1.8, 1.8, 1.8);
                        break;
                    case R.id.effect_contrast:
                        bitmap = pictureEffects.applyContrastEffect(bitmap, 70);
                        break;
                    case R.id.effect_flea:
                        bitmap = pictureEffects.applyFleaEffect(bitmap);
                        break;
                    case R.id.effect_gaussian_blur:
                        bitmap = pictureEffects.applyGaussianBlurEffect(bitmap);
                        break;
                    case R.id.effect_round_corner:
                        bitmap = pictureEffects.applyRoundCornerEffect(bitmap, 45);
                        break;
                    case R.id.effect_sepia:
                        bitmap = pictureEffects.applySepiaToningEffect(bitmap, 10, 1.5, 0.6, 0.12);
                        break;
                    case R.id.effect_sepiaB:
                        bitmap = pictureEffects.applySepiaToningEffect(bitmap, 10, 0.88, 2.45, 1.43);
                        break;
                    case R.id.effect_sepiaG:
                        bitmap = pictureEffects.applySepiaToningEffect(bitmap, 10, 1.2, 0.87, 2.1);
                        break;
                    case R.id.effect_smooth:
                        bitmap = pictureEffects.applySmoothEffect(bitmap, 2);
                        break;
                    case R.id.effect_tint:
                        bitmap = pictureEffects.applyTintEffect(bitmap, 100);
                        break;
                }
                new Thread(() -> {
                    while (index + 1 < thumbnail_history.size()) {
                        thumbnail_history.remove(index + 1);
                        bitmap_history.remove(index + 1);
                    }
                    thumbnail_history.add(thumbnail);
                    bitmap_history.add(bitmap);
                    index++;
                }).start();
            }).start();
        });
    }

    @SuppressLint("NonConstantResourceId")
    public void filterClicked(View v) {
        PictureFilters pictureFilters = new PictureFilters();
        switch (v.getId()) {
            case R.id.filter_default:
                imageView.setImageBitmap(thumbnail);
                break;
            case R.id.filter_black:
                filter = pictureFilters.applyBlackFilter(thumbnail);
                imageView.setImageBitmap(filter);
                break;
            case R.id.filter_red:
                filter = pictureFilters.applyColorFilterEffect(thumbnail, 255, 0, 0);
                imageView.setImageBitmap(filter);
                break;
            case R.id.filter_green:
                filter = pictureFilters.applyColorFilterEffect(thumbnail, 0, 255, 0);
                imageView.setImageBitmap(filter);
                break;
            case R.id.filter_blue:
                filter = pictureFilters.applyColorFilterEffect(thumbnail, 0, 0, 255);
                imageView.setImageBitmap(filter);
                break;
            case R.id.filter_hue:
                filter = pictureFilters.applyHueFilter(thumbnail, 2);
                imageView.setImageBitmap(filter);
                break;
            case R.id.filter_saturation:
                filter = pictureFilters.applySaturationFilter(thumbnail, 1);
                imageView.setImageBitmap(filter);
                break;
            case R.id.filter_shading_cyan:
                filter = pictureFilters.applyShadingFilter(thumbnail, Color.CYAN);
                imageView.setImageBitmap(filter);
                break;
            case R.id.filter_shading_yellow:
                filter = pictureFilters.applyShadingFilter(thumbnail, Color.YELLOW);
                imageView.setImageBitmap(filter);
                break;
            case R.id.filter_shading_green:
                filter = pictureFilters.applyShadingFilter(thumbnail, Color.GREEN);
                imageView.setImageBitmap(filter);
                break;
        }
        filterKey = v.getId();
        System.out.println("filterKey: " + filterKey);
    }

    @SuppressLint("NonConstantResourceId")
    public void effectClicked(View v) {
        PictureEffects pictureEffects = new PictureEffects();
        switch (v.getId()) {
            case R.id.effect_default:
                imageView.setImageBitmap(thumbnail);
                break;
            case R.id.effect_invert:
                effect = pictureEffects.applyInvertEffect(thumbnail);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_greyscale:
                effect = pictureEffects.applyGreyscaleEffect(thumbnail);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_gamma:
                effect = pictureEffects.applyGammaEffect(thumbnail, 1.8, 1.8, 1.8);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_contrast:
                effect = pictureEffects.applyContrastEffect(thumbnail, 70);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_flea:
                effect = pictureEffects.applyFleaEffect(thumbnail);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_gaussian_blur:
                effect = pictureEffects.applyGaussianBlurEffect(thumbnail);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_round_corner:
                effect = pictureEffects.applyRoundCornerEffect(thumbnail, 45);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_sepia:
                effect = pictureEffects.applySepiaToningEffect(thumbnail, 10, 1.5, 0.6, 0.12);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_sepiaB:
                effect = pictureEffects.applySepiaToningEffect(thumbnail, 10, 0.88, 2.45, 1.43);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_sepiaG:
                effect = pictureEffects.applySepiaToningEffect(thumbnail, 10, 1.2, 0.87, 2.1);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_smooth:
                effect = pictureEffects.applySmoothEffect(thumbnail, 2);
                imageView.setImageBitmap(effect);
                break;
            case R.id.effect_tint:
                effect = pictureEffects.applyTintEffect(thumbnail, 100);
                imageView.setImageBitmap(effect);
                break;
        }
        effectKey = v.getId();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        assert data != null;
        imageUri = data.getData();
        new Thread() {
            @Override
            public void run() {
                super.run();
                final BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
                bmpOptions.inBitmap = bitmap;
                bmpOptions.inJustDecodeBounds = true;
                try (InputStream input = getContentResolver().openInputStream(imageUri)) {
                    bitmap = BitmapFactory.decodeStream(input, null, bmpOptions);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                bmpOptions.inJustDecodeBounds = false;

                //get resizeScale bitmap
                width = bmpOptions.outWidth;
                height = bmpOptions.outHeight;
                widthThumbnail = width;
                heightThumbnail = height;
                int resizeScale = 1;
                if (width > MAX_PIXEL_COUNT) {
                    resizeScale = width / MAX_PIXEL_COUNT;
                } else if (height > MAX_PIXEL_COUNT) {
                    resizeScale = height / MAX_PIXEL_COUNT;
                }
                if (width / resizeScale > MAX_PIXEL_COUNT || height / resizeScale > MAX_PIXEL_COUNT) {
                    resizeScale++;
                }
                //get resizeScale thumbnail
                int resizeScaleThumbnail = 1;
                if (widthThumbnail > MAX_PIXEL_COUNT_THUMBNAIL) {
                    resizeScaleThumbnail = widthThumbnail / MAX_PIXEL_COUNT_THUMBNAIL;
                } else if (heightThumbnail > MAX_PIXEL_COUNT_THUMBNAIL) {
                    resizeScaleThumbnail = heightThumbnail / MAX_PIXEL_COUNT_THUMBNAIL;
                }
                if (widthThumbnail / resizeScaleThumbnail > MAX_PIXEL_COUNT_THUMBNAIL || heightThumbnail / resizeScaleThumbnail > MAX_PIXEL_COUNT_THUMBNAIL) {
                    resizeScaleThumbnail++;
                }

                bmpOptions.inSampleSize = resizeScale;
                InputStream input;
                try {
                    input = getContentResolver().openInputStream(imageUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    recreate();
                    return;
                }
                bitmap = BitmapFactory.decodeStream(input, null, bmpOptions);
                thumbnail = Bitmap.createScaledBitmap(bitmap, widthThumbnail / resizeScaleThumbnail, heightThumbnail / resizeScaleThumbnail, true);
                runOnUiThread(() -> {
                    bitmap_history = new ArrayList<>();
                    thumbnail_history = new ArrayList<>();
                    index = 0;
                    bitmap_history.add(bitmap);
                    thumbnail_history.add(thumbnail);
                    imageView.setImageBitmap(thumbnail);
                    flipBtn.setEnabled(true);
                    rotateLeftBtn.setEnabled(true);
                    contrastBtn.setEnabled(true);
                    filterBtn.setEnabled(true);
                    brightnessBtn.setEnabled(true);
                    effectBtn.setEnabled(true);
                    undoBtn.setEnabled(true);
                    redoBtn.setEnabled(true);
                    cropBtn.setEnabled(true);
                    imageView.setVisibility(View.VISIBLE);
                    nonImageView.setVisibility(View.GONE);
                });
                width = bitmap.getWidth();
                height = bitmap.getHeight();
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                saveAllBtn.setEnabled(true);
            }
        }.start();
    }

    private File createImageFile() {
        @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName = "/JPEG_" + timeStamp + ".jpg";
        final File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir + imageFileName);
    }

    @Override
    public void onBackPressed() {
        if (svMenu.getVisibility() == View.GONE) {
            llContrast.setVisibility(View.GONE);
            llBrightness.setVisibility(View.GONE);
            svFilter.setVisibility(View.GONE);
            svEffect.setVisibility(View.GONE);
            svMenu.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(thumbnail);
        } else {
            super.onBackPressed();
        }
    }

    private void initVariables() {
        addPhotoBtn = findViewById(R.id.addPhotoBtn);
        saveAllBtn = findViewById(R.id.saveAllBtn);

        undoBtn = findViewById(R.id.undoBtn);
        redoBtn = findViewById(R.id.redoBtn);

        imageView = findViewById(R.id.photoView);
        imageCropView = findViewById(R.id.photoCropView);
        nonImageView = findViewById(R.id.nonPhotoView);

        //toolbar
        svMenu = findViewById(R.id.svMenu);
        flipBtn = findViewById(R.id.flipBtn);
        rotateLeftBtn = findViewById(R.id.rotateLeftBtn);

        //crop
        cropBtn = findViewById(R.id.cropBtn);
        cropTool = findViewById(R.id.cropTool);
        cropCloseBtn = findViewById(R.id.cropCloseBtn);
        cropSquareBtn = findViewById(R.id.cropSquareBtn);
        cropLandscapeBtn = findViewById(R.id.cropLandscapeBtn);
        cropPortraitBtn = findViewById(R.id.cropPortraitBtn);
        cropSaveBtn = findViewById(R.id.cropSaveBtn);

        //contrast
        contrastBtn = findViewById(R.id.contrastBtn);
        contrastCloseBtn = findViewById(R.id.contrastCloseBtn);
        contrastAcceptBtn = findViewById(R.id.contrastAcceptBtn);
        contrastValue = findViewById(R.id.contrastValue);
        seekBarContrast = findViewById(R.id.seekBarContrast);
        llContrast = findViewById(R.id.llContrast);

        //brightness
        brightnessBtn = findViewById(R.id.brightnessBtn);
        brightnessCloseBtn = findViewById(R.id.brightnessCloseBtn);
        brightnessAcceptBtn = findViewById(R.id.brightnessAcceptBtn);
        brightnessValue = findViewById(R.id.brightnessValue);
        seekBarBrightness = findViewById(R.id.seekBarBrightness);
        llBrightness = findViewById(R.id.llBrightness);

        //effect
        effectBtn = findViewById(R.id.effectBtn);
        effectCloseBtn = findViewById(R.id.effectCloseBtn);
        effectAcceptBtn = findViewById(R.id.effectAcceptBtn);
        svEffect = findViewById(R.id.svEffect);
        default_effect = findViewById(R.id.effect_default);
        invert_effect = findViewById(R.id.effect_invert);
        greyscale_effect = findViewById(R.id.effect_greyscale);
        gamma_effect = findViewById(R.id.effect_gamma);
        contrast_effect = findViewById(R.id.effect_contrast);
        flea_effect = findViewById(R.id.effect_flea);
        gaussian_blur_effect = findViewById(R.id.effect_gaussian_blur);
        round_corner_effect = findViewById(R.id.effect_round_corner);
        sepia_effect = findViewById(R.id.effect_sepia);
        sepiaG_effect = findViewById(R.id.effect_sepiaG);
        sepiaB_effect = findViewById(R.id.effect_sepiaB);
        smooth_effect = findViewById(R.id.effect_smooth);
        tint_effect = findViewById(R.id.effect_tint);

        //filter
        filterBtn = findViewById(R.id.filterBtn);
        filterCloseBtn = findViewById(R.id.filterCloseBtn);
        filterAcceptBtn = findViewById(R.id.filterAcceptBtn);
        svFilter = findViewById(R.id.svFilter);
        default_filter = findViewById(R.id.filter_default);
        black_filter = findViewById(R.id.filter_black);
        red_filter = findViewById(R.id.filter_red);
        green_filter = findViewById(R.id.filter_green);
        blue_filter = findViewById(R.id.filter_blue);
        hue_filter = findViewById(R.id.filter_hue);
        saturation_filter = findViewById(R.id.filter_saturation);
        cyan_filter = findViewById(R.id.filter_shading_cyan);
        yellow_filter = findViewById(R.id.filter_shading_yellow);
        green2_filter = findViewById(R.id.filter_shading_green);
    }
}
