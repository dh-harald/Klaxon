package com.koushikdutta.klaxon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public class WallpaperHelper {
	public static void setWindowBackground(Activity activity)
	{
    	Class style = android.R.style.class;
    	try {
			java.lang.reflect.Field field = style.getField("Theme_Wallpaper_NoTitleBar");
			activity.setTheme(field.getInt(null));
			return;
		} catch (Exception e) {
			activity.setTheme(android.R.style.Theme_NoTitleBar);
		}

		Display display = ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay(); 
        Drawable d = activity.getWallpaper();
        Window window = activity.getWindow();
        int xdif = d.getIntrinsicWidth() - display.getWidth();
        int ydif = d.getIntrinsicHeight() - display.getHeight();
        int xofs = xdif / 2;
        int yofs = ydif / 2;
        d.setBounds(new Rect(-xofs, -yofs, display.getWidth() + xofs, display.getHeight() + yofs));
        
        Bitmap bmp = Bitmap.createBitmap(display.getWidth(), display.getHeight(), Config.RGB_565);
        Canvas canvas = new Canvas(bmp);
        d.draw(canvas);
        BitmapDrawable bd = new BitmapDrawable(bmp);
        
        window.setBackgroundDrawable(bd);
	}
}
