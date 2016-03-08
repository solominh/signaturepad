package originally.us.signaturepad;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import originally.us.originally.us.signaturepad.signaturepad.R;
import originally.us.signaturepad.signaturepad.BitmapUtils;
import originally.us.signaturepad.signaturepad.DrawingPad;

public class DrawingPadActivity extends AppCompatActivity {

    private DrawingPad mDrawingPad;
    private Button mBtnClear, mBtnSave, mBtnReset;
    private Button mBtnUndo, mBtnRedo;
    private Button mBtnErase, mBtnBrush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing_pad);

        initUi();
        bindData();
    }

    protected void initUi() {
        mDrawingPad = (DrawingPad) findViewById(R.id.drawing_pad);
        mBtnClear = (Button) findViewById(R.id.btn_clear);
        mBtnSave = (Button) findViewById(R.id.btn_save);
        mBtnReset = (Button) findViewById(R.id.btn_reset);

        mBtnUndo = (Button) findViewById(R.id.btn_undo);
        mBtnRedo = (Button) findViewById(R.id.btn_redo);

        mBtnBrush = (Button) findViewById(R.id.btn_brush);
        mBtnErase = (Button) findViewById(R.id.btn_erase);

    }

    protected void bindData() {
        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawingPad.clear();
            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDrawingImage();
            }
        });


        mBtnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawingPad.reset();
            }
        });

        mBtnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawingPad.undo();
            }
        });

        mBtnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawingPad.redo();
            }
        });

        mBtnErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawingPad.enableErasingMode(0, 0);
            }
        });

        mBtnBrush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawingPad.enableDrawingMode(0, 0);
            }
        });

        initSignaturePad();
    }

    private void initSignaturePad() {
        loadDrawingImage();
    }

    private void loadDrawingImage() {
        mDrawingPad.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//                mDrawingPad.setPreloadBitmap(bitmap);
            }
        });
    }

    private void saveDrawingImage() {
        Bitmap bitmap = mDrawingPad.getTransparentBitmap();
        BitmapUtils.trimBitmap(bitmap);
    }

}