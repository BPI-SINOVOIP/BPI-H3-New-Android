/* mixer.c
**
** Copyright 2011, The Android Open Source Project
**
** Redistribution and use in source and binary forms, with or without
** modification, are permitted provided that the following conditions are met:
**     * Redistributions of source code must retain the above copyright
**       notice, this list of conditions and the following disclaimer.
**     * Redistributions in binary form must reproduce the above copyright
**       notice, this list of conditions and the following disclaimer in the
**       documentation and/or other materials provided with the distribution.
**     * Neither the name of The Android Open Source Project nor the names of
**       its contributors may be used to endorse or promote products derived
**       from this software without specific prior written permission.
**
** THIS SOFTWARE IS PROVIDED BY The Android Open Source Project ``AS IS'' AND
** ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
** IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
** ARE DISCLAIMED. IN NO EVENT SHALL The Android Open Source Project BE LIABLE
** FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
** DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
** SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
** CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
** LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
** OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
** DAMAGE.
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <ctype.h>
#include <cutils/log.h>
#include <sys/ioctl.h>

#include <linux/ioctl.h>
#define __force
#define __bitwise
#define __user
#include <sound/asound.h>

#include <tinyalsa/asoundlib.h>

#define MIXER_FILE_CONTROL "/dev/snd/controlC%i"
struct mixer_ctl {
    struct mixer *mixer;
    struct snd_ctl_elem_info *info;
    char **ename;
};

struct mixer {
    int fd;
    struct snd_ctl_card_info card_info;
    struct snd_ctl_elem_info *elem_info;
    struct mixer_ctl *ctl;
    unsigned int count;
};

void mixer_close(struct mixer *mixer)
{
    unsigned int n,m;

    if (!mixer)
        return;

    if (mixer->fd >= 0)
        close(mixer->fd);

    if (mixer->ctl) {
        for (n = 0; n < mixer->count; n++) {
            if (mixer->ctl[n].ename) {
                unsigned int max = mixer->ctl[n].info->value.enumerated.items;
                for (m = 0; m < max; m++)
                    free(mixer->ctl[n].ename[m]);
                free(mixer->ctl[n].ename);
            }
        }
        free(mixer->ctl);
    }

    if (mixer->elem_info)
        free(mixer->elem_info);

    free(mixer);

    /* TODO: verify frees */
}

struct mixer *mixer_open(unsigned int card)
{
    struct snd_ctl_elem_list elist;
    struct snd_ctl_elem_info tmp;
    struct snd_ctl_elem_id *eid = NULL;
    struct mixer *mixer = NULL;
    unsigned int n, m;
    int fd;
    char fn[256];

    snprintf(fn, sizeof(fn), "/dev/snd/controlC%u", card);
    fd = open(fn, O_RDWR);
    if (fd < 0)
        return 0;

    memset(&elist, 0, sizeof(elist));
    if (ioctl(fd, SNDRV_CTL_IOCTL_ELEM_LIST, &elist) < 0)
        goto fail;

    mixer = calloc(1, sizeof(*mixer));
    if (!mixer)
        goto fail;

    mixer->ctl = calloc(elist.count, sizeof(struct mixer_ctl));
    mixer->elem_info = calloc(elist.count, sizeof(struct snd_ctl_elem_info));
    if (!mixer->ctl || !mixer->elem_info)
        goto fail;

    if (ioctl(fd, SNDRV_CTL_IOCTL_CARD_INFO, &mixer->card_info) < 0)
        goto fail;

    eid = calloc(elist.count, sizeof(struct snd_ctl_elem_id));
    if (!eid)
        goto fail;

    mixer->count = elist.count;
    mixer->fd = fd;
    elist.space = mixer->count;
    elist.pids = eid;
    if (ioctl(fd, SNDRV_CTL_IOCTL_ELEM_LIST, &elist) < 0)
        goto fail;

    for (n = 0; n < mixer->count; n++) {
        struct snd_ctl_elem_info *ei = mixer->elem_info + n;
        ei->id.numid = eid[n].numid;
        if (ioctl(fd, SNDRV_CTL_IOCTL_ELEM_INFO, ei) < 0)
            goto fail;
        mixer->ctl[n].info = ei;
        mixer->ctl[n].mixer = mixer;
        if (ei->type == SNDRV_CTL_ELEM_TYPE_ENUMERATED) {
            char **enames = calloc(ei->value.enumerated.items, sizeof(char*));
            if (!enames)
                goto fail;
            mixer->ctl[n].ename = enames;
            for (m = 0; m < ei->value.enumerated.items; m++) {
                memset(&tmp, 0, sizeof(tmp));
                tmp.id.numid = ei->id.numid;
                tmp.value.enumerated.item = m;
                if (ioctl(fd, SNDRV_CTL_IOCTL_ELEM_INFO, &tmp) < 0)
                    goto fail;
                enames[m] = strdup(tmp.value.enumerated.name);
                if (!enames[m])
                    goto fail;
            }
        }
    }

