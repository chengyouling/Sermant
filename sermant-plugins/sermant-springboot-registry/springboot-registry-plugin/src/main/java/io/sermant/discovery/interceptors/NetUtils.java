/*

 * Copyright (C) 2020-2024 Huawei Technologies Co., Ltd. All rights reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.discovery.interceptors;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(NetUtils.class);

  private static final String IPV4_KEY = "_v4";

  private static final String IPV6_KEY = "_v6";

  private static final String PREFERRED_INTERFACE = "eth";

  // one interface can bind to multiple address
  // we only save one ip for each interface name.
  // eg:
  // 1. eth0 -> ip1 ip2
  //    last data is eth0 -> ip2
  // 2. eth0 -> ip1
  //    eth0:0 -> ip2
  //    eth0:1 -> ip3
  //    on interface name conflict, all data saved

  // key is network interface name and type
  private static Map<String, InetAddress> allInterfaceAddresses = new HashMap<>();

  private static String hostName;

  private static String hostAddress;

  private static String hostAddressIpv6;

  static {
    doGetHostNameAndHostAddress();
  }

  private static void doGetHostNameAndHostAddress() {
    try {
      doGetAddressFromNetworkInterface();
      // getLocalHost will throw exception in some docker image and sometimes will do a hostname lookup and time consuming
      InetAddress localHost = InetAddress.getLocalHost();
      hostName = localHost.getHostName();
      LOGGER.info("localhost hostName={}, hostAddress={}.", hostName, localHost.getHostAddress());

      if (!isLocalAddress(localHost)) {
        if (Inet6Address.class.isInstance(localHost)) {
          hostAddressIpv6 = trimIpv6(localHost.getHostAddress());
          hostAddress = tryGetHostAddressFromNetworkInterface(false, localHost);
          LOGGER.info("Host address info ipV4={}, ipV6={}.", hostAddress, hostAddressIpv6);
          return;
        }
        hostAddress = localHost.getHostAddress();
        hostAddressIpv6 = trimIpv6(tryGetHostAddressFromNetworkInterface(true, localHost));
        LOGGER.info("Host address info ipV4={}, ipV6={}.", hostAddress, hostAddressIpv6);
        return;
      }
      hostAddressIpv6 = trimIpv6(tryGetHostAddressFromNetworkInterface(true, localHost));
      hostAddress = tryGetHostAddressFromNetworkInterface(false, localHost);
      LOGGER.info("Host address info ipV4={}, ipV6={}.", hostAddress, hostAddressIpv6);
    } catch (Exception e) {
      LOGGER.error("got exception when trying to get addresses:", e);
      if (!allInterfaceAddresses.isEmpty()) {
        InetAddress entry = allInterfaceAddresses.entrySet().iterator().next().getValue();
        // get host name will do a reverse name lookup and is time consuming
        hostName = entry.getHostName();
        hostAddress = entry.getHostAddress();
        LOGGER.info("add host name from interfaces:" + hostName + ",host address:" + hostAddress);
      }
    }
  }

  private static String tryGetHostAddressFromNetworkInterface(boolean isIpv6, InetAddress localhost) {
    InetAddress result = null;
    for (Entry<String, InetAddress> entry : allInterfaceAddresses.entrySet()) {
      if (isIpv6 && entry.getKey().endsWith(IPV6_KEY)) {
        result = entry.getValue();
        if (entry.getKey().startsWith(PREFERRED_INTERFACE)) {
          return result.getHostAddress();
        }
      } else if (!isIpv6 && entry.getKey().endsWith(IPV4_KEY)) {
        result = entry.getValue();
        if (entry.getKey().startsWith(PREFERRED_INTERFACE)) {
          return result.getHostAddress();
        }
      }
    }

    if (result == null) {
      return localhost.getHostAddress();
    }

    return result.getHostAddress();
  }

  private NetUtils() {
  }

  /**
   * docker环境中，有时无法通过InetAddress.getLocalHost()获取 ，会报unknown host Exception， system error
   * 此时，通过遍历网卡接口的方式规避，出来的数据不一定对
   */
  private static void doGetAddressFromNetworkInterface() throws SocketException {
    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

    while (networkInterfaces.hasMoreElements()) {
      NetworkInterface network = networkInterfaces.nextElement();

      if (!network.isUp() || network.isLoopback() || network.isVirtual()) {
        continue;
      }

      Enumeration<InetAddress> addresses = network.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress address = addresses.nextElement();

        if (isLocalAddress(address)) {
          continue;
        }

        if (address instanceof Inet4Address) {
          LOGGER.info("add ipv4 network interface:" + network.getName() + ",host address:" + address.getHostAddress());
          allInterfaceAddresses.put(network.getName() + IPV4_KEY, address);
        } else if (address instanceof Inet6Address) {
          LOGGER.info("add ipv6 network interface:" + network.getName() + ",host address:" + address.getHostAddress());
          allInterfaceAddresses.put(network.getName() + IPV6_KEY, address);
        }
      }
    }
  }

  private static String trimIpv6(String hostAddress) {
    int index = hostAddress.indexOf("%");
    if (index >= 0) {
      return hostAddress.substring(0, index);
    }
    return hostAddress;
  }

  private static boolean isLocalAddress(InetAddress address) {
    return address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isMulticastAddress();
  }

  public static String getHostAddress() {
    //If failed to get host address ,micro-service will registry failed
    //So I add retry mechanism
    if (hostAddress == null) {
      doGetHostNameAndHostAddress();
    }
    return hostAddress;
  }

  public static String getIpv6HostAddress() {
    //If failed to get host address ,micro-service will registry failed
    //So I add retry mechanism
    if (hostAddressIpv6 == null) {
      doGetHostNameAndHostAddress();
    }
    return hostAddressIpv6;
  }

  @SuppressWarnings({"unused", "try"})
  public static boolean canTcpListen(InetAddress address, int port) {
    try (ServerSocket ss = new ServerSocket(port, 0, address)) {
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
