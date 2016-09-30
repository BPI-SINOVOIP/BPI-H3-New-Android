/*
**
** Copyright 2012, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
//#define LOG_NDEBUG 0
#define LOG_TAG "MediaPlayerFactory"

#define PLATFORM_CMCCWASU	0x08
#define PLATFORM_ALIYUN		0x09
#define PLATFORM_TVD		0x0A
#define UNKNOWN_PLATFORM	0xFF

#include <utils/Log.h>

#include <cutils/properties.h>
#include <media/IMediaPlayer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <utils/Errors.h>
#include <utils/misc.h>

#include <WasabiAospUtil.h>

#include "MediaPlayerFactory.h"

#include "MidiFile.h"
#include "TestPlayerStub.h"
#include "StagefrightPlayer.h"
#include "nuplayer/NuPlayerDriver.h"

#include "SimpleMediaFormatProbe.h"

#include "tplayer.h"

#include "awplayer.h"
#include "cmccplayer.h"



namespace android 
{

Mutex MediaPlayerFactory::sLock;
MediaPlayerFactory::tFactoryMap MediaPlayerFactory::sFactoryMap;
bool MediaPlayerFactory::sInitComplete = false;
// TODO: Temp hack until we can register players
typedef struct 
{
    const char *extension;
    const player_type playertype;
} extmap;

extmap FILE_EXTS [] =  
{
		{".ogg",  AW_PLAYER},
		{".mp3",  AW_PLAYER},
		{".wav",  AW_PLAYER},
		{".amr",  AW_PLAYER},
		{".flac", AW_PLAYER},
		{".m4a",  AW_PLAYER},
		{".m4r",  AW_PLAYER},
	    {".3gpp", AW_PLAYER},
		{".out",  AW_PLAYER},
		{".3gp",  AW_PLAYER},
        {".aac",  AW_PLAYER},
            
        {".mid",  SONIVOX_PLAYER},
        {".midi", SONIVOX_PLAYER},
        {".smf",  SONIVOX_PLAYER},
        {".xmf",  SONIVOX_PLAYER},
        {".mxmf", SONIVOX_PLAYER},
        {".imy",  SONIVOX_PLAYER},
        {".rtttl",SONIVOX_PLAYER},
        {".rtx",  SONIVOX_PLAYER},
        {".ota",  SONIVOX_PLAYER},
            
        {".ape", AW_PLAYER},
        {".ac3", AW_PLAYER},
        {".dts", AW_PLAYER},
        {".wma", AW_PLAYER},
        {".aac", AW_PLAYER},
        {".mp2", AW_PLAYER},
        {".mp1", AW_PLAYER},

        {".athumb", THUMBNAIL_PLAYER},
};

player_type getPlayerType_l(int fd, int64_t offset, int64_t length)
{
	int r_size;
	int file_format;

    // Some kind of MIDI?
    EAS_DATA_HANDLE easdata;
    if (EAS_Init(&easdata) == EAS_SUCCESS) 
    {
        EAS_FILE locator;
        locator.path = NULL;
        locator.fd = fd;
        locator.offset = offset;
        locator.length = length;
        EAS_HANDLE  eashandle;
        if (EAS_OpenFile(easdata, &locator, &eashandle) == EAS_SUCCESS) 
        {
            EAS_CloseFile(easdata, eashandle);
            EAS_Shutdown(easdata);
            return SONIVOX_PLAYER;
        }
        EAS_Shutdown(easdata);
    }

    return AW_PLAYER;
}

player_type getPlayerType_l(const char* url)
{
	char *strpos;

    if (TestPlayerStub::canBeUsed(url)) 
    {
            return TEST_PLAYER;
    }

    if (!strncasecmp("http://", url, 7) || !strncasecmp("https://", url, 8)) 
    {
		if((strpos = strrchr(url,'?')) != NULL) 
		{
			for (int i = 0; i < NELEM(FILE_EXTS); ++i) 
			{
				int len = strlen(FILE_EXTS[i].extension);
				if (!strncasecmp(strpos -len, FILE_EXTS[i].extension, len)) 
				{
					return FILE_EXTS[i].playertype;
				}
			}
		}
	}

	if (!strncmp("data:;base64", url, strlen("data:;base64")))
	{
		return STAGEFRIGHT_PLAYER;
	}

    // use MidiFile for MIDI extensions
    int lenURL = strlen(url);
    int len;
    int start;
    for (int i = 0; i < NELEM(FILE_EXTS); ++i) 
    {
        len = strlen(FILE_EXTS[i].extension);
        start = lenURL - len;
        if (start > 0)
        {
            if (!strncasecmp(url + start, FILE_EXTS[i].extension, len)) 
            {
                return FILE_EXTS[i].playertype;
            }
        }
    }
    if (IsWasabiSourceUrl(url)) {
        ALOGD("USE nuplayer.");
        return NU_PLAYER;
    }
    
    return AW_PLAYER;
}

status_t MediaPlayerFactory::registerFactory_l(IFactory* factory,
                                               player_type type) 
{
    if (NULL == factory) 
    {
        ALOGE("Failed to register MediaPlayerFactory of type %d, factory is"
              " NULL.", type);
        return BAD_VALUE;
    }

    if (sFactoryMap.indexOfKey(type) >= 0) 
    {
        ALOGE("Failed to register MediaPlayerFactory of type %d, type is"
              " already registered.", type);
        return ALREADY_EXISTS;
    }

    if (sFactoryMap.add(type, factory) < 0) 
    {
        ALOGE("Failed to register MediaPlayerFactory of type %d, failed to add"
              " to map.", type);
        return UNKNOWN_ERROR;
    }

    return OK;
}

player_type MediaPlayerFactory::getDefaultPlayerType() {
    char value[PROPERTY_VALUE_MAX];
    if (property_get("media.stagefright.use-nuplayer", value, NULL)
            && (!strcmp("1", value) || !strcasecmp("true", value))) {
        return NU_PLAYER;
    }
    
#if 0
    return STAGEFRIGHT_PLAYER;
#else
    return AW_PLAYER;
#endif

}

status_t MediaPlayerFactory::registerFactory(IFactory* factory,
                                             player_type type) {
    Mutex::Autolock lock_(&sLock);
    return registerFactory_l(factory, type);
}

void MediaPlayerFactory::unregisterFactory(player_type type) {
    Mutex::Autolock lock_(&sLock);
    sFactoryMap.removeItem(type);
}

#define GET_PLAYER_TYPE_IMPL_ORIGINAL(a...)                      \
    Mutex::Autolock lock_(&sLock);                      \
                                                        \
    player_type ret = STAGEFRIGHT_PLAYER;               \
    float bestScore = 0.0;                              \
                                                        \
    for (size_t i = 0; i < sFactoryMap.size(); ++i) {   \
                                                        \
        IFactory* v = sFactoryMap.valueAt(i);           \
        float thisScore;                                \
        CHECK(v != NULL);                               \
        thisScore = v->scoreFactory(a, bestScore);      \
        if (thisScore > bestScore) {                    \
            ret = sFactoryMap.keyAt(i);                 \
            bestScore = thisScore;                      \
        }                                               \
    }                                                   \
                                                        \
    if (0.0 == bestScore) {                             \
        bestScore = getDefaultPlayerType();             \
    }                                                   \
                                                        \
    return ret;


#define GET_PLAYER_TYPE_IMPL(a...)                      \
    Mutex::Autolock lock_(&sLock);                      \
                                                        \
    player_type ret = AW_PLAYER;                    \
    float bestScore = 0.0;                              \
                                                        \
    for (size_t i = 0; i < sFactoryMap.size(); ++i) {   \
                                                        \
        IFactory* v = sFactoryMap.valueAt(i);           \
        float thisScore;                                \
        CHECK(v != NULL);                               \
        thisScore = v->scoreFactory(a, bestScore);      \
        if (thisScore > bestScore) {                    \
            ret = sFactoryMap.keyAt(i);                 \
            bestScore = thisScore;                      \
        }                                               \
    }                                                   \
                                                        \
    if (0.0 == bestScore) {                             \
        bestScore = getDefaultPlayerType();             \
    }                                                   \
                                                        \
    return ret;

player_type MediaPlayerFactory::getPlayerType(const sp<IMediaPlayer>& client,
                                              const char* url) {
    ALOGV("MediaPlayerFactory::getPlayerType: url = %s", url);
    
    return android::getPlayerType_l(url);
    
    #if 0
    GET_PLAYER_TYPE_IMPL(client, url);
    #endif
}

player_type MediaPlayerFactory::getPlayerType(const sp<IMediaPlayer>& client,
                                              int fd,
                                              int64_t offset,
                                              int64_t length,
                                              bool check_cedar) {
    ALOGV("MediaPlayerFactory::getPlayerType: fd = 0x%x", fd);
    
    if (true == check_cedar)
    {
        return android::getPlayerType_l(fd, offset, length);
    }
    else
    {
        GET_PLAYER_TYPE_IMPL_ORIGINAL(client, fd, offset, length);
    }
}

player_type MediaPlayerFactory::getPlayerType(const sp<IMediaPlayer>& client,
                                              const sp<IStreamSource> &source) {
    GET_PLAYER_TYPE_IMPL(client, source);
}

#undef GET_PLAYER_TYPE_IMPL

sp<MediaPlayerBase> MediaPlayerFactory::createPlayer(
        player_type playerType,
        void* cookie,
        notify_callback_f notifyFunc) {
    sp<MediaPlayerBase> p;
    IFactory* factory;
    status_t init_result;
    Mutex::Autolock lock_(&sLock);

    if (sFactoryMap.indexOfKey(playerType) < 0) {
        ALOGE("Failed to create player object of type %d, no registered"
              " factory", playerType);
        return p;
    }

    factory = sFactoryMap.valueFor(playerType);
    CHECK(NULL != factory);
    p = factory->createPlayer();

    if (p == NULL) {
        ALOGE("Failed to create player object of type %d, create failed",
               playerType);
        return p;
    }

    init_result = p->initCheck();
    if (init_result == NO_ERROR) {
        p->setNotifyCallback(cookie, notifyFunc);
    } else {
        ALOGE("Failed to create player object of type %d, initCheck failed"
              " (res = %d)", playerType, init_result);
        p.clear();
    }

    return p;
}

/*****************************************************************************
 *                                                                           *
 *                     Built-In Factory Implementations                      *
 *                                                                           *
 *****************************************************************************/

