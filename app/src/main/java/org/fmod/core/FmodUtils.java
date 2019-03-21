package org.fmod.core;

import android.app.Activity;

/**
 * @author onestravel
 * @version 1.0.0
 * @name org.fmod.example.FmodUtils
 * @description //TODO
 * @createTime 2019/3/18 15:38
 */
public class FmodUtils {
    public static FmodUtils mInstance;

    private FmodUtils() {

    }

    public static FmodUtils getInstance() {
        synchronized (FmodUtils.class) {
            if (mInstance == null) {
                mInstance = new FmodUtils();
            }
        }
        return mInstance;
    }


    public native String getButtonLabel(int index);
    public native void buttonDown(int index);
    public native void buttonUp(int index);
    public native void setStateCreate();
    public native void setStateStart();
    public native void setStateStop();
    public native void setStateDestroy();
    public native void main(Activity context);

    static
    {
        /*
         * To simplify our examples we try to load all possible FMOD
         * libraries, the Android.mk will copy in the correct ones
         * for each example. For real products you would just load
         * 'fmod' and if you use the FMOD Studio tool you would also
         * load 'fmodstudio'.
         */

        // Try debug libraries...
//    	try { System.loadLibrary("fmodD");
//    		  System.loadLibrary("fmodstudioD"); }
//    	catch (UnsatisfiedLinkError e) { }
        // Try logging libraries...
        try { System.loadLibrary("fmodL");
//    		  System.loadLibrary("fmodstudioL");
        }
        catch (UnsatisfiedLinkError e) { }
        // Try release libraries...
        try { System.loadLibrary("fmod");
//		      System.loadLibrary("fmodstudio");
        }
        catch (UnsatisfiedLinkError e) { }

//        System.loadLibrary("sound");
//    	System.loadLibrary("stlport_shared");
    }
}
