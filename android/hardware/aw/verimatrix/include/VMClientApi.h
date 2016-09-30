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

#ifndef _VMCLIENT_API_H_
#define _VMCLIENT_API_H_

#include <stdint.h>
#include "VMCallbackTypes.h"

#pragma GCC visibility push(default)

#ifdef __cplusplus
extern "C" {
#endif

// ---------- Common Functions ----------
int VMConfig( const char * pStorePath, const char *pIpAddress, 
        unsigned short nPort, unsigned int nTimeout, unsigned int nRetries, 
        unsigned short nIpVersionPreferred );

int VMConfigOffline( const char *pStoreFilePath );

void * VMCreateContext( const char * pINIFileName
                                           , int bUseIniFile
                                           );
int   VMResetStream(void *pContext);
int   VMDecryptStreamData( void *pContext
                                              , unsigned char *pBuffer
                                              , int iBufferLength
                                              , int * piBytesProcessed
                                              );
void  VMLogVersion(void);

#define VMGetCASystemID(x)   0x5601

int VMSetECM( void * pContext, 
                              int isSection, 
							  int pid,
                              unsigned char *pBuffer, 
                              int iBufferLength);

int   VMUpdateChannelKeys(void *pContext);
int   VMSetCallback( void *pContext
                                        , RenderFunc fnCallback
                                        , unsigned long size
                                        , void * pArg
                                        );
void VMRegisterDecodeFailureCallback( void *pContext
                         , tDecodeFailureCallback pDecodeFailureCallback
                         , void * pUserParam
                         );
void  VMFlushBuffer( void *pContext, int bTriggerCallback );

int VMDestroyClient( void );

// ---------- Special Functions ----------
const char * VMGetVersionString(void);
int   VMRequestVODKey(void *pContext,  int iMovieID );
int   VMRemoveVodKey( void * pContext, int iMovieID );
int   VMPurgeVodKeyCache( void * pContext );
int   VMPurgeLastKeyCache( void * pContext );
int   VMPurgePvrKeyCache( void * pContext );
int   VMGetKeyUpdateWindow(void *pContext
                                               , int *piMinTime
                                               , int *piMaxTime
                                               , int *piSuggestedTime
                                               );
void  VMSetStoreReadCallback( DataReadFunc fnReader
                                                 , void * pArg 
                                                 );
void  VMSetStoreWriteCallback( DataWriteFunc fnWriter
                                                  , void * pArg 
                                                  );
void  VMSetRatingLevel( void *pContext
                                      , unsigned char ucRatingLevel );

void  VMOverrideRatingLevel( void *pContext);

unsigned char VMGetRatingLevel( void *pContext 
	                                           , unsigned char *pbOverride 
											   , unsigned char *pucRatingLevel);

void  VMRegisterUserdataCallback ( void *pContext
                                        , tUserDataCallback pUserDataFunction
                                        , void *pUserParam
                                        );

int   VMLogMessage( int          iLevel
                                       , const char * pMsg 
                                       );
void   VMDestroyContext(void *pContext);

int	VMRegisterFingerPrintingCallback ( void *pContext
                                        , tFingerPrintingCallback pFingerPrintingFunction
                                        , void *pUserParam
                                        );
int	VMRegisterCopyControlCallback ( void *pContext
                                        , tCopyControlCallback pCopyControlFunction
                                        , void *pUserParam
                                        );
int	VMRegisterOsdMessageCallback ( tOsdMessageCallback pCallbackFunction, void *pUserParam );

int VMSetEMM( const unsigned char **pTSPacketBuffer, unsigned int *nNumTSPackets );

int VMRegisterIrdOpaqueCallback( tIrdOpaqueDataCallback pCallbackFunction, void *pUserParam );

void VMSetEmmInfoCallback( tEmmTransportInfoCallback fn, void * pArg );

// ---------- INI Parameter Functions ----------
void  VMSetErrorLevel(void *pContext, int iErrLevel);

int VMSetMiddlewareAuthenticator( const unsigned char * pData
                                , unsigned int nLength 
                                );

int VMSetVcasCommunicationProtocol( eVcasVersion_t eVersion );

//-------------------------------------------------------------------
// Function:    GetUniqueIdentifier
//
// Purpose:     Returns the ID string used by the client to identify
//              itself to the VCAS server.
//
// Parameters:  buffer = buffer into which to copy ID
//              maxlen = size of the buffer
//              len    = used to return length of the ID
//
// Returns:     0 on Success.
//              VMCLIENT_ERR_BADARG on NULL len.
//              VMCLIENT_ERR_SIZE   if provided buffer is too small or NULL.
//                                  Required size returned in len.
//              VMCLIENT_ERR_NOBUF  if failed to get ID.
//-------------------------------------------------------------------
int GetUniqueIdentifier( char * buffer
                                       , unsigned int maxlen
                                       , unsigned int * len );
int VMSetBskId( const char * bsk_id );
int VMBindToServerByName( const char * server_name );


#define VMX_PUBAUTH_MAXIDLEN         128
#define VMX_PUBAUTH_AUTHENTICATORLEN 256

int  VMServiceStart(void *pContext, uint8_t bServiceIdx, uint8_t bPidCount, unsigned short *pawStreamPid);
void  VMServiceStop(void *pContext, uint8_t bServiceIdx);


#define VMCLIENT_ERR_NOCONTEXT       -1
#define VMCLIENT_ERR_CATCH           -2
#define VMCLIENT_ERR_FEATUREDISABLED -3
#define VMCLIENT_ERR_SIZE            -4
#define VMCLIENT_ERR_BADARG          -5
#define VMCLIENT_ERR_NOBUF           -6
#define VMCLIENT_ERR_UNSUPPORTED     -7

#define VMFMT_VCASDEFAULT        0
#define VMFMT_VCAS1153SB109      1
#define VMFMT_VCAS1153           1
#define VMFMT_VCAS1154           2
#define VMFMT_VCAS1155           3
#define VMFMT_VCAS1156           4
#define VMFMT_VCAS1157           5

// Values for the iInfoRequested parameter of the VMGetInfo function
#define REQINFO_NOTHING             0
#define REQINFO_VERSIONSTRING       1
#define REQINFO_INIFILENAME         2
#define REQINFO_OPERATORID          3
#define REQINFO_AUTHORIZEDSERVICES  4
#define REQINFO_ISSERVICEAUTHORIZED 5
#define REQINFO_RATINGLEVEL         6
#define REQINFO_CURRENTSTREAM       7
#define REQINFO_KEYUPDATEWINDOW     8

#define STREAM_TYPE_UNKNOWN   0
#define STREAM_TYPE_ECM       1
#define STREAM_TYPE_ECMVOD    2

#ifdef __cplusplus
}
#endif

#pragma GCC visibility pop


#endif


