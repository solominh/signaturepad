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

    private SignaturePad signaturePad;
    private Button btnClear, btnSave, btnReset;
    private Button btnUndo, btnRedo;
    private Button btnErase, btnBrush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
        bindData();
    }

    protected void initUi() {
        signaturePad = (SignaturePad) findViewById(R.id.signature_pad);
        btnClear = (Button) findViewById(R.id.btn_clear);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnReset = (Button) findViewById(R.id.btn_reset);

        btnUndo = (Button) findViewById(R.id.btn_undo);
        btnRedo = (Button) findViewById(R.id.btn_redo);

        btnBrush = (Button) findViewById(R.id.btn_brush);
        btnErase = (Button) findViewById(R.id.btn_erase);
    }

    protected void bindData() {
        signaturePad.setVelocityFilterWeight(10);

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.clearAll();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDrawingImage();
            }
        });


        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.reset();
            }
        });

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.undo();
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.redo();
            }
        });

        btnErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.setErasing(true);
            }
        });

        btnBrush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.setErasing(false);
            }
        });

        initSignaturePad();
    }

    private void initSignaturePad() {
        signaturePad.setMinWidth(SIGNATURE_MIN_STROKE);
        signaturePad.setMaxWidth(SIGNATURE_MAX_STROKE);
        signaturePad.setPenColor(Color.BLACK);
        loadDrawingImage();
    }

    private void loadDrawingImage() {
        signaturePad.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                signaturePad.setSignatureBitmap(bitmap);
            }
        });
    }

    private void saveDrawingImage() {

    }

}
