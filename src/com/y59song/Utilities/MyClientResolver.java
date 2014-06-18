package com.y59song.Utilities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.IClientResolver;
import org.sandroproxy.utils.network.NetworkInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by y59song on 04/06/14.
 */

public class MyClientResolver implements IClientResolver {


  private static boolean DEBUG = false;
  private static String TAG = MyClientResolver.class.getSimpleName();

  private PackageManager packageManager;

  public MyClientResolver(Context context){
    packageManager = context.getPackageManager();
  }

  @Override
  public ConnectionDescriptor getClientDescriptorBySocket(Socket socket) {

    int port = socket.getPort();
    String address = socket.getInetAddress().getHostAddress();
    BufferedReader reader = null;
    try {
      File tcp = new File(NetworkInfo.TCP_6_FILE_PATH);
      reader = new BufferedReader(new FileReader(tcp));
      String line;
      StringBuilder builder = new StringBuilder();

      // find line that has port number inside
      String hexPort = Integer.toHexString(port);
      if(DEBUG) Log.d(TAG, hexPort);
      while ((line = reader.readLine()) != null) {
        if (line.toUpperCase().contains(hexPort.toUpperCase())){
          builder.append(line);
        }
      }
      reader.close();

      String content = builder.toString();
      if(DEBUG) Log.d(TAG, content);
      if (content != null && content .length() > 0){
        Matcher m6 = Pattern.compile(NetworkInfo.TCP_6_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL).matcher(content);

        while (m6.find()) {
          String srcAddressEntry = m6.group(1);
          String srcPortEntry    = m6.group(2);
          String dstAddressEntry = m6.group(3);
          String dstPortEntry    = m6.group(4);
          String status          = m6.group(5);
          int uidEntry    = Integer.valueOf(m6.group(6));
          int srcPort = Integer.valueOf(srcPortEntry, 16);
          int dstPort = Integer.valueOf(dstPortEntry, 16);
          int connStatus = Integer.valueOf(status, 16);

          srcAddressEntry = getIPAddrByHex(srcAddressEntry);
          dstAddressEntry = getIPAddrByHex(dstAddressEntry);

          if (srcPort == port && !srcAddressEntry.contains("7F00:0001")) {
            String[] packagesForUid = packageManager.getPackagesForUid(uidEntry);
            if (packagesForUid != null) {
              String packageName = packagesForUid[0];
              PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
              String version = pInfo.versionName;
              String name = pInfo.applicationInfo.name;
              return new ConnectionDescriptor(new String[]{packageName}, new String[]{name}, new String[]{version}, NetworkInfo.TCP6_TYPE, connStatus, srcAddressEntry, srcPort, dstAddressEntry, dstPort, null, uidEntry);
            }
          }
        }
      }

      // this means that no connection with that port could be found in the tcp6 file
      // try the tcp one

      tcp = new File(NetworkInfo.TCP_4_FILE_PATH);
      reader = new BufferedReader(new FileReader(tcp));
      builder = new StringBuilder();

      while ((line = reader.readLine()) != null) {
        if (line.toUpperCase().contains(hexPort.toUpperCase())){
          builder.append(line);
        }
      }

      reader.close();

      content = builder.toString();
      if(DEBUG) Log.d(TAG, content);

      if (content != null && content.length() > 0){
        if(DEBUG) Log.d(TAG, NetworkInfo.TCP_4_PATTERN);
        Matcher m4 = Pattern.compile(NetworkInfo.TCP_4_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL).matcher(content);

        while (m4.find()) {
          String srcAddressEntry = m4.group(1);
          String srcPortEntry    = m4.group(2);
          String dstAddressEntry = m4.group(3);
          String dstPortEntry    = m4.group(4);
          String connStatus      = m4.group(5);
          int uidEntry    = Integer.valueOf(m4.group(6));
          int srcPort = Integer.valueOf(srcPortEntry, 16);
          int dstPort = Integer.valueOf(dstPortEntry, 16);
          int status  = Integer.valueOf(connStatus, 16);
          if(DEBUG) Log.d(TAG, srcPortEntry);

          srcAddressEntry = getIPAddrByHex(srcAddressEntry);
          dstAddressEntry = getIPAddrByHex(dstAddressEntry);

          if (srcPort == port && srcAddressEntry != "127.0.0.1") {
            String[] packagesForUid = packageManager.getPackagesForUid(uidEntry);

            if (packagesForUid != null) {
              String packageName = packagesForUid[0];
              PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
              String version = pInfo.versionName;
              String name = pInfo.applicationInfo.name;
              return new ConnectionDescriptor(new String[]{packageName}, new String[]{name}, new String[]{version}, NetworkInfo.TCP_TYPE, status,
                srcAddressEntry, srcPort, dstAddressEntry, dstPort, null, uidEntry);
            }
          }
        }
      }

      // nothing found we create descriptor with what we got as input
      if (DEBUG) Log.d(TAG, "No data for " + address + ":" + port);
      return null;

    } catch (Exception e) {
      if(DEBUG) Log.e(TAG, "parsing client data error : " + e.getMessage());
      if (reader != null){
        try {
          reader.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
    if (DEBUG) Log.d(TAG, "No data for " + address + ":" + port);
    return null;
  }

  private String getIPAddrByHex(String hexHost) {
    String ret = "";
    if(hexHost.length() == 8) {
      for (int i = 0; i < 4; i++) {
        ret = "." + Integer.parseInt(hexHost.substring(i * 2, i * 2 + 2), 16) + ret;
      }
    } else {
      for (int i = 0; i < 4; i ++) {
        String temp = "";
        for(int j = 0; j < 2; j ++) {
          for(int k = 0; k < 2; k ++) {
            int start = i * 8 + j * 4 + k * 2;
            temp = hexHost.substring(start, start + 2) + temp;
          }
          temp = ":" + temp;
        }
        ret = ret + temp;
      }
    }
    return ret.substring(1);
  }
}

