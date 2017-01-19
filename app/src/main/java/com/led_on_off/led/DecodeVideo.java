package com.led_on_off.led;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by thoriqsalafi on 19/1/17.
 */
//Class to decode video
public class DecodeVideo {
    //Initialise fields
    private final static String TAG = "Class::DecodeVideo"; //Tag for logging purposes
    private MediaCodec.BufferInfo mBufferInfo;  //Buffer info
    private Context mContext;   //Context for setting MediaExtractor source
    private File mVideoFile;    //File directory for saving files
    private Uri mVideoUri;  //Uri for selected video
    private Handler mHandler;   //Handler to update the main UI thread
    private Mat mCombinedMat;   //Matrix that stores the consolidated image

    private String mimeFormat;  //String to store video MIME format
    private int vWidth; //Video width
    private int vHeight;    //Video height
    private int vRotation;  //Video rotation (degrees)

    private static final int PROCESS_INCOMPLETE = 0;
    private static final int IMG_UPDATE = 1;
    private static final int TEXT_UPDATE = 2;
    private static final int COMBINE_FRAME = 3;
    private static final int FRAMES_DECODED = 4;

    //Method to call wrapper
    public void callWrapper(){
        decodeWrapper.runDecode(this); //Run with this instance of DecodeVideo
    }

    //Create a wrapper to run DecodeVideo on a separate thread without a looper
    private static class decodeWrapper implements Runnable {
        //Initialise fields
        private DecodeVideo mDecodeVideo;

        //Constructor for the wrapper
        private decodeWrapper(DecodeVideo decodeVideo){
            mDecodeVideo = decodeVideo;
        }

        @Override
        public void run() {
            //Attempts to rune the decode method in the DecodeVideo class
            try {
                mDecodeVideo.decode();
            } catch (Throwable throwable){
                Log.e(TAG,"Failed to run 'decode' method in DecodeVideo");
            }
        }

        //Actual code to run the task
        private static void runDecode(DecodeVideo decodeVideo){
            decodeWrapper wrapper = new decodeWrapper(decodeVideo);
            Thread thread = new Thread(wrapper,"decodeThread");
            thread.start();
        }
    }

    //Constructor for DecodeVideo (with OpenCV matrix)
    public DecodeVideo(File videoFile, Uri videoUri, Context runContext, Handler handler, Mat combinedMat){
        mVideoFile = videoFile; //File directory
        mVideoUri = videoUri;   //Uri for selected video
        mContext = runContext;  //Context for MediaExtractor
        mHandler = handler; //Handler for main UI thread
        mCombinedMat = combinedMat; //Matrix to hold consolidated image
        mBufferInfo = new MediaCodec.BufferInfo();  //Create new buffer info
        Log.d(TAG,"Initialised DecodeVideo fields");
    }

    //Constructor for DecodeVideo (without OpenCV matrix)
    public DecodeVideo(File videoFile, Uri videoUri, Context runContext, Handler handler){
        mVideoFile = videoFile; //File directory
        mVideoUri = videoUri;   //Uri for selected video
        mContext = runContext;  //Context for MediaExtractor
        mHandler = handler; //Handler for main UI thread
        mBufferInfo = new MediaCodec.BufferInfo();  //Create new buffer info
        Log.d(TAG,"Initialised DecodeVideo fields");
    }



