# BPI-M2-Plus Android 4.4 Source code (SDK1.2)
----------
###1 Build Android BSP
 $ cd lichee
 
   $ ./build.sh config    
      

Welcome to mkscript setup progress
All available chips:
   1. sun8iw6p1
   2. sun8iw7p1
   3. sun8iw8p1
   4. sun9iw1p1
 
Choice: 1


All available platforms:
   1. android
   2. dragonboard
   3. linux
 
Choice: 1


All available business:
   1. dolphin
   2. secure
   3. karaok

Choice: 1

   $ ./build.sh 


***********

###2 Build Android 
   $cd ../android

   $source build/envsetup.sh
   
   $lunch    //(dolphin_bpi_m2p-eng)
   
   $extract-bsp
   
   $make -j8
   
   $pack
   
   









