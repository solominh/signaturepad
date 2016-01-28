package originally.us.signaturepad.signaturepad;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by hoangminh on 1/28/16.
 */
public class SignaturePadUtils {

    public static final int SCALE_TIME = 8;
    public static final int PX_DISTANCE = 1;

    public static Bitmap getTrimBitmapFast(Bitmap bitmap) {
        // Sanity check
        if (bitmap == null)
            return null;

        // Create smaller bitmap
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / SCALE_TIME, bitmap.getHeight() / SCALE_TIME, false);

        // Init data
        int scaleBitmapWidth = scaleBitmap.getWidth();
        int scaleBitmapHeight = scaleBitmap.getHeight();

        int backgroundColor = Color.TRANSPARENT;

        int xMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE;
        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;

        // Get pixel array
        int[][] bitmapArray = new int[scaleBitmapHeight][scaleBitmapWidth];
        for (int y = 0; y < scaleBitmapHeight; y++) {
            scaleBitmap.getPixels(bitmapArray[y], 0, scaleBitmapWidth, 0, y, scaleBitmapWidth, 1);
        }


        // Find bitmap bound
        boolean foundPixel = false;
        // Find xMin
        for (int x = 0; x < scaleBitmapWidth; x += PX_DISTANCE) {
            boolean stop = false;
            for (int y = 0; y < scaleBitmapHeight; y += PX_DISTANCE) {
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
        for (int y = 0; y < scaleBitmapHeight; y += PX_DISTANCE) {
            boolean stop = false;
            for (int x = xMin; x < scaleBitmapWidth; x += PX_DISTANCE) {
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
        for (int x = scaleBitmapWidth - 1; x >= xMin; x -= PX_DISTANCE) {
            boolean stop = false;
            for (int y = yMin; y < scaleBitmapHeight; y += PX_DISTANCE) {
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
        for (int y = scaleBitmapHeight - 1; y >= yMin; y -= PX_DISTANCE) {
            boolean stop = false;
            for (int x = xMin; x <= xMax; x += PX_DISTANCE) {
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
        int offset = PX_DISTANCE * 2;
        int left = Math.max(xMin - offset, 0);
        int top = Math.max(yMin - offset, 0);
        int right = Math.min(xMax + offset, scaleBitmapWidth);
        int bottom = Math.min(yMax + offset, scaleBitmapHeight);

        // Get real bitmap bound
        left *= SCALE_TIME;
        top *= SCALE_TIME;
        right *= SCALE_TIME;
        bottom *= SCALE_TIME;


        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }

}
