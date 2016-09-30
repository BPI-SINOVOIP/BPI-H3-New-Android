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
/*  File:           io.h                                                     */
/*  Description:    definitions for Verimatrix test app io                   */
/*                                                                           */
/*****************************************************************************/
#ifndef _IO_H_
#define _IO_H_

#define MTU 1500
#define TS_MTU 1316
#define TS_PACKET_SIZE 188

#define IOFILE      1
#define IOMCAST     2
#define IOUDP       3

extern int  InitInput(char *instream, int intype, char *pInterfaceAddress);
extern int  InitOutput(char *outstream, int outtype, char *pInterfaceAddress);
extern int  StreamInput(unsigned char *pBuffer);
extern int  StreamOutput(unsigned char *pBuffer);
extern void CloseInput(void);
extern void CloseOutput(void);

#endif /* _IO_H_ */

/* end of file io.h */
