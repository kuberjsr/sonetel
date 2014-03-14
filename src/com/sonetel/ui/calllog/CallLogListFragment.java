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

package com.sonetel.ui.calllog;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.PendingIntent.CanceledException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.sonetel.R;
import com.sonetel.api.ISipService;
import com.sonetel.api.SipManager;
import com.sonetel.api.SipProfile;
import com.sonetel.api.SipUri;
import com.sonetel.db.DBAdapter;
import com.sonetel.plugins.sonetelcallthru.Location_Finder;
import com.sonetel.ui.SipHome.ViewPagerVisibilityListener;
import com.sonetel.ui.calllog.CallLogAdapter.OnCallLogAction;
import com.sonetel.ui.dialpad.DialerFragment;
import com.sonetel.utils.CallHandlerPlugin;
import com.sonetel.utils.Log;
import com.sonetel.utils.PreferencesWrapper;
import com.sonetel.utils.CallHandlerPlugin.OnLoadListener;
import com.sonetel.widgets.AccountChooserButton;
import com.sonetel.widgets.CSSListFragment;

import java.util.ArrayList;

/**
 * Displays a list of call log entries.
 */
public class CallLogListFragment extends CSSListFragment implements ViewPagerVisibilityListener,
        CallLogAdapter.CallFetcher, OnCallLogAction {

    private static final String THIS_FILE = "CallLogFragment";

    private boolean mShowOptionsMenu;
    private CallLogAdapter mAdapter;
    private ProgressDialog pDialog = null;
    private boolean mDualPane;

    private ActionMode mMode;

    private String actualCall; 
    private PreferencesWrapper prefsWrapper;
    private ISipService service = null;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }
	private ServiceConnection connection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);

		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
    };
    private void attachAdapter() {
        if(getListAdapter() == null) {
            if(mAdapter == null) {
                Log.d(THIS_FILE, "Attach call log adapter now");
                // Adapter
                mAdapter = new CallLogAdapter(getActivity(), this);
                mAdapter.setOnCallLogActionListener(this);
            }
            setListAdapter(mAdapter);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
    	 
    	 getActivity().bindService(new Intent(SipManager.INTENT_SIP_SERVICE), connection,
                 Context.BIND_AUTO_CREATE);
    	   	return inflater.inflate(R.layout.call_log_fragment, container, false);
        
        
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View management
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);


        // Modify list view
        ListView lv = getListView();
        lv.setVerticalFadingEdgeEnabled(true);
        // lv.setCacheColorHint(android.R.color.transparent);
        if (mDualPane) {
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lv.setItemsCanFocus(false);
        } else {
            lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
            lv.setItemsCanFocus(true);
        }
        
        // Map long press
        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> ad, View v, int pos, long id) {
                turnOnActionMode();
                getListView().setItemChecked(pos, true);
                mMode.invalidate();
                return true;
            }
        });
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        if(pDialog!=null)
        	pDialog.dismiss();
        fetchCalls();
   	 getActivity().bindService(new Intent(SipManager.INTENT_SIP_SERVICE), connection,
             Context.BIND_AUTO_CREATE);
    }
    

    @Override
    public void fetchCalls() {
        attachAdapter();
        if(isResumed()) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    boolean alreadyLoaded = false;
    
    @Override
    public void onVisibilityChanged(boolean visible) {
        if (mShowOptionsMenu != visible) {
            mShowOptionsMenu = visible;
            // Invalidate the options menu since we are changing the list of
            // options shown in it.
            SherlockFragmentActivity activity = getSherlockActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
        

        if(visible) {
            attachAdapter();
            // Start loading
            if(!alreadyLoaded) {
                getLoaderManager().initLoader(0, null, this);
                alreadyLoaded = true;
            }
        }
        
        
        if (visible && isResumed()) {
            //getLoaderManager().restartLoader(0, null, this);
            ListView lv = getListView();
            if (lv != null && mAdapter != null) {
                final int checkedPos = lv.getCheckedItemPosition();
                if (checkedPos >= 0) {
                    // TODO post instead
                    Thread t = new Thread() {
                        public void run() {
                            final long[] selectedIds = mAdapter.getCallIdsAtPosition(checkedPos);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    viewDetails(checkedPos, selectedIds);  
                                }
                            });
                        };
                    };
                    t.start();
                }
            }
        }
        
        
        if(!visible && mMode != null) {
            mMode.finish();
        }
    }

    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        int actionRoom = getResources().getBoolean(R.bool.menu_in_bar) ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER;
        MenuItem delMenu = menu.add(R.string.callLog_delete_all);
        delMenu.setIcon(R.drawable.ic_ab_trash_dark).setShowAsAction(actionRoom);
        delMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                deleteAllCalls();
                return true;
            }
        });
    }

    private void deleteAllCalls() {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(R.string.callLog_delDialog_title);
        alertDialog.setMessage(getString(R.string.callLog_delDialog_message));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.callLog_delDialog_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().getContentResolver().delete(SipManager.CALLLOG_URI, null,
                                null);
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.callLog_delDialog_no),
                (DialogInterface.OnClickListener) null);
        try {
            alertDialog.show();
        } catch (Exception e) {
            Log.e(THIS_FILE, "error while trying to show deletion yes/no dialog");
        }
    }

    // Loader
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(getActivity(), SipManager.CALLLOG_URI, new String[] {
                CallLog.Calls._ID, CallLog.Calls.CACHED_NAME, CallLog.Calls.CACHED_NUMBER_LABEL,
                CallLog.Calls.CACHED_NUMBER_TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE,
                CallLog.Calls.NEW, CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
                SipManager.CALLLOG_PROFILE_ID_FIELD
        },
                null, null,
                Calls.DEFAULT_SORT_ORDER);
    }


    @Override
    public void viewDetails(int position, long[] callIds) {
        ListView lv = getListView();
        if(mMode != null) {
            lv.setItemChecked(position, !lv.isItemChecked(position));
            mMode.invalidate();
            // Don't see details in this case
            return;
        }
        
        if (mDualPane) {
            // If we are not currently showing a fragment for the new
            // position, we need to create and install a new one.
            CallLogDetailsFragment df = new CallLogDetailsFragment();
            Bundle bundle = new Bundle();
            bundle.putLongArray(CallLogDetailsFragment.EXTRA_CALL_LOG_IDS, callIds);
            df.setArguments(bundle);
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, df, null);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();

            getListView().setItemChecked(position, true);
        } else {
            Intent it = new Intent(getActivity(), CallLogDetailsActivity.class);
            it.putExtra(CallLogDetailsFragment.EXTRA_CALL_LOG_IDS, callIds);
            getActivity().startActivity(it);
        }
    }
    protected boolean isRoaming()
    {
    	TelephonyManager tm = (TelephonyManager)getActivity().getSystemService(Context.TELEPHONY_SERVICE);
    	
    	return tm.isNetworkRoaming();
    	
    }
    @Override
    public void placeCall(String nbr, Long accId) {
    	
       if(!TextUtils.isEmpty(nbr)) {
       	// registerReceiver(regStateReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
       	//SipProfile acc = mAccountChooserButton.getSelectedAccount();
       	if (accId == 1) {

               // It is a SIP account, try to call service for that
       		//Long id = acc.id;
       		 try {
                    service.makeCall(nbr, accId.intValue());//makeCallWithOptions(nbr, id.intValue(), null);
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Service can't be called to make the call");
               }
              
           } 
           else if (accId != SipProfile.INVALID_ID) {
               // It's an external account, find correct external account
               
              // String actualCall = toCall;
               
              if(accId == -2 || accId == 3)
              {
            	  if(!isRoaming())
            	  {
            	  CallHandlerPlugin ch = new CallHandlerPlugin(getActivity());
            	  actualCall = getLocalAccessNo(getActivity());
        		  if(actualCall != null)
        		  {
        			  if(!actualCall.equalsIgnoreCase(""))
        			  {
        				  	pDialog = new ProgressDialog(getActivity());
        				  	pDialog.setTitle("Sonetel call thru");
        				  	pDialog.setMessage("Authenticating...");
        				  	pDialog.setIndeterminate(true);
        				  	pDialog.show();  
        				  	
        	            	  accId = (long) -2;
        	              	   ch.loadFrom(accId, actualCall,nbr, new OnLoadListener() {
        	                         @Override
        	                         public void onLoad(CallHandlerPlugin ch) {
        	                             //placePluginCall(ch);
        	                         }
        	                     });
        			  }
             		 else
       				{
       					Location_Finder LocFinder = new Location_Finder(getActivity());
       					if(LocFinder.getContryName().startsWith("your"))
       						Toast.makeText(getActivity(), "Country of your current location unknown. Call thru not available.", Toast.LENGTH_LONG).show();
       					else
       						Toast.makeText(getActivity(), "No Call thru access number available in "+LocFinder.getContryName(), Toast.LENGTH_LONG).show();						
       					//Toast.makeText(context, "No Call thru access number available in ", Toast.LENGTH_LONG).show();
       				}
        		  }
            	  }
            	  else
            		  Toast.makeText(getActivity(), "Call thru is not available when roaming. Please buy a local SIM card if you want to use Call thru in this country.", Toast.LENGTH_LONG).show();
             	  //toCall =  getLocalAccessNo(getActivity());
              }
              else if(accId == -3 || accId == 4)
              {
            	  CallHandlerPlugin ch = new CallHandlerPlugin(getActivity());
           	   pDialog = new ProgressDialog(getActivity());
           	   pDialog.setTitle("Sonetel call back");
           	   pDialog.setMessage("Please wait. You will receive a call in a moment.");
           	   pDialog.setIndeterminate(true);
           	   pDialog.show();
           	   accId = (long) -3;
			  	ch.loadFrom(accId, null,nbr, new OnLoadListener() {
					   @Override
					   public void onLoad(CallHandlerPlugin ch) {
						  // placePluginCall(ch);
					   }
				   });
              }
           }
       }
            
    }
    public void placePluginCall(CallHandlerPlugin ch) {
        try {
            String nextExclude = ch.getNextExcludeTelNumber();
            if (service != null && nextExclude != null) {
                try {
                    service.ignoreNextOutgoingCallFor(nextExclude);
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Impossible to ignore next outgoing call", e);
                }
            }
            if(ch.getIntent()!= null)
            	ch.getIntent().send();
        } catch (CanceledException e) {
            Log.e(THIS_FILE, "Pending intent cancelled", e);
        }
    }

    private String getLocalAccessNo(Context ctxt)
    {
 	   String calThruNbr = new String();
 	   String countryID = new String();
 	   String toCall = new String();
       if (prefsWrapper == null) {
           prefsWrapper = new PreferencesWrapper(getActivity());
       }
 	   
 	   calThruNbr = prefsWrapper.getCallthruNumber();
 	   DBAdapter dbAdapt = new DBAdapter(ctxt);
		   Location_Finder LocFinder = new Location_Finder(ctxt);
			
			if(calThruNbr == null || calThruNbr.equalsIgnoreCase(""))
			{
			
				countryID = LocFinder.getCurrentLocation();
				calThruNbr = dbAdapt.getCallthruNbr(countryID,ctxt);
			}
			if(calThruNbr != null)
				toCall =  calThruNbr ;
			return toCall;

    }
    
    // Action mode
    
    private void turnOnActionMode() {
        Log.d(THIS_FILE, "Long press");
        mMode = getSherlockActivity().startActionMode(new CallLogActionMode());
        ListView lv = getListView();
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
    }
    
    private class CallLogActionMode  implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.d(THIS_FILE, "onCreateActionMode");
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.call_log_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.d(THIS_FILE, "onPrepareActionMode");
            ListView lv = getListView();
            int nbrCheckedItem = 0;

            for (int i = 0; i < lv.getCount(); i++) {
                if (lv.isItemChecked(i)) {
                    nbrCheckedItem++;
                }
            }
            menu.findItem(R.id.delete).setVisible(nbrCheckedItem > 0);
            menu.findItem(R.id.dialpad).setVisible(nbrCheckedItem == 1);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if(itemId == R.id.delete) {
                actionModeDelete();
                return true;
            }else if(itemId == R.id.invert_selection) {
                actionModeInvertSelection();
                return true;
            }else if(itemId == R.id.dialpad) {
                actionModeDialpad();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(THIS_FILE, "onDestroyActionMode");

            ListView lv = getListView();
            // Uncheck all
            int count = lv.getAdapter().getCount();
            for (int i = 0; i < count; i++) {
                lv.setItemChecked(i, false);
            }
            mMode = null;
        }
        
    }
    
    private void actionModeDelete() {
        ListView lv = getListView();
        
        ArrayList<Long> checkedIds = new ArrayList<Long>();
        
        for(int i = 0; i < lv.getCount(); i++) {
            if(lv.isItemChecked(i)) {
                long[] selectedIds = mAdapter.getCallIdsAtPosition(i);
                
                for(long id : selectedIds) {
                    checkedIds.add(id);
                }
                
            }
        }
        if(checkedIds.size() > 0) {
            String strCheckedIds = TextUtils.join(", ", checkedIds);
            Log.d(THIS_FILE, "Checked positions ("+ strCheckedIds +")");
            getActivity().getContentResolver().delete(SipManager.CALLLOG_URI, CallLog.Calls._ID + " IN ("+strCheckedIds+")", null);
            mMode.finish();
        }
    }
    
    private void actionModeInvertSelection() {
        ListView lv = getListView();

        for(int i = 0; i < lv.getCount(); i++) {
            lv.setItemChecked(i, !lv.isItemChecked(i));
        }
        mMode.invalidate();
    }
    
    private void actionModeDialpad() {
        
        ListView lv = getListView();

        for(int i = 0; i < lv.getCount(); i++) {
            if(lv.isItemChecked(i)) {
                mAdapter.getItem(i);
                String number = mAdapter.getCallRemoteAtPostion(i);
                if(!TextUtils.isEmpty(number)) {
                    Intent it = new Intent(Intent.ACTION_DIAL);
                    it.setData(SipUri.forgeSipUri(SipManager.PROTOCOL_SIP, number));
                    startActivity(it);
                }
                break;
            }
        }
        mMode.invalidate();
        
    }
    
    @Override
    public void changeCursor(Cursor c) {
        mAdapter.changeCursor(c);
    }
    
}
