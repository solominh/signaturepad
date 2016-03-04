package originally.us.signaturepad.signaturepad;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by hoangminh on 1/28/16.
 */
public class BitmapUtils {


    public static final int SCAN_INTERVAL = 1;  // pixel

    public static Bitmap trimBitmap(Bitmap bitmap) {
        // Sanity check
        if (bitmap == null)
            return null;

        // Create smaller bitmap if bitmap too big
        int scale = 1;
        if (bitmap.getWidth() > 500) {
            scale = 8;
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / scale, bitmap.getHeight() / scale, false);
        }

        // Find bounds
        int[] bounds = findBoundsOfVisibleBitmap(bitmap);

        // Get real bitmap bound
        int left = bounds[0] * scale;
        int top = bounds[1] * scale;
        int right = bounds[2] * scale;
        int bottom = bounds[3] * scale;

        // Trim bitmap
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }

    public static int[] findBoundsOfVisibleBitmap(Bitmap bitmap) {
        // Sanity check
        if (bitmap == null)
            return null;

        // Init data
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // Color
        int backgroundColor = Color.TRANSPARENT;

        // Bounds
        int xMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE;
        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;

        // Get pixel array
        int[][] bitmapArray = new int[bitmapHeight][bitmapWidth];
        for (int y = 0; y < bitmapHeight; y++) {
            bitmap.getPixels(bitmapArray[y], 0, bitmapWidth, 0, y, bitmapWidth, 1);
        }

        // Find bitmap bound
        boolean foundPixel = false;
        // Find xMin
        for (int x = 0; x < bitmapWidth; x += SCAN_INTERVAL) {
            boolean stop = false;
            for (int y = 0; y < bitmapHeight; y += SCAN_INTERVAL) {
                if (bitmapArray[y][x] != backgroundColor) {
                    xMin = x;
                    stop = true;
                    foundPixel = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Image is empty...
        if (!foundPixel)
            return null;

        // Find yMin
        for (int y = 0; y < bitmapHeight; y += SCAN_INTERVAL) {
            boolean stop = false;
            for (int x = xMin; x < bitmapWidth; x += SCAN_INTERVAL) {
                if (bitmapArray[y][x] != backgroundColor) {
                    yMin = y;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Find xMax
        for (int x = bitmapWidth - 1; x >= xMin; x -= SCAN_INTERVAL) {
            boolean stop = false;
            for (int y = yMin; y < bitmapHeight; y += SCAN_INTERVAL) {
                if (bitmapArray[y][x] != backgroundColor) {
                    xMax = x;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Find yMax
        for (int y = bitmapHeight - 1; y >= yMin; y -= SCAN_INTERVAL) {
            boolean stop = false;
            for (int x = xMin; x <= xMax; x += SCAN_INTERVAL) {
                if (bitmapArray[y][x] != backgroundColor) {
                    yMax = y;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Expand bitmap with offset
        int offset = SCAN_INTERVAL;
        int left = Math.max(xMin - offset, 0);
        int top = Math.max(yMin - offset, 0);
        int right = Math.min(xMax + offset, bitmapWidth);
        int bottom = Math.min(yMax + offset, bitmapHeight);

        int[] bound = {left, top, right, bottom};
        return bound;
    }
}
