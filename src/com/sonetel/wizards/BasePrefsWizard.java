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

package com.sonetel.wizards;

import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.sonetel.R;
import com.sonetel.api.SipConfigManager;
import com.sonetel.api.SipManager;
import com.sonetel.api.SipProfile;
import com.sonetel.db.DBAdapter;
import com.sonetel.db.DBProvider;
import com.sonetel.models.Filter;
import com.sonetel.plugins.sonetelcallthru.Location_Finder;
import com.sonetel.ui.filters.AccountFilters;
import com.sonetel.ui.prefs.GenericPrefs;
import com.sonetel.ui.prefs.IPreferenceHelper;
import com.sonetel.utils.Log;
import com.sonetel.utils.PreferencesProviderWrapper;
import com.sonetel.utils.PreferencesWrapper;
import com.sonetel.wizards.WizardUtils.WizardInfo;

import java.util.List;

public class BasePrefsWizard extends GenericPrefs implements IPreferenceHelper{
	
	public static final int SAVE_MENU = Menu.FIRST + 1;
	public static final int TRANSFORM_MENU = Menu.FIRST + 2;
	public static final int FILTERS_MENU = Menu.FIRST + 3;
	public static final int DELETE_MENU = Menu.FIRST + 4;

	private static final String THIS_FILE = "Base Prefs wizard";

	protected SipProfile account = null;
	private Button saveButton;
	private String wizardId = "";
	private WizardIface wizard = null;
	private Preference customPref;
	protected BasePrefsWizard parent;
	private static PreferencesWrapper prefsWrapper;
	private static PreferencesProviderWrapper prefsProvidWrapper;
	//protected EditTextPreference callthru_number;
	//protected static String CALLTHRU_NUMBER = "callthru_number";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Get back the concerned account and if any set the current (if not a
		// new account is created)
		Intent intent = getIntent();
		long accountId = intent.getLongExtra(SipProfile.FIELD_ID, SipProfile.INVALID_ID);

		// TODO : ensure this is not null...
		setWizardId(intent.getStringExtra(SipProfile.FIELD_WIZARD));

		account = SipProfile.getProfileFromDbId(this, accountId, DBProvider.ACCOUNT_FULL_PROJECTION);

		super.onCreate(savedInstanceState);

