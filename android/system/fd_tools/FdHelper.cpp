/*
 * Copyright (C) 2008 The Android Open Source Project
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

#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <dirent.h>
#include <ctype.h>
#include <pwd.h>
#include <stdlib.h>
#include <poll.h>
#include <sys/stat.h>
#include <signal.h>

#include "FdHelper.h"

int FdHelper::readSymLink(const char *path, char *link, size_t max) {
    struct stat s;
    int length;

    if (lstat(path, &s) < 0)
        return 0;
    if ((s.st_mode & S_IFMT) != S_IFLNK)
        return 0;
   
    // we have a symlink    
    length = readlink(path, link, max- 1);
    if (length <= 0) 
        return 0;
    link[length] = 0;
    return 1;
}

int FdHelper::pathMatchesMountPoint(const char* path, const char* mountPoint) {
    int length = strlen(mountPoint);
    if (length > 0 && strncmp(path, mountPoint, length) == 0) {
        // we need to do extra checking if mountPoint does not end in a '/'
        if (mountPoint[length - 1] == '/')
            return 1;
        // if mountPoint does not have a trailing slash, we need to make sure
        // there is one in the path to avoid partial matches.
        return (path[length] == 0 || path[length] == '/');
    }
    else if (length==0)
        return 1;
    return 0;
}

void FdHelper::getProcessName(int pid, char *buffer, size_t max) {
    int fd;
    snprintf(buffer, max, "/proc/%d/cmdline", pid);
    fd = open(buffer, O_RDONLY);
    if (fd < 0) {
        strcpy(buffer, "???");
    } else {
        int length = read(fd, buffer, max - 1);
        buffer[length] = 0;
        close(fd);
    }
}

int FdHelper::checkFileDescriptorSymLinks(int pid, const char *mountPoint) {

    // compute path to process's directory of open files
    bool firstprint=true;
    char    path[PATH_MAX];
    sprintf(path, "/proc/%d/fd", pid);
    DIR *dir = opendir(path);
    if (!dir)
        return 0;

    // remember length of the path
    int parent_length = strlen(path);
    // append a trailing '/'
    path[parent_length++] = '/';

    struct dirent* de;
    while ((de = readdir(dir))) {
        if (!strcmp(de->d_name, ".") || !strcmp(de->d_name, "..")
                || strlen(de->d_name) + parent_length + 1 >= PATH_MAX)
            continue;

        // append the file name, after truncating to parent directory
        path[parent_length] = 0;
        strcat(path, de->d_name);

        char link[PATH_MAX];

        if (readSymLink(path, link, sizeof(link)) && pathMatchesMountPoint(link, mountPoint)) {
            //printf
            if (firstprint)
            {
                firstprint=false;
                char name[PATH_MAX];
                getProcessName(pid, name, sizeof(name));
                printf("proc: %s   pid: %d \n",name,pid);
            }
            printf("%s\n",link);
        }
    }
    if (!firstprint)
    {
        printf("\n");
    }
    closedir(dir);
    return 0;
}

int FdHelper::checkSymLink(int pid, const char *mountPoint, const char *name) {
    char    path[PATH_MAX];
    char    link[PATH_MAX];

    sprintf(path, "/proc/%d/%s", pid, name);
    if (readSymLink(path, link, sizeof(link)) && pathMatchesMountPoint(link, mountPoint)) 
        return 1;
    return 0;
}

int FdHelper::getPid(const char *s) {
    int result = 0;
    while (*s) {
        if (!isdigit(*s)) return -1;
        result = 10 * result + (*s++ - '0');
    }
    return result;
}

void FdHelper::queryProcessesWithOpenFiles(const char *path) {
    DIR*    dir;
    struct dirent* de;

    if (!(dir = opendir("/proc"))) {
        printf("opendir failed (%s)", strerror(errno));
        return;
    }

    while ((de = readdir(dir))) {
        int killed = 0;
        int pid = getPid(de->d_name);
        if (pid == -1)
            continue;
        checkFileDescriptorSymLinks(pid, path);
        checkSymLink(pid, path, "exe");
    }
    closedir(dir);
}
