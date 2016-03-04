package originally.us.signaturepad.signaturepad;

import android.support.annotation.ColorRes;

/**
 * Created by hoangminh on 3/4/16.
 */
public interface DrawingOptions {
    void clear();

    void reset();

    void undo();

    void redo();

    void enableErasingMode(int strokeWidth, @ColorRes int strokeColor);

    void enableDrawingMode(int strokeWidth, @ColorRes int strokeColor);
}
