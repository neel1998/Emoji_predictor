package com.neel.trivedi.emojipredictor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;

public class MyCanvas extends View {

    Paint paint;
    Path path;

    public MyCanvas(Context context) {
        super(context);
        paint = new Paint();
        path = new Path();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(50f);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(1000, 1000);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        this.setLayoutParams(layoutParams);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xPos = event.getX();
        float yPos = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(xPos, yPos);
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(xPos, yPos);
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Pair<float[], String> getData() throws IOException {
        this.setDrawingCacheEnabled(true);
        this.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(this.getDrawingCache());
        this.setDrawingCacheEnabled(false);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
        float[] data = new float[10000];
        String x = "";
        int k = 0;
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                if (scaledBitmap.getPixel(j, i) > -8388608) {
                    data[k++] = 255;
                    x += 255 + ",";
                } else {
                    data[k++] = 0;
                    x += 0 + ",";
                }
            }
        }
        x = x.substring(0, x.length() - 1);

        Mat m = new Mat();
        Utils.bitmapToMat(scaledBitmap, m);
        Imgproc.cvtColor(m, m, Imgproc.COLOR_RGB2GRAY);
        Log.d("mat", m.size().height + " " + m.size().width + " " + m.channels());
        Imgproc.threshold(m, m, 127, 255, Imgproc.THRESH_TOZERO);
        String d = m.dump();
        d = d.replace(";", ",");
        d = d.replace("\n", "");
        return new Pair<>(data, d);
    }

    public void saveBitmap() {
        this.setDrawingCacheEnabled(true);
        this.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(this.getDrawingCache());
        this.setDrawingCacheEnabled(false);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
        try {
            FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    File.separator + System.currentTimeMillis() + "_emoji.jpg");
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {

        }
    }

    public void clearCanvas() {
        path = new Path();
    }
}
