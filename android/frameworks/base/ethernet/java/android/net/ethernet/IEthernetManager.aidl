/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.ethernet;

import android.net.ethernet.EthernetDevInfo;
import android.net.DhcpInfo;

/**
 * Interface that allows controlling and querying Ethernet connectivity.
 */
interface IEthernetManager
{
    boolean addInterfaceToService(String name);
    boolean removeInterfaceFromService(String name);
    List<EthernetDevInfo> getDeviceList();
    EthernetDevInfo getDevInfo();
    String getEthernetMode();
    void setEthernetMode(String mode, in EthernetDevInfo info);
    void setEthernetEnabled(boolean enable);
    int getEthernetState();
    boolean getLinkState();
    boolean getWifiDisguiseState();
    void setWifiDisguise(boolean enable);
    EthernetDevInfo getStaticConfig();
    void setStaticConfig(in EthernetDevInfo info);
    EthernetDevInfo getLoginInfo(String mode);
    DhcpInfo getDhcpInfo();
    int getPppoeStatus();
    boolean checkLink(String ifname);
    void disconnect();
}

