package com.fieldorg.quezbird.paint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;


public class PaintView extends View {

    public static int BRUSH_SIZE = 50;
    public static final int DEFAULT_COLOR = Color.BLACK;
    public static final int DEFAULT_BG_COLOR = Color.WHITE;
    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private ArrayList<FingerPath> paths = new ArrayList<>();
    private int currentColor;
    private int backgroundColor = DEFAULT_BG_COLOR;
    private int strokeWidth;
    private boolean emboss;
    private boolean blur;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    private int bigPixWidth;
    private int[] bigPixValues;
    private String[] screenWrite  = new String[11];
    private String bestGuess = "?";

    private final String[] W00 = getResources().getStringArray(R.array.W00);
    private final String[] W01 = getResources().getStringArray(R.array.W01);
    private static double[][] W0;
    private static double[] W1 = NNWeights.W1;
    private static double[][] W2 = NNWeights.W2;
    private static double[] W3 = NNWeights.W3;


    public PaintView(Context context) {
        this(context, null);
    }

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);

        mEmboss = new EmbossMaskFilter(new float[] {1, 1, 1}, 0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);

        bigPixValues = new int[28*28];

        for(int k = 0; k < 28*28; k++) {
            bigPixValues[k] = 0xFFFFFFFF;
        }
    }

    public void init(DisplayMetrics metrics) {
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = DEFAULT_COLOR;
        strokeWidth = BRUSH_SIZE;

        int H = mBitmap.getHeight();
        int W = mBitmap.getWidth();

        int w = (W - W%28)/28;
        int h = (H - H%28)/28;

        if(w < h){
            bigPixWidth = w;
        }else{
            bigPixWidth = h;
        }

        W0 = new double[28*28][128];

        //W0[0][0] = Double.parseDouble(W00[0]);


        //for(int j = 0; j < 128*(28*14); j++){
            //Double.parseDouble(W00[j]);
        //}
        //Double.parseDouble(W00[128*(28+13)]);
        //Double.parseDouble(W00[128*(28+13)+1]);

        //screenWrite = "###"+W00[128*(28+13)]+"###";

        for(int i = 0; i < screenWrite.length; i++){
            screenWrite[i] = "0";
        }

        for(int i = 0; i < 28*14; i++){
            double[] d = new double[128];
            for(int j = 0; j < 128; j++){

                d[j] = Double.parseDouble(W00[128*28*14-1]);

                d[j] = Double.parseDouble(W00[128*i+j]);


            }
            W0[i] = d;
        }

        for(int i = 0; i < 28*14; i++){
            double[] d = new double[128];
            for(int j = 0; j < 128; j++){
                d[j] = Double.parseDouble(W01[128*i+j]);
            }
            W0[28*14 + i] = d;
        }/**/
        //screenWrite = W00[0];

    }

    public void normal() {
        emboss = false;
        blur = false;
    }

    public void emboss() {
        emboss = true;
        blur = false;
    }

    public void blur() {
        emboss = false;
        blur = true;
    }

    public void clear() {
        backgroundColor = DEFAULT_BG_COLOR;
        paths.clear();
        normal();
        invalidate();
        for(int k = 0; k < 28*28; k++) {
            bigPixValues[k] = 0xFFFFFFFF;
        }
    }

    public void save28() {
        mCanvas.drawColor(backgroundColor);

        mPaint.setMaskFilter(null);
        mPaint.setStyle(Paint.Style.FILL);
        for(int j = 0; j < 28; j++){

            for(int i = 0; i < 28; i++) {
                mPaint.setColor(bigPixValues[28*i+j]);
                mCanvas.drawRect((float)((float)j)*bigPixWidth,(float)((float)i)*bigPixWidth,(float)((float)j+1)*bigPixWidth, (float)((float)i+1)*bigPixWidth, mPaint);
            }
        }
        mPaint.setStyle(Paint.Style.STROKE);

        for (FingerPath fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mPaint.setMaskFilter(null);

            if (fp.emboss)
                mPaint.setMaskFilter(mEmboss);
            else if (fp.blur)
                mPaint.setMaskFilter(mBlur);

            mCanvas.drawPath(fp.path, mPaint);

        }
        int minX=39, minY=39, maxX=-10, maxY=-10;
        for(int i = 0; i < 28; i++){
            for(int j = 0; j < 28; j++){

                int pixel_value_sum = 0;
                for(int x = 0; x < bigPixWidth; x++){
                    for(int y = 0; y < bigPixWidth; y++){
                        pixel_value_sum += mBitmap.getPixel(j*bigPixWidth +x,i*bigPixWidth +y)&0x000000FF;
                    }
                }
                pixel_value_sum /= (bigPixWidth*bigPixWidth);

                if((bigPixValues[28*i + j]&0x000000FF) != 0xFF){
                    pixel_value_sum += bigPixValues[28*i + j]&0x000000FF;
                    pixel_value_sum /= 2;
                }
                pixel_value_sum = 0xFF000000 + (pixel_value_sum<<16) + (pixel_value_sum<<8) + pixel_value_sum;

                if((pixel_value_sum &0x000000FF) < 0x20) { // round to black
                    pixel_value_sum = 0xFF000000;
                }
                if((pixel_value_sum &0x000000FF) > 0xa0){ // round to white
                    pixel_value_sum = 0xFFFFFFFF;
                }else{
                    if(i < minY){
                        minY = i;
                    }
                    if(i > maxY){
                        maxY = i;
                    }

                    if(j < minX){
                        minX = j;
                    }
                    if(j > maxX){
                        maxX = j;
                    }

                }


                bigPixValues[28*i + j] = pixel_value_sum;

            }
        }

        int shiftX = 13 -(minX+maxX)/2;
        int shiftY = 13 - (minY+maxY)/2;
        screenWrite[10] = "["+minX+","+maxX+"]"+shiftX;

        //shiftX = 3;

        int[] bigPV2 = bigPixValues.clone();

        for(int i = 0; i < 28; i++){
            for(int j = 0; j < 28; j++){
                if(28*(i-shiftY)+(j-shiftX) < 28*28 && 28*(i-shiftY)+(j-shiftX) >= 0){
                    bigPixValues[28*i+j] = bigPV2[28*(i-shiftY)+(j-shiftX)];
                }else{
                    bigPixValues[28*i+j] = 0xFFFFFFFF;
                }
            }
        }

        paths.clear();
        //nnOutput(bigPixValues);
        for(int k = 0; k < 10; k++){
            screenWrite[k] = ""+nnOutput(bigPixValues)[k];
        }


    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mCanvas.drawColor(backgroundColor);

        mPaint.setMaskFilter(null);
        mPaint.setStyle(Paint.Style.FILL);
        for(int j = 0; j < 28; j++){

            for(int i = 0; i < 28; i++) {
                mPaint.setColor(bigPixValues[28*i+j]);
                mCanvas.drawRect((float)((float)j)*bigPixWidth,(float)((float)i)*bigPixWidth,(float)((float)j+1)*bigPixWidth, (float)((float)i+1)*bigPixWidth, mPaint);


            }
        }

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(2);
        for(int j = 0; j < 29; j++){
            mCanvas.drawLine(j*bigPixWidth, 0, j*bigPixWidth,28*bigPixWidth, mPaint);
            mCanvas.drawLine(0,j*bigPixWidth, 28*bigPixWidth, j*bigPixWidth, mPaint);
        }


        for (FingerPath fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mPaint.setMaskFilter(null);

            if (fp.emboss)
                mPaint.setMaskFilter(mEmboss);
            else if (fp.blur)
                mPaint.setMaskFilter(mBlur);

            mCanvas.drawPath(fp.path, mPaint);

        }

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.BLACK);

        int textHeight = (mBitmap.getHeight()-28*bigPixWidth)/18;
        textHeight -= 2;
        mPaint.setTextSize(textHeight);
        for(int i = 0; i < 10; i++){
            mCanvas.drawText(i+" : ",5,28*bigPixWidth+(textHeight+2)*(i+1),mPaint);
            mCanvas.drawRect((float)5+textHeight*2,(float)28*bigPixWidth+(textHeight+2)*(i)+2,(float)(5+textHeight*2 + Double.parseDouble(screenWrite[i])*mBitmap.getWidth()*0.5), (float)28*bigPixWidth+(textHeight+2)*(i+1),mPaint);
            //mCanvas.drawRect(0.0f,(float)28*bigPixWidth+(textHeight+2)*(i),100f, (float)28*bigPixWidth+(textHeight+2)*(i)+ textHeight, mPaint);
        }



        mCanvas.drawText("+ : "+screenWrite[10],5,28*bigPixWidth+(textHeight+2)*(11),mPaint);

        textHeight = (mBitmap.getHeight()-28*bigPixWidth)-20;
        mPaint.setTextSize(textHeight);
        mCanvas.drawText(bestGuess,mBitmap.getWidth()-textHeight/2,mBitmap.getHeight()-textHeight/3,mPaint);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();



    }

    private void touchStart(float x, float y) {
        mPath = new Path();
        FingerPath fp = new FingerPath(currentColor, emboss, blur, strokeWidth, mPath);
        paths.add(fp);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
        save28();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN :
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE :
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP :
                touchUp();
                invalidate();
                break;
        }

        return true;
    }

    public double[] nnOutput(double[] img){
        //screenWrite[10] = ":"+W1[0]+"-";
        double[] dense = W1.clone();//W1; // set to biases




        for(int i = 0; i < 128; i++){

            for(int j = 0; j < 28*28; j++){
                dense[i] += img[j]*W0[j][i];
            }

            if(dense[i] < 0){
                dense[i] = 0;
            }
        }


        double[] r = W3.clone();

        for(int i = 0; i < 10; i++){

            for(int j = 0; j < 128; j++){
                r[i] += dense[j]*W2[j][i];
            }

            r[i] = Math.exp(r[i]);
        }

        double scale = 0;

        for(int i = 0; i < 10; i++){
            scale += r[i];
        }
        for(int i = 0; i < 10; i++){
            r[i] /= scale;
        }/**/
        //double[] r = new double[] {0.1,0.2};//

        double maxVal = r[0];

        for(int i = 0; i < 10; i++){
            if(r[i] >= maxVal){
                maxVal = r[i];
                bestGuess = ""+i;
            }
        }

        return r;
    }

    public double[] nnOutput(int[] img){
        double[] img1 = new double[28*28];

        for(int k = 0; k < 28*28; k++){
            img1[k] = 1 - ((double) (img[k]&0x000000FF))/255;
        }

        return nnOutput(img1);
    }
}