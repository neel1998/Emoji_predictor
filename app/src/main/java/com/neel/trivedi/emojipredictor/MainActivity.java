package com.neel.trivedi.emojipredictor;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    MyCanvas canvas;
    float[] data = new float[10000];
    String dataString = "";
    Pair<float[], String> res;
    int predicted;
    int[] emojis = new int[]{R.drawable.emoji0, R.drawable.emoji1, R.drawable.emoji2, R.drawable.emoji3, R.drawable.emoji4, R.drawable.emoji5};
    ImageView imageView;
    TensorFlowInferenceInterface tensorflow;
    RelativeLayout relativeLayout;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        relativeLayout = findViewById(R.id.main_relative_layout);
        canvas = new MyCanvas(this);
        relativeLayout.addView(canvas, 0);
        imageView = findViewById(R.id.image_view);
        progressBar = findViewById(R.id.progress_bar);


        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Is the predicted emoji correct?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressBar.setVisibility(View.VISIBLE);
                                new UploadTask().execute(String.valueOf(predicted) + "," + dataString);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.corret_emoji_alert_layout, null);
                                new AlertDialog.Builder(MainActivity.this)
                                        .setView(dialogView)
                                        .setPositiveButton("Select", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (((RadioButton)dialogView.findViewById(R.id.emoji0_radio)).isChecked()) {
                                                    dataString = "0," + dataString;
                                                } else if (((RadioButton)dialogView.findViewById(R.id.emoji1_radio)).isChecked()) {
                                                    dataString = "1," + dataString;
                                                } else if (((RadioButton)dialogView.findViewById(R.id.emoji2_radio)).isChecked()) {
                                                    dataString = "2," + dataString;
                                                } else if (((RadioButton)dialogView.findViewById(R.id.emoji3_radio)).isChecked()) {
                                                    dataString = "3," + dataString;
                                                } else if (((RadioButton)dialogView.findViewById(R.id.emoji4_radio)).isChecked()) {
                                                    dataString = "4," + dataString;
                                                } else if (((RadioButton)dialogView.findViewById(R.id.emoji5_radio)).isChecked()) {
                                                    dataString = "5," + dataString;
                                                }
                                                progressBar.setVisibility(View.VISIBLE);
                                                new UploadTask().execute(dataString);
                                            }
                                        }).create().show();
                            }
                        }).create().show();
            }
        });

        Button predictButton = findViewById(R.id.predict_button);
        predictButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                new PredictTask().execute();
            }
        });

        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setVisibility(View.GONE);
                canvas.clearCanvas();
                canvas = new MyCanvas(MainActivity.this);
                relativeLayout.removeViewAt(0);
                relativeLayout.addView(canvas, 0);
            }
        });

        tensorflow = new TensorFlowInferenceInterface(getAssets(), "saved_model_80.pb");
    }

    private int maxIndex(float[] array) {
        int max = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > array[max]) {
                max = i;
            }
        }
        return max;
    }

    //        float[] all_input = new float[90*10000];
//        int[] answer = new int[90];
//        try {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("100_test_600.csv")));
//            String line = reader.readLine();
//            int k = 0;
//            int x = 0;
//            while (line != null) {
//                String[] data = line.split(",");
//                answer[k] = Integer.valueOf(data[0]);
//                for (int i = 1; i < data.length; i ++) {
//                    all_input[x++] = Float.valueOf(data[i]);
//                }
//                k ++;
//                line = reader.readLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        float[][] res = new float[90][6];
//        int j = -1, l = 0;
//        for (int i = 0; i < output.length; i ++) {
//            if (i%6 == 0) {
//                l = 0;
//                j++;
//            }
//            res[j][l] = output[i];
//            l ++;
//        }
//        int correct = 0, incorrect = 0;
//        for (int i = 0; i < 90; i ++) {
//            if (answer[i] == maxIndex(res[i])) {
//                correct ++;
//            } else {
//                incorrect ++;
//            }
//        }
//        Log.d("res", correct + " " + incorrect + " " + correct*100.0/90);
    private class PredictTask extends AsyncTask<Void, Void, float[]> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected float[] doInBackground(Void... voids) {
            try {
                res = canvas.getData();
                data = res.first;
                dataString = res.second;
            } catch (IOException e) {
                e.printStackTrace();
            }
            tensorflow.feed("conv2d_1_input", data, 1, 100, 100, 1);
            tensorflow.run(new String[]{"dense_1/Softmax"});
            float[] output = new float[6];
            tensorflow.fetch("dense_1/Softmax", output);
            return output;
        }

        @Override
        protected void onPostExecute(float[] output) {
            predicted = maxIndex(output);
            Toast.makeText(MainActivity.this, predicted + "", Toast.LENGTH_SHORT).show();
            imageView.setImageResource(emojis[predicted]);
            imageView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    private class UploadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String res = "";
            String d = strings[0];
            OkHttpClient client = new OkHttpClient();
            String url = "https://script.google.com/macros/s/AKfycbxYQ2AK_SSI6J9ean9QyNza1w8cpesByMcDU4-D8JARQ41Ld5dk/exec";
            FormBody body = new FormBody.Builder()
                    .add("action", "addItem")
                    .add("data", d)
                    .build();
            Request request = new Request.Builder().
                    post(body)
                    .url(url)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                res = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return res;
        }

        @Override
        protected void onPostExecute(String res) {
            Toast.makeText(MainActivity.this, res, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }
}

