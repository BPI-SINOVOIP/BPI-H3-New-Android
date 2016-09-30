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

#ifndef _DECCTX_H_
#define _DECCTX_H_

#include "VMCallbackTypes.h"
#include "HardwareStreamDecryption.h"
#include "SoftwareStreamDecryption.h"

class COperatorInstance;


class CDecryptionContext
{
public:

    typedef enum 
    {
        UNKNOWN = 0,
        HW = 1,
        SW = 2
    } state_t;

    tDecodeFailureCallback   m_pFnDecodeFail;
    void                   * m_pArgDecodeFail;

    tControlWordCallback     m_pFnSetCw;
    void                   * m_pArgSetCw;

    tUserDataCallback        m_pFnUserdata;
    void                   * m_pArgUserdata;

    tCopyControlCallback     m_pFnCopyCtrl;
    void                   * m_pArgCopyCtrl;

    CBaseDecryption        * m_pDecCtx;

    COperatorInstance      * m_pOpInst;

    uint32_t        m_idLast;
    eVmDecodeResult m_resLast;

    CDecryptionContext( COperatorInstance * pOp, unsigned int id );
    virtual ~CDecryptionContext();

    void SetCallback_DecodeFail(  tDecodeFailureCallback pFn, void * pArg )
        {
            m_pFnDecodeFail  = pFn;
            m_pArgDecodeFail = pArg;
        };
    void SetCallback_ControlWord( tControlWordCallback pFn, void * pArg )
        {
            m_pFnSetCw = pFn;
            m_pArgSetCw = pArg;
        };
    void SetCallback_Userdata(  ::tUserDataCallback pFn, void * pArg )
        {
            m_pFnUserdata  = pFn;
            m_pArgUserdata = pArg;
        };
    void SetCallback_CopyControl(  ::tCopyControlCallback pFn, void * pArg )
        {
            m_pFnCopyCtrl  = pFn;
            m_pArgCopyCtrl = pArg;
        };

    uint32_t GetId()
        {
            if(m_pDecCtx != NULL)
            {
                return m_pDecCtx->GetContextId();
            }
			else
				return -1;
        };

    static void EventCb( void         * pUserParam
                       , eVmxEvent_t    eEventType
                       , const void   * pEventData 
                       );

    uint8_t m_uiServiceIdx;
    uint8_t m_uiPidCount;
    unsigned short * m_pawStreamPid;
    bool m_bHasServiceStarted;

    bool m_bIsRatingSet;
    unsigned char m_ucRatingLevel;

    unsigned int m_uiId;
    state_t m_eState;
private:
    // make copy constructor and assignment operator inaccessible
    CDecryptionContext(const CDecryptionContext &ref);
    CDecryptionContext &operator=(const CDecryptionContext &ref);



    void MakeDecodeFailureCallback( uint32_t id, eVmDecodeResult res );	
};

#endif