class StagefrightPlayerFactory :
    public MediaPlayerFactory::IFactory {
  public:
    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               int fd,
                               int64_t offset,
                               int64_t length,
                               float curScore) {
        char buf[20];
        lseek(fd, offset, SEEK_SET);
        read(fd, buf, sizeof(buf));
        lseek(fd, offset, SEEK_SET);

        long ident = *((long*)buf);

        // Ogg vorbis?
        if (ident == 0x5367674f) // 'OggS'
            return 1.0;

        return 0.0;
    }

    virtual sp<MediaPlayerBase> createPlayer() {
        ALOGV(" create StagefrightPlayer");
        return new StagefrightPlayer();
    }
};

class NuPlayerFactory : public MediaPlayerFactory::IFactory {
  public:
    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               const char* url,
                               float curScore) {
        static const float kOurScore = 0.8;

        if (kOurScore <= curScore)
            return 0.0;

        if (IsWasabiSourceUrl(url)) {
            return 1.0;
        }

        if (!strncasecmp("http://", url, 7)
                || !strncasecmp("https://", url, 8)) {
            size_t len = strlen(url);
            if (len >= 5 && !strcasecmp(".m3u8", &url[len - 5])) {
                return kOurScore;
            }

            if (strstr(url,"m3u8")) {
                return kOurScore;
            }

            if ((len >= 4 && !strcasecmp(".sdp", &url[len - 4])) || strstr(url, ".sdp?")) {
                return kOurScore;
            }
        }

        if (!strncasecmp("rtsp://", url, 7)) {
            return kOurScore;
        }

        return 0.0;
    }

    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               const sp<IStreamSource> &source,
                               float curScore) {
        return 1.0;
    }

    virtual sp<MediaPlayerBase> createPlayer() {
        ALOGV(" create NuPlayer");
        return new NuPlayerDriver;
    }
};