    free(eid);
    return mixer;

fail:
    /* TODO: verify frees in failure case */
    if (eid)
        free(eid);
    if (mixer)
        mixer_close(mixer);
    else if (fd >= 0)
        close(fd);
    return 0;
}

const char *mixer_get_name(struct mixer *mixer)
{
    return (const char *)mixer->card_info.name;
}

unsigned int mixer_get_num_ctls(struct mixer *mixer)
{
    if (!mixer)
        return 0;

    return mixer->count;
}

struct mixer_ctl *mixer_get_ctl(struct mixer *mixer, unsigned int id)
{
    if (mixer && (id < mixer->count))
        return mixer->ctl + id;

    return NULL;
}

struct mixer_ctl *mixer_get_ctl_by_name(struct mixer *mixer, const char *name)
{
    unsigned int n;

    if (!mixer)
        return NULL;

    for (n = 0; n < mixer->count; n++)
        if (!strcmp(name, (char*) mixer->elem_info[n].id.name))
            return mixer->ctl + n;

    return NULL;
}

void mixer_ctl_update(struct mixer_ctl *ctl)
{
    ioctl(ctl->mixer->fd, SNDRV_CTL_IOCTL_ELEM_INFO, ctl->info);
}

const char *mixer_ctl_get_name(struct mixer_ctl *ctl)
{
    if (!ctl)
        return NULL;

    return (const char *)ctl->info->id.name;
}

enum mixer_ctl_type mixer_ctl_get_type(struct mixer_ctl *ctl)
{
    if (!ctl)
        return MIXER_CTL_TYPE_UNKNOWN;

    switch (ctl->info->type) {
    case SNDRV_CTL_ELEM_TYPE_BOOLEAN:    return MIXER_CTL_TYPE_BOOL;
    case SNDRV_CTL_ELEM_TYPE_INTEGER:    return MIXER_CTL_TYPE_INT;
    case SNDRV_CTL_ELEM_TYPE_ENUMERATED: return MIXER_CTL_TYPE_ENUM;
    case SNDRV_CTL_ELEM_TYPE_BYTES:      return MIXER_CTL_TYPE_BYTE;
    case SNDRV_CTL_ELEM_TYPE_IEC958:     return MIXER_CTL_TYPE_IEC958;
    case SNDRV_CTL_ELEM_TYPE_INTEGER64:  return MIXER_CTL_TYPE_INT64;
    default:                             return MIXER_CTL_TYPE_UNKNOWN;
    };
}

const char *mixer_ctl_get_type_string(struct mixer_ctl *ctl)
{
    if (!ctl)
        return "";

    switch (ctl->info->type) {
    case SNDRV_CTL_ELEM_TYPE_BOOLEAN:    return "BOOL";
    case SNDRV_CTL_ELEM_TYPE_INTEGER:    return "INT";
    case SNDRV_CTL_ELEM_TYPE_ENUMERATED: return "ENUM";
    case SNDRV_CTL_ELEM_TYPE_BYTES:      return "BYTE";
    case SNDRV_CTL_ELEM_TYPE_IEC958:     return "IEC958";
    case SNDRV_CTL_ELEM_TYPE_INTEGER64:  return "INT64";
    default:                             return "Unknown";
    };
}

unsigned int mixer_ctl_get_num_values(struct mixer_ctl *ctl)
{
    if (!ctl)
        return 0;

    return ctl->info->count;
}

static int percent_to_int(struct snd_ctl_elem_info *ei, int percent)
{
    int range;

    if (percent > 100)
        percent = 100;
    else if (percent < 0)
        percent = 0;

    range = (ei->value.integer.max - ei->value.integer.min);

    return ei->value.integer.min + (range * percent) / 100;
}

static int int_to_percent(struct snd_ctl_elem_info *ei, int value)
{
    int range = (ei->value.integer.max - ei->value.integer.min);

    if (range == 0)
        return 0;

    return ((value - ei->value.integer.min) / range) * 100;
}

int mixer_ctl_get_percent(struct mixer_ctl *ctl, unsigned int id)
{
    if (!ctl || (ctl->info->type != SNDRV_CTL_ELEM_TYPE_INTEGER))
        return -EINVAL;

    return int_to_percent(ctl->info, mixer_ctl_get_value(ctl, id));
}

int mixer_ctl_set_percent(struct mixer_ctl *ctl, unsigned int id, int percent)
{
    if (!ctl || (ctl->info->type != SNDRV_CTL_ELEM_TYPE_INTEGER))
        return -EINVAL;

    return mixer_ctl_set_value(ctl, id, percent_to_int(ctl->info, percent));
}

