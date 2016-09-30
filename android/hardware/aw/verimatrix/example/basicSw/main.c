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
/*  File:           main.c                                                   */
/*  Description:    Basic CA application to test Verimatrix CA               */
/*                                                                           */
/*****************************************************************************/

/*-------------------------- System Includes --------------------------------*/
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

/*-------------------------- Local Includes ---------------------------------*/
#include "io.h"
#include "VMClientApi.h"
#define ERR_DISABLED         -6667
#define ERR_OFFLINE          -6669

/*-------------------------- Local Variables --------------------------------*/
#define SERVERPORT 12686
#define PID(pPkt)               ((((pPkt)[1]<<8)+(pPkt)[2]) & 0x1FFF)

static char    *instream;
static int     intype = 0;
static char    *outstream;
static char    outtype = 0;
static int     nodecrypt = 0;
static char    *Storefilepath = NULL;
static char    *Storefile = NULL;
static char    *ServerIPaddress = NULL;
static char    *Portnumber = NULL;
static int     ServerPortno = 0;
static char    *pInterfaceAddress = NULL;
static int     bFreeStorefile = 0;
static int     bConfigSuccess = 0;
static int     bOffline = 0;

/*-------------------------- Local Functions --------------------------------*/
static int ProcessArgs(int argc, char **argv);
static int InitVerimatrixCA(void **pContext);
static int InitVCASConfigure(void);
static void usage(char *cmd);

int VMRegisterControlWordCallback( void *pContext
                                    , tControlWordCallback pControlWordCallback
                                    , void *pUserParam
                                    , int nAlgorithmCount
                                    , int *nAlgorithmList);



/*---------------------------------------------------------------------------*/
/* main() - simple packet by packet decrypt                                  */
/*          NOTE: in the real world you may want to handle an MTU or more    */
/*          just pass in a larger # of ts packets                            */
/*---------------------------------------------------------------------------*/
int main(int argc, char **argv)
{
   int result = 0;
   void *pContext = NULL;
   unsigned char pBuffer[TS_PACKET_SIZE];
   unsigned char ecmLast[TS_PACKET_SIZE];
   int iBytesRead;

   memset( ecmLast, 0, sizeof(ecmLast) );

   if ( (result = ProcessArgs(argc, argv)) ) return result;
   if ( (result = InitVCASConfigure()) ) return result;
   if ( (result = InitInput(instream, intype, pInterfaceAddress)) ) return result;
   if ( (result = InitOutput(outstream, outtype, pInterfaceAddress)) ) return result;

   result = InitVerimatrixCA(&pContext);
   if ( result && (result != ERR_OFFLINE) ) 
      return result;

   /* Decryption will be disabled until the request for the             */
   /* EMM Decryption Key succeeds. There is currently no API            */
   /* to check for this state. One solution would be for the main       */
   /* decrypt loop to continue if we see this error rather than exit.   */
   /* Also note that this error code is defined above.  Future          */
   /* versions may have different codes, or alternate methods           */
   /* for handling this condition.                                      */

   /* Start processing input stream */
   while(StreamInput(pBuffer))
   {
		
        result = nodecrypt? 0 : VMDecryptStreamData( pContext, pBuffer, TS_PACKET_SIZE, &iBytesRead);
        if ( result != 0 ) 
        {
            fprintf(stderr, "VMDecryptStreamData Error %d\n", result );
            return -1;
        }
        else
        {
            static int i = 0;			
            if( i == 1 )
            {
                printf("Decrypting.");
            }
            i++;
            if( !(i%1000) )
            {
                printf(".");
            }
            StreamOutput(pBuffer);
        }
    }

   CloseInput();
   CloseOutput();

   VMDestroyContext(pContext);

   if ( bFreeStorefile && Storefile ) free( Storefile );
   return result;
}


void MyDecodeFailureCallback( void *pUserData,          // for caller's use
                         unsigned long lChannel,   // channel decode failed on
                         eVmDecodeResult eReason); // failure code
void MyDecodeFailureCallback( void *pUserData,          // for caller's use
                         unsigned long lChannel,   // channel decode failed on
                         eVmDecodeResult eReason) // failure code
{
    printf("\n\nMyDecodeFailureCallback( %p, %lu, %d)\n\n", pUserData, lChannel, eReason);
}


int MyControlWordCallback(void *pUserParam, descramble_param_t *dscParam); 
int MyControlWordCallback(void *pUserParam, descramble_param_t *dscParam)
{
    int i;
    if ( !dscParam )
    {
        return 0;
    }
    printf("  Alg : %u\n", dscParam->encAlg );
    if ( dscParam->even.control_flag )
    {
        printf("  Even: " );
        for( i = 0; i < dscParam->keylength; i++ )
        {
            printf("%02x", dscParam->even.key[i]);
        }
        printf("\n");
    }
    else
    {
        printf("Even: NONE.\n");
    }
    if ( dscParam->odd.control_flag )
    {
        printf("  Odd : " );
        for( i = 0; i < dscParam->keylength; i++ )
        {
            printf("%02x", dscParam->odd.key[i]);
        }
        printf("\n");
    }
    else
    {
        printf("Odd : NONE.\n");
    }
    return 0;
}
/*-------------------------------------------------------------------------------*/
/* InitVCASConfigure() - setup the environment for Verimatrix client to interact */
/*                    with the VCAS Server.                                      */
/*-------------------------------------------------------------------------------*/
static int InitVCASConfigure( void )
{
	if(NULL == Storefilepath)
		Storefile = "./Verimatrix.store";
	else
	{
		int len = strlen(Storefilepath) + strlen("/Verimatrix.store");
		Storefile = (char *)malloc((len*sizeof(char))+1);
		strcpy(Storefile,Storefilepath);
		strcat(Storefile, "/Verimatrix.store");
		bFreeStorefile = 1;
	}

	if(NULL == ServerIPaddress)
	{
		printf("\nPlease provide VCAS server ip address\n");
		return -1;
	}

	if(NULL == Portnumber)
		ServerPortno = SERVERPORT;
	else
		ServerPortno = atoi(Portnumber);

	return 0;

}



