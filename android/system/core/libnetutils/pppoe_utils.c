/*
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <cutils/properties.h>
#include <sys/types.h>
#include <sys/wait.h>

#define MAX_INTERFACE_LENGTH  25
#define MAX_USERNAME_LENGTH   25
static const char START_PPPOE_DAEMON[] = "pppoe";
static const char STOP_PPPOE_DAEMON[] = "pppoe_stop";
static const char DAEMON_PROP_NAME[] = "init.svc";

static const int NAP_TIME = 100;   /* wait for 100ms at a time */
                                  /* when polling for property values */
static char errmsg[100];

static int fill_ip_info(const char *phyif,
                     char *pppif,
                     char *ipaddr,
                     char *gateway,
                     uint32_t *prefixLength,
                     char *dns[])
{
    char prop_name[PROPERTY_KEY_MAX];
    char prop_value[PROPERTY_VALUE_MAX];
    int x;

    snprintf(prop_name, sizeof(prop_name), "net.%s-pppoe.phyif", phyif);
    if(!property_get(prop_name, pppif, NULL)) {
        strcpy(pppif, "ppp0");
    }

    snprintf(prop_name, sizeof(prop_name), "net.%s-%s.local-ip", phyif, pppif);
    property_get(prop_name, ipaddr, NULL);
    snprintf(prop_name, sizeof(prop_name), "net.%s-%s.remote-ip", phyif, pppif);
    property_get(prop_name, gateway, NULL);
    {
        int p;
        in_addr_t mask = ntohl(inet_addr("255.255.255.254"));
        for (p = 0; p < 32; p++) {
            if (mask == 0) break;
            // check for non-contiguous netmask, e.g., 255.254.255.0
            if ((mask & 0x80000000) == 0) {
                snprintf(errmsg, sizeof(errmsg), "pppoe gave invalid net mask %s", prop_value);
                return -1;
            }
            mask = mask << 1;
        }
        *prefixLength = p;
    }
    for (x=0; dns[x] != NULL; x++) {
        snprintf(prop_name, sizeof(prop_name), "net.%s-%s.dns%d", phyif, pppif, x+1);
        property_get(prop_name, dns[x], NULL);
    }

    return 0;
}

static const char *ipaddr_to_string(in_addr_t addr)
{
    struct in_addr in_addr;

    in_addr.s_addr = addr;
    return inet_ntoa(in_addr);
}

char *pppoe_get_errmsg() {
    return errmsg;
}

/*
 * Get pppoe status.
 */
#define PPPOE_STOPPED 0
#define PPPOE_STARTING 1
#define PPPOE_STARTED 2
#define PPPOE_STOPPING 3
int get_pppoe_status(const char *interface)
{
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    char status_prop_name[PROPERTY_KEY_MAX] = {'\0'};
    pid_t pid = 1;

    snprintf(status_prop_name, sizeof(status_prop_name), "net.%s-pppoe.status", interface);

    if (!property_get(status_prop_name, prop_value, NULL)) {
        return PPPOE_STOPPED;
    } else if(0 == strcmp(prop_value, "starting")) {
        return PPPOE_STARTING;
    } else if(0 == strcmp(prop_value, "started")) {
        return PPPOE_STARTED;
    } else if(0 == strcmp(prop_value, "stopping")) {
        return PPPOE_STOPPING;
    } else {
        return PPPOE_STOPPED;
    }
}

/*
 * Stop the pppoe.
 */
int stop_pppoe(const char *interface)
{
    const char *ctrl_prop = "ctl.start";
    char daemon_cmd[PROPERTY_VALUE_MAX + MAX_INTERFACE_LENGTH + MAX_USERNAME_LENGTH + 2] = {'\0'};
    int maxnaps = (10 * 1000) / NAP_TIME;    // wait 10s
    int ret = 0;

    if (PPPOE_STOPPED == get_pppoe_status(interface)) {
        return 0;
    }

    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s:%s", STOP_PPPOE_DAEMON, interface);
    property_set(ctrl_prop, daemon_cmd);
    while (maxnaps-- > 0) {
        usleep(NAP_TIME * 1000);
        if(PPPOE_STOPPED == get_pppoe_status(interface)) {
            return 0;
        }
    }
    return -1;
}
/*
 * start pppoe & pppd
 */
int start_pppoe(const char *phyif,
                    const char *username,
                    char *pppif,
                    char *ipaddr,
                    char *gateway,
                    uint32_t *prefixLength,
                    char *dns[])
{
    int i;
    const char *ctrl_prop = "ctl.start";
    char daemon_cmd[PROPERTY_VALUE_MAX + MAX_INTERFACE_LENGTH + MAX_USERNAME_LENGTH + 2] = {'\0'};
    char status_prop_name[PROPERTY_KEY_MAX] = {'\0'};
    int maxnaps = (60 * 1000) / NAP_TIME;    // wait 60s

    while(PPPOE_STARTING == get_pppoe_status(phyif) ||
            PPPOE_STARTED == get_pppoe_status(phyif)) {
        return 0;
    }
    // wait pppoe state change to stopped
    while(PPPOE_STOPPING == get_pppoe_status(phyif)) {
        usleep(NAP_TIME * 1000);
    }
    /*
     * TODO: set pppoe status to "starting"
     */
    //snprintf(status_prop_name, sizeof(status_prop_name), "net.%s-pppoe.status", phyif);
    //property_set(status_prop_name, "starting");

    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:%s %s", START_PPPOE_DAEMON, phyif,
            phyif, username);
    property_set(ctrl_prop, daemon_cmd);

    /* Wait for the daemon to return a result */
    for(i = 0; i < maxnaps; i++) {
        usleep(NAP_TIME * 1000);
        if (get_pppoe_status(phyif) == PPPOE_STARTED) {
            break;
        } else if (PPPOE_STOPPED == get_pppoe_status(phyif)) {
            snprintf(errmsg, sizeof(errmsg), "start pppoe result was failed");
            return -1;
        }
    }

    if( maxnaps == i) {
        snprintf(errmsg, sizeof(errmsg), " start pppoe result was timeout after 60s");
        stop_pppoe(phyif);
        return -1;
    }
    if (fill_ip_info(phyif, pppif, ipaddr, gateway, prefixLength, dns) == -1) {
        snprintf(errmsg, sizeof(errmsg), " fill ip info to dhcpResults error");
        return -1;
    }
    return 0;

}