		// Bind buttons to their actions
		Button bt = (Button) findViewById(R.id.cancel_bt);
		bt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				//if(getAccountCount() != 0)
				//{
					setResult(RESULT_CANCELED, getIntent());
					finish();
				//}
				//else
				//{
				//	finish();
				//	System.exit(0);
				//}
				/**	new AlertDialog.Builder(getBaseContext()) 

					.setTitle("No Account") 

					.setMessage("No Account info found to login. Do you want to exit?") 
					
					.setIcon(R.drawable.ic_wizard_sonetel)
					
					.setPositiveButton("Ok", 

					new DialogInterface.OnClickListener() { 

					public void onClick(DialogInterface dialog, 

					int which) { 
						System.exit(0); 
					} 

					}
					)
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() { 

						public void onClick(DialogInterface dialog, 

						int which) { 
							//System.exit(0); 
						} 

						}
						).show(); */
				//}
			}
		});

		saveButton = (Button) findViewById(R.id.save_bt);
		saveButton.setEnabled(false);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveAndFinish();
			}
		});
		
		customPref = (Preference) findPreference("customPref");
		
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			 public boolean onPreferenceClick(Preference preference) {
				 startBrowser();
				 return false;
				 
			 }
			
		});
        wizard.fillLayout(account);
        
        prefsProvidWrapper = new PreferencesProviderWrapper(this);
	}

	private boolean isResumed = false;
	@Override
	protected void onResume() {
		super.onResume();
		wizard.fillLayout(account);

        isResumed = true;
		updateDescriptions();
		updateValidation();
		wizard.onStart();
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    isResumed = false;
	    wizard.onStop();
	}
    public void onBackPressed() {
    	if(getAccountCount() != 0)
		{
			setResult(RESULT_CANCELED, getIntent());
			finish();
		}
    	//System.exit(0);
    }
	private boolean setWizardId(String wId) {
		if (wizardId == null) {
			return setWizardId(WizardUtils.EXPERT_WIZARD_TAG);
		}

		WizardInfo wizardInfo = WizardUtils.getWizardClass(wId);
		if (wizardInfo == null) {
			if (!wizardId.equals(WizardUtils.EXPERT_WIZARD_TAG)) {
				return setWizardId(WizardUtils.EXPERT_WIZARD_TAG);
			}
			return false;
		}

		try {
			wizard = (WizardIface) wizardInfo.classObject.newInstance();
		} catch (IllegalAccessException e) {
			Log.e(THIS_FILE, "Can't access wizard class", e);
			if (!wizardId.equals(WizardUtils.EXPERT_WIZARD_TAG)) {
				return setWizardId(WizardUtils.EXPERT_WIZARD_TAG);
			}
			return false;
		} catch (InstantiationException e) {
			Log.e(THIS_FILE, "Can't access wizard class", e);
			if (!wizardId.equals(WizardUtils.EXPERT_WIZARD_TAG)) {
				return setWizardId(WizardUtils.EXPERT_WIZARD_TAG);
			}
			return false;
		}
		wizardId = wId;
		wizard.setParent(this);
		if(getSupportActionBar() != null) {
		    //getSupportActionBar().setIcon(WizardUtils.getWizardIconRes(wizardId));
			getSupportActionBar().setIcon(R.drawable.ic_launcher_nightly);
		     getSupportActionBar().setTitle("Sign into your Sonetel account");
		}
		return true;
	}
	public void startBrowser() {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://sonetel.com/en/html/tryfree/try-free.shtml"));//https://portal.sonetel.com/portal/UI/Pages/getStarted/GetStarted_1.aspx"));
		startActivity(browserIntent);
		//finish();
	}
	@Override
	protected void beforeBuildPrefs() {
		// Use our custom wizard view
		setContentView(R.layout.wizard_prefs_base);

			//super.beforeBuildPrefs();
			
			//PreferencesWrapper pfw = new PreferencesWrapper(this);
			
			//String callthruNum = pfw.getCallthruNumber();


		//	if(callthruNum != null)
		//	{
		//		if(callthruNum.equalsIgnoreCase(""))
		//			setCallthruNumber();
		//		else
		//			pfw.setCallthruNumber(callthruNum);
		//	}
	
	}
	
	//@Override
	//protected void afterBuildPrefs() {
	//super.afterBuildPrefs();
	//PreferencesWrapper pfw = new PreferencesWrapper(this);
	
	///String callthruNum = pfw.getCallthruNumber();

	
	//	if(callthruNum != null)
	//	{
	//		if(callthruNum.equalsIgnoreCase(""))
		//		setCallthruNumber();
	//		else
		//		pfw.setCallthruNumber(callthruNum);
	//	}
	//}
	public int getAccountCount()
	{
		Cursor c = getBaseContext().getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
	            SipProfile.FIELD_ID
	    }, null, null, null);
	    int accountCount = 0;
	    if (c != null) {
	        accountCount = c.getCount();
	    }
	    c.close();

	    return accountCount;
	}
	
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	    if(isResumed) {
    		updateDescriptions();
    		updateValidation();
	    }
	}

	/**
	 * Update validation state of the current activity.
	 * It will check if wizard can be saved and if so 
	 * will enable button
	 */
	public void updateValidation() {
		saveButton.setEnabled(wizard.canSave());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, SAVE_MENU, Menu.NONE, R.string.save).setIcon(android.R.drawable.ic_menu_save);
		//if (account.id != SipProfile.INVALID_ID) {
		//	menu.add(Menu.NONE, TRANSFORM_MENU, Menu.NONE, R.string.choose_wizard).setIcon(android.R.drawable.ic_menu_edit);
		//	menu.add(Menu.NONE, FILTERS_MENU, Menu.NONE, R.string.filters).setIcon(R.drawable.ic_menu_filter);
		//	menu.add(Menu.NONE, DELETE_MENU, Menu.NONE, R.string.delete_account).setIcon(android.R.drawable.ic_menu_delete);
		//}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(SAVE_MENU).setVisible(wizard.canSave());

		return super.onPrepareOptionsMenu(menu);
	}


    private static final int CHOOSE_WIZARD = 0;
    private static final int MODIFY_FILTERS = CHOOSE_WIZARD + 1;
    
    private static final int FINAL_ACTIVITY_CODE = MODIFY_FILTERS;
    
    private int currentActivityCode = FINAL_ACTIVITY_CODE;
    public int getFreeSubActivityCode() {
        currentActivityCode ++;
        return currentActivityCode;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SAVE_MENU:
			saveAndFinish();
			return true;
		case TRANSFORM_MENU:
			startActivityForResult(new Intent(this, WizardChooser.class), CHOOSE_WIZARD);
			return true;
		case DELETE_MENU:
			if (account.id != SipProfile.INVALID_ID) {
				getContentResolver().delete(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, account.id), null, null);
				setResult(RESULT_OK, getIntent());
				finish();
			}
			return true;
		case FILTERS_MENU:
			if (account.id != SipProfile.INVALID_ID) {
				Intent it = new Intent(this, AccountFilters.class);
				it.putExtra(SipProfile.FIELD_ID, account.id);
				it.putExtra(SipProfile.FIELD_DISPLAY_NAME, account.display_name);
				it.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
				startActivityForResult(it, MODIFY_FILTERS);
				return true;
			}
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CHOOSE_WIZARD && resultCode == RESULT_OK && data != null && data.getExtras() != null) {
			String wizardId = data.getStringExtra(WizardUtils.ID);
			if (wizardId != null) {
				saveAccount(wizardId);
				setResult(RESULT_OK, getIntent());
				finish();
			}
		}
		
		if(requestCode > FINAL_ACTIVITY_CODE) {
		    wizard.onActivityResult(requestCode, resultCode, data);
		}
	}

	/**
	 * Save account and end the activity
	 */
	public void saveAndFinish() {
		saveAccount();
		Intent intent = getIntent();
		setResult(RESULT_OK, intent);
		
		if(!isConnectivityValid())
			Toast.makeText(this, "No network available for VoIP calls. Check your settings.", Toast.LENGTH_LONG).show();
		finish();
	}
	public boolean isConnectivityValid() {
		
	    if(prefsProvidWrapper.isValidConnectionForIncoming()) 
	    {
	    	if(prefsProvidWrapper.isValidConnectionForOutgoing(false))
	    		return true;
	    }
	    return false;
	}
	/*
	 * Save the account with current wizard id
	 */
	private void saveAccount() {
		saveAccount(wizardId);
	}
	
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    getSharedPreferences(WIZARD_PREF_NAME, MODE_PRIVATE).edit().clear().commit();
	}
	
	/**
	 * Save the account with given wizard id
	 * @param wizardId the wizard to use for account entry
	 */
	private void saveAccount(String wizardId) {
		boolean needRestart = false;

		PreferencesWrapper prefs = new PreferencesWrapper(getApplicationContext());
		account = wizard.buildAccount(account);
		account.wizard = wizardId;
		if (account.id == SipProfile.INVALID_ID) {
			// This account does not exists yet
		    prefs.startEditing();
			wizard.setDefaultParams(prefs);
			prefs.endEditing();
			Uri uri = getContentResolver().insert(SipProfile.ACCOUNT_URI, account.getDbContentValues());
			
			// After insert, add filters for this wizard 
			account.id = ContentUris.parseId(uri);
			List<Filter> filters = wizard.getDefaultFilters(account);
			if (filters != null) {
				for (Filter filter : filters) {
					// Ensure the correct id if not done by the wizard
					filter.account = (int) account.id;
					getContentResolver().insert(SipManager.FILTER_URI, filter.getDbContentValues());
				}
			}
			// Check if we have to restart
			needRestart = wizard.needRestart();
			


		} else {
			// TODO : should not be done there but if not we should add an
			// option to re-apply default params
            prefs.startEditing();
			wizard.setDefaultParams(prefs);
            prefs.endEditing();
			getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, account.id), account.getDbContentValues(), null, null);
		}

		// Mainly if global preferences were changed, we have to restart sip stack 
		if (needRestart) {
			Intent intent = new Intent(SipManager.ACTION_SIP_REQUEST_RESTART);
			sendBroadcast(intent);
		}
	}

	@Override
	protected int getXmlPreferences() {
		return wizard.getBasePreferenceResource();
	}
	
	@Override
	protected void updateDescriptions() {
		wizard.updateDescriptions();
		
		setStringFieldSummary(SipConfigManager.STUN_SERVER);
		//String CountryName = new String();	
		//Location_Finder LocFinder = new Location_Finder(getBaseContext());
		//CountryName = LocFinder.getContryName();
		
		//setCustomCallthruNumber(getBaseContext(),this, SipConfigManager.CALLTHRU_NUMBER);
		//setCallthruSummary(SipConfigManager.CALLTHRU_NUMBER,CountryName);
		
	}

	@Override
	protected String getDefaultFieldSummary(String fieldName) {
		return wizard.getDefaultFieldSummary(fieldName);
	}
	
	private static final String WIZARD_PREF_NAME = "Wizard";
	
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
	    return super.getSharedPreferences(WIZARD_PREF_NAME, mode);
	}

}