class SonivoxPlayerFactory : public MediaPlayerFactory::IFactory {
  public:
    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               const char* url,
                               float curScore) {
        static const float kOurScore = 0.4;
        static const char* const FILE_EXTS[] = { ".mid",
                                                 ".midi",
                                                 ".smf",
                                                 ".xmf",
                                                 ".mxmf",
                                                 ".imy",
                                                 ".rtttl",
                                                 ".rtx",
                                                 ".ota" };
        if (kOurScore <= curScore)
            return 0.0;

        // use MidiFile for MIDI extensions
        int lenURL = strlen(url);
        for (int i = 0; i < NELEM(FILE_EXTS); ++i) {
            int len = strlen(FILE_EXTS[i]);
            int start = lenURL - len;
            if (start > 0) {
                if (!strncasecmp(url + start, FILE_EXTS[i], len)) {
                    return kOurScore;
                }
            }
        }

        return 0.0;
    }

    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               int fd,
                               int64_t offset,
                               int64_t length,
                               float curScore) {
        static const float kOurScore = 0.8;

        if (kOurScore <= curScore)
            return 0.0;

        // Some kind of MIDI?
        EAS_DATA_HANDLE easdata;
        if (EAS_Init(&easdata) == EAS_SUCCESS) {
            EAS_FILE locator;
            locator.path = NULL;
            locator.fd = fd;
            locator.offset = offset;
            locator.length = length;
            EAS_HANDLE  eashandle;
            if (EAS_OpenFile(easdata, &locator, &eashandle) == EAS_SUCCESS) {
                EAS_CloseFile(easdata, eashandle);
                EAS_Shutdown(easdata);
                return kOurScore;
            }
            EAS_Shutdown(easdata);
        }

        return 0.0;
    }

    virtual sp<MediaPlayerBase> createPlayer() {
        ALOGV(" create MidiFile");
        return new MidiFile();
    }
};