int mixer_ctl_get_value(struct mixer_ctl *ctl, unsigned int id)
{
    struct snd_ctl_elem_value ev;
    int ret;

    if (!ctl || (id >= ctl->info->count))
        return -EINVAL;

    memset(&ev, 0, sizeof(ev));
    ev.id.numid = ctl->info->id.numid;
    ret = ioctl(ctl->mixer->fd, SNDRV_CTL_IOCTL_ELEM_READ, &ev);
    if (ret < 0)
        return ret;

    switch (ctl->info->type) {
    case SNDRV_CTL_ELEM_TYPE_BOOLEAN:
        return !!ev.value.integer.value[id];

    case SNDRV_CTL_ELEM_TYPE_INTEGER:
        return ev.value.integer.value[id];

    case SNDRV_CTL_ELEM_TYPE_ENUMERATED:
        return ev.value.enumerated.item[id];

    case SNDRV_CTL_ELEM_TYPE_BYTES:
        return ev.value.bytes.data[id];

    default:
        return -EINVAL;
    }

    return 0;
}

int mixer_ctl_get_array(struct mixer_ctl *ctl, void *array, size_t count)
{
    struct snd_ctl_elem_value ev;
    int ret;
    size_t size;
    void *source;

    if (!ctl || (count > ctl->info->count) || !count || !array)
        return -EINVAL;

    memset(&ev, 0, sizeof(ev));
    ev.id.numid = ctl->info->id.numid;

    ret = ioctl(ctl->mixer->fd, SNDRV_CTL_IOCTL_ELEM_READ, &ev);
    if (ret < 0)
        return ret;

    switch (ctl->info->type) {
    case SNDRV_CTL_ELEM_TYPE_BOOLEAN:
    case SNDRV_CTL_ELEM_TYPE_INTEGER:
        size = sizeof(ev.value.integer.value[0]);
        source = ev.value.integer.value;
        break;

    case SNDRV_CTL_ELEM_TYPE_BYTES:
        size = sizeof(ev.value.bytes.data[0]);
        source = ev.value.bytes.data;
        break;

    default:
        return -EINVAL;
    }

    memcpy(array, source, size * count);

    return 0;
}

int mixer_ctl_set_value(struct mixer_ctl *ctl, unsigned int id, int value)
{
    struct snd_ctl_elem_value ev;
    int ret;

    if (!ctl || (id >= ctl->info->count))
        return -EINVAL;

    memset(&ev, 0, sizeof(ev));
    ev.id.numid = ctl->info->id.numid;
    ret = ioctl(ctl->mixer->fd, SNDRV_CTL_IOCTL_ELEM_READ, &ev);
    if (ret < 0)
        return ret;

    switch (ctl->info->type) {
    case SNDRV_CTL_ELEM_TYPE_BOOLEAN:
        ev.value.integer.value[id] = !!value;
        break;

    case SNDRV_CTL_ELEM_TYPE_INTEGER:
        ev.value.integer.value[id] = value;
        break;

    case SNDRV_CTL_ELEM_TYPE_ENUMERATED:
        ev.value.enumerated.item[id] = value;
        break;

    default:
        return -EINVAL;
    }

    return ioctl(ctl->mixer->fd, SNDRV_CTL_IOCTL_ELEM_WRITE, &ev);
}

int mixer_ctl_set_array(struct mixer_ctl *ctl, const void *array, size_t count)
{
    struct snd_ctl_elem_value ev;
    size_t size;
    void *dest;

    if (!ctl || (count > ctl->info->count) || !count || !array)
        return -EINVAL;

    memset(&ev, 0, sizeof(ev));
    ev.id.numid = ctl->info->id.numid;

    switch (ctl->info->type) {
    case SNDRV_CTL_ELEM_TYPE_BOOLEAN:
    case SNDRV_CTL_ELEM_TYPE_INTEGER:
        size = sizeof(ev.value.integer.value[0]);
        dest = ev.value.integer.value;
        break;

    case SNDRV_CTL_ELEM_TYPE_BYTES:
        size = sizeof(ev.value.bytes.data[0]);
        dest = ev.value.bytes.data;
        break;

    default:
        return -EINVAL;
    }

    memcpy(dest, array, size * count);

    return ioctl(ctl->mixer->fd, SNDRV_CTL_IOCTL_ELEM_WRITE, &ev);
}

int mixer_ctl_get_range_min(struct mixer_ctl *ctl)
{
    if (!ctl || (ctl->info->type != SNDRV_CTL_ELEM_TYPE_INTEGER))
        return -EINVAL;

    return ctl->info->value.integer.min;
}

