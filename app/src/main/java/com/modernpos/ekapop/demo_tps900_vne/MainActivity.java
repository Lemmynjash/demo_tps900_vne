package com.modernpos.ekapop.demo_tps900_vne;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.printer.ThermalPrinter;

import java.io.File;
import java.util.Hashtable;


public class MainActivity extends AppCompatActivity {

    Button btnPrintContent;
    EditText txtContent,editTextLeftDistance,editTextLineDistance,editTextContent,editTextWordFont,editTextPrintGray,edittext_maker_search_distance,edittext_input_command,edittext_maker_walk_distance;
    TextView lbContent,textPrintVersion;

    private Boolean nopaper = false;
    private boolean LowBattery = false;

    private final int NOPAPER = 3;
    private final int LOWBATTERY = 4;
    private final int PRINTVERSION = 5;
    private final int PRINTBARCODE = 6;
    private final int PRINTQRCODE = 7;
    private final int PRINTPAPERWALK = 8;
    private final int PRINTCONTENT = 9;
    private final int CANCELPROMPT = 10;
    private final int PRINTERR = 11;
    private final int OVERHEAT = 12;
    private final int MAKER = 13;
    private final int PRINTPICTURE = 14;
    private final int EXECUTECOMMAND = 15;

    private int leftDistance = 0;
    private int lineDistance;
    private int wordFont;
    private int printGray;
    private final static int MAX_LEFT_DISTANCE = 255;
    private ProgressDialog progressDialog;

    public static String printContent;
    private static String printVersion;
    public static String barcodeStr;
    private String Result;
    public static String qrcodeStr;
    public static int paperWalk;

    MyHandler handler;
    ProgressDialog dialog;
    private String picturePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/111.bmp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new MyHandler();

        btnPrintContent = findViewById(R.id.btnPrintContent);
        txtContent = findViewById(R.id.txtContent);
        editTextLeftDistance = findViewById(R.id.editTextLeftDistance);
        editTextLineDistance = findViewById(R.id.editTextLineDistance);
        editTextContent = findViewById(R.id.editTextContent);
        editTextWordFont = findViewById(R.id.editTextWordFont);
        editTextPrintGray = findViewById(R.id.editTextPrintGray);
        edittext_maker_search_distance = findViewById(R.id.edittext_maker_search_distance);
        edittext_input_command = findViewById(R.id.edittext_input_command);
        edittext_maker_walk_distance = findViewById(R.id.edittext_maker_walk_distance);

        lbContent = findViewById(R.id.lbContent);
        textPrintVersion = findViewById(R.id.textPrintVersion);

        btnPrintContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String editText;
                editText = editTextLeftDistance.getText().toString();
                if (editText == null || editText.length() < 1) {
                    Toast.makeText(MainActivity.this, getString(R.string.left_margin) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG).show();
                    return;
                }
                leftDistance = Integer.parseInt(editText);
                editText = editTextLineDistance.getText().toString();
                if (editText == null || editText.length() < 1) {
                    Toast.makeText(MainActivity.this, getString(R.string.row_space) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG).show();
                    return;
                }
                lineDistance = Integer.parseInt(editText);
                printContent = editTextContent.getText().toString();
                editText = editTextWordFont.getText().toString();
                if (editText == null || editText.length() < 1) {
                    Toast.makeText(MainActivity.this, getString(R.string.font_size) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG).show();
                    return;
                }
                wordFont = Integer.parseInt(editText);
                editText = editTextPrintGray.getText().toString();
                if (editText == null || editText.length() < 1) {
                    Toast.makeText(MainActivity.this, getString(R.string.gray_level) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG).show();
                    return;
                }
                printGray = Integer.parseInt(editText);
                if (leftDistance > MAX_LEFT_DISTANCE) {
                    Toast.makeText(MainActivity.this, getString(R.string.outOfLeft), Toast.LENGTH_LONG).show();
                    return;
                } else if (lineDistance > 255) {
                    Toast.makeText(MainActivity.this, getString(R.string.outOfLine), Toast.LENGTH_LONG).show();
                    return;
                } else if (wordFont > 4 || wordFont < 1) {
                    Toast.makeText(MainActivity.this, getString(R.string.outOfFont), Toast.LENGTH_LONG).show();
                    return;
                } else if (printGray < 0 || printGray > 12) {
                    Toast.makeText(MainActivity.this, getString(R.string.outOfGray), Toast.LENGTH_LONG).show();
                    return;
                }
                if (printContent == null || printContent.length() == 0) {
                    Toast.makeText(MainActivity.this, getString(R.string.empty), Toast.LENGTH_LONG).show();
                    return;
                }
                if (LowBattery == true) {
                    handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
                } else {
                    if (!nopaper) {
                        progressDialog = ProgressDialog.show(MainActivity.this, getString(R.string.bl_dy), getString(R.string.printing_wait));
                        handler.sendMessage(handler.obtainMessage(PRINTCONTENT, 1, 0, null));
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.ptintInit), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setTitle(R.string.idcard_czz);
        dialog.setMessage(getText(R.string.watting));
        dialog.setCancelable(false);
        dialog.show();

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    ThermalPrinter.start(MainActivity.this);
                    ThermalPrinter.reset();
                    printVersion = ThermalPrinter.getVersion();
                } catch (TelpoException e) {
                    e.printStackTrace();
                } finally {
                    if (printVersion != null) {
                        Message message = new Message();
                        message.what = PRINTVERSION;
                        message.obj = "1";
                        handler.sendMessage(message);
                    } else {
                        Message message = new Message();
                        message.what = PRINTVERSION;
                        message.obj = "0";
                        handler.sendMessage(message);
                    }
                    ThermalPrinter.stop(MainActivity.this);
                }
            }
        }).start();
    }
    private void noPaperDlg() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        dlg.setTitle(getString(R.string.noPaper));
        dlg.setMessage(getString(R.string.noPaperNotice));
        dlg.setCancelable(false);
        dlg.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ThermalPrinter.stop(MainActivity.this);
            }
        });
        dlg.show();
    }
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOPAPER:
                    noPaperDlg();
                    break;
                case LOWBATTERY:
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setTitle(R.string.operation_result);
                    alertDialog.setMessage(getString(R.string.LowBattery));
                    alertDialog.setPositiveButton(getString(R.string.dialog_comfirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    alertDialog.show();
                    break;
                case PRINTVERSION:
                    dialog.dismiss();
                    if (msg.obj.equals("1")) {
                        textPrintVersion.setText(printVersion);
                    } else {
                        Toast.makeText(MainActivity.this, R.string.operation_fail, Toast.LENGTH_LONG).show();
                    }
                    break;
                case PRINTBARCODE:
                    new barcodePrintThread().start();
                    break;
                case PRINTQRCODE:
                    new qrcodePrintThread().start();
                    break;
                case PRINTPAPERWALK:
                    new paperWalkPrintThread().start();
                    break;
                case PRINTCONTENT:
                    new contentPrintThread().start();
                    break;
                case MAKER:
                    new MakerThread().start();
                    break;
                case PRINTPICTURE:
                    new printPicture().start();
                    break;
                case CANCELPROMPT:
                    if (progressDialog != null && !MainActivity.this.isFinishing()) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    break;
                case EXECUTECOMMAND:
                    new executeCommand().start();
                    break;
                case OVERHEAT:
                    AlertDialog.Builder overHeatDialog = new AlertDialog.Builder(MainActivity.this);
                    overHeatDialog.setTitle(R.string.operation_result);
                    overHeatDialog.setMessage(getString(R.string.overTemp));
                    overHeatDialog.setPositiveButton(getString(R.string.dialog_comfirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    overHeatDialog.show();
                    break;
                default:
                    Toast.makeText(MainActivity.this, "Print Error!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
    private class barcodePrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                ThermalPrinter.start(MainActivity.this);
                ThermalPrinter.reset();
                ThermalPrinter.setGray(printGray);
                Bitmap bitmap = CreateCode(barcodeStr, BarcodeFormat.CODE_128, 320, 176);
                if(bitmap != null){
                    ThermalPrinter.printLogo(bitmap);
                }
                ThermalPrinter.addString(barcodeStr);
                ThermalPrinter.printString();
                ThermalPrinter.walkPaper(100);
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
                    nopaper = true;
                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper){
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
                ThermalPrinter.stop(MainActivity.this);
            }
        }
    }
    private class qrcodePrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                ThermalPrinter.start(MainActivity.this);
                ThermalPrinter.reset();
                ThermalPrinter.setGray(printGray);
                Bitmap bitmap = CreateCode(qrcodeStr, BarcodeFormat.QR_CODE, 256, 256);
                if(bitmap != null){
                    ThermalPrinter.printLogo(bitmap);
                }
                ThermalPrinter.addString(qrcodeStr);
                ThermalPrinter.printString();
                ThermalPrinter.walkPaper(100);
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
                    nopaper = true;
                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper){
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
                ThermalPrinter.stop(MainActivity.this);
            }
        }
    }
    private class paperWalkPrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                ThermalPrinter.start(MainActivity.this);
                ThermalPrinter.reset();
                ThermalPrinter.walkPaper(paperWalk);
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
                    nopaper = true;
                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper){
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
                ThermalPrinter.stop(MainActivity.this);
            }
        }
    }
    private class contentPrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                ThermalPrinter.start(MainActivity.this);
                ThermalPrinter.reset();
                ThermalPrinter.setAlgin(ThermalPrinter.ALGIN_LEFT);
                ThermalPrinter.setLeftIndent(leftDistance);
                ThermalPrinter.setLineSpace(lineDistance);
                if (wordFont == 4) {
                    ThermalPrinter.setFontSize(2);
                    ThermalPrinter.enlargeFontSize(2, 2);
                } else if (wordFont == 3) {
                    ThermalPrinter.setFontSize(1);
                    ThermalPrinter.enlargeFontSize(2, 2);
                } else if (wordFont == 2) {
                    ThermalPrinter.setFontSize(2);
                } else if (wordFont == 1) {
                    ThermalPrinter.setFontSize(1);
                }
                ThermalPrinter.setGray(printGray);
                ThermalPrinter.addString(printContent);
                ThermalPrinter.printString();
                ThermalPrinter.walkPaper(100);
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
                    nopaper = true;
                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper){
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
                ThermalPrinter.stop(MainActivity.this);
            }
        }
    }
    private class MakerThread extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                ThermalPrinter.start(MainActivity.this);
                ThermalPrinter.reset();
                ThermalPrinter.searchMark(Integer.parseInt(edittext_maker_search_distance.getText().toString()),
                        Integer.parseInt(edittext_maker_walk_distance.getText().toString()));
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
                    nopaper = true;
                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper){
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
                ThermalPrinter.stop(MainActivity.this);
            }
        }
    }
    private class printPicture extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                ThermalPrinter.start(MainActivity.this);
                ThermalPrinter.reset();
                ThermalPrinter.setGray(printGray);
                ThermalPrinter.setAlgin(ThermalPrinter.ALGIN_MIDDLE);
                File file = new File(picturePath);
                if (file.exists()) {
                    ThermalPrinter.printLogo(BitmapFactory.decodeFile(picturePath));
                    ThermalPrinter.walkPaper(100);
                } else {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, getString(R.string.not_find_picture), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
                    nopaper = true;
                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper){
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
                ThermalPrinter.stop(MainActivity.this);
            }
        }
    }
    private class executeCommand extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                ThermalPrinter.start(MainActivity.this);
                ThermalPrinter.reset();
                ThermalPrinter.sendCommand(edittext_input_command.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
                    nopaper = true;
                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper){
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
                ThermalPrinter.stop(MainActivity.this);
            }
        }

    }

    /**
     * 生成条码     //barcode
     *
     * @param str
     *            条码内容      //เนื้อหาบาร์โค้ด
     * @param type
     *            条码类型： AZTEC, CODABAR, CODE_39, CODE_93, CODE_128, DATA_MATRIX,        //ประเภทบาร์โค้ด
     *            EAN_8, EAN_13, ITF, MAXICODE, PDF_417, QR_CODE, RSS_14,
     *            RSS_EXPANDED, UPC_A, UPC_E, UPC_EAN_EXTENSION;
     * @param bmpWidth
     *            生成位图宽,宽不能大于384，不然大于打印纸宽度      //สร้างความกว้างบิตแมปความกว้างต้องไม่เกิน 384 หรือใหญ่กว่าความกว้างของกระดาษ
     * @param bmpHeight
     *            生成位图高，8的倍数            //สร้างความสูงบิตแมปหลายเท่าจาก 8
     */
    public Bitmap CreateCode(String str, com.google.zxing.BarcodeFormat type, int bmpWidth, int bmpHeight) throws WriterException {
        Hashtable<EncodeHintType,String> mHashtable = new Hashtable<EncodeHintType,String>();
        mHashtable.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // 生成二维矩阵,编码时要指定大小,不要生成了图片以后再进行缩放,以防模糊导致识别失败
        //สร้างเมทริกซ์สองมิติระบุขนาดเมื่อเข้ารหัสอย่าสร้างภาพแล้วซูมในกรณีที่ความพร่ามัวทำให้การจดจำล้มเหลว
        BitMatrix matrix = new MultiFormatWriter().encode(str, type, bmpWidth, bmpHeight, mHashtable);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        // 二维矩阵转为一维像素数组（一直横着排）
        //เมทริกซ์สองมิติจะถูกแปลงเป็นอาร์เรย์พิกเซลหนึ่งมิติ (แนวนอนเสมอ)
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                } else {
                    pixels[y * width + x] = 0xffffffff;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 通过像素数组生成bitmap,具体参考api
        //สร้างบิตแมปจากอาร์เรย์พิกเซลอ้างอิง api
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
