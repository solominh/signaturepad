package originally.us.signaturepad;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import originally.us.originally.us.signaturepad.signaturepad.R;
import originally.us.signaturepad.signaturepad.SignaturePad;

public class MainActivity extends AppCompatActivity {

    public static final float SIGNATURE_MIN_STROKE = 2f;
    public static final float SIGNATURE_MAX_STROKE = 4f;

    private SignaturePad mSignaturePad;
    private Button mBtnClear, mBtnSave, mBtnReset;
    private Button mBtnUndo, mBtnRedo;
    private Button mBtnErase, mBtnBrush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
        bindData();
    }

    protected void initUi() {
        mSignaturePad = (SignaturePad) findViewById(R.id.signature_pad);

        mBtnClear = (Button) findViewById(R.id.btn_clear);
        mBtnSave = (Button) findViewById(R.id.btn_save);
        mBtnReset = (Button) findViewById(R.id.btn_reset);

        mBtnUndo = (Button) findViewById(R.id.btn_undo);
        mBtnRedo = (Button) findViewById(R.id.btn_redo);

        mBtnBrush = (Button) findViewById(R.id.btn_brush);
        mBtnErase = (Button) findViewById(R.id.btn_erase);
    }

    protected void bindData() {
        mSignaturePad.setVelocityFilterWeight(10);

        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignaturePad.clear();
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
                mSignaturePad.reset();
            }
        });

        mBtnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignaturePad.undo();
            }
        });

        mBtnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignaturePad.redo();
            }
        });

        mBtnErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignaturePad.setErasing(true);
            }
        });

        mBtnBrush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignaturePad.setErasing(false);
            }
        });

        initSignaturePad();
    }

    private void initSignaturePad() {
        mSignaturePad.setMinWidth(SIGNATURE_MIN_STROKE);
        mSignaturePad.setMaxWidth(SIGNATURE_MAX_STROKE);
        mSignaturePad.setPenColor(Color.BLACK);
        loadDrawingImage();
    }

    private void loadDrawingImage() {
        mSignaturePad.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                mSignaturePad.setSignatureBitmap(bitmap);
            }
        });
    }

    private void saveDrawingImage() {
        Bitmap bitmap = mSignaturePad.getTrimBitmap();
    }

}
