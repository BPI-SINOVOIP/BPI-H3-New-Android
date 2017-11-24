# BPI-M2-Plus-Zero Android 4.4 Source code (SDK1.2)
----------
###1 Build Android BSP

   $ cd lichee
 
   $ ./build.sh config    
      

Welcome to mkscript setup progress
All available chips:

   0. sun8iw7p1
   
Choice: 0


All available platforms:

   0. android
   1. dragonboard
   2. linux
 
Choice: 0


All available business:

   0. dolphin
   1. secure
   2. karaok

Choice: 0

   $ ./build.sh 


***********

###2 Build Android 

   $cd ../android

   $source build/envsetup.sh
   
   $lunch    //(dolphin_bpi_m2p-eng  or dolphin_bpi_m2z-eng)
   
   $extract-bsp
   
   $make -j8
   
   $pack
   
   









