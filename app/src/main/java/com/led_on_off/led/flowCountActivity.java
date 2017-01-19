package com.led_on_off.led;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.opencsv.CSVWriter;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class flowCountActivity extends ActionBarActivity {
    //Declare fields
    private final static String TAG = "Activity::FlowCount";    //Tag for logging purposes
    private File imgFile;   //Directory for the app
    private String imgPath; //String path for the app directory
    private File resultsFile;   //Directory for the results

    ImageView imageView;    //ImageView to display any images or videos
    TextView outputText;    //TextView for title text at the top
    Button saveButton;  //Save button that appears once it's time to save the result
    Mat combinedMat;    //Matrix to store the combined imaged to be further tested

    private static final int PROCESS_INCOMPLETE = 0;
    private static final int IMG_UPDATE = 1;
    private static final int TEXT_UPDATE = 2;
    private static final int COMBINE_FRAME = 3;
    private static final int FRAMES_DECODED = 4;
    private String outputFileName;

    private static int minResult = 25;  //Minimum result to save

    //Configurations for optimization loops
    private static int[] blobThresh = {
            5,  //Threshold step
            90,    //Lowest min threshold
            120,    //Lowest max threshold
            120     //Highest max threshold
    };

    private static int[] blobArea = {
            15, //Lowest max area
            30  //Highest max area
    };

    private static int[] blobRepeatability = {
            2,  //Lowest min repeatability
            4   //Highest min repeatability
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_count);

        //Get image URI from previous intent
        Intent prevIntent = getIntent();
        Uri videoUri = prevIntent.getParcelableExtra("videoPath");
        outputFileName = getFileName(videoUri);

        //Declare files
        imgFile = new File(Environment.getExternalStorageDirectory(), "CellCount");    //Creates a CellCount folder for use in the phone
        imgPath = imgFile.getPath(); //Get the path of the directory in string form
        resultsFile = new File(imgPath + "/Results/" + outputFileName);

        //Create necessary directories
        imgFile.mkdirs();
        //resultsFile.mkdirs();

        //Set ImageView and TextView
        imageView = (ImageView) findViewById(R.id.resultsImgView);
        outputText = (TextView) findViewById(R.id.resultsTextView);
        saveButton = (Button) findViewById(R.id.saveButton);

        //Create new matrix to hold consolidated image
        combinedMat = new Mat();

        //Call function to perform processing on a separate thread
        decodeVideo(videoUri); //Decode the video using MediaCodec and an OpenGL surface
    }

    //Create handler to update UI thread using messages from other threads
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //Update image view with a matrix or bmp
            if(msg.what == IMG_UPDATE) {
                if(msg.obj instanceof Mat){
                    //Get matrix from message
                    Mat img = (Mat) msg.obj;

                    //Convert mat into bitmap
                    Bitmap bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(img, bitmap);

                    //Set bitmap to image view
                    imageView.setImageBitmap(bitmap);
                    //Log.v(TAG, "Updated image view with matrix");
                } else if (msg.obj instanceof Bitmap){
                    //Get bitmap from message
                    Bitmap bitmap = (Bitmap) msg.obj;

                    //Set bitmap to image view
                    imageView.setImageBitmap(bitmap);
                    //Log.v(TAG, "Updated image view with bitmap");
                }
            }
            //Update text view with a string
            else if(msg.what == TEXT_UPDATE){
                //Get string from message
                String textUpdate = (String) msg.obj;

                //Set the string to text view
                outputText.setText(textUpdate);
                //Log.v(TAG,"Updated text view");
            }
            //Update frame into consolidated image
            else if(msg.what == COMBINE_FRAME){
                //Convert mat into bitmap
                Bitmap combinedBmp = Bitmap.createBitmap(combinedMat.cols(), combinedMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(combinedMat, combinedBmp);

                //Set bitmap to image view
                imageView.setImageBitmap(combinedBmp);
                //Log.v(TAG, "Updated image view with matrix");

            }
            //Run blob detection
            else if(msg.what == FRAMES_DECODED){
                //Run blob detector
                processVideo(combinedMat,true); //True for loop, false for one-time
            }
        }
    };

    //Function to scan all frames and count cells
    public void processVideo(final Mat testMat, final boolean loopFlag){
        //Create a runnable for a new thread
        Runnable processRun = new Runnable() {
            @Override
            public void run() {
                //Declare fields
                Mat outImg;
                MatOfKeyPoint keypoints;
                FeatureDetector detector;

                int cellCount;
                String textUpdate;

                //Declare messages
                Message resultMsg;
                Message textMsg;

                //Covert image to 8-bit grayscale for analysis
                //Use different conversion depending on the number of channels (usually 3)
                int colorChannels = (testMat.channels() == 3) ? Imgproc.COLOR_BGR2GRAY : ((testMat.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);
                Imgproc.cvtColor(testMat,testMat,colorChannels);

                //Setup a simple blob detector
                detector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);

                //Create matrix to store key points
                keypoints = new MatOfKeyPoint();

                //Check if looping to optimise
                if(loopFlag){
                    //Create array to store all results
                    List<String[]> testData = new ArrayList<String[]>();

                    //Add in first line of header
                    testData.add(new String[] {"minThresh","maxThresh","maxBArea","minRepeat","cellCount"});

                    //Create file directory
                    resultsFile.mkdirs();

                    //Save consolidated image
                    if (Imgcodecs.imwrite(resultsFile.toString() + "/" + outputFileName + "_ConsolidatedImg.png",combinedMat)){
                        //Add entry to log
                        Log.d(TAG,"Consolidated image successfully written");
                    } else {
                        //Add entry to log
                        Log.e(TAG,"Failed to save consolidated image");
                    }

                    //Create counters
                    int loopCount = 0;
                    int maxCount = 0;

                    //Loop through pixel max threshold
                    for(int minThresh = blobThresh[1]; minThresh <= blobThresh[2]; minThresh++) {
                        for (int maxThresh = blobThresh[2]; maxThresh <= blobThresh[3]; maxThresh++) {
                            //Loop through blob max area
                            for (int maxBArea = blobArea[0]; maxBArea <= blobArea[1]; maxBArea++) {
                                //Loop through minimum repeatability
                                for (int minRepeat = blobRepeatability[0]; minRepeat <= blobRepeatability[1]; minRepeat++) {
                                    //Write parameters
                                    createParams(blobThresh[0],minThresh,maxThresh,minRepeat,maxBArea,imgPath);

                                    //Read parameters and detect blobs
                                    detector.read(imgPath + "/BlobParams.xml");
                                    detector.detect(testMat, keypoints);
                                    //Log parameters
                                    Log.d(TAG, "Detected for minTresh " + minThresh + ", maxThresh " + maxThresh + ", maxBArea " + maxBArea + ", repeatability " + minRepeat);

                                    //Draw new image with keypoints
                                    outImg = testMat.clone();
                                    Features2d.drawKeypoints(testMat, keypoints, outImg);


                                    //Count number of rows for keypoints mat
                                    cellCount = keypoints.rows();
                                    Log.d(TAG, "Cell count: " + cellCount); //Add cell count to log

                                    //Update max count
                                    if(cellCount > maxCount) maxCount = cellCount;

                                    //Only save if cell count hits minResult
                                    if(cellCount >= minResult) {
                                        //Save results
                                        if (Imgcodecs.imwrite(resultsFile.toString() + "/" + outputFileName + "_minT" + minThresh + "_maxT" + maxThresh + "_A" + maxBArea + "_R" + minRepeat + ".png", outImg)) {
                                            Log.d(TAG, "Image saved");
                                        } else {
                                            //Add error log if failed to save
                                            Log.e(TAG, "Failed to save results for _minT" + minThresh + "_maxT" + maxThresh + "_A" + maxBArea + "_R" + minRepeat + ".png");
                                        }


                                        //Record data into data array
                                        //Min thresh, max thresh, max blob area, min repeatability, cell count
                                        testData.add(new String[]{Double.toString(minThresh), Double.toString(maxThresh), Double.toString(maxBArea), Integer.toString(minRepeat), Integer.toString(cellCount)});

                                        //Update loop count
                                        loopCount += 1;

                                        //Update UI
                                        textUpdate = "Processing Iteration: " + loopCount;
                                        textMsg = handler.obtainMessage(TEXT_UPDATE,textUpdate);
                                        textMsg.sendToTarget();
                                    }
                                }
                            }
                        }
                    }


                    //Store results in CSV
                    try {
                        CSVWriter csvWriter = new CSVWriter(new FileWriter(resultsFile.toString() + "/" + outputFileName + "OptimiseResults.csv"));

                        csvWriter.writeAll(testData);
                        csvWriter.close();
                        Log.d(TAG,"CSV file written and closed");

                    } catch (IOException e){
                        e.printStackTrace();
                        Log.e(TAG,"CSV file failed to be written");
                    }

                    //Update UI on completion
                    textMsg = handler.obtainMessage(TEXT_UPDATE,"Analysis complete, max count: " + maxCount);
                    textMsg.sendToTarget();

                }
                //If not, just do a one-off calculation
                else {
                    //(Optional) Write parameters first
                    createParams(blobThresh[0],100,120,2,15,imgPath);

                    //Try to read parameters
                    detector.read(imgPath + "/BlobParams.xml");
                    Log.d(TAG,"Attempted to read parameters"); //Add entry to log

                    //Write current parameters to double-check
                    //detector.write(imgPath + "/RealParams.xml");

                    //Detect using parameters
                    detector.detect(testMat, keypoints);
                    Log.d(TAG,"Blob detector finished running"); //Add entry to log

                    //Draw new image with keypoints
                    outImg = testMat.clone();
                    Features2d.drawKeypoints(testMat,keypoints,outImg);

                    //Send output image with keypoints to imageview
                    resultMsg = handler.obtainMessage(IMG_UPDATE,outImg);
                    resultMsg.sendToTarget();

                    //Count number of rows for keypoints mat
                    cellCount = keypoints.rows();
                    Log.d(TAG,"Cell count: " + cellCount); //Add cell count to log

                    //Send final count to text view
                    textUpdate = "Cell count: " + cellCount;
                    textMsg = handler.obtainMessage(TEXT_UPDATE,textUpdate);
                    textMsg.sendToTarget();

                    //Set save image button to be visible
                    saveButton.setVisibility(View.VISIBLE);
                }
            }
        };

        //Create new thread to run "runnable"
        Thread processThread = new Thread(processRun);
        processThread.start();
    }

    //Function to decode video file
    public void decodeVideo(final Uri videoUri){
        Runnable decodeRunnable = new Runnable() {
            @Override
            public void run() {
                //Create DecodeVideo and its wrapper
                DecodeVideo decodeVideo = new DecodeVideo(imgFile,videoUri,getBaseContext(),handler, combinedMat);
                decodeVideo.callWrapper();
            }
        };

        //Start decoding on a new thread
        Thread wrapperThread = new Thread(decodeRunnable,"wrapperThread");
        wrapperThread.start();
    }

    //Function to create parameters file
    public void createParams(double thresholdStep, double minThreshold, double maxThreshold, int minRepeatability, double maxArea, String filePath) {

        //Default parameters for blob detector
        double minDistBetweenBlobs = 1;
        int filterByColor = 0;
        int blobColor = 0;
        int filterByArea = 1;
        double minArea = 2;
        int filterByCircularity = 0;
        double minCircularity = 8;
        double maxCircularity = 3.4;
        int filterByInertia = 0;
        double minInertiaRatio = 1;
        double maxInertiaRatio = 3.4;
        int filterByConvexity = 0;
        double minConvexity = 9.49;
        double maxConvexity = 3.4;


        //Try to create new document
        try{
            //Create DocumentBuilderFactory and DocumentBuilder
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            //Create document
            Document document = docBuilder.newDocument();

            //Create root "opencv_storage" tag
            Element rootOpenCV = document.createElement("opencv_storage");
            document.appendChild(rootOpenCV);

            //thresholdStep
            Element thresholdStepE = document.createElement("thresholdStep");
            thresholdStepE.appendChild(document.createTextNode(Double.toString(thresholdStep)));
            rootOpenCV.appendChild(thresholdStepE);

            //minThreshold
            Element minThresholdE = document.createElement("minThreshold");
            minThresholdE.appendChild(document.createTextNode(Double.toString(minThreshold)));
            rootOpenCV.appendChild(minThresholdE);

            //maxThreshold
            Element maxThresholdE = document.createElement("maxThreshold");
            maxThresholdE.appendChild(document.createTextNode(Double.toString(maxThreshold)));
            rootOpenCV.appendChild(maxThresholdE);

            //minRepeatability
            Element minRepeatabilityE = document.createElement("minRepeatability");
            minRepeatabilityE.appendChild(document.createTextNode(Double.toString(minRepeatability)));
            rootOpenCV.appendChild(minRepeatabilityE);

            //minDistBetweenBlobs
            Element minDistBetweenBlobsE = document.createElement("minDistBetweenBlobs");
            minDistBetweenBlobsE.appendChild(document.createTextNode(Double.toString(minDistBetweenBlobs)));
            rootOpenCV.appendChild(minDistBetweenBlobsE);

            //filterByColor
            Element filterByColorE = document.createElement("filterByColor");
            filterByColorE.appendChild(document.createTextNode(Integer.toString(filterByColor)));
            rootOpenCV.appendChild(filterByColorE);

            //blobColor
            Element blobColorE = document.createElement("blobColor");
            blobColorE.appendChild(document.createTextNode(Integer.toString(blobColor)));
            rootOpenCV.appendChild(blobColorE);

            //filterByArea
            Element filterByAreaE = document.createElement("filterByArea");
            filterByAreaE.appendChild(document.createTextNode(Integer.toString(filterByArea)));
            rootOpenCV.appendChild(filterByAreaE);

            //minArea
            Element minAreaE = document.createElement("minArea");
            minAreaE.appendChild(document.createTextNode(Double.toString(minArea)));
            rootOpenCV.appendChild(minAreaE);

            //maxArea
            Element maxAreaE = document.createElement("maxArea");
            maxAreaE.appendChild(document.createTextNode(Double.toString(maxArea)));
            rootOpenCV.appendChild(maxAreaE);

            //filterByCircularity
            Element filterByCircularityE = document.createElement("filterByCircularity");
            filterByCircularityE.appendChild(document.createTextNode(Integer.toString(filterByCircularity)));
            rootOpenCV.appendChild(filterByCircularityE);

            //minCircularity
            Element minCircularityE = document.createElement("minCircularity");
            minCircularityE.appendChild(document.createTextNode(Double.toString(minCircularity)));
            rootOpenCV.appendChild(minCircularityE);

            //maxCircularity
            Element maxCircularityE = document.createElement("maxCircularity");
            maxCircularityE.appendChild(document.createTextNode(Double.toString(maxCircularity)));
            rootOpenCV.appendChild(maxCircularityE);

            //filterByInertia
            Element filterByInertiaE = document.createElement("filterByInertia");
            filterByInertiaE.appendChild(document.createTextNode(Integer.toString(filterByInertia)));
            rootOpenCV.appendChild(filterByInertiaE);

            //minInertiaRatio
            Element minInertiaRatioE = document.createElement("minInertiaRatio");
            minInertiaRatioE.appendChild(document.createTextNode(Double.toString(minInertiaRatio)));
            rootOpenCV.appendChild(minInertiaRatioE);

            //maxInertiaRatio
            Element maxInertiaRatioE = document.createElement("maxInertiaRatio");
            maxInertiaRatioE.appendChild(document.createTextNode(Double.toString(maxInertiaRatio)));
            rootOpenCV.appendChild(maxInertiaRatioE);

            //filterByConvexity
            Element filterByConvexityE = document.createElement("filterByConvexity");
            filterByConvexityE.appendChild(document.createTextNode(Integer.toString(filterByConvexity)));
            rootOpenCV.appendChild(filterByConvexityE);

            //minConvexity
            Element minConvexityE = document.createElement("minConvexity");
            minConvexityE.appendChild(document.createTextNode(Double.toString(minConvexity)));
            rootOpenCV.appendChild(minConvexityE);

            //maxConvexity
            Element maxConvexityE = document.createElement("maxConvexity");
            maxConvexityE.appendChild(document.createTextNode(Double.toString(maxConvexity)));
            rootOpenCV.appendChild(maxConvexityE);

            //Transform output
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document),new StreamResult(new File(filePath + "/BlobParams.xml")));

        } catch (javax.xml.parsers.ParserConfigurationException e) {
            Log.e(TAG,"Parser configuration error");
            e.printStackTrace();
        } catch (javax.xml.transform.TransformerConfigurationException e){
            Log.e(TAG,"Transformer configuration error");
            e.printStackTrace();
        } catch (javax.xml.transform.TransformerException e){
            Log.e(TAG,"Transformer error");
            e.printStackTrace();
        }
        Log.d(TAG,"BlobParams.xml written");
    }


    //Function to save output image from ImageView
    public void saveImage(View view){
        //Create file directory
        resultsFile.mkdirs();

        //Write matrix image into the designated directory
        imageView.buildDrawingCache();
        Bitmap bmp = imageView.getDrawingCache();
        Mat img = new Mat();
        Utils.bitmapToMat(bmp,img);

        if (Imgcodecs.imwrite(resultsFile.toString() + "/" + outputFileName + "_Results.png",img)){
            //Add entry to log
            Log.d(TAG,"Image successfully written");
        } else {
            //Add entry to log
            Log.e(TAG,"Failed to save image");
        }
    }

    //Method to get filename from Uri
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {

            //Create an android cursor to access uri data
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                    int ext = result.lastIndexOf('.');  //Look for start of extension
                    result = result.substring(0,ext);   //Cut away extension
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }
}