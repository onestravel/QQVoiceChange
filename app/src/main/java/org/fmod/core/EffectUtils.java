package org.fmod.core;

/**
 * @author onestravel
 * @version 1.0.0
 * @name org.fmod.core.EffectUtils
 * @description //TODO
 * @createTime 2019/3/21 15:46
 */
public class EffectUtils {
    public static final int MODE_NORMAL = 0;
    public static final int MODE_LUOLI = 1;
    public static final int MODE_DASHU = 2;
    public static final int MODE_GAOGUAI = 3;
    public static final int MODE_KONGLING = 4;
    public static final int MODE_JINGSONG = 5;

    /**
     * 音效处理 native 方法
     *
     * @param path 音频源文件路径
     * @param mode 特效模式
     */
    public native void effect(String path, int mode);


    static {

        try {
            System.loadLibrary("fmodL");
        } catch (UnsatisfiedLinkError e) {
        }
        try {
            System.loadLibrary("fmod");
        } catch (UnsatisfiedLinkError e) {
        }
        //特效处理的 动态库
        System.loadLibrary("effect");
    }
}
