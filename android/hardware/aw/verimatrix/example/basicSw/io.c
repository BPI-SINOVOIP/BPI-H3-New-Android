/*
* Copyright (c) 2014 Verimatrix, Inc.  All Rights Reserved.
* The Software or any portion thereof may not be reproduced in any form
* whatsoever except as provided by license, without the written consent of
* Verimatrix.
*
* THIS NOTICE MAY NOT BE REMOVED FROM THE SOFTWARE BY ANY USER THEREOF.
* NEITHER VERIMATRIX NOR ANY PERSON OR ORGANIZATION ACTING ON BEHALF OF
* THEM:
*
* 1. MAKES ANY WARRANTY OR REPRESENTATION WHATSOEVER, EXPRESS OR IMPLIED,
*    INCLUDING ANY WARRANTY OF MERCHANTABILITY OR FITNESS FOR ANY
*    PARTICULAR PURPOSE WITH RESPECT TO THE SOFTWARE;
*
* 2. ASSUMES ANY LIABILITY WHATSOEVER WITH RESPECT TO ANY USE OF THE
*    PROGRAM OR ANY PORTION THEREOF OR WITH RESPECT TO ANY DAMAGES WHICH
*     MAY RESULT FROM SUCH USE.
*
* RESTRICTED RIGHTS LEGEND:  Use, duplication or disclosure by the
* Government is subject to restrictions set forth in subparagraphs
* (a) through (d) of the Commercial Computer Software—Restricted Rights
* at FAR 52.227-19 when applicable, or in subparagraph (c)(1)(ii) of the
* Rights in Technical Data and Computer Software clause at
* DFARS 252.227-7013, and in similar clauses in the NASA FAR supplement,
* as applicable. Manufacturer is Verimatrix, Inc.
*/


/*****************************************************************************/
/*                                                                           */
/*  File:           io.c                                                     */
/*  Description:    Input and output functions for basic ca applications     */
/*                                                                           */
/*****************************************************************************/

/*-------------------------- System Includes --------------------------------*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <ctype.h>
#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

/*-------------------------- Local Includes ---------------------------------*/
#include "io.h"

/*-------------------------- Local Variables --------------------------------*/
static int inputtype             = 0;
static int outputtype            = 0;
static FILE *fInFile;
static FILE *fOutFile;
int             in_sockfd       = 0;
socklen_t       in_salen;
struct sockaddr *in_sa;

int             out_sockfd      = 0;
socklen_t       out_salen;
struct sockaddr *out_sa;

unsigned char in_sockBuffer[MTU];
unsigned char out_sockBuffer[TS_MTU];

/*-------------------------- Local Functions --------------------------------*/
static int getPacket(int sockfd,
              unsigned char *pBuffer,
              struct sockaddr *sa,
              socklen_t *salen);
static int sendPacket(int sockfd,
              unsigned char *pBuffer,
              int len,
              struct sockaddr *sa,
              socklen_t salen);
static int process_mcast_args(char *mcast_str,
                        int *pSockfd,
                        struct sockaddr **sa,
                        socklen_t *salen,
                        char *pInterfaceAddress);

/*----------------------------------------------------------------------------*/
/* InitInput() - opens a file or a network stream for input                   */
/*----------------------------------------------------------------------------*/
int  InitInput(char *instream, int intype, char *pInterfaceAddress)
{
   if (intype == IOFILE)
   {
      fInFile = fopen(instream,"r+");
      if ( fInFile == NULL ) {
          fprintf(stderr, "Failed to open input file '%s'\n", instream);
          return -2;
      }
   }
   else if (intype == IOMCAST)
   {
      if ( process_mcast_args( instream, &in_sockfd, &in_sa, &in_salen, pInterfaceAddress )) {
          fprintf(stderr, "Failed to join %s\n", instream);
          return -5;
      }
   }

   inputtype = intype;
   return 0;
}
/*----------------------------------------------------------------------------*/
/* InitOutput() - opens a file or a network stream for output                 */
/*----------------------------------------------------------------------------*/
int  InitOutput(char *outstream, int outtype, char *pInterfaceAddress)
{
   if (outtype == IOFILE)
   {
      fOutFile = fopen(outstream,"w+");
      if ( fOutFile == NULL ) {
          fprintf(stderr, "Failed to open output file '%s'\n", outstream);
          return -4;
      }
   }
   else if (outtype == IOMCAST)
   {
      if ( process_mcast_args(outstream, &out_sockfd, &out_sa, &out_salen, pInterfaceAddress )) {
          fprintf(stderr, "Failed to join %s\n", outstream);
          return -7;
      }
   }

   outputtype = outtype;
   return 0;
}
/*----------------------------------------------------------------------------*/
/* StreamInput() - retrieves a transport packet from file or network          */
/* NOTE: requires transport aligned packets or file!                          */
/*----------------------------------------------------------------------------*/
int  StreamInput(unsigned char *pBuffer)
{
   int result = 0;

   if (inputtype == IOFILE)
      result = fread(pBuffer,1,TS_PACKET_SIZE,fInFile);
   else if (inputtype == IOMCAST)
      result = getPacket(in_sockfd, pBuffer, in_sa, &in_salen);

   if (result != TS_PACKET_SIZE)
      return 0;

   return result;
}
/*----------------------------------------------------------------------------*/
/* StreamOutput() - sends a transport packet to file or network               */
/*----------------------------------------------------------------------------*/
int  StreamOutput(unsigned char *pBuffer)
{
   if (outputtype == IOFILE)
      fwrite(pBuffer, 1, TS_PACKET_SIZE, fOutFile);
   else if (outputtype == IOMCAST)
      sendPacket(out_sockfd,
                 pBuffer,
                 TS_PACKET_SIZE,
                 out_sa,
                 out_salen);

   return 0;
}