/*----------------------------------------------------------------------------*/
/* InitVerimatrixCA() - startup the Verimatrix client                         */
/*----------------------------------------------------------------------------*/
static int InitVerimatrixCA(void **pContext)
{
   int result = 0;
   char idbuf[ 128 ];
   unsigned int idsize = 0;

   printf(" Storefile-> %s\n",Storefile);
   printf(" ServerIPaddress-> %s\n", ServerIPaddress);
   printf(" Server Port number-> %d\n", ServerPortno);

   VMSetErrorLevel(NULL, 5);
   if ( bOffline )
   {
      printf("Running in OFFLINE mode\n");
      result = VMConfigOffline( Storefile );
   }
   else
   {
      printf("Running in ONLINE mode\n");
      result = VMConfig( Storefile, ServerIPaddress, ServerPortno, 2, 2, 0 );
   }

   if ( result != 0 ) { 
      /* VMConfig failed, this is a stopping failure. 
      // Middleware app can not proceed with Mutual 
      // Authentication mode until this basic VRClient 
      // step is successful.  */
      printf( "VMConfig failed, middleware must handle error accordingly\n" );
      //return -1;
   }
   else
   {	 
      bConfigSuccess = 1;
   }

   VMSetErrorLevel(NULL, 5);
   if ( 0 == GetUniqueIdentifier( idbuf, sizeof(idbuf), &idsize ) )
   {
      printf("Got ClientID as '%s'\n", idbuf );
   }
   else
   {
      printf("GetUniqueIdentifier Failed\n");
   }

   *pContext =  VMCreateContext(NULL,1);
   if ( NULL == *pContext )
   {
      printf("Failed to create decryption context\n");
      return -1;
   }
	VMServiceStart(*pContext, 0x40, 0, NULL);


   VMRegisterDecodeFailureCallback( *pContext, MyDecodeFailureCallback, (void*)0xDEADBEEF );

    /*VMRegisterControlWordCallback( *pContext
                                    , MyControlWordCallback
                                    , NULL
                                    , 0
                                    , NULL);*/
   VMLogVersion();
   result = VMResetStream( *pContext );
   if (result) fprintf(stderr, "VMResetStream() failed with result: %d\n", result);
   return result;
}



/*----------------------------------------------------------------------------*/
/* ProcessArgs() - processes command line args for file or mcast i/o          */
/*----------------------------------------------------------------------------*/
static int ProcessArgs(int argc, char **argv)
{
   int c;

   if (argc < 3) {
      usage(argv[0]); return -1;
   }

   // parse the command options
   while ((c = getopt(argc, argv, "hnfm:a:i:o:r:s:c:p:")) != -1)
        switch (c) {
        case 'i':
         instream = optarg;
         intype = IOFILE;
        break;
        case 'o':
         outstream = optarg;
         outtype = IOFILE;
        break;
        case 'm':
         instream = optarg;
         intype = IOMCAST;
        break;
        case 'r':
         outstream = optarg;
         outtype = IOMCAST;
        break;
        case 'n':
         nodecrypt++;
        break;
        case 'a':
         pInterfaceAddress = optarg;
        break;
	case 's':
		Storefilepath = optarg;
		break;
	case 'p':
		Portnumber = optarg;
		break;
    case 'f':
        bOffline = 1;
        break;
	case 'c':
		ServerIPaddress = optarg;
		break;
        case '?':
        case 'h':
         usage(argv[0]);
         return -1;
        }

   return 0;
}

/*----------------------------------------------------------------------------*/
/* usage() - usage description                                                */
/*----------------------------------------------------------------------------*/
static void usage(char *cmd)
{
   printf("usage %s [-i input_file -m mcastaddr -o output_file -r mcastaddr -n]\n", cmd );
   printf("where:\n");
   printf("    -p port number of the VCAS configuration server\n");
   printf("    -c IP address of the VCAS configuration server\n");
   printf("    -s path of the Store file\n");
   printf("    -a adapter IP address on local host for stream i/o (for multi-nic)\n");
   printf("    -i input_file\n");
   printf("    -m mcastaddr is ip:port to receive encrypted stream\n");
   printf("    -o output_file\n");
   printf("    -r mcastaddr is ip:port to retransmit decrypted stream\n");
   printf("    -n skip decryption\n");
   printf("examples:\n");
   printf("    '%s -m 235.1.1.1:8208 -o out.mpg'    will decrypt a stream into a file\n",cmd);
   printf("    '%s -i rtes.mpg -o out.mpg'          will decrypt a file into a file\n",cmd);
   printf("    '%s -m 235.1.1.1:8208'               will decrypt a stream and not save\n",cmd);
   printf("    '%s -m 235.1.1.1:8208 -r 224.1.2.1:2000'  will decrypt a stream and retransmit\n",cmd);
   printf("    '%s -c 10.0.249.23 -p 12686 -s /root/stb will contact to 10.0.249.23 VCAS server on its 1234 port and save the .store file in the /root/stb location\n",cmd);
}

/* end of main.c */
