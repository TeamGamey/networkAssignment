package com.example.networkapp;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DownloadActivity extends Activity {
    Button downloadButton;
    EditText logHeader;
    TextView statusTextView;
    TextView throughputTextView;
    TextView latencyTextView;

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            statusTextView.setText("Working...");
            downloadButton.setEnabled(false);
            appendLog("");
            appendLog("Header: " + logHeader.getText().toString());
            DownloadFile downloadFile = new DownloadFile();
            downloadFile.execute("http://web.mit.edu/21w.789/www/papers/griswold2004.pdf");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        downloadButton = (Button) findViewById(R.id.button1);
        downloadButton.setOnClickListener(clickListener);
        logHeader = (EditText) findViewById(R.id.editText1);
        statusTextView = (TextView) findViewById(R.id.TextView01);
        throughputTextView = (TextView) findViewById(R.id.TextView02);
        latencyTextView = (TextView) findViewById(R.id.TextView03);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_download, menu);
        return true;
    }

    private class DownloadFile extends AsyncTask<String, Integer, String> {
        Long latency;
        Double throughput;

        @Override
        protected String doInBackground(String... sUrl) {
            long startTime = System.currentTimeMillis();
            appendLog("New log timestamp at " + Calendar.getInstance().getTime().toGMTString());
            try {
                URL url = new URL(sUrl[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                // this will be useful so that you can show a typical 0-100%
                // progress bar
                int fileLength = connection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream());
                // overwrite the file since we care only of statistics
                OutputStream output = new FileOutputStream("sdcard/21w-new.pdf");

                final int MILLISECONDS_IN_PERIOD = 5000;
                final int BYTES_PER_STEP = 512;
                final double BYTES_IN_KB = 1024.0;

                byte data[] = new byte[BYTES_PER_STEP];
                int count;
                int currentPeriod = 0;
                long bytesDownloaded = 0;
                long previousMilliseconds = 0;
                long previousBytesDownloaded = 0;
                boolean firstIteration = true;
                while ((count = input.read(data)) != -1) {
                    if (firstIteration) {
                        firstIteration = false;
                        latency = (System.currentTimeMillis() - startTime);
                        currentPeriod = (int) (latency / MILLISECONDS_IN_PERIOD);
                        appendLog("Latency: " + (System.currentTimeMillis() - startTime) + " (ms)");
                    }

                    bytesDownloaded += count;
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime >= (currentPeriod + 1) * MILLISECONDS_IN_PERIOD) {
                        currentPeriod++;
                        long milliseconds = (System.currentTimeMillis() - startTime);
                        String logDataString = (System.currentTimeMillis() - startTime) + "ms : "
                                + bytesDownloaded + "/" + fileLength + " bytes.";
                        appendLog(logDataString);
                        double throughput = ((bytesDownloaded - previousBytesDownloaded) / BYTES_IN_KB)
                                / ((milliseconds - previousMilliseconds) * 0.001);
                        appendLog("Throughput last 5 s: " + throughput + " (KB/s)");

                        previousMilliseconds = milliseconds;
                        previousBytesDownloaded = bytesDownloaded;
                    }
                    // publishing the progress....
                    // publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }

                throughput = (fileLength / BYTES_IN_KB)
                        / (0.001 * (System.currentTimeMillis() - startTime));
                appendLog("Overall average throughput: " + throughput);

                output.flush();
                output.close();
                input.close();

            } catch (Exception e) {
            }
            
            
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // mProgressDialog.setProgress(progress[0]);
        }
        
        protected void onPostExecute(String result) {
            downloadButton.setEnabled(true);
            statusTextView.setText("Done...");
            throughputTextView.setText(throughput.toString());
            latencyTextView.setText(latency.toString());
        }
    }

    /**
     * Adds to a log file
     * 
     * @param text
     */
    public void appendLog(String text) {
        // move this to on start
        File logFile = new File("sdcard/log.file"); // create log file with
                                                    // date/time
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            // BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
