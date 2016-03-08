package originally.us.signaturepad.signaturepad;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import originally.us.originally.us.signaturepad.signaturepad.R;

/**
 * Created by hoangminh on 3/4/16.
 */
public class DrawingPad extends View implements DrawingOptions {

    // Current view
    private Bitmap mDrawingBitmap = null;
    private Canvas mDrawingBitmapCanvas = null;

    // For Erasing function
    private Bitmap mErasingBitmap;
    private Canvas mErasingBitmapCanvas;
    private boolean mIsInEraseMode = false;

    // For added image
    private Bitmap mPreloadBitmap = null;

    // Paint
    private Paint mDrawingPaint;
    private Paint mErasingPaint;
    private Paint mFillErasingPaint;

    // For undo and redo function
    private List<PathAndPaint> mMovePathList = new ArrayList<>();
    private List<PathAndPaint> mUndoPathList = new ArrayList<>();

    //-----------------------------------------------------------------------------
    // Init - hoangminh - 11:51 AM - 3/4/16
    //-----------------------------------------------------------------------------

    public DrawingPad(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Init Paint
        mDrawingPaint = createNewDrawingPaint(null);
        mErasingPaint = createNewErasingPaint(null);
        mFillErasingPaint = createNewFillErasingPaint(null);

        // Configurable parameters
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DrawingPad, 0, 0);
        try {
            float strokeWidth = a.getDimensionPixelSize(R.styleable.DrawingPad_stroke_width, dp2px(2));
            int strokeColor = a.getColor(R.styleable.DrawingPad_stroke_color, Color.BLACK);

            mDrawingPaint.setStrokeWidth(strokeWidth);
            mDrawingPaint.setColor(strokeColor);
        } finally {
            a.recycle();
        }

