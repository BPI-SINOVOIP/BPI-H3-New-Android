/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include "bootAudioManager.h"
#define LOG_TAG "BootAnimation"

namespace android {

int data_query_callback(void *d,int nCol, char** values, char** names)
{
    audio_vol_t *ctx = (audio_vol_t *)d;
    if(nCol < 3){ // 3 columns
        return 0;
    }
    if(0 == strcmp(values[1], SYS_VOLUME_DATANAME)){
        ctx->sys_volume = atoi(values[2]);
        ALOGD("sys_volume:%f\n", ctx->sys_volume);
    }
	if(0 == strcmp(values[1], MASTER_VOLUME_DATANAME)){
        ctx->master_volume = atof(values[2]);
        ALOGD("master_volume:%f\n", ctx->master_volume);
    }
	if(0 == strcmp(values[1], MASTER_MUTE_DATANAME)){
        ctx->master_mute = atoi(values[2]);
        ALOGD("master_mute:%d\n", ctx->master_mute);
    }
    return 0;
}

int audio_query_database(audio_vol_t *ctx)
{
    char *errMsg = NULL;
    if(!ctx) {
        return -1;
    }
    sqlite3_exec(ctx->db, AUDIO_QUERY_SQL, data_query_callback, ctx, &errMsg);
    if(errMsg) {
        ALOGE("sql exec error: %s", errMsg);
        sqlite3_free(errMsg);
        return -1;
    }
    return 0;
}

int get_sys_volume_from_db(audio_vol_t *ctx)
{
    int vol = 0;
    if(!ctx) { 
	    return -1;
    }   
    if(SQLITE_OK != sqlite3_open(DATABASE_FILE, &ctx->db)){
        ALOGE("database open failed: %s",sqlite3_errmsg(ctx->db));
	    return -1;
    }
	if(0 != audio_query_database(ctx)){
        ALOGE("audio query database failed");
        sqlite3_close(ctx->db);
        return -1;
    }
    sqlite3_close(ctx->db);
    ALOGD("get sys volume: %f",ctx->sys_volume);
	vol = ctx->sys_volume;
    if(ctx->sys_volume < 0 || ctx->sys_volume > SYS_VOLUME_MAX)
		ctx->sys_volume = SYS_VOLUME_DEFAULT;
	if(ctx->master_volume < 0 || ctx->master_volume > MASTER_VOLUME_MAX)
        ctx->master_volume = MASTER_VOLUME_DEFAULT;
    //adev->master_volume = ctx->master_volume;
	//ALOGD("adev->master_volume: %f",adev->master_volume);
	if(ctx->master_mute == 1){
		vol = 0;
    }else{
        vol = ctx->sys_volume;
    }
    return vol;
}

int get_system_volume_index()
{
    audio_vol_t ctx;
    int vol = 0;
    memset(&ctx, 0, sizeof(ctx));
    ctx.sys_volume = SYS_VOLUME_DEFAULT;
	ctx.master_volume = MASTER_VOLUME_DEFAULT;
	//master_volume = ctx.master_volume;
	ctx.master_mute = 0;
    if((vol = get_sys_volume_from_db(&ctx)) >= 0){
	    return vol;
    }
    return SYS_VOLUME_DEFAULT;
}

float get_system_volume(){
	float Volume;
	int index = get_system_volume_index();
	if(SYS_VOLUME_MAX == 15)
		Volume = index15_to_amp[index];
    else if(SYS_VOLUME_MAX == 32)
		Volume = index32_to_amp[index];
    else
		Volume = 1.0;
	ALOGD("##index = %d,volume = %f",index, Volume);

    return Volume;
}

}//end namespace android

