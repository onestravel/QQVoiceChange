//
// Created by Administrator on 2019/3/21.
//

#include "org_fmod_core_EffectUtils.h"
#include "inc/fmod.h"
#include "inc/fmod.hpp"
#include <string.h>
#include <unistd.h>
#include <android/log.h>

#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"FILE_PATCH",FORMAT,__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"FILE_PATCH",FORMAT,__VA_ARGS__);

JNIEXPORT void JNICALL Java_org_fmod_core_EffectUtils_effect
        (JNIEnv *env, jobject jobj, jstring j_file_path, jint j_mode) {
    //路径转换
    const char *file_path = env->GetStringUTFChars(j_file_path, NULL);
    bool playing = true;
    //加载音频文件
    FMOD::System *system;
    FMOD::Sound *sound;
    FMOD::Channel *channel;
    FMOD::DSP *dsp;
    float frequency;
    try {
        //初始化
        System_Create(&system);
        system->init(32, FMOD_INIT_NORMAL, NULL);
        LOGI("%s", file_path);
        //加载录音文件到 sound
        system->createSound(file_path, FMOD_DEFAULT,NULL, &sound);
        //播放音频文件
        system->playSound(sound, 0, false, &channel);
        //播放过程中实现不同的音效配置
        switch (j_mode) {
            case org_fmod_core_EffectUtils_MODE_NORMAL:
                //无需处理
                LOGI("%s", "正常播放");

                break;
            case org_fmod_core_EffectUtils_MODE_LUOLI:
                //女声为高声，需要将声音提高
                //DSP 中定义了一些对声音处理的特效，
                system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 2.5);
                channel->addDSP(0, dsp);
                LOGI("%s", "萝莉")
                break;
            case org_fmod_core_EffectUtils_MODE_DASHU:
                //女声为高声，需要将声音提高
                //DSP 中定义了一些对声音处理的特效，
                system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 0.6);
                channel->addDSP(0, dsp);
                LOGI("%s", "大叔")
                break;
            case org_fmod_core_EffectUtils_MODE_GAOGUAI:
                //搞怪
                //提高说话的速度
                channel->getFrequency(&frequency);
                frequency = frequency * 1.6;
                channel->setFrequency(frequency);
                LOGI("%s", "搞怪")
                break;
            case org_fmod_core_EffectUtils_MODE_KONGLING:
                //空灵就是产生回声
                system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 0.9);
                channel->addDSP(0, dsp);
                system->createDSPByType(FMOD_DSP_TYPE_ECHO, &dsp);
                dsp->setParameterFloat(FMOD_DSP_ECHO_DELAY, 350);
                dsp->setParameterFloat(FMOD_DSP_ECHO_FEEDBACK, 15);
                channel->addDSP(1, dsp);
                LOGI("%s", "空灵")
                break;
            case org_fmod_core_EffectUtils_MODE_JINGSONG:
                //惊悚
                system->createDSPByType(FMOD_DSP_TYPE_TREMOLO, &dsp);
//                dsp->setParameterFloat(FMOD_DSP_TREMOLO_FREQUENCY, 3);
                dsp->setParameterFloat(FMOD_DSP_TREMOLO_DEPTH, 0.5);
//                dsp->setParameterFloat(FMOD_DSP_TREMOLO_SKEW, 0.3);
//                dsp->setParameterFloat(FMOD_DSP_TREMOLO_SPREAD, 0.6);
                channel->addDSP(0,dsp);

                LOGI("%s", "惊悚")
                break;
            default:
                break;

        }

    } catch (...) {
        LOGE("%s", "effect error");
        goto end;
    }
    system->update();
    //在fmod中，处理音效就是更改channel（音轨）
    while (playing) {
        LOGI("%s,%d", "playing",playing)
        channel->isPlaying(&playing);
        usleep(1 * 1000 * 1000);
        LOGI("%s,%d", "playing",playing)
    }

    goto end;
    end:
    env->ReleaseStringUTFChars(j_file_path, file_path);
    sound->release();
    system->close();
    system->release();
}