        // Drawing
        this.setOnTouchListener(new OnTouchToDrawSimplePath());
    }

    //-----------------------------------------------------------------------------
    // OnDraw - hoangminh - 12:00 PM - 3/4/16
    //-----------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawingBitmap != null) {
            canvas.drawBitmap(mDrawingBitmap, 0, 0, mDrawingPaint);
        }

        if (mIsInEraseMode && mErasingBitmap != null) {
            canvas.drawBitmap(mErasingBitmap, 0, 0, mDrawingPaint);
        }
    }

    //-----------------------------------------------------------------------------
    // OnTouchListener - hoangminh - 1:45 PM - 1/28/16
    //-----------------------------------------------------------------------------

    // Drawing mode
    private class OnTouchToDrawSimplePath implements View.OnTouchListener {
        private Path mDrawingPath;
        private float mPreviousX, mPreviousY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Create new path for array list cache new object (avoid reference error)
                    mDrawingPath = new Path();

                    // Cache path and paint
                    PathAndPaint pathAndPaint = new PathAndPaint(mDrawingPath, mDrawingPaint);
                    mMovePathList.add(pathAndPaint);

                    // Move to touching point
                    mDrawingPath.moveTo(eventX, eventY);

                    // Cache
                    mPreviousX = eventX;
                    mPreviousY = eventY;

                    ensureDrawingBitmap();
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Drawing path
                    mDrawingPath.lineTo(eventX, eventY);
                    mDrawingBitmapCanvas.drawLine(mPreviousX, mPreviousY, eventX, eventY, mDrawingPaint);

                    // Cache
                    mPreviousX = eventX;
                    mPreviousY = eventY;
                    break;

                case MotionEvent.ACTION_UP:
                    break;
                default:
                    return false;
            }
            invalidate();
            return true;
        }
    }

    // Erasing mode
    private class OnTouchToErase implements View.OnTouchListener {
        private Path mErasePath;

        private float mPreviousX, mPreviousY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Create new path for array list cache new object (avoid reference error)
                    mErasePath = new Path();

                    // Cache path and paint
                    PathAndPaint pathAndPaint = new PathAndPaint(mErasePath, mErasingPaint);
                    mMovePathList.add(pathAndPaint);

                    getParent().requestDisallowInterceptTouchEvent(true);
                    mPreviousX = eventX;
                    mPreviousY = eventY;
                    ensureErasingBitmap();

                    mErasePath.moveTo(eventX, eventY);
                    break;

                case MotionEvent.ACTION_MOVE:
                    mErasePath.lineTo(eventX, eventY);

                    mErasingBitmapCanvas.drawLine(mPreviousX, mPreviousY, eventX, eventY, mFillErasingPaint);
                    mPreviousX = eventX;
                    mPreviousY = eventY;
                    break;

                case MotionEvent.ACTION_UP:
                    mDrawingBitmapCanvas.drawBitmap(mErasingBitmap, 0, 0, mErasingPaint);
                    mErasingBitmap = null;
                    break;

                default:
                    return false;
            }
            invalidate();
            return true;
        }
    }

    //-----------------------------------------------------------------------------
    // Expose methods - hoangminh - 1:46 PM - 1/28/16
    //-----------------------------------------------------------------------------

    @Override
    public void clear() {
        removeDrawingBitmap();
        removeErasingBitmap();
        resetPathList();
    }

    @Override
    public void reset() {
        setPreloadBitmap(mPreloadBitmap);
        removeErasingBitmap();
        resetPathList();
    }

    @Override
    public void undo() {
        if (mMovePathList.size() <= 0)
            return;

        // Remove path from move list
        PathAndPaint pathAndPaint = mMovePathList.remove(mMovePathList.size() - 1);
        // Add path to undo list
        mUndoPathList.add(pathAndPaint);

        drawPathList();
        invalidate();
    }

    @Override
    public void redo() {
        if (mUndoPathList.size() <= 0)
            return;

        // Remove path from undo list
        PathAndPaint pathAndPaint = mUndoPathList.remove(mUndoPathList.size() - 1);
        // Add path to remove list
        mMovePathList.add(pathAndPaint);

        drawPathList();
        invalidate();
    }

    @Override
    public void enableErasingMode(int strokeWidth, @ColorRes int strokeColor) {
        if (strokeWidth > 0 || strokeColor > 0) {
            mErasingPaint = createNewErasingPaint(mErasingPaint);
            mFillErasingPaint = createNewFillErasingPaint(mFillErasingPaint);
            applySetting(mErasingPaint, strokeWidth, strokeColor);
            applySetting(mFillErasingPaint, strokeWidth, strokeColor);
        }

        mIsInEraseMode = true;
        this.setOnTouchListener(new OnTouchToErase());
    }

    @Override
    public void enableDrawingMode(int strokeWidth, @ColorRes int strokeColor) {
        if (strokeWidth > 0 || strokeColor > 0) {
            mDrawingPaint = createNewDrawingPaint(mDrawingPaint);
            applySetting(mDrawingPaint, strokeWidth, strokeColor);
        }

        mIsInEraseMode = false;
        this.setOnTouchListener(new OnTouchToDrawSimplePath());
    }


    //-----------------------------------------------------------------------------
    // Helper - hoangminh - 5:05 PM - 1/28/16
    //-----------------------------------------------------------------------------

    private void drawPathList() {
        // Reset bitmap
        setPreloadBitmap(mPreloadBitmap);

        // Draw path
        for (PathAndPaint model : mMovePathList) {
            mDrawingBitmapCanvas.drawPath(model.path, model.paint);
        }
    }

    private void applySetting(Paint paint, int strokeWidth, @ColorRes int strokeColor) {
        if (strokeWidth > 0)
            paint.setStrokeWidth(strokeWidth);
        if (strokeColor > 0)
            paint.setColor(getResources().getColor(strokeColor));
    }

    private void resetPathList() {
        mMovePathList.clear();
        mUndoPathList.clear();
    }

    private void removeDrawingBitmap() {
        if (mDrawingBitmap != null) {
            mDrawingBitmap = null;
            mDrawingBitmapCanvas = null;
        }

        invalidate();
    }

    private void removeErasingBitmap() {
        if (mErasingBitmap != null) {
            mErasingBitmap = null;
            mErasingBitmapCanvas = null;
        }

        invalidate();
    }

    //-----------------------------------------------------------------------------
    //- Drawing paint - hoangminh - 3:27 PM - 3/4/16

    private Paint createPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        return paint;
    }

    private Paint createNewDrawingPaint(Paint oldPaint) {
        Paint paint = createPaint();
        float strokeWidth = oldPaint == null || oldPaint.getStrokeWidth() <= 0 ? dp2px(2) : oldPaint.getStrokeWidth();
        int color = oldPaint == null || oldPaint.getColor() == 0 ? Color.BLACK : oldPaint.getColor();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        return paint;
    }

    private Paint createNewFillErasingPaint(Paint oldPaint) {
        Paint paint = createPaint();
        float strokeWidth = oldPaint == null || oldPaint.getStrokeWidth() <= 0 ? dp2px(30) : oldPaint.getStrokeWidth();
        int color = oldPaint == null || oldPaint.getColor() == 0 ? Color.YELLOW : oldPaint.getColor();
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(color);

        return paint;
    }

    private Paint createNewErasingPaint(Paint oldPaint) {
        Paint paint = createNewFillErasingPaint(oldPaint);

        // Important one
        PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
        paint.setXfermode(porterDuffXfermode);

        return paint;
    }

    //-----------------------------------------------------------------------------
    // Get and set bitmap - hoangminh - 1:47 PM - 1/28/16
    //-----------------------------------------------------------------------------

    //-----------------------------------------------------------------------------
    //- Get bitmap - hoangminh - 12:20 PM - 3/4/16

    public Bitmap getTransparentBitmap() {
        ensureDrawingBitmap();
        return mDrawingBitmap;
    }

    public Bitmap getTrimBitmap() {
        Bitmap transparentBitmap = getTransparentBitmap();
        return BitmapUtils.trimBitmap(transparentBitmap);
    }

    public Bitmap getBitmapWithBackgroundColor(@ColorInt int bgColor) {
        Bitmap originalBitmap = getTransparentBitmap();
        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(bgColor);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        return whiteBgBitmap;
    }

    //-----------------------------------------------------------------------------
    //- Set bitmap - hoangminh - 12:20 PM - 3/4/16

    // Warning: must wait for bitmap width and height available => ViewTreeObserver
    public void setPreloadBitmap(Bitmap bitmap) {
        // Clear bitmap
        mPreloadBitmap = bitmap;
        removeDrawingBitmap();
        ensureDrawingBitmap();

        // Sanity check
        if (bitmap == null)
            return;

        // Get drawMatrix
        RectF tempSrc = new RectF();
        RectF tempDst = new RectF();

        int dWidth = bitmap.getWidth();
        int dHeight = bitmap.getHeight();
        int vWidth = getWidth();
        int vHeight = getHeight();

        // Generate the required transform.
        tempSrc.set(0, 0, dWidth, dHeight);
        tempDst.set(0, 0, vWidth, vHeight);

        Matrix drawMatrix = new Matrix();
        drawMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);

        // Draw preload bitmap
        mDrawingBitmapCanvas.drawBitmap(bitmap, drawMatrix, null);

        invalidate();
    }

    //-----------------------------------------------------------------------------
    // Drawing utils - hoangminh - 1:48 PM - 1/28/16
    //-----------------------------------------------------------------------------

    private void ensureDrawingBitmap() {
        if (mDrawingBitmap != null)
            return;
        mDrawingBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mDrawingBitmapCanvas = new Canvas(mDrawingBitmap);
    }

    private void ensureErasingBitmap() {
        if (mErasingBitmap != null)
            return;
        mErasingBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mErasingBitmapCanvas = new Canvas(mErasingBitmap);
    }

    private int dp2px(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

}

