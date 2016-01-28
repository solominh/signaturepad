package originally.us.signaturepad.signaturepad;

/**
 * Created by hoangminh on 12/11/15.
 */

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
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import originally.us.originally.us.signaturepad.signaturepad.R;


public class SignaturePad extends View {
    //View state
    private List<TimedPoint> mPoints;
    private boolean mIsEmpty;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mLastVelocity;
    private float mLastWidth;
    private RectF mDirtyRect;

    //Configurable parameters
    private int mMinWidth;
    private int mMaxWidth;
    private float mVelocityFilterWeight;
    private OnSignedListener mOnSignedListener;

    private Paint mDrawingPaint = new Paint();

    // For view
    private Bitmap mSignatureBitmap = null;

    private Canvas mSignatureBitmapCanvas = null;

    // For added image
    private Bitmap mPreloadBitmap = null;

    // For Erasing function
    private Bitmap mErasureBitmap;
    private Canvas mErasureBitmapCanvas;
    private boolean mIsInEraseMode = false;

    // For undo and redo function
    private List<Path> mMovePathList = new ArrayList<>();
    private List<Path> mUndoPathList = new ArrayList<>();

    public SignaturePad(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SignaturePad,
                0, 0);

        //Configurable parameters
        try {
            mMinWidth = a.getDimensionPixelSize(R.styleable.SignaturePad_minWidth, convertDpToPx(3));
            mMaxWidth = a.getDimensionPixelSize(R.styleable.SignaturePad_maxWidth, convertDpToPx(7));
            mVelocityFilterWeight = a.getFloat(R.styleable.SignaturePad_velocityFilterWeight, 0.9f);
            mDrawingPaint.setColor(a.getColor(R.styleable.SignaturePad_penColor, Color.BLACK));
        } finally {
            a.recycle();
        }

        //Fixed parameters
        mDrawingPaint.setAntiAlias(true);
        mDrawingPaint.setStyle(Paint.Style.STROKE);
        mDrawingPaint.setStrokeCap(Paint.Cap.ROUND);
        mDrawingPaint.setStrokeJoin(Paint.Join.ROUND);

        //Dirty rectangle to update only the changed portion of the view
        mDirtyRect = new RectF();

        clear();