/*----------------------------------------------------------------------------*/
/* CloseInput() -                                                             */
/*----------------------------------------------------------------------------*/
void CloseInput(void)
{
   if (inputtype == IOFILE)
      fclose(fInFile);
   else if (inputtype == IOMCAST)
      close(in_sockfd);
}

/*----------------------------------------------------------------------------*/
/* CloseOutput() -                                                            */
/*----------------------------------------------------------------------------*/
void CloseOutput(void)
{
   if (outputtype == IOFILE)
      fclose(fOutFile);
   else if (outputtype == IOMCAST)
      close(out_sockfd);
}

/*----------------------------------------------------------------------------*/
/* getPacket() - utility to save 1316 bytes recvd but return 188 each call    */
/*----------------------------------------------------------------------------*/
static int getPacket(int sockfd,
              unsigned char *pBuffer,
              struct sockaddr *sa,
              socklen_t *salen)
{
    static int pindex = 0;
    static int rcvd = 0;

    // get more data
    if ( pindex >= rcvd ) {
        pindex = 0;
        rcvd = recvfrom(sockfd, in_sockBuffer, MTU, 0, sa, salen);
        if ( rcvd < TS_PACKET_SIZE )
            return rcvd;
        if ( rcvd % TS_PACKET_SIZE ) {
            printf("odd size packet\n");
        }
    }

    if (pindex+TS_PACKET_SIZE <= rcvd) {
        memcpy(pBuffer, in_sockBuffer + pindex, TS_PACKET_SIZE);
        pindex += TS_PACKET_SIZE;
        return TS_PACKET_SIZE;
    }

    return 0;
}

/*----------------------------------------------------------------------------*/
/* sendPacket() - utility to save 7 188 byte packets to send a full MTU       */
/*----------------------------------------------------------------------------*/
static int sendPacket(int sockfd,
              unsigned char *pBuffer,
              int len,
              struct sockaddr *sa,
              socklen_t salen)
{
    static int pindex = 0;

    // copy to buffer
    if (pindex + len < TS_MTU)
    {
        memcpy(out_sockBuffer + pindex, pBuffer, len);
        pindex += len;
    }
    else
    {
        memcpy(out_sockBuffer + pindex, pBuffer, TS_MTU-pindex);
        pindex = TS_MTU;
    }

    // if we have a 7 TS packets, send
    if ( pindex >= TS_MTU ) {
        pindex = 0;
        sendto(sockfd, out_sockBuffer, TS_MTU, 0, sa, salen);
    }

    return 0;
}

/*----------------------------------------------------------------------------*/
/* process_mcst_args() - utility to parse and open mcast sockets              */
/*----------------------------------------------------------------------------*/
static int process_mcast_args(char *mcast_str,
                        int *pSockfd,
                        struct sockaddr **sa,
                        socklen_t *salen,
                        char *pInterfaceAddress)
{
    int             i = 0;
    int             len, ret;
    char            *host, *port;
    char            *pPortStr;
    int             sockfd, n;
    struct addrinfo hints, *res, *ressave;
    struct ip_mreq  mreq;

    // parse the multicast string
    pPortStr = strchr(mcast_str, ':');
    if ( pPortStr ) {

        // get the port #
        pPortStr++;
        do {
            if ( !isdigit(pPortStr[i]) ) {
                return -1;
            }
        } while(pPortStr[++i]);

        port = (char *)malloc(strlen(pPortStr)+1);
        strcpy(port,pPortStr);

        // get the host addr
        len = strcspn(mcast_str,":");
        if ( !len ) {
            return -1;
        }
        host = (char *)malloc(len+1);
        memcpy(host,mcast_str,len);
        host[len] = '\0';
    }
    // do nothing if not multicast addr
    else
        return -1;

    // create an IPV4 udp socket
    memset(&hints, 0, sizeof(struct addrinfo));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_DGRAM;

    if ((n = getaddrinfo(host, port, &hints, &res)) != 0)
    {
        printf("udp_client error for %s, %s: %s",
                host, port, gai_strerror(n));
        return -2;
    }

    ressave = res;
    do
    {
        sockfd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
        if (sockfd >= 0)
            break;
    }
    while ( (res = res->ai_next) != NULL);

    if (res == NULL)
    {
        printf("udp_client error for %s, %s\n", host, port);
        return -2;
    }

    *sa = (struct sockaddr *)malloc(res->ai_addrlen);
    memcpy(*sa, res->ai_addr, res->ai_addrlen);
    *salen = res->ai_addrlen;

    freeaddrinfo(ressave);

    // bind socket to local address
    bind(sockfd, *sa, *salen);

    // join the multicast group
    memcpy(&mreq.imr_multiaddr,
            &((struct sockaddr_in *)*sa)->sin_addr,
           sizeof(struct in_addr));

    if ( pInterfaceAddress )
        mreq.imr_interface.s_addr = inet_addr(pInterfaceAddress);
    else
        mreq.imr_interface.s_addr = htonl(INADDR_ANY);

    ret = setsockopt(sockfd, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq));
    if ( ret ) {
        printf("could not join multicast group %s:%s\n\n",host, port);
        return -2;
    }

    *pSockfd = sockfd;
    return 0;
}

/* end of inout.c */
