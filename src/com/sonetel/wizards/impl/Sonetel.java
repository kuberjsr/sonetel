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

package com.sonetel.wizards.impl;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.sonetel.R;
import com.sonetel.api.SipConfigManager;
import com.sonetel.api.SipManager;
import com.sonetel.api.SipProfile;
import com.sonetel.plugins.sonetelcallthru.AccessNumbers;
import com.sonetel.plugins.sonetelcallthru.Location_Finder;
import com.sonetel.utils.PreferencesWrapper;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class Sonetel extends SimpleImplementation    {
	
	private PreferencesWrapper prefWrapper;
	private static final String THIS_FILE = "Sonetel.java";
	private Thread HTTPThread;
	@Override
	protected String getDefaultName() {
		return "Sonetel";
	}

	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		if(account.getMobileNumber()==null || account.getMobileNumber().equalsIgnoreCase("") || account.getMobileNumber().length()<=4)
		{
			prefWrapper = new PreferencesWrapper(parent.getBaseContext());
		
			String telContryCode = "+" + prefWrapper.getTelCountryCode();
			mobileNumber.setText(telContryCode);
		}
		accountUsername.setTitle(R.string.w_sonetel_email);
		accountUsername.setDialogTitle(R.string.w_sonetel_email);
		accountUsername.setDialogMessage(R.string.w_sonetel_email_desc);
		if( ! TextUtils.isEmpty(account.username) && !TextUtils.isEmpty(account.getSipDomain()) ){
			accountUsername.setText(account.username+"@"+account.getSipDomain());
		}
		mobileNumber.getEditText().setSelection(mobileNumber.getEditText().length());
	}
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return parent.getString(R.string.w_sonetel_email_desc);
		}
		return super.getDefaultFieldSummary(fieldName);
	}
	
	
	public SipProfile buildAccount(SipProfile account) {
		final SipProfile account1 = super.buildAccount(account);
		String[] emailParts = getText(accountUsername).trim().split("@");
		if(emailParts.length == 2) {
			account1.username = emailParts[0];
			account1.acc_id = "<sip:"+ getText(accountUsername).trim() +">";
			
			//account.reg_uri = "sip:"+ emailParts[1];
			// Already done by super, just to be sure and let that modifiable for future if needed re-add there
			// Actually sounds that it also work and that's also probably cleaner :
			account1.reg_uri = "sip:"+getDomain();
			account1.proxies = new String[] { "sip:"+getDomain() } ;
		}
		account1.mobile_nbr = account.getMobileNumber();
		
		//final SipProfile tmpAccount = account;
		
		 HTTPThread = new Thread() {
	            public void run() {
	            	
	                SendMsgToNetWork(account1);
	           };
	        };
	        HTTPThread.start();
	        
		return account1;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_ICE,true);
		prefs.setPreferenceBooleanValue(SipConfigManager.USE_VIDEO,false);

		//Only G711a/u  on WB & NB
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"245");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"244");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
		
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"245");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"244");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
		
		
		// For Narrowband
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB, "60");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "50");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "230");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "239");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("CODEC2/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G7221/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G7221/32000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("ISAC/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("ISAC/32000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("AMR/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("opus/48000/1", SipConfigManager.CODEC_NB, "240");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_NB, "0");

        // For Wideband
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB, "60");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB, "50");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB, "0"); 
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
        //prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,supportFloating ? "235" : "0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "220");
        prefs.setCodecPriority("CODEC2/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G7221/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G7221/32000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("ISAC/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("ISAC/32000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("AMR/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("opus/48000/1", SipConfigManager.CODEC_WB, "240");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_WB, "0");

        // Bands repartition
        prefs.setPreferenceStringValue("band_for_wifi", SipConfigManager.CODEC_WB);
        prefs.setPreferenceStringValue("band_for_other", SipConfigManager.CODEC_WB);
        prefs.setPreferenceStringValue("band_for_3g", SipConfigManager.CODEC_NB);
        prefs.setPreferenceStringValue("band_for_gprs", SipConfigManager.CODEC_NB);
        prefs.setPreferenceStringValue("band_for_edge", SipConfigManager.CODEC_NB);
        
        prefs.addStunServer("stun.sonetel.com");
       //prefs.addStunServer("192.168.2.123");
		
	}
	
	public boolean canSave() {
		boolean canSave = super.canSave();
		
		String[] emailParts = getText(accountUsername).split("@");		
		canSave &= checkField(accountUsername, (emailParts.length != 2));
		
		return canSave;
		
	}

	@Override
	public boolean needRestart() {
		return true;
	}


	@Override
	protected String getDomain() {
		return "sonetel.net";
		//return "192.168.2.123:5067";
		//return "sonetelindia.dyndns.org:5055";
		//return "eu.test.sonetel.net";
		//return "192.168.2.152:5160";
	}
	public void SendMsgToNetWork(SipProfile account)
	{	
	try {
		
		account = AccessNumbers.sendPostReq(account,"1",null,parent.getBaseContext(),null);
		
		int i=0;
		if(account.pin == null)
		{
			//serviceHandler.sendMessage(serviceHandler.obtainMessage(TOAST_MESSAGE, 0, 0,"Unable to configure callthru. Please check your registration or contact Sonetel support"));
			//Toast.makeText(parent,"Unable to configure automated callthru. You may be need to enter your credentials", Toast.LENGTH_LONG).show();
			Log.e(THIS_FILE, "AccessNumbers.sendPostReq() failed. Callthru may not work");
			//return false;
		}
		else
			 i = parent.getBaseContext().getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, account.id), account.getDbContentValues(), null, null);
		
		//HTTPThread.stop();
			
	} catch (ClientProtocolException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	//return false;
	}

	

}