        this.setOnTouchListener(new OnTouchToDrawSimplePath());
    }

    /**
     * Set the pen color from a given resource.
     * If the resource is not found, {@link Color#BLACK} is assumed.
     *
     * @param colorRes the color resource.
     */
    public void setPenColorRes(int colorRes) {
        try {
            setPenColor(getResources().getColor(colorRes));
        } catch (Resources.NotFoundException ex) {
            setPenColor(Color.BLACK);
        }
    }

    /**
     * Set the pen color from a given color.
     *
     * @param color the color.
     */
    public void setPenColor(int color) {
        mDrawingPaint.setColor(color);
    }

    /**
     * Set the minimum width of the stroke in pixel.
     *
     * @param minWidth the width in dp.
     */
    public void setMinWidth(float minWidth) {
        mMinWidth = convertDpToPx(minWidth);
    }

    /**
     * Set the maximum width of the stroke in pixel.
     *
     * @param maxWidth the width in dp.
     */
    public void setMaxWidth(float maxWidth) {
        mMaxWidth = convertDpToPx(maxWidth);
    }

    /**
     * Set the velocity filter weight.
     *
     * @param velocityFilterWeight the weight.
     */
    public void setVelocityFilterWeight(float velocityFilterWeight) {
        mVelocityFilterWeight = velocityFilterWeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mSignatureBitmap != null) {
            canvas.drawBitmap(mSignatureBitmap, 0, 0, mDrawingPaint);
        }

        if (mIsInEraseMode && mErasureBitmap != null) {
            canvas.drawBitmap(mErasureBitmap, 0, 0, mDrawingPaint);
        }

    }

    //-----------------------------------------------------------------------------
    // OnTouchListener - hoangminh - 1:45 PM - 1/28/16
    //-----------------------------------------------------------------------------

    private class OnTouchToDraw implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    mPoints.clear();
                    mLastTouchX = eventX;
                    mLastTouchY = eventY;
                    addPoint(new TimedPoint(eventX, eventY));

                case MotionEvent.ACTION_MOVE:
                    resetDirtyRect(eventX, eventY);
                    addPoint(new TimedPoint(eventX, eventY));
                    break;

                case MotionEvent.ACTION_UP:
                    resetDirtyRect(eventX, eventY);
                    addPoint(new TimedPoint(eventX, eventY));
                    getParent().requestDisallowInterceptTouchEvent(true);
                    setIsEmpty(false);
                    break;

                default:
                    return false;
            }

            //invalidate();
            invalidate(
                    (int) (mDirtyRect.left - mMaxWidth),
                    (int) (mDirtyRect.top - mMaxWidth),
                    (int) (mDirtyRect.right + mMaxWidth),
                    (int) (mDirtyRect.bottom + mMaxWidth));

            return true;
        }
    }

    private class OnTouchToErase implements OnTouchListener {
        private Paint mErasePaint = new Paint();
        private float mPreviousX, mPreviousY;

        public OnTouchToErase() {
            initPaint();
        }

        private void initPaint() {
            mErasePaint.setColor(Color.YELLOW);
            mErasePaint.setAntiAlias(true);
            mErasePaint.setStyle(Paint.Style.STROKE);
            mErasePaint.setStrokeCap(Paint.Cap.ROUND);
            mErasePaint.setStrokeJoin(Paint.Join.ROUND);
            mErasePaint.setStrokeWidth(30);
        }

        private void enableClearMode() {
            PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
            mErasePaint.setXfermode(porterDuffXfermode);
        }

        private void disableClearMode() {
            mErasePaint.setXfermode(null);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    mPreviousX = eventX;
                    mPreviousY = eventY;
                    disableClearMode();
                    createNewErasureBitmap();
                    break;

                case MotionEvent.ACTION_MOVE:
                    mErasureBitmapCanvas.drawLine(mPreviousX, mPreviousY, eventX, eventY, mErasePaint);
                    mPreviousX = eventX;
                    mPreviousY = eventY;
                    break;

                case MotionEvent.ACTION_UP:
                    enableClearMode();
                    mSignatureBitmapCanvas.drawBitmap(mErasureBitmap, 0, 0, mErasePaint);
                    mErasureBitmap = null;
                    break;

                default:
                    return false;
            }
            invalidate();
            return true;
        }
    }

    private class OnTouchToDrawSimplePath implements OnTouchListener {
        private Path mDrawingPath = new Path();

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float touchX = event.getX();
            float touchY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Move to touching point
                    mDrawingPath.moveTo(touchX, touchY);

                    // Cache path
                    mMovePathList.add(mDrawingPath);
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Drawing path
                    mDrawingPath.lineTo(touchX, touchY);
                    mSignatureBitmapCanvas.drawPath(mDrawingPath, mDrawingPaint);
                    break;
                case MotionEvent.ACTION_UP:
                    // Create new path for array list cache new object (avoid reference error)
                    mDrawingPath = new Path();
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

    public void clearAll(){
        clear();
        resetPathList();
    }

    public void reset() {
        setSignatureBitmap(mPreloadBitmap);
        resetPathList();
    }

    public void undo() {
        if (mMovePathList.size() <= 0)
            return;

        // Remove path from move list
        Path path = mMovePathList.remove(mMovePathList.size() - 1);
        // Add path to undo list
        mUndoPathList.add(path);

        drawPathList();
        invalidate();
    }

    public void redo() {
        if (mUndoPathList.size() <= 0)
            return;

        // Remove path from undo list
        Path path = mUndoPathList.remove(mUndoPathList.size() - 1);
        // Add path to remove list
        mMovePathList.add(path);

        drawPathList();
        invalidate();
    }

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

    private void resetPathList(){
        mMovePathList.clear();
        mUndoPathList.clear();
    }

    private void drawPathList() {
        // Reset bitmap
        setSignatureBitmap(mPreloadBitmap);

        // Draw path
        for (Path path : mMovePathList) {
            mSignatureBitmapCanvas.drawPath(path, mDrawingPaint);
        }
    }

    public void clear() {
        mPoints = new ArrayList<>();
        mLastVelocity = 0;
        mLastWidth = (mMinWidth + mMaxWidth) / 2;

        if (mSignatureBitmap != null) {
            mSignatureBitmap = null;
            ensureSignatureBitmap();
        }

        setIsEmpty(true);

        invalidate();
    }

//-----------------------------------------------------------------------------
// Listener - hoangminh - 1:46 PM - 1/28/16
//-----------------------------------------------------------------------------

    public interface OnSignedListener {
        void onSigned();

        void onClear();

    }

    public void setOnSignedListener(OnSignedListener listener) {
        mOnSignedListener = listener;
    }

    public boolean isEmpty() {
        return mIsEmpty;
    }

    //-----------------------------------------------------------------------------
    // Get and set bitmap - hoangminh - 1:47 PM - 1/28/16
    //-----------------------------------------------------------------------------

    public Bitmap getTransparentSignatureBitmap() {
        ensureSignatureBitmap();
        return mSignatureBitmap;
    }

    public Bitmap getSignatureBitmap() {
        Bitmap originalBitmap = getTransparentSignatureBitmap();
        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        return whiteBgBitmap;
    }

    public void setSignatureBitmap(Bitmap bitmap) {
        if (bitmap == null)
            return;
        mPreloadBitmap = bitmap;
        clear();
        ensureSignatureBitmap();

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

        mSignatureBitmapCanvas.drawBitmap(bitmap, drawMatrix, null);
        setIsEmpty(false);
        invalidate();
    }

    //-----------------------------------------------------------------------------
    // Drawing utils - hoangminh - 1:48 PM - 1/28/16
    //-----------------------------------------------------------------------------

    private void addPoint(TimedPoint newPoint) {
        mPoints.add(newPoint);
        if (mPoints.size() > 2) {
            // To reduce the initial lag make it work with 3 mPoints
            // by copying the first point to the beginning.
            if (mPoints.size() == 3) mPoints.add(0, mPoints.get(0));

            ControlTimedPoints tmp = calculateCurveControlPoints(mPoints.get(0), mPoints.get(1), mPoints.get(2));
            TimedPoint c2 = tmp.c2;
            tmp = calculateCurveControlPoints(mPoints.get(1), mPoints.get(2), mPoints.get(3));
            TimedPoint c3 = tmp.c1;
            Bezier curve = new Bezier(mPoints.get(1), c2, c3, mPoints.get(2));

            TimedPoint startPoint = curve.startPoint;
            TimedPoint endPoint = curve.endPoint;

            float velocity = endPoint.velocityFrom(startPoint);
            velocity = Float.isNaN(velocity) ? 0.0f : velocity;

            velocity = mVelocityFilterWeight * velocity + (1 - mVelocityFilterWeight) * mLastVelocity;


            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            float newWidth = strokeWidth(velocity);

            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mPoints.
            addBezier(curve, mLastWidth, newWidth);

            mLastVelocity = velocity;
            mLastWidth = newWidth;

            // Remove the first element from the list,
            // so that we always have no more than 4 mPoints in mPoints array.
            mPoints.remove(0);
        }
    }

    private void addBezier(Bezier curve, float startWidth, float endWidth) {
        ensureSignatureBitmap();
        float originalWidth = mDrawingPaint.getStrokeWidth();
        float widthDelta = endWidth - startWidth;
        float drawSteps = (float) Math.floor(curve.length());

        for (int i = 0; i < drawSteps; i++) {
            // Calculate the Bezier (x, y) coordinate for this step.
            float t = ((float) i) / drawSteps;
            float tt = t * t;
            float ttt = tt * t;
            float u = 1 - t;
            float uu = u * u;
            float uuu = uu * u;

            float x = uuu * curve.startPoint.x;
            x += 3 * uu * t * curve.control1.x;
            x += 3 * u * tt * curve.control2.x;
            x += ttt * curve.endPoint.x;

            float y = uuu * curve.startPoint.y;
            y += 3 * uu * t * curve.control1.y;
            y += 3 * u * tt * curve.control2.y;
            y += ttt * curve.endPoint.y;

            // Set the incremental stroke width and draw.
            mDrawingPaint.setStrokeWidth(startWidth + ttt * widthDelta);
            mSignatureBitmapCanvas.drawPoint(x, y, mDrawingPaint);
            expandDirtyRect(x, y);
        }

        mDrawingPaint.setStrokeWidth(originalWidth);
    }

    private ControlTimedPoints calculateCurveControlPoints(TimedPoint s1, TimedPoint s2, TimedPoint s3) {
        float dx1 = s1.x - s2.x;
        float dy1 = s1.y - s2.y;
        float dx2 = s2.x - s3.x;
        float dy2 = s2.y - s3.y;

        TimedPoint m1 = new TimedPoint((s1.x + s2.x) / 2.0f, (s1.y + s2.y) / 2.0f);
        TimedPoint m2 = new TimedPoint((s2.x + s3.x) / 2.0f, (s2.y + s3.y) / 2.0f);

        float l1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
        float l2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

        float dxm = (m1.x - m2.x);
        float dym = (m1.y - m2.y);
        float k = l2 / (l1 + l2);
        TimedPoint cm = new TimedPoint(m2.x + dxm * k, m2.y + dym * k);

        float tx = s2.x - cm.x;
        float ty = s2.y - cm.y;

        return new ControlTimedPoints(new TimedPoint(m1.x + tx, m1.y + ty), new TimedPoint(m2.x + tx, m2.y + ty));
    }

    private float strokeWidth(float velocity) {
        return Math.max(mMaxWidth / (velocity + 1), mMinWidth);
    }

    /**
     * Called when replaying history to ensure the dirty region includes all
     * mPoints.
     *
     * @param historicalX the previous x coordinate.
     * @param historicalY the previous y coordinate.
     */
    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < mDirtyRect.left) {
            mDirtyRect.left = historicalX;
        } else if (historicalX > mDirtyRect.right) {
            mDirtyRect.right = historicalX;
        }
        if (historicalY < mDirtyRect.top) {
            mDirtyRect.top = historicalY;
        } else if (historicalY > mDirtyRect.bottom) {
            mDirtyRect.bottom = historicalY;
        }
    }

    /**
     * Resets the dirty region when the motion event occurs.
     *
     * @param eventX the event x coordinate.
     * @param eventY the event y coordinate.
     */
    private void resetDirtyRect(float eventX, float eventY) {

        // The mLastTouchX and mLastTouchY were set when the ACTION_DOWN motion event occurred.
        mDirtyRect.left = Math.min(mLastTouchX, eventX);
        mDirtyRect.right = Math.max(mLastTouchX, eventX);
        mDirtyRect.top = Math.min(mLastTouchY, eventY);
        mDirtyRect.bottom = Math.max(mLastTouchY, eventY);
    }

    private void setIsEmpty(boolean newValue) {
        mIsEmpty = newValue;
        if (mOnSignedListener != null) {
            if (mIsEmpty) {
                mOnSignedListener.onClear();
            } else {
                mOnSignedListener.onSigned();
            }
        }
    }

    private void ensureSignatureBitmap() {
        if (mSignatureBitmap != null)
            return;
        mSignatureBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mSignatureBitmapCanvas = new Canvas(mSignatureBitmap);
    }

    private void createNewErasureBitmap() {
        mErasureBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mErasureBitmapCanvas = new Canvas(mErasureBitmap);
    }

    private int convertDpToPx(float dp) {
        return Math.round(dp * (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

}

