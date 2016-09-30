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

#ifndef BOOTAUDIOMANANER_H_
#define BOOTAUDIOMANANER_H_

#include <sqlite3.h>
#include <fcntl.h>
#include <errno.h>
#include <pthread.h>
#include <stdint.h>
#include <sys/time.h>
#include <stdlib.h>
#include <cutils/log.h>

#define DATABASE_FILE "/data/data/com.android.providers.settings/databases/settings.db"
#define SYS_VOLUME_MAX         15   // music, see DEFAULT_STREAM_VOLUME @ AudioManager.java
#define SYS_VOLUME_DEFAULT     11   // music, see DEFAULT_STREAM_VOLUME @ AudioManager.java
#define MASTER_VOLUME_MAX      1.0
#define MASTER_VOLUME_DEFAULT  0.36
#define AUDIO_QUERY_SQL          "select * from system"
//"volume_music_aux_digital"
#define SYS_VOLUME_DATANAME     "volume_music_speaker"
#define MASTER_VOLUME_DATANAME  "volume_master"
#define MASTER_MUTE_DATANAME    "volume_master_mute"

//index convert to amplifier
//volume level = 0~32
static float index32_to_amp[33] =
{
    0.00000, 0.00207, 0.00309, 0.00460, 0.00687, 0.01025, 0.01528, 0.02132,
    0.02778, 0.03388, 0.04133, 0.05041, 0.06148, 0.07499, 0.09146, 0.11156,
    0.14538, 0.17732, 0.21627, 0.26379, 0.30026, 0.33018, 0.36308, 0.39926,
    0.45316, 0.49831, 0.54796, 0.60256, 0.66260, 0.72862, 0.80122, 0.88105,
    1.00000
};
//volume level = 0~15
static float index15_to_amp[16] =
{
 0.00000, 0.00309, 0.00785, 0.01995,
 0.02968, 0.04718, 0.07499, 0.11156,
 0.17732, 0.28184, 0.34080, 0.42535,
 0.53089, 0.64195, 0.80122, 1.00000
};

namespace android {
   typedef struct audio_vol_s {
    sqlite3 *db;
	float sys_volume;
	float master_volume;
	int master_mute;
   } audio_vol_t;

   int data_query_callback(void *d,int nCol, char** values, char** names);
   int audio_query_database(audio_vol_t *ctx);
   int get_sys_volume_from_db(audio_vol_t *ctx);
   int get_system_volume_index();
   float get_system_volume();
}

#endif

