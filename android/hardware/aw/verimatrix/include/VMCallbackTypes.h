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

#ifndef _VMCALLBACKTYPES_H_
#define _VMCALLBACKTYPES_H_

typedef enum {
    eVcasVersion_31x = 0,
    eVcasVersion_320,
    eVcasVersion_321,
    eVcasVersion_330,
    eVcasVersion_331,
    eVcasVersion_340,
    eVcasVersion_350,
    eVcasVersion_360,
    eVcasVersion_370
} eVcasVersion_t;
#define MAX_VCAS_PROTO eVcasVersion_340

typedef enum {
   eVmDecodeInit = -1,                   // Initial state to detect first decode
   eVmDecodeOk = 0,                      // Decode proceeding
   eVmDecodeFailurePinRequired = 100,    // program Rating Level exceeds STB's
   eVmDecodeFailureKeyNotAvailable,      // Could be not authorized or network error getting key
   eVmDecodeFailureBlackout,             // Service Class >= 0xFF0000 matched
   eVmDecodeFailureTA,                   // decryption fail due to trustzone applet error.
   eVmDecodeFailureOther                 // TBD
} eVmDecodeResult;
// the following enum was deprecated because it was too specific 
// for the general failure being reported. This define is here to
// support any existing code
#define   eVmDecodeFailureNoServiceClass    eVmDecodeFailureKeyNotAvailable

typedef struct keydata_s {
    int control_flag;       /* non-zero means this key is valid */
    unsigned char key[32];
    unsigned char iv[32];
} keydata_t;

typedef enum {
    eEncAlg_null = 0,
    eEncAlg_RC4 = 1, 
    eEncAlg_AESECBT = 2,
    eEncAlg_proprietary1 = 3,
    eEncAlg_AESECBL = 4,
    eEncAlg_DVBCSA = 5,
    eEncAlg_proprietary2 = 6,
    eEncAlg_AESCBC1 = 7
} eEncryptionAlgorithm_t;

#define nMaxNumEncAlg 5    /* max number of simultaneous hw algorithms supported */

typedef struct descramble_param_s {
    eEncryptionAlgorithm_t encAlg;
    int    keylength;
    struct keydata_s odd;
    struct keydata_s even;        
    int    iCwp_data_length;
    void  *pCwp_data;  // NULL means clear CW, otherwise target specific structure

} descramble_param_t;

typedef enum {
    eEmmTransportUnknown = 0,
    eEmmTransportOutband,
    eEmmTransportInband
} emm_transport_t;

typedef struct emm_transport_info_s {
    emm_transport_t emm_transport_method;
    unsigned short  emm_multicast_port;
    unsigned short  emm_ca_system_id;
    char            emm_multicast_addr[ 256 ];
} emm_transport_info_t;

#ifdef __cplusplus
  extern "C" {
#endif

#ifndef WIN32
  #define __cdecl
#endif
      
typedef void (__cdecl *RenderFunc)(void *, unsigned char *, unsigned long);

typedef int  (__cdecl *DataWriteFunc)(void *, char *, int  );
typedef int  (__cdecl *DataReadFunc )(void *, char *, unsigned int *);

typedef int  (__cdecl *PasswordSaveFunc)(void * pArg, char * pSubjKeyId, char * pEncryptedPwd, int iPwdLen);
typedef int  (__cdecl *PasswordRetrieveFunc)(void * pArg, char * pSubjKeyId, char * pEncryptedPwd, int iMaxLen, int * pPwdLen);

typedef void (__cdecl *StreamChangeCallback)(void * pArg, unsigned long dwStreamId);

typedef void (__cdecl *LoggingCallbackFunc)(void *, char *);

typedef void (__cdecl *tDecodeFailureCallback) (void *pUserData,          // for caller's use
                                                unsigned long lChannel,   // channel decode failed on
                                                eVmDecodeResult eReason); // failure code

typedef void (__cdecl *tUserDataCallback) (void *pUserParam,       // for caller's use
                                           int   userdataLength,   // userdata length
                                           void *pUserDataValue);  // userdata pointer

typedef int (__cdecl *tControlWordCallback) (void *pUserParam, descramble_param_t *dscParam); 

typedef void (__cdecl *tFingerPrintingCallback) (void *pUserParam,	// for caller's use
						 int	FingerPrintingdataLength, 	// fingerPrinting data length
						 void	*pFingerPrintingData,				// fingerPrinting data pointer
						 int	localdataLength,						// local data pointer
						 void	*pLocalData);								// local data pointer

typedef void (__cdecl *tCopyControlCallback) (void *pUserParam,		// for caller's use
												 int  dataLength,	// data length
												 void *pData);		// data pointer

typedef void (*tOsdMessageCallback) (void *pUserParam, unsigned int nDataSize, void *pOsdMessage );

typedef void (*tIrdOpaqueDataCallback) (void*pUserParam, unsigned int nDataSize, void *pIrdOpaqueData );

typedef void (*tEmmTransportInfoCallback)( void * pUserParam, emm_transport_info_t * pEmmInfo );

#ifdef __cplusplus
  }
#endif


#endif // _VMCALLBACKTYPES_H_

