package boyw165.com.my_surprised_captions.tool;

import android.os.Looper;
import android.util.Log;

public class LogUtils {

    public static void log(String msg) {
        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        String postfix = Looper.myLooper() == Looper.getMainLooper() ?
            " (main thread)" :
            " (NOT main thread)";

        Log.d(ts, msg + postfix);
    }

    public static void log(String tag, String msg) {
        String postfix = Looper.myLooper() == Looper.getMainLooper() ?
            " (main thread)" :
            " (NOT main thread)";

        Log.d(tag, msg + postfix);
    }
}
