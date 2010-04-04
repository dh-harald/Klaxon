package koushikdutta.klaxon;

import android.content.Context;

public class Helper {
	public static boolean isPremium(Context context) {
		return context.getPackageManager().checkSignatures(context.getPackageName(), "com.koushikdutta.klaxon") >= 0;
	}
}