int mixer_ctl_get_range_max(struct mixer_ctl *ctl)
{
    if (!ctl || (ctl->info->type != SNDRV_CTL_ELEM_TYPE_INTEGER))
        return -EINVAL;

    return ctl->info->value.integer.max;
}

unsigned int mixer_ctl_get_num_enums(struct mixer_ctl *ctl)
{
    if (!ctl)
        return 0;

    return ctl->info->value.enumerated.items;
}

const char *mixer_ctl_get_enum_string(struct mixer_ctl *ctl,
                                      unsigned int enum_id)
{
    if (!ctl || (ctl->info->type != SNDRV_CTL_ELEM_TYPE_ENUMERATED) ||
        (enum_id >= ctl->info->value.enumerated.items))
        return NULL;

    return (const char *)ctl->ename[enum_id];
}

int mixer_ctl_set_enum_by_string(struct mixer_ctl *ctl, const char *string)
{
    unsigned int i, num_enums;
    struct snd_ctl_elem_value ev;
    int ret;

    if (!ctl || (ctl->info->type != SNDRV_CTL_ELEM_TYPE_ENUMERATED))
        return -EINVAL;

    num_enums = ctl->info->value.enumerated.items;
    for (i = 0; i < num_enums; i++) {
        if (!strcmp(string, ctl->ename[i])) {
            memset(&ev, 0, sizeof(ev));
            ev.value.enumerated.item[0] = i;
            ev.id.numid = ctl->info->id.numid;
            ret = ioctl(ctl->mixer->fd, SNDRV_CTL_IOCTL_ELEM_WRITE, &ev);
            if (ret < 0)
                return ret;
            return 0;
        }
    }

    return -EINVAL;
}

/**
 * Transplant from android 4.1 alsa-lib
 * */
int mixer_do_card_load(int card)
{
    int open_dev;
    char control[sizeof(MIXER_FILE_CONTROL) + 10];
    sprintf(control, MIXER_FILE_CONTROL, card);
 
    open_dev = open(control, O_RDONLY);
	ALOGD("##open_dev =%d,card = %d,control = %s",open_dev,card,control);
    if (open_dev >= 0) {
        close (open_dev);
        return 0;
    } else {
        return -errno;
    }
}

/**
 * Transplant from android 4.1 alsa-lib
 * brief Try to load the driver for a card.
 * param card Card number.
 * return 1 if driver is present, zero if driver is not present
 * */
int mixer_card_load(int card)
{
    return !!(mixer_do_card_load(card) == 0);
}

/**
 * Transplant from android 4.1 alsa-lib
 * brief Try to determine the next card.
 * param rcard pointer to card number
 * result zero if success, otherwise a negative error code
 *
 * Tries to determine the next card from given card number.
 * If card number is -1, then the first available card is
 * returned. If the result card number is -1, no more cards
 * are available.
 * */
int mixer_card_next(int *rcard)
{
    int card;

    if (rcard == NULL)
        return -EINVAL;

    card = *rcard;
    card = card < 0 ? 0 : card + 1;
    for (; card < 32; card++) {
        if (mixer_card_load(card)) {
            *rcard = card;
			ALOGE("%s card = %d",__func__,card);
            return 0;
        }
    }
    *rcard = -1;
    return 0;
}

/**
 * Transplant from android 4.1 alsa-lib
 * brief Get next PCM device number
 * param device current device on entry and next device on return
 * return 0 on success otherwise a negative error code
 * */
int mixer_ctl_pcm_next_device(struct mixer *mixer, int *device)
{
    assert(mixer && device);

    if (ioctl(mixer->fd, SNDRV_CTL_IOCTL_PCM_NEXT_DEVICE, device) < 0)
        return -errno;
    return 0;
}

/**
 * Transplant from android 4.1 alsa-lib
 * brief Get info about a PCM device
 * param info PCM device id/info pointer
 * return 0 on success otherwise a negative error code
 **/
int mixer_ctl_pcm_info(struct mixer *mixer, snd_pcm_info_t *info)
{
    assert(mixer && info);

    if (ioctl(mixer->fd, SNDRV_CTL_IOCTL_PCM_INFO, info) < 0)
        return -errno;
    return 0;
}

/**
 * Transplant from android 4.1 alsa-lib
 * brief Get card identifier from a CTL card info
 * param obj CTL card info
 * return card identifier
 **/
const char *mixer_ctl_card_info_get_id(const struct snd_ctl_card_info *obj)
{
    assert(obj);
    return (const char *)obj->id;
}

/**
 * Transplant from android 4.1 alsa-lib
 * brief Get card identifier from a CTL card info
 * param obj CTL card info
 * return card name
 **/
const char *mixer_ctl_card_info_get_name(const struct snd_ctl_card_info *obj)
{
    assert(obj);
    return (const char *)obj->name;
}
