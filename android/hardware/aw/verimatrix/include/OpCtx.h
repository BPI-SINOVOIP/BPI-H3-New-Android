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

#ifndef _OPCTX_H_
#define _OPCTX_H_

#include "VMCallbackTypes.h"
#include "OperatorInstance.h"

typedef void (*tEmmDebugCallback) (void *pUserParam, unsigned char *pTsPacket, unsigned int nDataSize );

class COperatorContext
{
public:
    static const unsigned int MaxIdentifierLength = 64;
    static const unsigned int MaxStorePathLength = 1024;
    static const unsigned int MaxIpAddressLength = 128;

    // Tags used to identify data in the LocalData tuples (tag/length/value)
    static const uint16_t FingerprintTag_ID      = 0x0001;
    static const uint16_t FingerprintTag_Version = 0x0002;
    static const uint16_t FingerprintTag_Time    = 0x0003;

    char m_strStbId[  COperatorContext::MaxIdentifierLength ];
    char m_strChipId[ COperatorContext::MaxIdentifierLength ];
    char m_strBskId[  COperatorContext::MaxIdentifierLength ];
    char m_strStorePath[ COperatorContext::MaxStorePathLength ];
    char m_strIpAddr[ COperatorContext::MaxIpAddressLength];
    unsigned short m_nBootPort;
    unsigned int   m_nTimeout;
    unsigned int   m_nRetries;
    COperatorInstance::eIpPreferrence m_eIpPreferrence;
    COperatorInstance m_Operator;

     
    tFingerPrintingCallback   m_pFnFingerprint;
    void                    * m_pArgFingerprint;

    tIrdOpaqueDataCallback    m_pFnIrd;
    void                    * m_pArgIrd;

    tOsdMessageCallback       m_pFnOsdMsg;
    void                    * m_pArgOsdMsg;

    tEmmDebugCallback         m_pFnQAEMM;
    void                    * m_pArgQAEMM;

    tEmmTransportInfoCallback m_pFnInfoEMM;
    void                    * m_pArgInfoEMM;

public:
    COperatorContext( const char * stb_id,
                      const char * chip_id,
                      const char * bsk_id,
                      const char * pStorePath,
                      const char *pIpAddress,
                      unsigned short nBootPort,
                      unsigned int nTimeout,
                      unsigned int nRetries,
                      unsigned short nIpPreferrence
                    );
    virtual ~COperatorContext();

    void Clear();


    void SetCallback_Fingerprint(  ::tFingerPrintingCallback pFn, void * pArg )
        {
            m_pFnFingerprint  = pFn;
            m_pArgFingerprint = pArg;
        };

    void SetCallback_Ird(  ::tIrdOpaqueDataCallback pFn, void * pArg )
        {
            m_pFnIrd  = pFn;
            m_pArgIrd = pArg;
        };

    void SetCallback_OsdMsg( ::tOsdMessageCallback pFn, void * pArg )
        {
            m_pFnOsdMsg  = pFn;
            m_pArgOsdMsg = pArg;
        };

    void SetCallback_QAEMM( ::tEmmDebugCallback pFn, void * pArg )
        {
            m_pFnQAEMM  = pFn;
            m_pArgQAEMM = pArg;
        };

    void SetCallback_EMMInfo( ::tEmmTransportInfoCallback pFn, void * pArg )
        {
            m_pFnInfoEMM  = pFn;
            m_pArgInfoEMM = pArg;
        };

    uint32_t GetId()
        {
            return m_Operator.GetContextId(); 
        };

private:
    // make copy constructor and assignment operator inaccessible
    COperatorContext(const COperatorContext &ref);
    COperatorContext &operator=(const COperatorContext &ref);

    void HandleOsdFingerprint( uint16_t u16MsgType
                             , uint8_t  u8MsgDuration
                             , uint8_t  u8MsgQuadrant
                             , uint8_t  u8Reserved
                             );
    static void EventCb( void         * pUserParam
                , eVmxEvent_t    eEventType
                , const void   * pEventData 
                );

};

#endif


