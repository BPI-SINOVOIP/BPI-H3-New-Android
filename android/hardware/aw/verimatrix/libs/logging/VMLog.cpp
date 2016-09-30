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
 * (a) through (d) of the Commercial Computer Software-Restricted Rights
 * at FAR 52.227-19 when applicable, or in subparagraph (c)(1)(ii) of the
 * Rights in Technical Data and Computer Software clause at
 * DFARS 252.227-7013, and in similar clauses in the NASA FAR supplement,
 * as applicable. Manufacturer is Verimatrix, Inc.
 */ 
#include <stdio.h>
#include <sys/stat.h> /* for umask */
#ifdef MULTITHREAD_TEST
#include <pthread.h>
#endif

#ifdef __ANDROID__
#define LOG_TAG "VMLog"
#include <utils/Log.h>
#endif

#include "VMLog.h"


FILE *m_pDebugFile = NULL;

/********************************************************************
***                                                               ***
***                  Required Log Functions                       ***
***                                                               ***
********************************************************************/

/*---------------------------------------------------------------------
 *  LogMessage
 *
 *  Purpose:    Requests a message be written to the log
 *
 *  Parameters:
 *              pTimrStr = String containing current time and message level
 *                         May be NULL. Example:
 *                         "(2015-02-12 13:34:49:934)	[  INFO]	"
 *              pMsg     = Message string to be logged.
 *
 *  Returns:    Nothing.
 */
void LogMessage(const char * pTimeStr, const char * pMsg)
{
    /* output log message by printf */
#ifdef MULTITHREAD_TEST
    printf("%lu --> %s%s\n", pthread_self(), pTimeStr, pMsg);
#else
    printf("%s%s\n", pTimeStr, pMsg);
#endif

#ifdef __ANDROID__
    ALOGD("%s%s", pTimeStr, pMsg);
#endif

    /* Output log message to file, if open. */
    if(m_pDebugFile)
    {
        fprintf(m_pDebugFile, "%s%s\n", pTimeStr, pMsg);
        fflush(m_pDebugFile);
    }
}

/********************************************************************
***                                                               ***
***          Implementation Dependant Log Functions               ***
***                                                               ***
***  These functions are not called by the ViewRight client       ***
***                                                               ***
********************************************************************/

/*---------------------------------------------------------------------
 * OpenLog
 *
 * Purpose:
 *     This implementation dependant function is never called by the 
 *     ViewRight client.  This function is intended to be called by the
 *     application if logging to a file is desired.
 *
 *     Opens the specified Log.  If iClearLog is non-zero, any existing Log
 *     data should be cleared.  Otherwise, Log data should be appended to the
 *     existing data.
 *
 * Return 0 on success, non-zero on failure
 */
int OpenLog(const char * LogName, int iAppendLog)
{
    mode_t      mask = 0;
    const char *mode = (iAppendLog != 0)? "a" : "w";

    if ( NULL == LogName )
    {
        return -1;
    }

    /* Create Log file with RW permissions for all */
    mask = umask(0000);

    /* Open the file */
    m_pDebugFile = fopen( LogName, mode );

    /* Return file permission mask to the previous value */
    umask( mask );

    if( m_pDebugFile == NULL )
    {
        return -1;
    }
    return 0;
}

/*---------------------------------------------------------------------
 * CloseLog
 *
 * Purpose:
 *     This implementation dependant function is never called by the 
 *     ViewRight client.  This function is intended to close the open
 *     log file.
 *
 *
 * Return 0 on success, non-zero on failure
 */
int CloseLog()
{
    /* Close the file if it is open. */
    if ( m_pDebugFile )
    {
        fclose( m_pDebugFile );
        m_pDebugFile = NULL;
    }
    return 0;  
}