class TestPlayerFactory : public MediaPlayerFactory::IFactory {
  public:
    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               const char* url,
                               float curScore) {
        if (TestPlayerStub::canBeUsed(url)) {
            return 1.0;
        }

        return 0.0;
    }

    virtual sp<MediaPlayerBase> createPlayer() {
        ALOGV("Create Test Player stub");
        return new TestPlayerStub();
    }
};

class TPlayerFactory : public MediaPlayerFactory::IFactory {
  public:
    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               int fd,
                               int64_t offset,
                               int64_t length,
                               float curScore) {

        return 0.0;
    }

    virtual sp<MediaPlayerBase> createPlayer() {
        ALOGV(" create TPlayer");
        return new TPlayer();
    }
};



#if(BUSINESS_PLATFORM == PLATFORM_CMCCWASU)
class CmccPlayerFactory : public MediaPlayerFactory::IFactory {
  public:
    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               int fd,
                               int64_t offset,
                               int64_t length,
                               float curScore) {

        return 0.0;
    }

    virtual sp<MediaPlayerBase> createPlayer() {
        ALOGV(" create CmccPlayer");
        return new CmccPlayer();
    }
};
#else
class AwPlayerFactory : public MediaPlayerFactory::IFactory {
  public:
    virtual float scoreFactory(const sp<IMediaPlayer>& client,
                               int fd,
                               int64_t offset,
                               int64_t length,
                               float curScore) {

        return 0.0;
    }

    virtual sp<MediaPlayerBase> createPlayer() {
        ALOGV(" create AwPlayer");
        return new AwPlayer();
    }
};
#endif

void MediaPlayerFactory::registerBuiltinFactories() {
    Mutex::Autolock lock_(&sLock);

    if (sInitComplete)
        return;

#if(BUSINESS_PLATFORM == PLATFORM_CMCCWASU)
	registerFactory_l(new CmccPlayerFactory(), AW_PLAYER);
#else
    registerFactory_l(new AwPlayerFactory(), AW_PLAYER);
#endif
    registerFactory_l(new TPlayerFactory(), THUMBNAIL_PLAYER);   
    registerFactory_l(new StagefrightPlayerFactory(), STAGEFRIGHT_PLAYER);
    registerFactory_l(new NuPlayerFactory(), NU_PLAYER);
    registerFactory_l(new SonivoxPlayerFactory(), SONIVOX_PLAYER);
    registerFactory_l(new TestPlayerFactory(), TEST_PLAYER);

    sInitComplete = true;
}

}  // namespace android
