#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <Timers.h>
#include "FdHelper.h"

int printHelp() {
    printf("usage: fdhelp [-f file] [-p pid] [-s] [-l ms]\n");
    printf("-f file : print all process open file*\n");
    printf("-p pid : print all the fd pid open\n");
    printf("-l ms : loop print per millisecond\n");
    printf("-s : mean not print pipe, socket and so on\n");
    printf("-h : print help\n");
    return 0;
}

int main(int argc, char * argv[]) {
    if (argc == 1) return printHelp();

    char *path=NULL;
    char *pidStr=NULL;
    bool simplePrint=false;
    int interval=0;
    do {
        int c;
        c = getopt(argc, argv, "f:p:l:hs");
        if (c == EOF)
            break;
        switch (c) {
        case 'f':
            path=optarg;
            break;
        case 'p':
            pidStr=optarg;
            break;
        case 'l':
            interval=FdHelper::getPid(optarg);
            if (interval<0)
                interval=0;
            break;
        case 'h':
            printHelp();
            break;
        case 's':
            simplePrint=true;
            break;
        case '?':
            printHelp();
            exit(1);
        }
    } while (1);


    do {
        if (path==NULL&&pidStr==NULL)
        {
            return 0;
        }
        else if (pidStr!=NULL)
        {
            char name[PATH_MAX];
            int pid= FdHelper::getPid(pidStr);
            if (pid>0)
            {
                if (path!=NULL)
                    FdHelper::checkFileDescriptorSymLinks(pid,path);
                else if (simplePrint)
                    FdHelper::checkFileDescriptorSymLinks(pid,"/");
                else
                    FdHelper::checkFileDescriptorSymLinks(pid,"");
            }
        }
        else if (path!=NULL)
        {
            FdHelper::queryProcessesWithOpenFiles(path);
        }

        if (interval==0)
            break;
        else
            usleep(interval*1000);

        printf("*********** %ld ms ***********\n",long(ns2ms(systemTime())));
    } while (1);
    return 0;
}
