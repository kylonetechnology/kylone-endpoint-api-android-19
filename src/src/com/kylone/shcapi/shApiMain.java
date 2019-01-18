/*
 * shApiMain.java
 * Authors: Gokhan Poyraz <gokhan@kylone.com>
 *
 * Kylone Client API for Android
 * Copyright (c) 2018, Kylone Technology International Ltd.
 * API Version 2.0.81
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
*/

package com.kylone.shcapi;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Matrix;
import android.bluetooth.BluetoothAdapter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.InetAddress;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Process;
import java.lang.Runtime;
import java.lang.InterruptedException;
import java.lang.Thread;
import java.lang.reflect.Method;
import java.util.Locale;
import java.net.NetworkInterface;
import java.util.List;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class shApiMain {

   /*
      Every functions implemented on native interface will return with an integer
      value which are explained below;

        SHC_OK          : calling function successful
        SHC_FAIL        : calling function get failed

      The method shApiMain.shconnect() may return following values in addition to above
      codes and shApiMain.shcbconnectfailed() callback method will be triggered accordingly.

        SHC_UNSUPPORTED_PLATFORM : this device is not supported by the server
        SHC_L3_DENIED            : accessing through routed networks are not allowed
        SHC_DEVICE_DISABLED      : this device status is disabled on server
   */
   public static final int SHC_OK = 0;      // calling function successful
   public static final int SHC_FAIL = -1;   // calling function get failed
   public static final int SHC_UNSUPPORTED_PLATFORM = 1; // device or platform is not supported
   public static final int SHC_L3_DENIED = 2; // access through routed networks not allowed
   public static final int SHC_DEVICE_DISABLED = 3; // device status is disabled on server side

   /*
      There are only two options used in this API, which are;

      SHC_OPT_DOCACHE: API will cache media objects such as JPEG or PNG files
      into local storage in advance to access them quickly whenever it possible.
      Since the cached objects live in local file system, API will check whether
      required object is already cached or not, if so, it will be used from the
      local quickly, otherwise object will be downloaded from the server.

      SHC_OPT_DONOTCACHE: Media files will not be cached and they will be downloaded
      from server every time.
   */
   public static final int SHC_OPT_DOCACHE = 1; // Store remote content as cache
   public static final int SHC_OPT_DONOTCACHE = 2; // Disable caching

   /*
      When server sends a message, API triggers a callback method with String
      message argument. Following messages can be sent by the server;

      import  Update Message (Banner Text)
      commit  Reload the content or Restart the Application (Commit launched on server side)
      kill    Suspend (app may get suspended or quits, "commit" should restart it)
      reboot  Reboot the system
      clean   Update the firmware (custom application will update itself, cleans the cache etc.)
      emerg   Emergency mode (*)
      popup   Pop-Up message received for this STB.
      insist  Bulk Pop-Up message received for all STBs in the system (*)
      stbscr  Scrolling text message received for this STB (*)
      txtscr  Bulk Scrolling Text message received for all STBs in the system (*)
   */
   public static final String SHC_MESSAGE_BANNERTEXT = "import";
   public static final String SHC_MESSAGE_RESTART = "commit";
   public static final String SHC_MESSAGE_SUSPEND = "kill";
   public static final String SHC_MESSAGE_RESUME = "resume";
   public static final String SHC_MESSAGE_REBOOTSYSTEM = "reboot";
   public static final String SHC_MESSAGE_FWUPDATE = "clean";
   public static final String SHC_MESSAGE_EMERGENCY = "emerg";
   public static final String SHC_MESSAGE_POPUP = "popup";
   public static final String SHC_MESSAGE_BULKPOPUP = "insmsg";
   public static final String SHC_MESSAGE_SCROLL = "stbscr";
   public static final String SHC_MESSAGE_BULKSCROLL = "txtscr";

   /*
      There are a few variables associated with data fetched from the server:

      - Configuration is saved in a hashtable called "tableconfig"
      - Content is saved in ContentList struct as a class "contentlist"
      - Weather information will be stored in WeatherItem "weatherlist"

      There is also required methods implemented to access such data from 
      the object itself.
   */
   private static Hashtable<String, String> tableconfig = null;
   public static ContentList contentlist = null;
   public static WeatherItem weatherlist = null;

   /*
      Instance and Context objects will be stored in below variables for further use.
   */
   protected static shApiMain m_apiInstance = null;
   protected static Context m_apiContext = null;

   /*
      Functions definitons on native interface
   */
   public native static String shgetapiversion();
   public native static int shcreate(String cachedir);
   public native static int shdelete();
   public native static int shserverisready(String targethost, int timeout);
   public native static int shconnect(String targethost, int cache);
   public native static String shgetbannertext();
   public native static String shgetwoemodel();
   public native static String shgetremotedata(String msg);
   public native static String shdrmquery(String sys, String typ, String meth, String pmid, String uuid, String pcode);
   public native static String shdrmquerydescr();
   public native static String shdrmtokenize(String src, String uuid);

   /*
     Natice interface library is cLshcapi.so
   */
   static {
      System.loadLibrary("cLshcapi");
   }

   /*
      Kylone server will always send data in XML format. After doing request to server,
      resulting XML data will be converted to a structured data using such classes which are
      implemented in this API to access and use content data easily with other UI components.

      Custom applications may directly use such resulting XML data instead of using ready
      to use classes when necessary.

      ContentAttribute: list of properties of a content item such as name, album, band of
      a music item.
      All properties will be stored as String value in hashtable with String keys.
   */
   public class ContentAttribute {
      /*
         Hashtable is for storing and accessing to data. Since there are methods implemented
         to manage this hashtable, it defined as private object
      */
      private Hashtable<String, String> arglist = new Hashtable<String, String>();
      /*
         Index object will be created using keys, after finish storing data to this object
         It is usefull to use this object with Adapters and ListViews etc.
      */
      private String[] index = null;
      /*
         Since hashtable and index variables are private, there should be some methods
         implemented: it is useful to access such information using only class object.

         Properties are accessible either using keys (string) or id (integer sequence number,
         starting with zero).
      */
      public int size() {
         return arglist.size();
      }
      // Returns an Enumeration object to get a list of keys of attributes in the object.
      public Enumeration<String> keys() {
         return arglist.keys();
      }
      public void put(String key, String val) {
        arglist.put(key, val);
      }
      // Returns the value of attribute with given name.
      public String get(String key) {
        return arglist.get(key);
      }
      // Returns the value of attribute with given index
      public String get(int idx) {
         return (idx < arglist.size()) ? arglist.get(index[idx]) : null;
      }
      /*
         Returns keys as a string list
         This function will be used in conjunction with String[] returned by the getList()
         method and the index value will be same with the index value of returning String[]
         object of getList() method. For example, custom application may use the getList()
         output in a ListView and then it may get particular value of an attribute by
         using the index of the list-item.
      */
      public String[] getList() {
         return index;
      }
   }

   /*
      ContentItem: list of items in a category, such as song list in category "rock".

      All properties will be stored as ContentAttribute value in hashtable with String keys.
      It is possible to access to a property of an item directy using this class.
   */
   public class ContentItem {
      private Hashtable<String, ContentAttribute> itmlist = new Hashtable<String, ContentAttribute>();
      private String[] index = null;
      public int size() {
         return itmlist.size();
      }
      public Enumeration<String> keys() {
         return itmlist.keys();
      }
      public void put(String key, ContentAttribute val) {
        itmlist.put(key, val);
      }
      public ContentAttribute get(String key) {
        return itmlist.get(key);
      }
      public ContentAttribute get(int idx) {
         return (idx < itmlist.size()) ? itmlist.get(index[idx]) : null;
      }
      public String get(String itemkey, String attributekey) {
         ContentAttribute a = itmlist.get(itemkey);
         if (a != null)
            return a.get(attributekey);
         Log.v("shApiMain", "ContentItem(): key '" + itemkey + "' not found");
         return null;
      }
      public String get(int itemindex, String attributekey) {
         ContentAttribute a = get(itemindex);
         if (a != null)
            return a.get(attributekey);
         Log.v("shApiMain", "ContentItem(): keyindex '" + itemindex + "' not found");
         return null;
      }
      public String[] getList() {
         return index;
      }
   }

   /*
      ContentCategory: list of categories in a content group, such as "pop", "rock", "dance"
      for cnntent group "music".

      In this API, all categories are defined as String and should be processed as String
      also even if their values are numbers. 

      There is only one special category called "0" which is used to kept all items in all other
      categories. It is usable to access all items in a content group without using category IDs
      if there is special group "0" present. In same way, if there is no any other category
      presented other than "0", means, there is no category for particular content group - so all
      items will be stored with category ID "0".
   */
   public class ContentCategory {
      private Hashtable<String, ContentItem> catlist = new Hashtable<String, ContentItem>();
      public String[] index = null;
      public String rankvalue;
      public int size() {
         return catlist.size();
      }
      public Enumeration<String> keys() {
         return catlist.keys();
      }
      public void put(String key, String rank, ContentItem val) {
        rankvalue = rank;
        catlist.put(key, val);
      }
      public ContentItem get(String key) {
        return catlist.get(key);
      }
      public ContentItem get(int idx) {
         return (idx < catlist.size()) ? catlist.get(index[idx]) : null;
      }
      public ContentAttribute get(String categorykey, String itemkey) {
         ContentItem l = catlist.get(categorykey);
         if (l != null)
            return l.get(itemkey);
         Log.v("shApiMain", "ContentCategory(): key '" + categorykey + "' not found");
         return null;
      }
      public ContentAttribute get(String categorykey, int itemindex) {
         ContentItem l = catlist.get(categorykey);
         if (l != null)
            return l.get(itemindex);
         Log.v("shApiMain", "ContentCategory(): key '" + categorykey + "' not found");
         return null;
      }
      public String get(String categorykey, String itemkey, String attributekey) {
         ContentAttribute l = get(categorykey, itemkey);
         if (l != null)
            return l.get(attributekey);
         return null;
      }
      public String get(String categorykey, int itemindex, String attributekey) {
         ContentAttribute l = get(categorykey, itemindex);
         if (l != null)
            return l.get(attributekey);
         return null;
      }
      public String[] getList() {
         return index;
      }
      public String getRank() {
         return rankvalue;
      }
   }

   /*
      ContentList: list of content groups which may called "rootitems" or "menuitems" also.
      This object will store content-group name as String and it's categories as a list as
      ContentCategory object.

      User application may display list of items in this object as "Main Menu Items" on the UI.
   */
   public class ContentList {
      private Hashtable<String, ContentCategory> list = new Hashtable<String, ContentCategory>();
      public String[] index = null;
      public int size() {
         return list.size();
      }
      public Enumeration<String> keys() {
         return list.keys();
      }
      public void put(String key, ContentCategory val) {
        list.put(key, val);
      }
      public ContentCategory get(String key) {
        return list.get(key);
      }
      public ContentCategory get(int idx) {
         return (idx < list.size()) ? list.get(index[idx]) : null;
      }
      public String[] getList() {
         return index;
      }
      public ContentItem get(String contentkey, String categorykey) {
         ContentCategory l = list.get(contentkey);
         if (l != null)
            return l.get(categorykey);
         Log.v("shApiMain", "ContentList(): key '" + contentkey + "' not found");
         return null;
      }
      public ContentAttribute get(String contentkey, String categorykey, String itemkey) {
         ContentItem l = get(contentkey, categorykey);
         if (l != null)
            return l.get(itemkey);
         return null;
      }
      public ContentAttribute get(String contentkey, String categorykey, int itemindex) {
         ContentItem l = get(contentkey, categorykey);
         if (l != null)
            return l.get(itemindex);
         return null;
      }
      public String get(String contentkey, String categorykey, String itemkey, String attributekey) {
         ContentAttribute l = get(contentkey, categorykey, itemkey);
         if (l != null)
            return l.get(attributekey);
         return null;
      }
      public String get(String contentkey, String categorykey, int itemindex, String attributekey) {
         ContentAttribute l = get(contentkey, categorykey, itemindex);
         if (l != null)
            return l.get(attributekey);
         return null;
      }
   }

   /*
      Generally, weather inforation will be fetched from external services. Since it is
      highly depending on external service, the weather data structure is separeted from
      the internal content-data structure of Kylone and defined as new classes.

      Below classes implemented for weather information accessed through Kylone server and
      it's structure kept very similiar to Kylone's internal data-structure also.  

      User application may use weather information through Kylone server using below classes
      or may implement their own types to manage and access information from their own services.

      WeatherAttribute: list of properties of a location
   */
   public class WeatherAttribute {
      private Hashtable<String, String> arglist = new Hashtable<String, String>();
      public String[] index = null;
      public int size() {
         return arglist.size();
      }
      public Enumeration<String> keys() {
         return arglist.keys();
      }
      public void put(String key, String val) {
        arglist.put(key, val);
      }
      public String get(String key) {
        return arglist.get(key);
      }
      public String get(int idx) {
         return (idx < arglist.size()) ? arglist.get(index[idx]) : null;
      }
      public String[] getList() {
         return index;
      }
   }

   /*
      WeatherForecastItem: list of forecast information of a location.
      Every forecast item will hold it's own property list.
   */
   public class WeatherForecastItem {
      private Hashtable<String, WeatherAttribute> itmlist = new Hashtable<String, WeatherAttribute>();
      public String[] index = null;
      public int size() {
         return itmlist.size();
      }
      public Enumeration<String> keys() {
         return itmlist.keys();
      }
      public void put(String key, WeatherAttribute val) {
        itmlist.put(key, val);
      }
      public WeatherAttribute get(String key) {
        return itmlist.get(key);
      }
      public WeatherAttribute get(int idx) {
         return (idx < itmlist.size()) ? itmlist.get(index[idx]) : null;
      }
      public String get(String itemkey, String attributekey) {
         WeatherAttribute a = itmlist.get(itemkey);
         if (a != null)
            return a.get(attributekey);
         Log.v("shApiMain", "WeatherForecastItem(): key '" + itemkey + "' not found");
         return null;
      }
      public String get(int itemindex, String attributekey) {
         WeatherAttribute a = get(itemindex);
         if (a != null)
            return a.get(attributekey);
         Log.v("shApiMain", "WeatherForecastItem(): keyindex '" + itemindex + "' not found");
         return null;
      }
      public String[] getList() {
         return index;
      }
   }

   /*
      WeatherItem: list of locations.
      Every location item will hold their own property and forecast list.
   */
   public class WeatherItem {
      private Hashtable<String, WeatherAttribute> itmlist = new Hashtable<String, WeatherAttribute>();
      private Hashtable<String, WeatherForecastItem> forecastlist = new Hashtable<String, WeatherForecastItem>();
      public String[] index = null;
      public int size() {
         return itmlist.size();
      }
      public Enumeration<String> keys() {
         return itmlist.keys();
      }
      public void put(String key, WeatherAttribute val, WeatherForecastItem fval) {
        itmlist.put(key, val);
        forecastlist.put(key, fval);
      }
      public WeatherAttribute get(String key) {
        return itmlist.get(key);
      }
      public WeatherForecastItem getForecast(String key) {
        return forecastlist.get(key);
      }
      public WeatherAttribute get(int idx) {
         return (idx < itmlist.size()) ? itmlist.get(index[idx]) : null;
      }
      public WeatherForecastItem getForecast(int idx) {
         return (idx < forecastlist.size()) ? forecastlist.get(index[idx]) : null;
      }
      public String get(String itemkey, String attributekey) {
         WeatherAttribute a = itmlist.get(itemkey);
         if (a != null)
            return a.get(attributekey);
         Log.v("shApiMain", "WeatherItem(): key '" + itemkey + "' not found");
         return null;
      }
      public String get(int itemindex, String attributekey) {
         WeatherAttribute a = get(itemindex);
         if (a != null)
            return a.get(attributekey);
         Log.v("shApiMain", "WeatherItem(): keyindex '" + itemindex + "' not found");
         return null;
      }
      public String[] getList() {
         return index;
      }
   }

   /*
      Constructor is mandatory to save some information: instance will
      be saved to use with static functions which will called by the
      native interface
   */
   public shApiMain() {
      m_apiInstance = this;
      Log.v("shApiMain", "constructed");
   }

   /*
      cbRemoteMessage: A protected method which should be overridden to handle
      messages sent by the server. This method will be triggered automatically
      when the server sent a message.
   */
   protected void cbRemoteMessage(final String msg) {
      Log.v("shApiMain", "callback: cbRemoteMessage(): " + msg);
   }

   public static void shcbremotemessage(final String msg) {
      Log.v("shApiMain", "shcbremotemessage(" + msg + ")");
      if (m_apiInstance != null)
         m_apiInstance.cbRemoteMessage(msg);
   }

   /*
      cbProgress: A protected method which should be overridden to handle
      progress callback generated by the native interface while communicating
      with the server.
     
      It's single argument is the progress percent which is an integer. So it
      will be in range of 0-100.
   */
   protected void cbProgress(final int p) {
      Log.v("shApiMain", "callback: Progress(): " + p);
   }

   public static void shcbprogress(int p) {
      if (m_apiInstance != null)
         m_apiInstance.cbProgress(p);
   }

   /*
      cbConnectFailed: A protected method which should be overridden to handle
      connectivity errors.
   */
   protected void cbConnectFailed(final int reason) {
      Log.v("shApiMain", "callback: cbConnectFailed(): " + reason);
   }

   public static void shcbconnectfailed(final int reason) {
      if (m_apiInstance != null)
         m_apiInstance.cbConnectFailed(reason);
   }

   /*
      cbConfigReady: A protected method which should be overridden to handle
      interim update while content is downloading from the server.

      This callback will be triggered when the "configuration" is downloaded
      with all it's relevant media properties and ready to use by the user
      application.

      For example, application may set background, logo and welcome text
      immediatelly when this callback is triggered while the content processing is
      still in progress.
   */
   protected void cbConfigReady() {
      Log.v("shApiMain", "callback: ConfigReady()");
   }

   /*
      createDocumentFromXML: helper function to create DOM document from XML.
   */
   public static Document createDocumentFromXML(final String xml) {
      Document res = null;
      DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
      try {
         DocumentBuilder b = f.newDocumentBuilder();
         try {
            InputSource is = new InputSource(new StringReader(xml));
            res = b.parse(is);
            res.getDocumentElement().normalize();
         } catch(Exception e) {
            Log.e("shApiMain", e.getMessage());
            e.printStackTrace();
         }
      } catch(Exception e) {
         Log.e("shApiMain", e.getMessage());
         e.printStackTrace();
      }
      return res;
   }

   public static void shcbconfigready(final String doc) {
      try {
         Document docconfig = createDocumentFromXML(doc);
         Element e = (Element)docconfig.getElementsByTagName("cLst").item(0);
         Element cnt = (Element)e.getElementsByTagName("content").item(0);
         Element cat = (Element)cnt.getElementsByTagName("category").item(0);
         Element itm = (Element)cat.getElementsByTagName("item").item(0);
         NodeList l = itm.getChildNodes();
         if (tableconfig == null) {
            tableconfig = new Hashtable<String, String>();
         } else {
            tableconfig.clear();
         }
         for (int ni = 0; ni < l.getLength(); ni++) {
             Node n = l.item(ni);
             if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element en = (Element)n;
                Node nv = n.getChildNodes().item(0);
                if (nv != null) {
                   if (nv.getNodeType() == Node.TEXT_NODE)
                      tableconfig.put(en.getAttribute("id"), nv.getNodeValue());
                }
             }
         }
         if (m_apiInstance != null)
            m_apiInstance.cbConfigReady();
      } catch(Exception e) {
         Log.e("shApiMain", e.getMessage());
         e.printStackTrace();
      }
   }

   /*
      cbContentReady: A protected method which should be overridden to handle
      final update of content processing phase.

      When this method is triggered, user application can start to use all
      data-structures and classes related with content data.
   */
   protected void cbContentReady() {
      Log.v("shApiMain", "callback: ContentReady()");
   }

   public static void shcbcontentready(final String doc) {
      if (m_apiInstance != null)
         m_apiInstance.shcbcontentready_private(doc);
   }

   private void shcbcontentready_private(final String doc) {
      cbProgress(100);
      try {
         Document doccontent = createDocumentFromXML(doc);
         Element clst = (Element)doccontent.getElementsByTagName("cLst").item(0);
         contentlist = new ContentList();
         NodeList tagcontents = clst.getElementsByTagName("content");
         for (int i = 0; i < tagcontents.getLength(); i++) {
            Node tagcontent = tagcontents.item(i);
            if (tagcontent.getNodeType() != Node.ELEMENT_NODE)
               continue;
            Element elmcontent = (Element)tagcontent;
            String currcontentid = elmcontent.getAttribute("id");
            ContentCategory tablecat = new ContentCategory();
            NodeList tagcats = elmcontent.getElementsByTagName("category");
            for (int c = 0; c < tagcats.getLength(); c++) {
               Node tagcat = tagcats.item(c);
               if (tagcat.getNodeType() != Node.ELEMENT_NODE)
                  continue;
               Element elmcat = (Element)tagcat;
               String currcatid = elmcat.getAttribute("id");
               String currcatrank = elmcat.getAttribute("rank");
               ContentItem tableitem = new ContentItem();
               NodeList tagitems = elmcat.getElementsByTagName("item");
               for (int j = 0; j < tagitems.getLength(); j++) {
                  Node tagitem = tagitems.item(j);
                  if (tagitem.getNodeType() != Node.ELEMENT_NODE)
                     continue;
                  String curritemid = "";
                  ContentAttribute tableattr = new ContentAttribute();
                  NodeList al = tagitem.getChildNodes();
                  for (int ai = 0; ai < al.getLength(); ai++) {
                     Node a = al.item(ai);
                     if (a.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                     Element ae = (Element)a;
                     String keyname = ae.getAttribute("id");
                     Node av = a.getChildNodes().item(0);
                     String keyval = "";
                     if (av != null) {
                        if (av.getNodeType() == Node.TEXT_NODE)
                           keyval = av.getNodeValue();
                     }
                     if (keyname.equals("txt"))
                        curritemid = keyval;
                     tableattr.put(keyname, keyval);
                  }
                  tableattr.put("cat", currcatid);
                  int aidx = 0;
                  tableattr.index = new String[tableattr.size()];
                  for (Enumeration<String> e = tableattr.keys(); e.hasMoreElements();)
                     tableattr.index[aidx++] = e.nextElement();
                  tableitem.put(curritemid, tableattr);
               }
               int iidx = 0;
               tableitem.index = new String[tableitem.size()];
               for (Enumeration<String> e = tableitem.keys(); e.hasMoreElements();)
                  tableitem.index[iidx++] = e.nextElement();
               tablecat.put(currcatid, currcatrank, tableitem);
            }
            int cidx = 0;
            tablecat.index = new String[tablecat.size()];
            for (Enumeration<String> e = tablecat.keys(); e.hasMoreElements();)
               tablecat.index[cidx++] = e.nextElement();
            contentlist.put(currcontentid, tablecat);
         }
         int tidx = 0;
         contentlist.index = new String[contentlist.size()];
         for (Enumeration<String> e = contentlist.keys(); e.hasMoreElements();)
            contentlist.index[tidx++] = e.nextElement();
         cbContentReady();
      } catch(Exception e) {
         Log.e("shApiMain", e.getMessage());
         e.printStackTrace();
      }
   }

   /*
      getConfigVal: helper function to access configuration data
   */
   public static String getConfigVal(final String id) {
      if (tableconfig == null)
         return "";
      return tableconfig.get(id);
   }

   /*
      getAlpha: helper function to translate transparency to alpha channel value
      On server side, only transparency value will be given in range of 0-100, so
      it needs to be adopted to alpha (0-255) like opacity.
   */
   public static int getAlpha(final String val) {
      int transparency = 100 - Integer.parseInt(val);
      return (int)((transparency * 255) / 100);
   }

   /*
      createBitmapFromURL: helper function to create graphics object from a media URL
   */
   public static Drawable createBitmapFromURL(final String url) {
      Log.v("shApiMain", "createBitmapFromURL(): loading image from " + url);
      BitmapDrawable res = null;
      try {
         URL u = new URL(url);
         try {
            Bitmap bmp = BitmapFactory.decodeStream(u.openConnection().getInputStream());
            res = new BitmapDrawable(m_apiContext.getResources(), bmp);
         } catch(IOException e) {
            Log.e("shApiMain", e.getMessage());
            e.printStackTrace();
         }
      } catch(MalformedURLException e) {
         Log.e("shApiMain", e.getMessage());
         e.printStackTrace();
      }
      return res;
   }

   /*
      Generally cover arts are stored in music files (with ID3 tags). There is a server-side
      implementation ready-to use to get such media data from the music file if the user
      application decides. User application may extract such info from the music file with
      it's own implementation also.
   */
   public static Drawable getCoverArt(final String hostaddr, final String mediabasepath) {
      final String url = "http://" + hostaddr + "/portal/?app=coverart&file=" + mediabasepath;
      return createBitmapFromURL(url);
   }

   /*
      createBitmapFromURLScaled: helper function to create scaled-graphics object
      from a media URL
   */
   public static Drawable createBitmapFromURLScaled(final String url, int width) {
      Log.v("shApiMain", "createBitmapFromURLScaled(): loading image from " + url);
      BitmapDrawable res = null;
      try {
         URL u = new URL(url);
         try {
            Bitmap bmp = BitmapFactory.decodeStream(u.openConnection().getInputStream());
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            float sfw = (((float) width) / ((float) w));
            float height = (((float)h) * ((float)width) / ((float)w));
            float sfh = (height / ((float) h));
            Matrix matrix = new Matrix();
            matrix.postScale(sfw, sfh);
            Bitmap sbmp = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true);
            res = new BitmapDrawable(m_apiContext.getResources(), sbmp);
         } catch(IOException e) {
            Log.e("shApiMain", e.getMessage());
            e.printStackTrace();
         }
      } catch(MalformedURLException e) {
         Log.e("shApiMain", e.getMessage());
         e.printStackTrace();
      }
      return res;
   }

   /*
      fetchWeatherData: accessing weather information on demand.
      Since such information is live data, it needs to be downloaded periodically.

      User application may decide to fetch this data periodically when required such
      as every 15 minutes.

      Please keep in mind that such information will be updated every 15 minutes
      in general on service provider side.
   */
   public static int fetchWeatherData() {
      if (m_apiInstance != null)
         return m_apiInstance.fetchWeatherData_private();
       return SHC_FAIL;
   }

   private int fetchWeatherData_private() {
      int rc = SHC_FAIL;
      String doc = null;
      try {
         doc = shgetwoemodel();
      } catch(Exception e) {
         Log.e("shApiMain", e.getMessage());
         e.printStackTrace();
      }
      if (doc == null)
         return SHC_FAIL;
      try {
         Document docweather = createDocumentFromXML(doc);
         Element clst = (Element)docweather.getElementsByTagName("cLst").item(0);
         weatherlist = new WeatherItem();
         NodeList taglocations = clst.getElementsByTagName("location");
         for (int i = 0; i < taglocations.getLength(); i++) {
            Node taglocation = taglocations.item(i);
            if (taglocation.getNodeType() != Node.ELEMENT_NODE)
               continue;
            Element elmlocation = (Element)taglocation;
            WeatherForecastItem tableforecast = new WeatherForecastItem();
            NodeList forecastlist = elmlocation.getElementsByTagName("forecast");
            if (forecastlist != null) {
               Element elmforecast = (Element)forecastlist.item(0);
               NodeList fl = elmforecast.getElementsByTagName("item");
               for (int fi = 0; fi < fl.getLength(); fi++) {
                  Node tagitem = fl.item(fi);
                  if (tagitem.getNodeType() != Node.ELEMENT_NODE)
                     continue;
                  String curritemid = "";
                  WeatherAttribute tableforecastattr = new WeatherAttribute();
                  Element elmitem = (Element)tagitem;
                  NodeList al = elmitem.getElementsByTagName("attr");
                  for (int ai = 0; ai < al.getLength(); ai++) {
                     Node a = al.item(ai);
                     if (a.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                     Element ae = (Element)a;
                     String keyname = ae.getAttribute("id");
                     Node av = a.getChildNodes().item(0);
                     String keyval = "";
                     if (av != null) {
                        if (av.getNodeType() == Node.TEXT_NODE)
                           keyval = av.getNodeValue();
                     }    
                     if (keyname.equals("day"))
                        curritemid = keyval;
                     tableforecastattr.put(keyname, keyval);
                  }
                  int aidx = 0;
                  tableforecastattr.index = new String[tableforecastattr.size()];
                  for (Enumeration<String> e = tableforecastattr.keys(); e.hasMoreElements();)
                     tableforecastattr.index[aidx++] = e.nextElement();
                  tableforecast.put(curritemid, tableforecastattr);
               }
               int aidx = 0;
               tableforecast.index = new String[tableforecast.size()];
               for (Enumeration<String> e = tableforecast.keys(); e.hasMoreElements();)
                  tableforecast.index[aidx++] = e.nextElement();
            }
            String currlocationid = "";
            WeatherAttribute tableattr = new WeatherAttribute();
            NodeList al = elmlocation.getElementsByTagName("attr");
            for (int ai = 0; ai < al.getLength(); ai++) {
               Node a = al.item(ai);
               if (a.getNodeType() != Node.ELEMENT_NODE)
                  continue;
               Element ae = (Element)a;
               String keyname = ae.getAttribute("id");
               Node av = a.getChildNodes().item(0);
               String keyval = ""; 
               if (av != null) {
                  if (av.getNodeType() == Node.TEXT_NODE)
                     keyval = av.getNodeValue();
               }     
               if (keyname.equals("woe"))
                  currlocationid = keyval;
               tableattr.put(keyname, keyval);
            }
            int aidx = 0;
            tableattr.index = new String[tableattr.size()];
            for (Enumeration<String> e = tableattr.keys(); e.hasMoreElements();)
               tableattr.index[aidx++] = e.nextElement();
            weatherlist.put(currlocationid, tableattr, tableforecast);
         }
         int widx = 0;
         weatherlist.index = new String[weatherlist.size()];
         for (Enumeration<String> e = weatherlist.keys(); e.hasMoreElements();)
            weatherlist.index[widx++] = e.nextElement();
         rc = SHC_OK;
      } catch(Exception e) {
         Log.e("shApiMain", e.getMessage());
         e.printStackTrace();
      }
      return rc;
   }

   /*
      killMyself: helper function
   */
   public static void killMyself(int reload) {
      Log.v("shApiMain", "killMyself: requested by sevrer");
      if (reload == 1) {
         if (m_apiInstance == null) {
            Log.e("shApiMain", "killMyself(reload): context not set!");
            return;
         }
         m_apiInstance.cbRemoteMessage("reload");
      } else {
         // terminate application as per server request
         android.os.Process.killProcess(android.os.Process.myPid());
      }
   }

   /*
      inetConnected: helper function to determine network connectivity
   */
   public static String inetConnected(int typ) {
      String res = "no";
      if (m_apiContext == null) {
         Log.e("shApiMain", "inetConnected: can't get context!");
         return res;
      }
      Log.v("shApiMain", "inetConnected: check");
      ConnectivityManager cm = (ConnectivityManager)m_apiContext.getSystemService(Context.CONNECTIVITY_SERVICE);

      NetworkInfo ether_net = cm.getActiveNetworkInfo();
      if (ether_net != null && ether_net.isConnected()) {
         res = "yes";
      }

      Log.v("shApiMain", "inetConnected: " + res);
      return res;
   }

   /*
      wifiConnected: helper function to determine wifi connectivity
      User application may decide to work or quit depending on wifi.
   */
   public static String wifiConnected(int typ) {
      String res = "no";
      if (m_apiContext == null) {
         Log.e("shApiMain", "wifiConnected: can't get context!");
         return res;
      }
      Log.v("shApiMain", "wifiConnected: check");
      ConnectivityManager cm = (ConnectivityManager)m_apiContext.getSystemService(Context.CONNECTIVITY_SERVICE);

      NetworkInfo wifi_net = cm.getActiveNetworkInfo();
      if (wifi_net != null && wifi_net.isConnectedOrConnecting()) {
         if (wifi_net.getType() == ConnectivityManager.TYPE_WIFI)
            res = "yes";
      }
      Log.v("shApiMain", "wifiConnected: " + res);
      return res;
   }

   /*
      gethwaddr: helper function to determine MAC address of the device
   */
   public static String gethwaddr(final String iface) {
      try {
         List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
         for (NetworkInterface nif : all) {
            if (!nif.getName().equalsIgnoreCase(iface))
               continue;

            byte[] macBytes = nif.getHardwareAddress();
            if (macBytes == null) {
               return "02:00:00:00:00:00";
            }

            StringBuilder res1 = new StringBuilder();
            for (byte b : macBytes) {
               res1.append(Integer.toHexString(b & 0xFF));
            }
            return res1.toString();
         }
      } catch (Exception e) {
         Log.e("shApiMain", e.getMessage());
         e.printStackTrace();
      }
      return "02:00:00:00:00:00";
   }

   /*
      getdevinfo: helper function to get device information
   */
   public static String getdevinfo() {
      return Build.MANUFACTURER + "," + Build.MODEL + "," + Build.VERSION.RELEASE;
   }

   /*
      getsysname: helper function to get host name
   */
   public static String getsysname() {
      BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (mBluetoothAdapter == null) {
          return "localhost";
      }
      return mBluetoothAdapter.getName();
   }

   /*
      getlocale: helper function to get locale
      User application may use this information to present different language on UI.
   */
   public static String getlocale() {
      return Locale.getDefault().getLanguage();
   }

}

