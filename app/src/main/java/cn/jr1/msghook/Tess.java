package cn.jr1.msghook;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.IOException;
import java.util.Date;


public class Tess {

    public  static  final  String TAG = "TESS";

    public static String parseImageToString(String imagePath) throws IOException {
        // 检验图片地址是否正确
        if (imagePath == null || imagePath.equals("")) {
            return "image path not found";
        }
        // 获取Bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        // 图片旋转角度
        int rotate = 0;
        ExifInterface exif = new ExifInterface(imagePath);
        // 先获取当前图像的方向，判断是否需要旋转
        int imageOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Log.e(TAG, "Current image orientation is " + imageOrientation);
        switch (imageOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            default:
                break;
        }
        Log.i(TAG, "Current image need rotate: " + rotate);
        // 获取当前图片的宽和高
//        int w = bitmap.getWidth();
//        int h = bitmap.getHeight();
//        // 使用Matrix对图片进行处理
//        Matrix mtx = new Matrix();
//        mtx.preRotate(rotate);
        // 旋转图片
//        bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
//        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        // 开始调用Tess函数对图像进行识别

        TessBaseAPI baseApi = new TessBaseAPI();
//        baseApi.setDebug(true);
        baseApi.setDebug(false);
        // 使用默认语言初始化BaseApi
        long   t1 = new Date().getTime();
        baseApi.init("/sdcard/Pictures/Screenshots/","eng" );
        baseApi.setImage(bitmap);
        // 获取返回值
        String recognizedText = baseApi.getUTF8Text();
        long   t2 = new Date().getTime();
        baseApi.end();
        Log.e("rectime",String.valueOf(t2-t1));

        return recognizedText;
    }
}

