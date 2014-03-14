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
package com.sonetel.plugins.sonetelcallback;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Toast;

import com.sonetel.R;
import com.sonetel.api.SipManager;
import com.sonetel.api.SipProfile;
import com.sonetel.db.DBAdapter;
import com.sonetel.plugins.sonetelcallthru.AccessNumbers;
import com.sonetel.service.impl.SipCallSessionImpl;
import com.sonetel.utils.CallHandlerPlugin;
import com.sonetel.utils.CallLogHelper;
import com.sonetel.utils.Log;
import com.sonetel.utils.PhoneCapabilityTester;
import com.sonetel.utils.PreferencesWrapper;
import com.sonetel.utils.CallHandlerPlugin.OnLoadListener;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.pjsip.pjsua.pjsua;

public class CallBack extends BroadcastReceiver {

	private static final String THIS_FILE = "CallHandlerTelephony";

	private static Bitmap sPhoneAppBmp = null;
	private static boolean sPhoneAppInfoLoaded = false;
	
	private Thread NetWorkThread;
	
	
	//String calThruNbr = new String();
	//String countryID = new String();
	//String myMobileNo = new String();
	SipProfile SipAccount = new SipProfile(); 

	
	@Override
	public void onReceive(final Context context, Intent intent) {
		if(SipManager.ACTION_GET_PHONE_HANDLERS.equals(intent.getAction())) {
			// Retrieve and cache infos from the phone app 
			if(!sPhoneAppInfoLoaded) {
				Resources r = context.getResources();
				BitmapDrawable drawable = (BitmapDrawable) r.getDrawable(R.drawable.ic_wizard_sonetel);
				sPhoneAppBmp = drawable.getBitmap();

    			sPhoneAppInfoLoaded = true;
			}
			
			Bundle results = getResultExtras(true);
			//if(pendingIntent != null) {
			//	results.putParcelable(CallHandlerPlugin.EXTRA_REMOTE_INTENT_TOKEN, pendingIntent);
			//}
			results.putString(Intent.EXTRA_TITLE,"Call back");// context.getResources().getString(R.string.use_pstn));
			if(sPhoneAppBmp != null) {
				results.putParcelable(Intent.EXTRA_SHORTCUT_ICON, sPhoneAppBmp);
			}
			if(intent.getStringExtra(Intent.EXTRA_TEXT) == null)
				return;
			
				final String dialledNum = intent.getStringExtra(Intent.EXTRA_TEXT);
				final String callthrunum = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				DBAdapter dbAdapt = new DBAdapter(context);
				
				// Build pending intent
				
				SipAccount = dbAdapt.getAccount(1);
				
				if(SipAccount != null)
				{
					
	
					//Intent i = new Intent(Intent.ACTION_CALL);
					//i.setData(Uri.fromParts("tel", callthrunum, null));
					//final PendingIntent pendingIntent  = PendingIntent.getActivity(context, 0, i,  PendingIntent.FLAG_CANCEL_CURRENT);
									
			       NetWorkThread = new Thread() {
			            public void run() {
			               SendMsgToNetWork(dialledNum,callthrunum,context,null);
			           };
			        };
				
			       NetWorkThread.start();

				}
				else if(SipAccount.acc_id != "1")
					Toast.makeText(context, "Unable to find Sonetel account. Please sign in using your Sonetel account", Toast.LENGTH_LONG).show();
	
		        if(!dialledNum.equalsIgnoreCase(""))
				{
		        	SipCallSessionImpl callInfo = new SipCallSessionImpl();
		        
		        	callInfo.setRemoteContact(dialledNum);
		        	callInfo.setIncoming(false);
		        	callInfo.callStart = System.currentTimeMillis();
		        	callInfo.setAccId(4);
		        	//callInfo.setLastStatusCode(pjsua.PJ_SUCCESS);
				
			
		        ContentValues cv = CallLogHelper.logValuesForCall(context, callInfo, callInfo.callStart);
		        context.getContentResolver().insert(SipManager.CALLLOG_URI, cv);

				}
				//}
				//else
				//{
				
				//}
			}
			

			
			// This will exclude next time tel:xxx is raised from csipsimple treatment which is wanted
			//results.putString(Intent.EXTRA_PHONE_NUMBER, callthrunum);
		
		}
	
	
public void SendMsgToNetWork(String dialedNum,String callthrunum,Context context,PendingIntent callthruIntent)
{
	try
	{
		SipAccount = AccessNumbers.sendPostReq(SipAccount,dialedNum,callthrunum,context,callthruIntent);
	} 
	catch (ClientProtocolException e) 
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	catch (IOException e) 
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
}
}
