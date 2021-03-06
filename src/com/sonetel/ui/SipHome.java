/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sonetel.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.sonetel.R;
import com.sonetel.api.SipConfigManager;
import com.sonetel.api.SipManager;
import com.sonetel.api.SipProfile;
import com.sonetel.db.DBAdapter;
import com.sonetel.db.DBProvider;
import com.sonetel.plugins.sonetelcallthru.AccessNumbers;
import com.sonetel.plugins.sonetelcallthru.Location_Finder;
import com.sonetel.ui.calllog.CallLogListFragment;
import com.sonetel.ui.dialpad.ContactFragment;
import com.sonetel.ui.dialpad.DialerFragment;
import com.sonetel.ui.favorites.FavListFragment;
import com.sonetel.ui.help.Help;
import com.sonetel.ui.messages.ConversationsListFragment;
import com.sonetel.ui.prefs.PrefsLogic;
import com.sonetel.ui.prefs.cupcake.PrefsLoaderActivity;
import com.sonetel.ui.warnings.WarningFragment;
import com.sonetel.ui.warnings.WarningUtils;
import com.sonetel.ui.warnings.WarningUtils.OnWarningChanged;
import com.sonetel.utils.Compatibility;
import com.sonetel.utils.CustomDistribution;
import com.sonetel.utils.Log;
import com.sonetel.utils.NightlyUpdater;
import com.sonetel.utils.NightlyUpdater.UpdaterPopupLauncher;
import com.sonetel.utils.PreferencesProviderWrapper;
import com.sonetel.utils.PreferencesWrapper;
import com.sonetel.wizards.BasePrefsWizard;
import com.sonetel.wizards.WizardUtils.WizardInfo;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class SipHome extends SherlockFragmentActivity implements OnWarningChanged {
    public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
    public static final int PARAMS_MENU = Menu.FIRST + 2;
    public static final int CLOSE_MENU = Menu.FIRST + 3;
    public static final int HELP_MENU = Menu.FIRST + 4;
    public static final int DISTRIB_ACCOUNT_MENU = Menu.FIRST + 5;

	public static final String LAST_KNOWN_VERSION_PREF = "last_known_version";
	public static final String LAST_KNOWN_ANDROID_VERSION_PREF = "last_known_aos_version";

    private static final String THIS_FILE = "SIP_HOME";

    private static int TAB_ID_DIALER = 0;
    private static int TAB_ID_CONTACT = 1;
    private static int TAB_ID_CALL_LOG = 2;
    private static int TAB_ID_FAVORITES = 3;
    private static int TAB_ID_MESSAGES = 4;
    private static int TAB_ID_WARNING = 5;

    // protected static final int PICKUP_PHONE = 0;
    private static final int REQUEST_EDIT_DISTRIBUTION_ACCOUNT = 0;

    private PreferencesWrapper prefWrapper;
    private PreferencesProviderWrapper prefProviderWrapper;

    private boolean hasTriedOnceActivateAcc = false;
    private boolean isInternetAvail = false;
    // private ImageButton pickupContact;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private boolean mDualPane;
    private Thread asyncSanityChecker;
    private Tab warningTab;
    private ObjectAnimator warningTabfadeAnim;

    public final static boolean USE_LIGHT_THEME = false;

    /**
     * Listener interface for Fragments accommodated in {@link ViewPager}
     * enabling them to know when it becomes visible or invisible inside the
     * ViewPager.
     */
    public interface ViewPagerVisibilityListener {
        void onVisibilityChanged(boolean visible);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        prefWrapper = new PreferencesWrapper(this);
        prefProviderWrapper = new PreferencesProviderWrapper(this);

        /*
         * Resources r; try { r =
         * getPackageManager().getResourcesForApplication("com.etatgere"); int
         * rThemeId = r.getIdentifier("com.etatgere:style/LightTheme", null,
         * null); Log.e(THIS_FILE, "Remote theme " + rThemeId); Theme t =
         * r.newTheme(); t.applyStyle(rThemeId, false); //getTheme().setTo(t); }
         * catch (NameNotFoundException e) { Log.e(THIS_FILE,
         * "Not found app etatgere"); }
         */
        if (USE_LIGHT_THEME) {
           // setTheme(R.style.LightTheme_noTopActionBar);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.sip_home);

        final ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // showAbTitle = Compatibility.hasPermanentMenuKey

        

        Tab dialerTab = ab.newTab()
                 .setContentDescription(R.string.dial_tab_name_text)
                .setIcon(R.drawable.ic_ab_dialer_holo_dark);
                // .setText("Dial Pad");
        Tab contactTab = ab.newTab()
                .setContentDescription(R.string.dial_tab_name_text)
               .setIcon(R.drawable.sntl_contact);
               //.setText("Contacts");
        Tab callLogTab = ab.newTab()
                 .setContentDescription(R.string.calllog_tab_name_text)
                .setIcon(R.drawable.ic_ab_history_holo_dark);
                // .setText("Call Logs");
      /*  Tab favoritesTab = ab.newTab()
                 .setContentDescription(R.string.favorites_tab_name_text)
                .setIcon(R.drawable.ic_ab_favourites_holo_dark);*/
        
        Tab messagingTab = null;
        if (CustomDistribution.supportMessaging()) {
            messagingTab = ab.newTab()
                    .setContentDescription(R.string.messages_tab_name_text)
                    .setIcon(R.drawable.ic_ab_text_holo_dark);
        }
        
        warningTab = ab.newTab().setIcon(android.R.drawable.ic_dialog_alert);
        warningTabfadeAnim = ObjectAnimator.ofInt(warningTab.getIcon(), "alpha", 255, 100);
        warningTabfadeAnim.setDuration(1500);
        warningTabfadeAnim.setRepeatCount(ValueAnimator.INFINITE);
        warningTabfadeAnim.setRepeatMode(ValueAnimator.REVERSE);
        
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mViewPager);
        mTabsAdapter.addTab(dialerTab, DialerFragment.class, TAB_ID_DIALER);
        mTabsAdapter.addTab(contactTab,ContactFragment.class, TAB_ID_CONTACT);
        mTabsAdapter.addTab(callLogTab, CallLogListFragment.class, TAB_ID_CALL_LOG);
      //  mTabsAdapter.addTab(favoritesTab, FavListFragment.class, TAB_ID_FAVORITES);
        if (messagingTab != null) {
            mTabsAdapter.addTab(messagingTab, ConversationsListFragment.class, TAB_ID_MESSAGES);
        }
        

        hasTriedOnceActivateAcc = false;

        if (!prefProviderWrapper.getPreferenceBooleanValue(SipConfigManager.PREVENT_SCREEN_ROTATION)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        selectTabWithAction(getIntent());
        Log.setLogLevel(prefProviderWrapper.getLogLevel());
        
        isInternetAvail = prefProviderWrapper.isInternetAvail();
        
        if(!isInternetAvail)
        {
        	new AlertDialog.Builder(this) 

			.setTitle("Sonetel") 

			.setMessage("No Internet Access") 
			
			.setIcon(R.drawable.ic_wizard_sonetel)
			
			.setNeutralButton("Exit", 

			new DialogInterface.OnClickListener() { 

			public void onClick(DialogInterface dialog, 

			int which) { 
				System.exit(0); 
			} 

			}).show(); 
        }

        // Async check
      /*  asyncSanityChecker = new Thread() {
            public void run() {
                asyncSanityCheck();
            };
        };
        asyncSanityChecker.start();*/
        
    }
   
    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost. It relies on a
     * trick. Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp
     * high (it is not shown) and the TabsAdapter supplies its own dummy view to
     * show as the tab content. It listens to changes in tabs, and takes care of
     * switch to the correct paged in the ViewPager whenever the selected tab
     * changes.
     */
    private class TabsAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final List<String> mTabs = new ArrayList<String>();
        private final List<Integer> mTabsId = new ArrayList<Integer>();
        private boolean hasClearedDetails = false;
        

        private int mCurrentPosition = -1;
        /**
         * Used during page migration, to remember the next position
         * {@link #onPageSelected(int)} specified.
         */
        private int mNextPosition = -1;

        public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = actionBar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, int tabId) {
            mTabs.add(clss.getName());
            mTabsId.add(tabId);
            mActionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }
        
        public void removeTabAt(int location) {
            mTabs.remove(location);
            mTabsId.remove(location);
            mActionBar.removeTabAt(location);
            notifyDataSetChanged();
        }
        
        public Integer getIdForPosition(int position) {
            if(position >= 0 && position < mTabsId.size()) {
                return mTabsId.get(position);
            }
            return null;
        }
        
        public Integer getPositionForId(int id) {
            int fPos = mTabsId.indexOf(id);
            if(fPos >= 0) {
                return fPos;
            }
            return null;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return Fragment.instantiate(mContext, mTabs.get(position), new Bundle());
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            clearDetails();
            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);

            if (mCurrentPosition == position) {
                Log.w(THIS_FILE, "Previous position and next position became same (" + position
                        + ")");
            }

            mNextPosition = position;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // Nothing to do
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            // Nothing to do
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Nothing to do
        }

        /*
         * public void setCurrentPosition(int position) { mCurrentPosition =
         * position; }
         */

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    if (mCurrentPosition >= 0) {
                        sendFragmentVisibilityChange(mCurrentPosition, false);
                    }
                    if (mNextPosition >= 0) {
                        sendFragmentVisibilityChange(mNextPosition, true);
                    }
                    invalidateOptionsMenu();

                    mCurrentPosition = mNextPosition;
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING:
                    clearDetails();
                    hasClearedDetails = true;
                    break;
                case ViewPager.SCROLL_STATE_SETTLING:
                    hasClearedDetails = false;
                    break;
                default:
                    break;
            }
        }

        private void clearDetails() {
            if (mDualPane && !hasClearedDetails) {
                FragmentTransaction ft = SipHome.this.getSupportFragmentManager()
                        .beginTransaction();
                ft.replace(R.id.details, new Fragment(), null);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    private DialerFragment mDialpadFragment;
    private CallLogListFragment mCallLogFragment;
    private ConversationsListFragment mMessagesFragment;
    private FavListFragment mPhoneFavoriteFragment;
    private WarningFragment mWarningFragment;

    private Fragment getFragmentAt(int position) {
        Integer id = mTabsAdapter.getIdForPosition(position);
        if(id != null) {
            if (id == TAB_ID_DIALER) {
                return mDialpadFragment;
            } 
            else if(id == TAB_ID_CONTACT) {
            	return mDialpadFragment;
            } else if (id == TAB_ID_CALL_LOG) {
                return mCallLogFragment;
            } else if (position == TAB_ID_MESSAGES) {
                return mMessagesFragment;
            } else if (position == TAB_ID_FAVORITES) {
                return mPhoneFavoriteFragment;
            } else if (position == TAB_ID_WARNING) {
                return mWarningFragment;
            }
        }
        throw new IllegalStateException("Unknown fragment index: " + position);
    }

    public Fragment getCurrentFragment() {
        if (mViewPager != null) {
            return getFragmentAt(mViewPager.getCurrentItem());
        }
        return null;
    }

    private void sendFragmentVisibilityChange(int position, boolean visibility) {
        try {
            final Fragment fragment = getFragmentAt(position);
            if (fragment instanceof ViewPagerVisibilityListener) {
                ((ViewPagerVisibilityListener) fragment).onVisibilityChanged(visibility);
            }
        }catch(IllegalStateException e) {
            Log.e(THIS_FILE, "Fragment not anymore managed");
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // This method can be called before onCreate(), at which point we cannot
        // rely on ViewPager.
        // In that case, we will setup the "current position" soon after the
        // ViewPager is ready.
        final int currentPosition = mViewPager != null ? mViewPager.getCurrentItem() : -1;
        Integer tabId = null; 
        if(mTabsAdapter != null) {
            tabId = mTabsAdapter.getIdForPosition(currentPosition);
        }
        if (fragment instanceof DialerFragment) {
            mDialpadFragment = (DialerFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_DIALER) {
                mDialpadFragment.onVisibilityChanged(true);
                initTabId = null;
            }
        }
        else if (fragment instanceof DialerFragment) {
            mDialpadFragment = (DialerFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_CONTACT) {
                mDialpadFragment.onVisibilityChanged(true);
                initTabId = null;
            }
        } else if (fragment instanceof CallLogListFragment) {
            mCallLogFragment = (CallLogListFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_CALL_LOG) {
                mCallLogFragment.onVisibilityChanged(true);
                initTabId = null;
            }
        } else if (fragment instanceof ConversationsListFragment) {
            mMessagesFragment = (ConversationsListFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_MESSAGES) {
                mMessagesFragment.onVisibilityChanged(true);
                initTabId = null;
            }
        } else if (fragment instanceof FavListFragment) {
            mPhoneFavoriteFragment = (FavListFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_FAVORITES) {
                mPhoneFavoriteFragment.onVisibilityChanged(true);
                initTabId = null;
            }
        } else if (fragment instanceof WarningFragment) {
            mWarningFragment = (WarningFragment) fragment;
            synchronized (warningList) {
                mWarningFragment.setWarningList(warningList);
                mWarningFragment.setOnWarningChangedListener(this);
            }
            
        }

    }


    private void asyncSanityCheck() {
        // if(Compatibility.isCompatible(9)) {
        // // We check now if something is wrong with the gingerbread dialer
        // integration
        // Compatibility.getDialerIntegrationState(SipHome.this);
        // }
        
        // Nightly build check
        if(NightlyUpdater.isNightlyBuild(this)) {
            Log.d(THIS_FILE, "Sanity check : we have a nightly build here");
            ConnectivityManager connectivityService = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo ni = connectivityService.getActiveNetworkInfo();
            // Only do the process if we are on wifi
            if (ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                // Only do the process if we didn't dismissed previously
                NightlyUpdater nu = new NightlyUpdater(this);

                if (!nu.ignoreCheckByUser()) {
                    long lastCheck = nu.lastCheck();
                    long current = System.currentTimeMillis();
                    long oneDay = 43200000; // 12 hours
                    if (current - oneDay > lastCheck) {
                        if (onForeground) {
                            // We have to check for an update
                            UpdaterPopupLauncher ru = nu.getUpdaterPopup(false);
                            if (ru != null && asyncSanityChecker != null) {
                                runOnUiThread(ru);
                            }
                        }
                    }
                }
            }
        }
        
        applyWarning(WarningUtils.WARNING_PRIVILEGED_INTENT, WarningUtils.shouldWarnPrivilegedIntent(this, prefProviderWrapper));
        applyWarning(WarningUtils.WARNING_NO_STUN, WarningUtils.shouldWarnNoStun(prefProviderWrapper));
        applyWarning(WarningUtils.WARNING_VPN_ICS, WarningUtils.shouldWarnVpnIcs(prefProviderWrapper));
        applyWarning(WarningUtils.WARNING_SDCARD, WarningUtils.shouldWarnSDCard(this, prefProviderWrapper));
    }

    // Service monitoring stuff
    private void startSipService() {
        Thread t = new Thread("StartSip") {
            public void run() {
                Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
                serviceIntent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, new ComponentName(SipHome.this, SipHome.class));
                startService(serviceIntent);
                postStartSipService();
            };
        };
        t.start();

    }

    private void postStartSipService() {
        // If we have never set fast settings
    	//DBAdapter db = new DBAdapter(this);
    	TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if (CustomDistribution.showFirstSettingScreen()) {
            if (!prefProviderWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, false)) {
            	
            	//ContentValues initialValues = new ContentValues(); 
            	

            		for(int i=1; i<=52; i++)
            		{
            			//initialValues = AccessNumbers.getContentValues(i);
            			getContentResolver().insert(SipProfile.ACCESS_URI, AccessNumbers.getContentValues(i));
            			
            			//db1.insert(db.databaseHelper.ACCESS_NUMS_TABLE_NAME, null, initialValues);
            		}
           
            	}
            
				prefProviderWrapper.setPreferenceBooleanValue(prefWrapper.ACCESS_NUM_ALREADY_FETCHED, true);// yuva should be true
				
				
				prefWrapper.setSimId(tm.getSubscriberId());
				Location_Finder locFinder = new Location_Finder(getBaseContext());
				prefWrapper.setTelCountryCode(locFinder.getTelContryCode());
				prefWrapper.setCurrentLocation( tm.getNetworkCountryIso());
				
				//PrefsLogic.setCallthruNumber(getBaseContext());
				

            }
        	else {
        			boolean doFirstParams = !prefProviderWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, false);
        			prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, true);
        			if(!tm.getSubscriberId().equalsIgnoreCase(prefWrapper.getSimId()))
        			{
        				//final DBAdapter db = new DBAdapter(this);
        				final EditText input = new EditText(this); 
        				
        				new AlertDialog.Builder(this) 

        				.setTitle("New SIM: Please enter new mobile number") 

        				//.setMessage("Please enter your new mobile number in Sign in screen") 
        				
        				.setIcon(R.drawable.ic_wizard_sonetel)
        				
        				.setView(input)
        				
        				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {   
        				    public void onClick(DialogInterface dialog, int whichButton) {
        				    	String mobile = input.getText().toString(); 
        					        
        				        int nbrOfAccount = doWeHaveAccount();
        				        SipProfile account = new SipProfile();
        				        if(nbrOfAccount != 0)
        				        {
        				        	account =  SipProfile.getProfileFromDbId(getActivity(), 1, DBProvider.ACCOUNT_FULL_PROJECTION);
        				        	account.mobile_nbr = mobile;
        				        	getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, 1), account.getDbContentValues(), null, null);
        				        }
              				        
        						TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        						
        						prefWrapper.setSimId( tm.getSubscriberId());
        				                           
        						
        				        if(mobile.length()< 8 && !mobile.startsWith("+"))
        						{
        					       	Toast.makeText(getBaseContext(),"Invalid mobile number. Please enter +MOBILENUMBER in the international format", Toast.LENGTH_LONG).show();
        						}
        				    }

							private Context getActivity() {
								// TODO Auto-generated method stub
								return null;
							}
        				     }) 
        				     

        				.show(); 
        			}
        			if (doFirstParams) {
        				prefProviderWrapper.resetAllDefaultValues();
        			}
         	}

        // If we have no account yet, open account panel,
        if (!hasTriedOnceActivateAcc && isInternetAvail) {
        	int accountCount = 0;
        
        	accountCount = doWeHaveAccount();
            
            if (accountCount == 0) {
                Intent accountIntent = null;
                WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
                if (distribWizard != null) {
                    accountIntent = new Intent(this, BasePrefsWizard.class);
                    accountIntent.putExtra(SipProfile.FIELD_WIZARD, distribWizard.id);
                }// else {
                 //   accountIntent = new Intent(this, AccountsEditList.class);
               // }

                if (accountIntent != null) {
                    accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(accountIntent);
                    hasTriedOnceActivateAcc = true;
                    return;
                }
            }
            hasTriedOnceActivateAcc = false;
        }
    }

    private boolean onForeground = false;
    
    private int doWeHaveAccount()
    {
    	int accountCount =0;
    	
    	Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
    			SipProfile.FIELD_ID
    	}, null, null, null);
    
    	if (c != null) {
    		accountCount =  c.getCount();
    }
    	c.close();
    	return accountCount;
    }
    @Override
    protected void onPause() {
        Log.d(THIS_FILE, "On Pause SIPHOME");
        onForeground = false;
        if(asyncSanityChecker != null) {
            if(asyncSanityChecker.isAlive()) {
                asyncSanityChecker.interrupt();
                asyncSanityChecker = null;
            }
        }
        super.onPause();

    }

    @Override
    protected void onResume() {
    	 
//    	if(!hasTriedOnceActivateAcc)
  //  		disconnect(true);
    	
        Log.d(THIS_FILE, "On Resume SIPHOME");
        super.onResume();
        onForeground = true;

        prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, false);
        
        // Set visible the currently selected account
        sendFragmentVisibilityChange(mViewPager.getCurrentItem(), true);
        
        Log.d(THIS_FILE, "WE CAN NOW start SIP service");
        startSipService();
             

    }
    

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        selectTabWithAction(intent);
    }

    Integer initTabId = null;
    private void selectTabWithAction(Intent intent) {
        if (intent != null) {
            String callAction = intent.getAction();
            if (!TextUtils.isEmpty(callAction)) {
                ActionBar ab = getSupportActionBar();
                Tab toSelectTab = null;
                Integer toSelectId = null;
                if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_DIALER)
                        || callAction.equalsIgnoreCase(Intent.ACTION_DIAL)) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_DIALER);
                    if(pos != null && pos == 1) {
                        toSelectTab = ab.getTabAt(pos);
                        Uri data = intent.getData();
                        if(data != null && mDialpadFragment != null) {
                            String nbr = data.getSchemeSpecificPart();
                            if(!TextUtils.isEmpty(nbr)) {
                                mDialpadFragment.setTextDialing(true);
                                mDialpadFragment.setTextFieldValue(nbr);
                            }
                        }
                        toSelectId = TAB_ID_DIALER;
                    }
                    else if (pos != null && pos == 2) {
                        toSelectTab = ab.getTabAt(pos);
                        Uri data = intent.getData();
                        if(data != null && mDialpadFragment != null) {
                            String nbr = data.getSchemeSpecificPart();
                            if(!TextUtils.isEmpty(nbr)) {
                                mDialpadFragment.setTextDialing(false);
                                mDialpadFragment.setTextFieldValue(nbr);
                            }
                        }
                        toSelectId = TAB_ID_CONTACT;
                    }
                } 
               /* else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_DIALER)
                        || callAction.equalsIgnoreCase(Intent.ACTION_DIAL)) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_CONTACT);
                    mDialpadFragment.setTextDialing(false);
                    if(pos != null) {
                        toSelectTab = ab.getTabAt(pos);
                        Uri data = intent.getData();
                        if(data != null && mDialpadFragment != null) {
                            String nbr = data.getSchemeSpecificPart();
                            if(!TextUtils.isEmpty(nbr)) {
                                mDialpadFragment.setTextDialing(false);
                                mDialpadFragment.setTextFieldValue(nbr);
                            }
                        }
                        toSelectId = TAB_ID_CONTACT;
                    }
                }*/
                else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_CALLLOG)) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_CALL_LOG);
                    if(pos != null) {
                        toSelectTab = ab.getTabAt(pos);
                        toSelectId = TAB_ID_CALL_LOG;
                    }
                } else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_FAVORITES)) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_FAVORITES);
                    if(pos != null) {
                        toSelectTab = ab.getTabAt(pos);
                        toSelectId = TAB_ID_FAVORITES;
                    }
                } else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_MESSAGES)) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_MESSAGES);
                    if(pos != null) {
                        toSelectTab = ab.getTabAt(pos);
                        toSelectId = TAB_ID_MESSAGES;
                    }
                }
                if (toSelectTab != null) {
                    ab.selectTab(toSelectTab);
                    initTabId = toSelectId;
                }else {
                    initTabId = null;
                }
                
            }
        }
    }

    @Override
    protected void onDestroy() {
        disconnect(false);
        super.onDestroy();
        Log.d(THIS_FILE, "---DESTROY SIP HOME END---");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        int actionRoom = MenuItem.SHOW_AS_ACTION_NEVER;//getResources().getBoolean(R.bool.menu_in_bar) ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER;
        
        WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
        if (distribWizard != null) {
           // menu.add(Menu.NONE, DISTRIB_ACCOUNT_MENU, Menu.NONE, "My " + distribWizard.label)
        	 menu.add(Menu.NONE, DISTRIB_ACCOUNT_MENU, Menu.NONE, "Sign in")//distribWizard.label)
                    .setIcon(distribWizard.icon)
                    .setShowAsAction(actionRoom);
        }
        if (CustomDistribution.distributionWantsOtherAccounts()) {
            int accountRoom = actionRoom;
            if(Compatibility.isCompatible(13)) {
                accountRoom |= MenuItem.SHOW_AS_ACTION_WITH_TEXT;
            }
            menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE,
                    (distribWizard == null) ? R.string.accounts : R.string.other_accounts)
                    .setIcon(R.drawable.ic_menu_account_list)
                    .setAlphabeticShortcut('a')
                    .setShowAsAction( accountRoom );
        }
        menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, R.string.prefs)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        menu.add(Menu.NONE, HELP_MENU, Menu.NONE, R.string.help)
                .setIcon(android.R.drawable.ic_menu_help)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, R.string.menu_disconnect)
                .setIcon(R.drawable.ic_lock_power_off)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ACCOUNTS_MENU:
                //startActivity(new Intent(this, AccountsEditList.class));
                return true;
            case PARAMS_MENU:
              // startActivityForResult(new Intent(SipManager.ACTION_UI_PREFS_GLOBAL), CHANGE_PREFS);
               // startActivity(new Intent(this,SonetelSettings.class));
               startActivity(getIntentForType(PrefsLogic.TYPE_SONETEL_SETTINGS));
                return true;
            case CLOSE_MENU:
                Log.d(THIS_FILE, "CLOSE");
                prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, true);
                disconnect(true);
               /** if (prefProviderWrapper.isValidConnectionForIncoming()) {
                    // Alert user that we will disable for all incoming calls as
                    // he want to quit
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.warning)
                            .setMessage(getString(R.string.disconnect_and_incoming_explaination))
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // prefWrapper.disableAllForIncoming();
                                    prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, true);
                                    disconnect(true);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    ArrayList<String> networks = prefProviderWrapper.getAllIncomingNetworks();
                    if (networks.size() > 0) {
                        String msg = getString(R.string.disconnect_and_will_restart,
                                TextUtils.join(", ", networks));
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                    disconnect(true);
                }*/
                return true;
            case HELP_MENU:
                // Create the fragment and show it as a dialog.
                DialogFragment newFragment = Help.newInstance();
                newFragment.show(getSupportFragmentManager(), "dialog");
                return true;
            case DISTRIB_ACCOUNT_MENU:
                WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();

                Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
                        SipProfile.FIELD_ID
                }, SipProfile.FIELD_WIZARD + "=?", new String[] {
                        distribWizard.id
                }, null);

                Intent it = new Intent(this, BasePrefsWizard.class);
                it.putExtra(SipProfile.FIELD_WIZARD, distribWizard.id);
                Long accountId = null;
                if (c != null && c.getCount() > 0) {
                    try {
                        c.moveToFirst();
                        accountId = c.getLong(c.getColumnIndex(SipProfile.FIELD_ID));
                    } catch (Exception e) {
                        Log.e(THIS_FILE, "Error while getting wizard", e);
                    } finally {
                        c.close();
                    }
                }
                if (accountId != null) {
                    it.putExtra(SipProfile.FIELD_ID, accountId);
                }
                startActivityForResult(it, REQUEST_EDIT_DISTRIBUTION_ACCOUNT);

                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
	private Intent getIntentForType(int t) {
	    Intent it = new Intent(this, PrefsLoaderActivity.class);
	    it.putExtra(PrefsLogic.EXTRA_PREFERENCE_TYPE, t);
	    return it;
	}
	
    private final static int CHANGE_PREFS = 1;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHANGE_PREFS) {
            sendBroadcast(new Intent(SipManager.ACTION_SIP_REQUEST_RESTART));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void disconnect(boolean quit) {
        Log.d(THIS_FILE, "True disconnection...");
        Intent intent = new Intent(SipManager.ACTION_OUTGOING_UNREGISTER);
        intent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, new ComponentName(this, SipHome.class));
        sendBroadcast(intent);
        if(quit) {
            finish();
        }
    }
    
    
    
    
    // Warning view
    
    private List<String> warningList = new ArrayList<String>();
    private void applyWarning(String warnCode, boolean active) {
        synchronized (warningList) {
            if(active) {
                warningList.add(warnCode);
            }else {
                warningList.remove(warnCode);
            }
        }
        runOnUiThread(refreshWarningTabRunnable);
    }
    
    Runnable refreshWarningTabRunnable = new Runnable() {
        @Override
        public void run() {
            refreshWarningTabDisplay();
        }
    };
    
    private void refreshWarningTabDisplay() {
        List<String> warnList = new ArrayList<String>();
        synchronized (warningList) {
            warnList.addAll(warningList);
        }
        if(mWarningFragment != null) {
            mWarningFragment.setWarningList(warnList);
            mWarningFragment.setOnWarningChangedListener(this);
        }
        if(warnList.size() > 0) {
            // Show warning tab if any to display
            if(mTabsAdapter.getPositionForId(TAB_ID_WARNING) == null) {
                // And not yet displayed
                Log.w(THIS_FILE, "Reason to warn " + warnList);
                
                mTabsAdapter.addTab(warningTab, WarningFragment.class, TAB_ID_WARNING);
                warningTabfadeAnim.start();
            }
        }else {
            // Hide warning tab since nothing to warn about
            ActionBar ab = getSupportActionBar();
            int selPos = -1;
            if(ab != null) {
                selPos = ab.getSelectedTab().getPosition();
            }
            Integer pos = mTabsAdapter.getPositionForId(TAB_ID_WARNING);
            if(pos != null) {
                mTabsAdapter.removeTabAt(pos);
                if(selPos == pos && ab != null) {
                    ab.selectTab(ab.getTabAt(0));
                }
            }
            if(warningTabfadeAnim.isStarted()) {
                warningTabfadeAnim.end();
            }
        }
    }

    @Override
    public void onWarningRemoved(String warnKey) {
        applyWarning(warnKey, false);
    }
}
