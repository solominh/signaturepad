package originally.us.signaturepad.signaturepad;

import android.content.Context;
import android.content.res.Resources;
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
public class DrawingPad extends View {

    // Current view
    private Bitmap mDrawingBitmap = null;
    private Canvas mDrawingBitmapCanvas = null;

    // For Erasing function
    private Bitmap mErasureBitmap;
    private Canvas mErasureBitmapCanvas;
    private boolean mIsInEraseMode = false;

    // For added image
    private Bitmap mPreloadBitmap = null;

    // Paint
    private Paint mDrawingPaint = new Paint();

    // For undo and redo function
    private List<PathAndPaint> mMovePathList = new ArrayList<>();
    private List<PathAndPaint> mUndoPathList = new ArrayList<>();

    //-----------------------------------------------------------------------------
    // Init - hoangminh - 11:51 AM - 3/4/16
    //-----------------------------------------------------------------------------

    public DrawingPad(Context context, AttributeSet attrs) {
        super(context, attrs);

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

        // Fixed parameters
        mDrawingPaint.setAntiAlias(true);
        mDrawingPaint.setStyle(Paint.Style.STROKE);
        mDrawingPaint.setStrokeCap(Paint.Cap.ROUND);
        mDrawingPaint.setStrokeJoin(Paint.Join.ROUND);

        clear();

        // Drawing
        this.setOnTouchListener(new OnTouchToDrawSimplePath());
    }

    //-----------------------------------------------------------------------------
    // Settings - hoangminh - 11:58 AM - 3/4/16
    //-----------------------------------------------------------------------------

    public void setStrokeColor(@ColorInt int color) {
        mDrawingPaint.setColor(color);
    }

    public void setStrokeColorRes(@ColorRes int colorRes) {
        try {
            setStrokeColor(getResources().getColor(colorRes));
        } catch (Resources.NotFoundException ex) {
            setStrokeColor(Color.BLACK);
        }
    }

    public void setStrokeWidth(float strokeWidth) {
        mDrawingPaint.setStrokeWidth(strokeWidth);
    }

    //-----------------------------------------------------------------------------
    // OnDraw - hoangminh - 12:00 PM - 3/4/16
    //-----------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawingBitmap != null) {
            canvas.drawBitmap(mDrawingBitmap, 0, 0, mDrawingPaint);
        }

        if (mIsInEraseMode && mErasureBitmap != null) {
            canvas.drawBitmap(mErasureBitmap, 0, 0, mDrawingPaint);
        }
    }

    //-----------------------------------------------------------------------------
    // OnTouchListener - hoangminh - 1:45 PM - 1/28/16
    //-----------------------------------------------------------------------------

    // Drawing mode
    private class OnTouchToDrawSimplePath implements View.OnTouchListener {
        private Path mDrawingPath;
        private Paint mDrawingPaint;
        private float mPreviousX, mPreviousY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Create path and paint
                    // Create new path for array list cache new object (avoid reference error)
                    mDrawingPath = new Path();
                    mDrawingPaint = createNewDrawingPaint();

                    // Cache path and paint
                    PathAndPaint pathAndPaint = new PathAndPaint(mDrawingPath, mDrawingPaint);
                    mMovePathList.add(pathAndPaint);

                    // Move to touching point
                    mDrawingPath.moveTo(eventX, eventY);

                    // Cache
                    mPreviousX = eventX;
                    mPreviousY = eventY;
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
        private Paint mTempErasePaint;

        private Path mErasePath;
        private Paint mErasePaint;

        private float mPreviousX, mPreviousY;

        public OnTouchToErase() {
            mTempErasePaint = createNewTempErasePaint();
            mErasePaint = createNewErasePaint();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Create path and paint
                    // Create new path for array list cache new object (avoid reference error)
                    mErasePath = new Path();

                    // Cache path and paint
                    PathAndPaint pathAndPaint = new PathAndPaint(mErasePath, mErasePaint);
                    mMovePathList.add(pathAndPaint);

                    getParent().requestDisallowInterceptTouchEvent(true);
                    mPreviousX = eventX;
                    mPreviousY = eventY;
                    createNewErasureBitmap();

                    mErasePath.moveTo(eventX, eventY);
                    break;

                case MotionEvent.ACTION_MOVE:
                    mErasePath.lineTo(eventX, eventY);

                    mErasureBitmapCanvas.drawLine(mPreviousX, mPreviousY, eventX, eventY, mTempErasePaint);
                    mPreviousX = eventX;
                    mPreviousY = eventY;
                    break;

                case MotionEvent.ACTION_UP:
                    mDrawingBitmapCanvas.drawBitmap(mErasureBitmap, 0, 0, mErasePaint);
                    mErasureBitmap = null;
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

    public void clear() {
        if (mDrawingBitmap != null) {
            mDrawingBitmap = null;
            ensureDrawingBitmap();
        }

        mMovePathList.clear();
        mUndoPathList.clear();

        invalidate();
    }

    public void reset() {
        setDrawingBitmap(mPreloadBitmap);
    }

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

    // Set erasing mode or drawing mode
    public void setErasing(boolean isErasing) {
        mIsInEraseMode = isErasing;

        if (isErasing)
            this.setOnTouchListener(new OnTouchToErase());
        else
            this.setOnTouchListener(new OnTouchToDrawSimplePath());
    }

    //-----------------------------------------------------------------------------
    // Helper - hoangminh - 5:05 PM - 1/28/16
    //-----------------------------------------------------------------------------

    private void drawPathList() {
        // Reset bitmap
        setDrawingBitmap(mPreloadBitmap);

        // Draw path
        for (PathAndPaint model : mMovePathList) {
            mDrawingBitmapCanvas.drawPath(model.path, model.paint);
        }
    }

    private Paint createNewDrawingPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(8f);
        return paint;
    }

    private Paint createNewTempErasePaint() {
        Paint paint = createNewDrawingPaint();
        paint.setStrokeWidth(30);
        paint.setColor(Color.YELLOW);

        return paint;
    }

    private Paint createNewErasePaint() {
        Paint paint = createNewTempErasePaint();
        PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
        paint.setXfermode(porterDuffXfermode);

        return paint;
    }

    //-----------------------------------------------------------------------------
    // Get and set bitmap - hoangminh - 1:47 PM - 1/28/16
    //-----------------------------------------------------------------------------

    //-----------------------------------------------------------------------------
    //- Get bitmap - hoangminh - 12:20 PM - 3/4/16

    public Bitmap getTransparentDrawingBitmap() {
        ensureDrawingBitmap();
        return mDrawingBitmap;
    }

    public Bitmap getTrimBitmap() {
        Bitmap transparentBitmap = getTransparentDrawingBitmap();
        return BitmapUtils.trimBitmap(transparentBitmap);
    }

    public Bitmap getWithBgDrawingBitmap(@ColorInt int bgColor) {
        Bitmap originalBitmap = getTransparentDrawingBitmap();
        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(bgColor);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        return whiteBgBitmap;
    }

    //-----------------------------------------------------------------------------
    //- Set bitmap - hoangminh - 12:20 PM - 3/4/16

    public void setDrawingBitmap(Bitmap bitmap) {
        // Sanity check
        if (bitmap == null)
            return;

        // Clear bitmap
        mPreloadBitmap = bitmap;
        clear();

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

    private void createNewErasureBitmap() {
        mErasureBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mErasureBitmapCanvas = new Canvas(mErasureBitmap);
    }

    private int dp2px(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

}

