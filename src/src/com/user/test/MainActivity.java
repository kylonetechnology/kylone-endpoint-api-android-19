/*
 * MainActivity.java
 * Authors: Gokhan Poyraz <gokhan@kylone.com>
 *
 * Example Application for Kylone Client API for Android
 * Copyright (c) 2018, Kylone Technology International Ltd.
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

package com.user.test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.content.Context;
import android.view.View;  
import android.view.View.OnClickListener;  
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;  
import android.widget.Toast;  
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.text.method.ScrollingMovementMethod;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import java.lang.Runnable;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.kylone.shcapi.shApiMain;

public class MainActivity extends Activity {
   private static final String targetHost = "10.47.48.1";
   private static MainActivity m_instance;
   private static Context m_context;
   private static Handler m_handler;

   private shApi m_api = null;
   private TextView textview = null;
   private TextView welcometext = null;
   private ListView listview = null;
   private Button buttonConnect = null;
   private Button[] menuButtons = null;
   private ProgressDialog progressdialog = null;
   private ImageView appback = null;
   private ImageView menuback = null;
   private ImageView logoview = null;

   // Extend shApiMain to override some required functions (callbacks)
   private class shApi extends shApiMain {
      /*
         It is mandatory to create constructor and save context
         which will be used in static functions in API class.
      */
      public shApi(Context ctx) {
         m_apiContext = ctx;
         Log.v("shApi", "constructed");
      }

      public void setBannerText() {
         final String banner = shApi.shgetbannertext();
         welcometext.setText(banner);
         feedback("banner text set as " + banner + "\n");
      }
      /*
         RemoteMessage callback may triggered as per config changes on server side.
         Server will send such info as "message" to inform app.
      */
      @Override
      protected void cbRemoteMessage(final String msg) {
         m_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               feedback("~ cbRemoteMessage(" + msg + "): ");
               if (msg.equals(shApi.SHC_MESSAGE_BANNERTEXT)) {
                  setBannerText();
               } else
               if (msg.equals(shApi.SHC_MESSAGE_RESTART)) {
                  feedback("app-restart requested by server\n");
               } else
               if (msg.equals(shApi.SHC_MESSAGE_SUSPEND)) {
                  feedback("suspending requested by server\n");
               } else
               if (msg.equals(shApi.SHC_MESSAGE_RESUME)) {
                  feedback("resuming requested by server\n");
               } else
               if (msg.equals(shApi.SHC_MESSAGE_REBOOTSYSTEM)) {
                  feedback("system-reboot requested by server\n");
               } else
               if (msg.equals(shApi.SHC_MESSAGE_FWUPDATE)) {
                  feedback("firmware update requested by server\n");
               } else {
                  feedback("unknown messsage\n");
               }
            }
         }, 50);
      }
      // Progress callback
      @Override
      protected void cbProgress(final int p) {
         m_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               progressdialog.setIndeterminate(false);
               progressdialog.setProgress(p);
               feedback("progress: " + p + "\n");
            }
         }, 50);
      }
      // Handling connection errors
      @Override
      protected void cbConnectFailed(final int reason) {
         runOnUiThread(new Runnable() {
            @Override
            public void run() {
               progressdialog.setProgress(0);
               progressdialog.cancel();
               feedback("! connect failed with reason code: " + reason + "\n");
            }
         });
      }
      // Handling config-ready trigger, see details on shconnect() below
      @Override
      protected void cbConfigReady() {
         m_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               feedback("~ confguration is ready\n");
               // since config is arrived, we can utilise some values
               Drawable res = shApi.createBitmapFromURL(shApi.getConfigVal("bgnd"));
               if (res != null) {
                  appback.setScaleType(ImageView.ScaleType.CENTER_CROP);
                  appback.setImageAlpha(shApi.getAlpha(shApi.getConfigVal("opac")));
                  appback.setImageDrawable(res);
                  appback.setVisibility(View.VISIBLE);
                  feedback("~ background image set\n");
               }
               res = shApi.createBitmapFromURL(shApi.getConfigVal("logo"));
               if (res != null) {
                  logoview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                  logoview.setImageDrawable(res);
                  logoview.setVisibility(View.VISIBLE);
                  feedback("~ logo image set\n");
               }
            }
         }, 50);
      }
      // Handling content-ready trigger, see details on shconnect() below
      @Override
      protected void cbContentReady() {
         m_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               progressdialog.setProgress(0);
               progressdialog.cancel();
               feedback("~ content is ready\n");
               /* 
                  When content arrived, shApiMain.contentlist variable (shApiMain.ContentList) class will be constructed.
                  You can access all content infornation through this constructed class

                  Following example shows how to get list of main content items.
                  It can be determined whether there is Main Menu, Movie, TV, Music etc contents exists or not.
               */
               feedback("\n> showing list of root items in content as an example\n");
               String[] rootlist = shApi.contentlist.getList();
               for (int i = 0; i < shApi.contentlist.size(); i++) {
                  feedback("   - " + rootlist[i] + "\n");
               }
               feedback("\n");
               /*
                  Below example shows how to get list of Main Menu content (with category all = 0)
                  We will print details witll calling feddback function and add a Button for every Menu Item.
               */
               shApi.ContentItem menuitemsinallcategory = shApi.contentlist.get("item", "0");
               if (menuitemsinallcategory != null) {
                  feedbackPrintListItems("showing list of Menu Items as an example", menuitemsinallcategory);
                  addMenuButtons(menuitemsinallcategory);
               }

               /*
                  Another example below shows to access list of Movies. Instead of using category id directly from
                  get() method of shApiContentList, we will walk over on the data structure.
               */
               shApi.ContentCategory movies = shApi.contentlist.get("movie");
               if (movies != null) {
                  shApi.ContentItem movieinallcategory = movies.get("0");
                  if (movieinallcategory != null) {
                     feedback("\n> showing list of Movies with attributes as an example\n");
                     String[] l = movieinallcategory.getList();
                     for (int i = 0; i < movieinallcategory.size(); i++) {
                        shApi.ContentAttribute movieattr = movieinallcategory.get(i);
                        if (movieattr == null)
                           continue;
                        feedbackPrintAttributes(l[i], movieattr);
                        /*
                           We will just display the first item by using break below;
                           Please remove it to see all items with attributes in the movie list
                        */
                        break;
                     }
                  }
               }
               feedback("\n> click one of the Menu Buttons to show its content in ListView\n");
               setButtonClickListeners();
            }
         }, 50);
      }
   }

   public class customArrayAdapter extends ArrayAdapter<String> {
      private final Activity m_context;
      private String rootitem;
      private String category;

      public customArrayAdapter(Activity context, String rootitem, String category,  String[] itemlist) {
         super(context, R.layout.listitem, itemlist);
         this.m_context = context;
         this.rootitem = rootitem;
         this.category = category;
      }

      public View getView(int position, View view, ViewGroup parent) {
         shApi.ContentAttribute itemattributes = shApi.contentlist.get(rootitem, category, position);
         LayoutInflater inflater = m_context.getLayoutInflater();
         View itemview = inflater.inflate(R.layout.listitem, null, true);
         ImageView itemicon = (ImageView)itemview.findViewById(R.id.itemicon);
         TextView itemtitle = (TextView)itemview.findViewById(R.id.itemtitle);
         TextView itemtext = (TextView)itemview.findViewById(R.id.itemtext);
         if (itemattributes != null) {
            final String logourl = itemattributes.get("logo");
            if (logourl != null) {
               Drawable img = shApi.createBitmapFromURL(itemattributes.get("logo"));
               if (img != null) {
                  itemicon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                  itemicon.setImageDrawable(img);
               }
            }
            /*
               Since we are using this single code in this example app, we will display information
               according to main menu item type: attributes are different for music and movie etc.
               Normally app should have it's own UI per main menu type to process relevant information correctly.
            */
            if (rootitem.equals("music")) {
               itemicon.setImageResource(R.drawable.music);
               itemtitle.setText(itemattributes.get("title"));
               itemtext.setText(itemattributes.get("artist"));
            } else
            if (rootitem.equals("movie")) {
               itemtitle.setText(itemattributes.get("txt"));
               itemtext.setText(itemattributes.get("kind"));
            } else
            if (rootitem.equals("clip")) {
               itemtitle.setText(itemattributes.get("txt"));
               itemtext.setText(itemattributes.get("star"));
            } else {
               itemtitle.setText(itemattributes.get("txt"));
            }
         }
         return itemview;
      }
   }

   public MainActivity() {
      // save instance to use with static functions
      m_instance = this;
      Log.v("MainActivity", "constructed");
   }

   public void feedback(String s) {
      // Display message in TextView
      textview.append(s);
      // log the message too
      Log.v("MainActivity", s);
   }

   public void feedbackPrintListItems(String title, shApi.ContentItem items) {
      feedback("> " + title + "\n");
      String[] list = items.getList();
      for (int i = 0; i < list.length; i++) {
         feedback("   - " + list[i] + "\n");
      }
      feedback("\n");
   }

   public void feedbackPrintAttributes(String title, shApi.ContentAttribute attr) {
      feedback("  - item: " + title + "\n");
      String list[] = attr.getList();
      for (int i = 0; i < list.length; i++) {
         feedback("      " + list[i] + ": " + attr.get(i) + "\n");
      }
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // save context to use with static functions
      m_context = this;
      m_handler = new Handler();
      // Since this is an example, let's make it as simple as possible
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
      // Add some widgets to display data or do interventions
      setContentView(R.layout.main);
      this.createWidgets();
      // Create API object
      m_api = new shApi(this);
      /*
         Call shcreate() method with cache folder as an argument.
         There will be a new folder called ".shcache" will be created under given folder.
         This .shcache folder should kept while app running if SHC_OPT_DONOTCACHE called with shconnect() method
         it can be freely deleted before calling the shconnect() method or during the termination of app.
      */
      if (shApi.shcreate(getFilesDir().getAbsolutePath()) != shApi.SHC_OK) {
         feedback("! onCreate(): Failed to create API\n");
         return;
      };
      // get API version and print as feedback
      final String apiver = shApi.shgetapiversion();
      if (apiver != null)
         feedback("> Using API version " + apiver + " \n");
     
      // Allow interventions from UI once API created
      this.addListenerConnectButtonOnClick();
      feedback("> ready to connect\n");
   }

   private void createWidgets() {
      appback = (ImageView)findViewById(R.id.appback);
      menuback = (ImageView)findViewById(R.id.menuback);
      // Add a welcome text
      welcometext = (TextView)findViewById(R.id.welcometext);
      // Add a logo
      logoview = (ImageView)findViewById(R.id.logoview);
      // Add a TextView to display some messages in this example app
      textview = (TextView)findViewById(R.id.textview);
      textview.setMovementMethod(new ScrollingMovementMethod());
      // Add a ListView to display a list content
      listview = (ListView)findViewById(R.id.listview);
      // Add a button to test shconnect method
      buttonConnect = new Button(this);
      buttonConnect.setVisibility(View.GONE);
      buttonConnect.setText("Connect");
      buttonConnect.setBackgroundResource(R.drawable.refresh);
      buttonConnect.setTextColor(Color.parseColor("#dfdfdf"));
      buttonConnect.setGravity(Gravity.BOTTOM | Gravity.CENTER);
      // Add connect button to layout
      LinearLayout l = (LinearLayout)findViewById(R.id.buttonlayout);
      l.addView(buttonConnect);
      // Prepare a progress dialog to track status of content while loading
      progressdialog = new ProgressDialog(this);
      progressdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressdialog.setMax(100);
      progressdialog.setProgress(0);
   }

   public void addListenerConnectButtonOnClick() {
      buttonConnect.setVisibility(View.VISIBLE);
      buttonConnect.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {  
            // Hide some widgets not necessary and stop playing at this phase
            listview.setVisibility(View.GONE);
            listview.setAdapter(null);
            removeMenuButtons();
            welcometext.setText("");
            logoview.setVisibility(View.GONE);
            appback.setVisibility(View.GONE);
            menuback.setVisibility(View.GONE);
            textview.append("\n\n> re-loading content...\n");

            if (!shApi.inetConnected(0).equals("yes")) {
                feedback("! there is no network connectivity\n");
                // required tasks should be implemented
                return;
            };

            if (shApi.shserverisready(targetHost, 5) != shApi.SHC_OK) {
                /*
                  Server may still be doing boot or its services may not ready yet.
                  Custom application may try it few more times and then may give up.
                  Required tasks should be implemented
                */
                feedback("! server is not ready yet, should be tried later\n");
                return;
            };

            // Show a progress dialog
            progressdialog.setMessage("Loading Content");
            progressdialog.setIndeterminate(true);
            progressdialog.show();
            /*
               Call shconnect method with args (string address, integer option)

               Target address can be an IP address or a domain name. It is not URL, so avoid to use protocol prefixes.

               The pption is for doing cache or not. When SHC_OPT_DOCACHE is passed as an option, all remote resources 
               such as icons and images will be downloaded into cache folder and references of such resources will be 
               changed to local representations. Otherwise all references of such resources will be indicated as a URL
               on the server side.

               When shconnect get successfull, it will create a new thread to collect data from server side and returns
               immediatelly. In this phase there may some callback functions will be triggered. You may override such
               callback functions from the API class and handle such triggers;

               shApiMain.cbConfigReady(): Called when the config data collected.
               shApiMain.cbContentReady(): Called when the full-content collected.

               Even if the API call shconnect() creates it's own thread, it will be good idea to invoke it with Runnable
               to make UI responsive.
            */
            Runnable r = new Runnable() {
               @Override public void run() {
                  if (shApi.shconnect(targetHost, shApi.SHC_OPT_DOCACHE) != shApi.SHC_OK) {
                     Log.e("MainActivity", "shconnect(): Failed to connect target host\n");
                     // required tasks should be implemented
                  };
               }
            };
            m_handler.postDelayed(r, 100);
            feedback("> shconnect(): launched\n");
         }
      });
   }

   public void removeMenuButtons() {
      if (menuButtons != null) {
         LinearLayout layout = (LinearLayout)findViewById(R.id.buttonlayout);
         for (int i = 0; i < menuButtons.length; i++)
            layout.removeView(menuButtons[i]);
      }
   }

   public void addMenuButtons(shApi.ContentItem items) {
      LinearLayout layout = (LinearLayout)findViewById(R.id.buttonlayout);
      removeMenuButtons();
      menuButtons = new Button[items.size()];
      for (int i = 0; i < items.size(); i++) {
         shApi.ContentAttribute attr = items.get(i);
         if (attr == null) {
            menuButtons[i] = null;
            continue;
         }
         menuButtons[i] = new Button(this);
         menuButtons[i].setVisibility(View.GONE);
         // save attributes in Button to access later
         menuButtons[i].setTag(attr);
         // set Button properties
         menuButtons[i].setTextColor(Color.parseColor("#dfdfdf"));
         menuButtons[i].setGravity(Gravity.BOTTOM | Gravity.CENTER);
         //menuButtons[i].setText(attr.get("txt"));
         Drawable res = shApi.createBitmapFromURLScaled(attr.get("logo"), 80);
         if (res != null)
            menuButtons[i].setBackground(res);
         layout.addView(menuButtons[i]);
      }
   }

   public void setItemClickListener() {
      listview.setOnItemClickListener(new OnItemClickListener() {
         @Override 
         public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) { 
            String rootitem = (String)(((ListView)adapterview).getTag(R.id.contentkey));
            String category = (String)(((ListView)adapterview).getTag(R.id.categorykey));
            shApi.ContentAttribute itemattributes = shApi.contentlist.get(rootitem, category, position);
            if (itemattributes == null)
               return;
            feedback("\n> Item Clicked: " + itemattributes.get("txt") + "\n- content id is '" + rootitem + "'\n");
            String[] l = itemattributes.getList();
            for (int i = 0; i < itemattributes.size(); i++) {
               feedback("     -- " + l[i] + ": " + itemattributes.get(i) + "\n");
            }
            /*
               Since we are using this single code in this example app, we will display information
               according to main menu item type: attributes are different for music and movie etc.
               Normally app should have it's own UI per main menu type to process relevant information correctly.
            */
            if (rootitem.equals("music")) {
               // Following is an example to request and load music item's cover art image the server side
               Drawable res = shApi.getCoverArt(targetHost, itemattributes.get("fbp"));
               if (res != null) {
                  menuback.setScaleType(ImageView.ScaleType.CENTER_CROP);
                  menuback.setImageAlpha(648);
                  menuback.setImageDrawable(res);
                  menuback.setVisibility(View.VISIBLE);
                  feedback("~ menu background set as cover art image of item clicked\n");
               }
            } else
            if (rootitem.equals("locs")) {
               Drawable res = shApi.createBitmapFromURL(itemattributes.get("logo"));
               if (res != null) {
                  menuback.setScaleType(ImageView.ScaleType.CENTER_CROP);
                  menuback.setImageAlpha(shApi.getAlpha(itemattributes.get("opac")));
                  menuback.setImageDrawable(res);
                  menuback.setVisibility(View.VISIBLE);
                  feedback("~ menu background image set with location image\n");
               }
               String woeid = itemattributes.get("woe");
               shApi.WeatherAttribute woeattr = shApi.weatherlist.get(woeid);
               if (woeattr != null) {
                  feedback("> weather information for '" + woeid + "' (" + itemattributes.get("txt") + ") is;\n");
                  String[] wl = woeattr.getList();
                  for (int i = 0; i < woeattr.size(); i++) {
                     feedback("     -- " + wl[i] + ": " + woeattr.get(i) + "\n");
                  }
                  shApi.WeatherForecastItem fcitem = shApi.weatherlist.getForecast(woeid);
                  if (fcitem != null) {
                     String[] fl = fcitem.getList();
                     for (int i = 0; i < fcitem.size(); i++) {
                        feedback("  forecast " + fl[i] + "\n");
                        shApi.WeatherAttribute fcattr = fcitem.get(i);
                        String[] il = fcattr.getList();
                        for (int j = 0; j < fcattr.size(); j++) {
                           feedback("     -- " + il[j] + ": " + fcattr.get(j) + "\n");
                        }
                     }
                  }
               } else {
                  feedback("> weather information not found for '" + woeid + "' (" + itemattributes.get("txt") + ")\n");
               }
            }
         }
      });
   }

   public void openMenu(final String rootitem, final String category, final String[] list) {
      m_handler.postDelayed(new Runnable() {
         @Override
         public void run() {
            ArrayAdapter adapter = new customArrayAdapter(m_instance, rootitem, category, list);
            listview.setAdapter(adapter);
            // save rootitem  for further use
            listview.setTag(R.id.contentkey, rootitem);
            // save category id for further use
            listview.setTag(R.id.categorykey, category);
            listview.setVisibility(View.VISIBLE);
            setItemClickListener();
            if (progressdialog.isShowing())
               progressdialog.cancel();
         }
      }, 50);
   }

   public void openWeatherMenu(final String rootitem, final String category, final String[] list, final String title) {
      progressdialog.setMessage(title);
      progressdialog.setIndeterminate(true);
      progressdialog.show();
      m_handler.postDelayed(new Runnable() {
         @Override
         public void run() {
            final int fres = shApi.fetchWeatherData();
            if (fres == shApi.SHC_FAIL) {
               // If whether info can't be fetched, app may display a warning
               progressdialog.cancel();
               feedback("! unable to fetch weather information\n");
               return;
            } else {
               feedback("- weather information fetched\n");
               openMenu(rootitem, category, list);
            }
         }
      }, 50);
   }

   public void openMenu(View view) {
      listview.setVisibility(View.GONE);
      listview.setAdapter(null);
      shApi.ContentAttribute attr = (shApi.ContentAttribute)view.getTag();
      if (attr == null)
         return;
      String rootitem = attr.get("src");
      String itemtext = attr.get("txt");
      feedback("\n> " + itemtext + " clicked\n");
      feedbackPrintAttributes(itemtext, attr);
      Drawable res = shApi.createBitmapFromURL(attr.get("bgnd"));
      if (res != null) {
         menuback.setScaleType(ImageView.ScaleType.CENTER_CROP);
         menuback.setImageAlpha(shApi.getAlpha(attr.get("opac")));
         menuback.setImageDrawable(res);
         menuback.setVisibility(View.VISIBLE);
         feedback("~ menu background image set\n");
      }
      // In this example app, we will display category with id "0" only
      String category = "0";
      shApi.ContentItem list = shApi.contentlist.get(rootitem, "0");
      if (list == null) {
         feedback("- no such content with category ID '0' for menuitem '" + rootitem + "'\n");
         /*
            If there is no category found with id "0", we may try to get first available category in this example app.
            Normally items should be displayed in group with categories.
         */
         shApi.ContentCategory categories = shApi.contentlist.get(rootitem);
         if (categories == null) {
            feedback("- no such content called " + rootitem + "\n");
            /*
               if there is no category found, this means there is no such content found with name equal to main menu id.
               This may happen to some items. For example, since it is an external content, there is "weather" menu ID but
               there is no content associated with this name. Instead, there are locations with content id "locs" in the
               content list and app should determine list of locations to display weather information from there.
               
               We can check Main Menu ID (if equals "weather") to show Weather information using "locs" content like below;
            */
            if (rootitem.equals("weather")) {
               feedback("- content set as 'locs' for menuitem '" + rootitem + "'\n");
               rootitem = "locs";
               category = "0"; // if there is no category id, then it will always be "0"
               list = shApi.contentlist.get(rootitem, category);
               if (list == null) {
                  // It seems there is no location configured on server side, App may display warning about that
                  feedback("- no such content configured for '" + rootitem +"'\n");
                  return;
               }
               /*
                  We found list of locations which configured on server side. 
                  Now, we need to load weather information available on server to display
                  Once it fetched from the server, required classes and data-sets will be configured to use such information
               */
               feedback("> loading weather information from server ..\n");
               openWeatherMenu(rootitem, category, list.getList(), "Loading weather information");
               return;
            } else {
               /*
                  Another usage is sub-menus. If attriute of menu item "sub" equals "1" then it means it's content located in
                  category with same name. For example, if "sub=1" of "info" item, then we need to look "info" category and 
                  display it's content as sub-menu. Please find below how it can be implemented;
               */
               final String isSubMenu = attr.get("sub");
               if (isSubMenu == null)
                  return;
               if (!isSubMenu.equals("1"))
                  return;
               category = rootitem; // we will look into category with same name of the menu item
               /*
                  Since we are looking for sub-menu, we need to access "item" content and find relevant category from inside
               */
               rootitem = "item";
               feedback("- info has sub-menu property (sub=1), looking for category '" + category + "' in '" + rootitem + "'\n");
               list = shApi.contentlist.get(rootitem, category);
               if (list == null)  {
                  // It seems there is no item configured in this submenu (on server side), App may display an empty list
                  feedback("- no such content available at '" + category + "' in '" + rootitem + "'\n");
                  return;
               }
               feedback("- sub-menu found at category '" + category + "' in '" + rootitem + "'\n");
            }
         } else {
            feedback("- there are categories except ID '0' for menuitem '" + rootitem + "'\n");
            String[] catlist = categories.getList();
            if (catlist.length > 0) {
               for (int i = 0; i < catlist.length; i++) {
                  category = catlist[i];
                  list = shApi.contentlist.get(rootitem, category);
                  if (list != null)
                     break;
               }
               if (list == null) {
                  // Some sanity check, normally this should not happen
                  feedback("- no such content configured for '" + rootitem +"'\n");
                  return;
               }
               feedback("- using first available category '" + category + "' for '" + rootitem + "' to display content for demo purposes\n");
            } else {
               /*
                  It seems that content is not available for this main menu item
                  App may display informaton about that
               */
               feedback("- no such content configured for '" + rootitem +"'\n");
               return;
            }
         }
      }
      if (list.size() == 0) {
         /*
            It seems that content is not available for this main menu item
            App may display informaton about that
         */
         feedback("- no such content configured for '" + rootitem +"'\n");
      }
      openMenu(rootitem, category, list.getList());
   }

   public void setButtonClickListeners() {
      for (int i = 0; i < menuButtons.length; i++) {
         if (menuButtons[i] == null)
            continue;
         menuButtons[i].setVisibility(View.VISIBLE);
         menuButtons[i].setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {  
               final View v = view;
               m_handler.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                     openMenu(v);
                  }
               }, 50);
            }
         });
      }
   }

}