    //Method to decode the video
    public void decode(){
        //Initialise fields
        MediaCodec decoder = null;
        MediaExtractor extractor = null;
        MediaFormat format;
        CodecOutputSurface outputSurface = null;

        //Try to create decoder and extractor
        try {
            //Setup MediaExtractor
            extractor = new MediaExtractor();
            extractor.setDataSource(mContext,mVideoUri,null);   //Set source to video URI
            extractor.selectTrack(0); //Assumes video track is in track 0
            Log.d(TAG,"MediaExtractor source set");

            //Create MediaFormat from extractor and get key details
            format = extractor.getTrackFormat(0); //Assumes video track is in track 0
            vWidth = format.getInteger(MediaFormat.KEY_WIDTH);  //Get width
            vHeight = format.getInteger(MediaFormat.KEY_HEIGHT);    //Get height
            Log.d(TAG,"Video dimensions: " + vWidth + "x" + vHeight);

            //Create custom output surface
            outputSurface = new CodecOutputSurface(vWidth,vHeight,mHandler,mCombinedMat);

            //Extract MIME format
            mimeFormat = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG,"MIME Format: " + mimeFormat);

            //Create decoder from MediaCodec
            decoder = MediaCodec.createDecoderByType(mimeFormat);
            decoder.configure(format,outputSurface.getSurface(),null,0); //Get surface from custom surface
            decoder.start();

            //Run extraction method
            extract(extractor,decoder,outputSurface);   //Run actual extraction method

        } catch (IOException e){
            e.printStackTrace();
            Log.e(TAG,"Error setting up MediaExtractor or MediaCodec");
        } finally {
            //Release all resources if not already null
            if (outputSurface != null) {
                outputSurface.release();
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }
            if (extractor != null) {
                extractor.release();
            }
        }
    }

    //Method to extract frames until loop ends
    private void extract(MediaExtractor extractor, MediaCodec decoder, CodecOutputSurface outputSurface){
        //Initialise fields
        final int TIMEOUT_USEC = 10000; //Value for timeout in microseconds
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers(); //Deprecated array of byte buffers
        int inputCount = 0;
        int decodeCount = 0; //Effectively the frame count
        Message updateMsg;  //Message for the handler

        boolean outputDone = false; //Set default value to false
        boolean inputDone = false;  //Set default value to false

        //Start loop until output is done
        while(!outputDone){
            //Feed data to input buffer if input is not done
            if(!inputDone){
                //Get index of input buffer to be filled with data
                int inputBuffIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                //If buffer is available (not -1)
                if (inputBuffIndex >= 0) {
                    //Obtain byte buffer from array of bytebuffers
                    ByteBuffer inputBuffer = decoderInputBuffers[inputBuffIndex];
                    //Take encoded data and store it in the bytebuffer
                    int chunkSize = extractor.readSampleData(inputBuffer, 0);
                    //If chunk size is negative, end of stream has been reached
                    if (chunkSize < 0) {
                        //Send empty frame with EOS flag
                        decoder.queueInputBuffer(inputBuffIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;   //Set inputDone to true, end input
                        Log.d(TAG, "Sent input EOS");
                    }
                    //If chunk size is positive, queue input buffer
                    else {
                        //Store current presentation time into presentationTimeUs
                        inputBuffer.clear(); //Clears buffer before copying
                        long presentationTimeUs = extractor.getSampleTime(); //Get timestamp of frame

                        //Queue buffer into the decoder
                        decoder.queueInputBuffer(inputBuffIndex, 0, chunkSize, presentationTimeUs, 0);
                        Log.v(TAG, "Submitted frame " + inputCount + " to decoder, size=" + chunkSize);
                        inputCount++;   //Increase input count
                        extractor.advance();    //Advance to the next sample
                    }
                } else {
                    Log.e(TAG, "Input buffer not available");
                }
            }

            //Feed data to output buffer if output is not done
            if(!outputDone){
                //Get the decoder's status
                int decoderStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                //If no output, try again later
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.e(TAG, "No output from decoder available");
                }
                //If buffers changed, add a log entry (not important for surface)
                else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.e(TAG, "Decoder output buffers changed");
                }
                //If output format changed, output new format (will call once at the start)
                else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                    Log.e(TAG, "Decoder output format changed to : " + newFormat);
                }
                //If status is negative, throw runtime exception
                else if (decoderStatus < 0) {
                    throw new RuntimeException("Unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                }
                //If status is positive, proceed with decoding
                else {
                    Log.v(TAG, "Surface decoder given buffer " + decoderStatus + " (size=" + mBufferInfo.size + ")");
                    //If end of stream flag is detected, end code
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.e(TAG, "Received output EOS");
                        outputDone = true;  //Set outputDone to true and end output

                        //Send message to signal end of output
                        updateMsg = mHandler.obtainMessage(TEXT_UPDATE,"Decode completed");   //Update text view
                        updateMsg.sendToTarget();
                        updateMsg = mHandler.obtainMessage(FRAMES_DECODED);   //Update status and initiate next step
                        updateMsg.sendToTarget();
                    }

                    //Check for positive buffer size and release output buffer
                    boolean doRender = (mBufferInfo.size != 0);
                    decoder.releaseOutputBuffer(decoderStatus, doRender);   //Does not render to surface if false

                    //Proceed if doRender is true
                    if (doRender) {
                        Log.v(TAG, "Awaiting decode of frame " + decodeCount);
                        outputSurface.awaitNewImage();  //Wait for new frame
                        outputSurface.drawImage(true);  //Render frame onto surface
                        //outputSurface.saveFrame(outputFile.toString());

                        //Send message and process frame
                        updateMsg = mHandler.obtainMessage(TEXT_UPDATE,"Processing Frame " + decodeCount);
                        updateMsg.sendToTarget();
                        Log.v(TAG,"Sending frame to UI thread");
                        outputSurface.combineFrame();  //Call function to send current frame to UI thread
                    }
                    decodeCount++;  //Increase decode count
                }
            }
        }
    }

    //Custom OpenGL surface by BigFlake
    private static class CodecOutputSurface implements SurfaceTexture.OnFrameAvailableListener {
        private DecodeVideo.STextureRender mTextureRender;
        private SurfaceTexture mSurfaceTexture;
        private Surface mSurface;
        private Mat mCombinedMat;

        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
        int mWidth;
        int mHeight;

        //Default settings for target location (hardcoded for now)
        int[] xCoords = {498,540};  //3rd column from left
        int yCoord = 339;   //Near the edge

        private Handler mHandler;

        private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
        private boolean mFrameAvailable;

        private ByteBuffer mPixelBuf;                       // used by saveFrame() and combineFrame()

        //Constructor for CodecOutputSurface, creates a surface for MediaCodec
        public CodecOutputSurface(int width, int height, Handler handler, Mat combinedMat) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException();
            }
            mWidth = width;
            mHeight = height;
            mHandler = handler;
            mCombinedMat = combinedMat;

            eglSetup();     //Setup EGL (interface between OpenGL and Android
            makeCurrent();  //Make the surface current
            setup();    //Setup textures and surface
            Log.d(TAG,"Output surface created");
        }

        //Creates TextureRenderer, SurfaceTexture and Surface
        private void setup() {
            //Create a new instance of STextureRender
            mTextureRender = new DecodeVideo.STextureRender();
            mTextureRender.surfaceCreated();
            Log.d(TAG, "STextureRender ID = " + mTextureRender.getTextureId());

            //Create a new instance of SurfaceTexture using the renderer ID
            mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
            mSurfaceTexture.setOnFrameAvailableListener(this);  //Set listener to this instance

            //Create a new instace of Surface using the SurfaceTexture
            mSurface = new Surface(mSurfaceTexture);

            //Allocate buffer
            mPixelBuf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
            mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);   //Set order to Little Endian
        }

        //Prepare EGL
        private void eglSetup() {
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("Unable to get EGL14 display");
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                mEGLDisplay = null;
                throw new RuntimeException("Unable to initialize EGL14");
            }

            // Configure EGL for pbuffer and OpenGL ES 2.0, 24-bit RGB.
            int[] attribList = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
                throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
            }

            // Configure context for OpenGL ES 2.0.
            int[] attrib_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attrib_list, 0);
            checkEglError("eglCreateContext");
            if (mEGLContext == null) {
                throw new RuntimeException("null context");
            }

            // Create a pbuffer surface.
            int[] surfaceAttribs = {
                    EGL14.EGL_WIDTH, mWidth,
                    EGL14.EGL_HEIGHT, mHeight,
                    EGL14.EGL_NONE
            };
            mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);
            checkEglError("eglCreatePbufferSurface");
            if (mEGLSurface == null) {
                throw new RuntimeException("surface was null");
            }
        }

        //Discard resources
        public void release() {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }
            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLSurface = EGL14.EGL_NO_SURFACE;

            mSurface.release();
            mTextureRender = null;
            mSurface = null;
            mSurfaceTexture = null;
        }

        //Make EGL context and surface current
        public void makeCurrent() {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }

        //Return the surface for configuring MediaCodec
        public Surface getSurface() {
            return mSurface;
        }

        //Latch output buffer from MediaCodec onto texture
        public void awaitNewImage() {
            final int TIMEOUT_MS = 2500;

            //Starts await new image
            synchronized (mFrameSyncObject) {
                while (!mFrameAvailable) {
                    try {
                        Log.i(TAG,"Trying to wait for onFrameAvailable");
                        // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                        // stalling the test if it doesn't arrive.
                        mFrameSyncObject.wait(TIMEOUT_MS);
                        /*if (!mFrameAvailable) {
                            // TODO: if "spurious wakeup", continue while loop
                            Log.i(TAG,"No frame available");
                            throw new RuntimeException("frame wait timed out");
                        }*/
                    } catch (InterruptedException ie) {
                        // shouldn't happen
                        throw new RuntimeException(ie);
                    }
                }
                mFrameAvailable = false;
            }

            // Latch the data.
            mTextureRender.checkGlError("before updateTexImage");
            mSurfaceTexture.updateTexImage();
        }

        //Render SurfaceTexture onto the surface, invert=true inverts the Y axis
        public void drawImage(boolean invert) {
            mTextureRender.drawFrame(mSurfaceTexture, invert);
        }

        // SurfaceTexture callback
        @Override
        public void onFrameAvailable(SurfaceTexture st) {
            Log.v(TAG, "New frame available");
            synchronized (mFrameSyncObject) {
                if (mFrameAvailable) {
                    throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
                }
                mFrameAvailable = true;
                mFrameSyncObject.notifyAll();
            }
        }

        //Combine current frame into consolidated image
        public void combineFrame(){
            //Reset buffer position
            mPixelBuf.rewind();

            //Read pixels and create bitmap
            GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);
            try {
                Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                mPixelBuf.rewind();
                bmp.copyPixelsFromBuffer(mPixelBuf);

                //Convert bitmap into Mat
                Mat frameMat = new Mat();
                Utils.bitmapToMat(bmp,frameMat);

                //Combine frame into the final image
                combineImg(frameMat, mCombinedMat);

                //Update main thread
                Message updateMsg = mHandler.obtainMessage(COMBINE_FRAME);
                updateMsg.sendToTarget();
            } finally {
                Log.d(TAG,"Bitmap successfully generated");
            }
        }

        //Check for EGL errors
        private void checkEglError(String msg) {
            int error;
            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
            }
        }

        //Function to add current data into consolidated image
        public void combineImg(Mat frameMat, Mat targetMat)
        {
            //Obtain region of interest around selected pixels
            Rect roi = new Rect(xCoords[0],yCoord,xCoords[1] - xCoords[0] + 1,1);
            Mat frameROI = new Mat(frameMat,roi);

            //Push frameROI as an additional row
            targetMat.push_back(frameROI);
            Log.v(TAG,"New height: " + targetMat.height());
        }
    }

    //SurfaceTexture Renderer
    private static class STextureRender {
        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private final float[] mTriangleVerticesData = {
                // X, Y, Z, U, V
                -1.0f, -1.0f, 0, 0.f, 0.f,
                1.0f, -1.0f, 0, 1.f, 0.f,
                -1.0f,  1.0f, 0, 0.f, 1.f,
                1.0f,  1.0f, 0, 1.f, 1.f,
        };

        private FloatBuffer mTriangleVertices;

        private static final String VERTEX_SHADER =
                "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uSTMatrix;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "void main() {\n" +
                        "    gl_Position = uMVPMatrix * aPosition;\n" +
                        "    vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                        "}\n";

        private static final String FRAGMENT_SHADER =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +      // highp here doesn't seem to matter
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                        "}\n";

        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];

        private int mProgram;
        private int mTextureID = -12345;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;

        //Constructor for the renderer
        public STextureRender() {
            mTriangleVertices = ByteBuffer.allocateDirect(
                    mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

            Matrix.setIdentityM(mSTMatrix, 0);
        }

        //Return the texture ID
        public int getTextureId() {
            return mTextureID;
        }

        //Draw SurfaceTexture onto surface
        public void drawFrame(SurfaceTexture st, boolean invert) {
            checkGlError("onDrawFrame start");
            st.getTransformMatrix(mSTMatrix);
            if (invert) {
                mSTMatrix[5] = -mSTMatrix[5];
                mSTMatrix[13] = 1.0f - mSTMatrix[13];
            }

            // (optional) clear to green so we can see if we're failing to set pixels
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }

        //Initialise GL state
        public void surfaceCreated() {
            mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            if (mProgram == 0) {
                throw new RuntimeException("failed creating program");
            }

            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkLocation(maPositionHandle, "aPosition");
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkLocation(maTextureHandle, "aTextureCoord");

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkLocation(muMVPMatrixHandle, "uMVPMatrix");
            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
            checkLocation(muSTMatrixHandle, "uSTMatrix");

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
            checkGlError("glBindTexture mTextureID");

            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            checkGlError("glTexParameter");
        }

        //Replace fragment shader
        public void changeFragmentShader(String fragmentShader) {
            if (fragmentShader == null) {
                fragmentShader = FRAGMENT_SHADER;
            }
            GLES20.glDeleteProgram(mProgram);
            mProgram = createProgram(VERTEX_SHADER, fragmentShader);
            if (mProgram == 0) {
                throw new RuntimeException("failed creating program");
            }
        }

        //Load shader
        private int loadShader(int shaderType, String source) {
            int shader = GLES20.glCreateShader(shaderType);
            checkGlError("glCreateShader type=" + shaderType);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
            return shader;
        }

        //Create program
        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            if (program == 0) {
                Log.e(TAG, "Could not create program");
            }
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
            return program;
        }

        //Check for GL errors
        public void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }

        //Check if location is negative
        public static void checkLocation(int location, String label) {
            if (location < 0) {
                throw new RuntimeException("Unable to locate '" + label + "' in program");
            }
        }
    }
}

