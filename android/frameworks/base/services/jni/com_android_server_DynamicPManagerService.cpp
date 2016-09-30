/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "PowerManagerService-JNI"

//#define LOG_NDEBUG 0

#include "JNIHelp.h"
#include "jni.h"

#include <ScopedUtfChars.h>

#include <limits.h>

#include <android_runtime/AndroidRuntime.h>
#include <android_runtime/Log.h>
#include <utils/String8.h>
#include <utils/Log.h>

#include "com_android_server_DynamicPManagerService.h"

namespace android {


static struct {
    jmethodID reset;
} gDynamicPManagerServiceMethodInfo;

static jobject gDynamicPManagerServiceObj;

#define FIND_CLASS(var, className) \
        var = env->FindClass(className); \
        LOG_FATAL_IF(! var, "Unable to find class " className);

#define GET_METHOD_ID(var, clazz, methodName, methodDescriptor) \
        var = env->GetMethodID(clazz, methodName, methodDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find method " methodName);

#define GET_FIELD_ID(var, clazz, fieldName, fieldDescriptor) \
        var = env->GetFieldID(clazz, fieldName, fieldDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find field " fieldName);



static bool checkAndClearExceptionFromCallback(JNIEnv* env, const char* methodName) {
    if (env->ExceptionCheck()) {
        ALOGE("An exception was thrown by callback '%s'.", methodName);
        LOGE_EX(env);
        env->ExceptionClear();
        return true;
    }
    return false;
}

void android_server_DynamicPManagerService_reset(){
     if (gDynamicPManagerServiceObj) {
        JNIEnv* env = AndroidRuntime::getJNIEnv();
        env->CallVoidMethod(gDynamicPManagerServiceObj,
                gDynamicPManagerServiceMethodInfo.reset);
        checkAndClearExceptionFromCallback(env, "reset");
     }
}

static void nativeInit(JNIEnv* env, jobject obj) {
    gDynamicPManagerServiceObj = env->NewGlobalRef(obj);

}

static JNINativeMethod gDynamicPManagerServiceMethods[] = {
    /* name, signature, funcPtr */
    { "nativeInit", "()V",
            (void*) nativeInit },
};

int register_android_server_DynamicPManagerService(JNIEnv* env) {
    int res = jniRegisterNativeMethods(env, "com/android/server/DynamicPManagerService",
            gDynamicPManagerServiceMethods, NELEM(gDynamicPManagerServiceMethods));
    LOG_FATAL_IF(res < 0, "Unable to register native methods.");

    jclass clazz;
    FIND_CLASS(clazz, "com/android/server/DynamicPManagerService");

    GET_METHOD_ID(gDynamicPManagerServiceMethodInfo.reset, clazz,
            "reset", "()V");

    return 0;
}

} /* namespace android */
