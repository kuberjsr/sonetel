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

package com.sonetel.ui.dialpad;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.PendingIntent.CanceledException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentTransaction;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.sonetel.R;
import com.sonetel.api.ISipService;
import com.sonetel.api.SipCallSession;
import com.sonetel.api.SipConfigManager;
import com.sonetel.api.SipManager;
import com.sonetel.api.SipProfile;
import com.sonetel.db.DBAdapter;
import com.sonetel.plugins.sonetelcallthru.Location_Finder;
import com.sonetel.ui.SipHome.ViewPagerVisibilityListener;
import com.sonetel.utils.CallHandlerPlugin;
import com.sonetel.utils.CallHandlerPlugin.OnLoadListener;
import com.sonetel.utils.DialingFeedback;
import com.sonetel.utils.Log;
import com.sonetel.utils.PreferencesWrapper;
import com.sonetel.utils.Theme;
import com.sonetel.utils.contacts.ContactsSearchAdapter;
import com.sonetel.widgets.AccountChooserButton;
import com.sonetel.widgets.AccountChooserButton.OnAccountChangeListener;
import com.sonetel.widgets.DialerCallBar;
import com.sonetel.widgets.DialerCallBar.OnDialActionListener;
import com.sonetel.widgets.Dialpad;
import com.sonetel.widgets.Dialpad.OnDialKeyListener;
import com.sonetel.widgets.EditSipUri;

public class ContactFragment extends SherlockFragment implements OnClickListener, OnLongClickListener,
        OnDialKeyListener, TextWatcher, OnDialActionListener, ViewPagerVisibilityListener, OnKeyListener {

    private static final String THIS_FILE = "DialerFragment";
    private EditSipUri sipTextUri;

    protected static final int PICKUP_PHONE = 0;
    private static ProgressDialog pDialog = null;
    //private Drawable digitsBackground, digitsEmptyBackground;
    private DigitsEditText digits;
    //private ImageButton switchTextView;
    private ConnectivityManager connectivityManager;
    //private View digitDialer;

    private AccountChooserButton accountChooserButton;
    private boolean isDigit;
    /* , isTablet */
    private String actualCall; 
    private DialingFeedback dialFeedback;

    /*
    private final int[] buttonsToAttach = new int[] {
            R.id.switchTextView
    };
    */
    private final int[] buttonsToLongAttach = new int[] {
            R.id.button0, R.id.button1
    };

    // TimingLogger timings = new TimingLogger("SIP_HOME", "test");

    private ISipService service;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
        	
            service = ISipService.Stub.asInterface(arg1);
            /*
             * timings.addSplit("Service connected"); if(configurationService !=
             * null) { timings.dumpToLog(); }
             */
            accountChooserButton.updateService(service);
			//sipTextUri.updateService(service);
			updateRegistrations();
			Context context = getActivity();
			actualCall = getLocalAccessNo(context);
		//	if(!isInternetAvail(context))
		//		Toast.makeText(context, "No Internet connection! VoIP calling and Automatic Call Thru disabled.", Toast.LENGTH_LONG).show();
			if(isRoaming())
				Toast.makeText(context, "Using Call thru in roaming can be expensive unless you have a local SIM card", Toast.LENGTH_LONG).show();
			if(isLocationChanged(context))
				changeAccessNo(context);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
            if(pDialog !=null)
            	pDialog.dismiss();
        }
    };

    // private GestureDetector gestureDetector;
    private Dialpad dialPad;

    private PreferencesWrapper prefsWrapper;
    private AlertDialog missingVoicemailDialog;

    // Auto completion for text mode
    private ListView autoCompleteList;
    private ContactsSearchAdapter autoCompleteAdapter;

    private DialerCallBar callBar;
    private boolean mDualPane;

    private DialerAutocompleteDetailsFragment autoCompleteFragment;
    private PhoneNumberFormattingTextWatcher digitFormater;
    private OnAutoCompleteListItemClicked autoCompleteListItemListener;

    private DialerLayout dialerLayout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);
        digitFormater = new PhoneNumberFormattingTextWatcher();
        // Auto complete list in case of text
        autoCompleteAdapter = new ContactsSearchAdapter(getActivity());
        autoCompleteListItemListener = new OnAutoCompleteListItemClicked(autoCompleteAdapter);

        // This implies
        isDigit = false;//prefsWrapper.startIsDigit();
        
        setHasOptionsMenu(true);
      //  updateRegistrations();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.dialer_digit, container, false);
        // Store the backgrounds objects that will be in use later
        /*
        Resources r = getResources();
        
        digitsBackground = r.getDrawable(R.drawable.btn_dial_textfield_active);
        digitsEmptyBackground = r.getDrawable(R.drawable.btn_dial_textfield_normal);
        */

        // Store some object that could be useful later
        digits = (DigitsEditText) v.findViewById(R.id.digitsText);
        dialPad = (Dialpad) v.findViewById(R.id.dialPad);
        callBar = (DialerCallBar) v.findViewById(R.id.dialerCallBar);
        autoCompleteList = (ListView) v.findViewById(R.id.autoCompleteList);
        accountChooserButton = (AccountChooserButton) v.findViewById(R.id.accountChooserButton);
        dialerLayout = (DialerLayout) v.findViewById(R.id.top_digit_dialer);
        //switchTextView = (ImageButton) v.findViewById(R.id.switchTextView);

        // isTablet = Compatibility.isTabletScreen(getActivity());

        // Digits field setup
        if(savedInstanceState != null) {
            isDigit = savedInstanceState.getBoolean(TEXT_MODE_KEY, isDigit);
        }
        
        digits.setOnEditorActionListener(keyboardActionListener);
        
        // Layout 
        dialerLayout.setForceNoList(mDualPane);

        // Account chooser button setup
        accountChooserButton.setShowExternals(true);
        accountChooserButton.setOnAccountChangeListener(accountButtonChangeListener);

        // Dialpad
        dialPad.setOnDialKeyListener(this);

        // We only need to add the autocomplete list if we
        autoCompleteList.setAdapter(autoCompleteAdapter);
        autoCompleteList.setOnItemClickListener(autoCompleteListItemListener);
        autoCompleteList.setFastScrollEnabled(true);

        // Bottom bar setup
        callBar.setOnDialActionListener(this);
        callBar.setVideoEnabled(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO));

        //switchTextView.setVisibility(Compatibility.isCompatible(11) ? View.GONE : View.VISIBLE);

        // Init other buttons
        initButtons(v);
        // Ensure that current mode (text/digit) is applied
        setTextDialing(!isDigit, true);

        // Apply third party theme if any
        applyTheme(v);
        v.setOnKeyListener(this);
//<<<<<<< .mine
//        updateRegistrations();
//=======
        applyTextToAutoComplete();
//>>>>>>> .r1897
        return v;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if(pDialog !=null)
        	pDialog.dismiss();
        if(callBar != null) {
            callBar.setVideoEnabled(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO));
        }
        actualCall = getLocalAccessNo(getActivity());
    }
	protected void updateRegistrations() {
		Log.d(THIS_FILE, "Update chooser choice");
		boolean canChangeIfValid = TextUtils.isEmpty(digits.getText().toString());
		accountChooserButton.updateRegistration();
		//sipTextUri.updateRegistration();
	}
    private void applyTheme(View v) {
        Theme t = Theme.getCurrentTheme(getActivity());
        if (t != null) {
            dialPad.applyTheme(t);
            
            View subV;
            // Delete button
            subV = v.findViewById(R.id.deleteButton);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_dial_delete");
                t.applyLayoutMargin(subV, "btn_dial_delete_margin");
                t.applyImageDrawable((ImageView) subV, "ic_dial_action_delete");
            }
            
            // Dial button
            subV = v.findViewById(R.id.dialButton);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_dial_action");
                t.applyLayoutMargin(subV, "btn_dial_action_margin");
                t.applyImageDrawable((ImageView) subV, "ic_dial_action_call");
            }
            
            // Additional button
            subV = v.findViewById(R.id.dialVideoButton);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_add_action");
                t.applyLayoutMargin(subV, "btn_dial_add_margin");
            }
            
            // Action dividers
            subV = v.findViewById(R.id.divider1);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_bar_divider");
                t.applyLayoutSize(subV, "btn_dial_divider");
            }
            subV = v.findViewById(R.id.divider2);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_bar_divider");
                t.applyLayoutSize(subV, "btn_dial_divider");
            }
            
            // Dialpad background
            subV = v.findViewById(R.id.dialPad);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "dialpad_background");
            }
            
            // Callbar background
            subV = v.findViewById(R.id.dialerCallBar);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "dialer_callbar_background");
            }
            
            // Top field background
            subV = v.findViewById(R.id.topField);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "dialer_textfield_background");
            }
            
            subV = v.findViewById(R.id.digitsText);
            if(subV != null) {
                t.applyTextColor((TextView) subV, "textColorPrimary");
            }
            
        }
        
        // Fix dialer background
        if(callBar != null) {
            Theme.fixRepeatableBackground(callBar);
        }
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(SipManager.INTENT_SIP_SERVICE), connection,
                Context.BIND_AUTO_CREATE);
        // timings.addSplit("Bind asked for two");
        if (prefsWrapper == null) {
            prefsWrapper = new PreferencesWrapper(getActivity());
        }
        if (dialFeedback == null) {
            dialFeedback = new DialingFeedback(getActivity(), false);
        }

        dialFeedback.resume();
        
    }

    @Override
    public void onDetach() {
    	if(pDialog!=null)
    		pDialog.dismiss();
        try {
            getActivity().unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
            Log.w(THIS_FILE, "Unable to un bind", e);
        }
        
        dialFeedback.pause();
        super.onDetach();
    }
    
    
    private final static String TEXT_MODE_KEY = "text_mode";
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(TEXT_MODE_KEY, isDigit);
        super.onSaveInstanceState(outState);
    }
    
    private OnEditorActionListener keyboardActionListener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView tv, int action, KeyEvent arg2) {
            if (action == EditorInfo.IME_ACTION_GO) {
                placeCall();
                return true;
            }
            return false;
        }
    };
    
    OnAccountChangeListener accountButtonChangeListener = new OnAccountChangeListener() {
        @Override
        public void onChooseAccount(SipProfile account) {
            long accId = SipProfile.INVALID_ID;
            if (account != null) {
                accId = account.id;
            }
            else
            	accId = -2;
            autoCompleteAdapter.setSelectedAccount(accId);
        }
    };
    
    private void attachButtonListener(View v, int id, boolean longAttach) {
        ImageButton button = (ImageButton) v.findViewById(id);
        if(button == null) {
            Log.w(THIS_FILE, "Not found button " + id);
            return;
        }
        if(longAttach) {
            button.setOnLongClickListener(this);
        }else {
            button.setOnClickListener(this);
        }
    }

    private void initButtons(View v) {
        /*
        for (int buttonId : buttonsToAttach) {
            attachButtonListener(v, buttonId, false);
        }
        */
        for (int buttonId : buttonsToLongAttach) {
            attachButtonListener(v, buttonId, true);
        }

        digits.setOnClickListener(this);
        digits.setKeyListener(DialerKeyListener.getInstance());
        digits.addTextChangedListener(this);
        digits.setCursorVisible(false);
        afterTextChanged(digits.getText());
    }

    
    
    private void keyPressed(int keyCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        digits.onKeyDown(keyCode, event);
    }

    private class OnAutoCompleteListItemClicked implements OnItemClickListener {
        private ContactsSearchAdapter searchAdapter;

        /**
         * Instanciate with a ContactsSearchAdapter adapter to search in when a
         * contact entry is clicked
         * 
         * @param adapter the adapter to use
         */
        public OnAutoCompleteListItemClicked(ContactsSearchAdapter adapter) {
            searchAdapter = adapter;
        }

        @Override
        public void onItemClick(AdapterView<?> list, View v, int position, long id) {
            Object selectedItem = searchAdapter.getItem(position);
            if (selectedItem != null) {
                CharSequence newValue = searchAdapter.getFilter().convertResultToString(
                        selectedItem);
                setTextFieldValue(newValue);
            }
        }

    }

    public void onClick(View view) {
        // ImageButton b = null;
        int viewId = view.getId();
        /*
        if (view_id == R.id.switchTextView) {
            // Set as text dialing if we are currently digit dialing
            setTextDialing(isDigit);
        } else */
        if (viewId == digits.getId()) {
            if (digits.length() != 0) {
                digits.setCursorVisible(true);
            }
        }
    }

    public boolean onLongClick(View view) {
        // ImageButton b = (ImageButton)view;
        int vId = view.getId();
        if (vId == R.id.button0) {
            dialFeedback.hapticFeedback();
            keyPressed(KeyEvent.KEYCODE_PLUS);
            return true;
        }else if(vId == R.id.button1) {
            if(digits.length() == 0) {
                placeVMCall();
                return true;
            }
        }
        return false;
    }

    public void afterTextChanged(Editable input) {
        // Change state of digit dialer
        final boolean notEmpty = digits.length() != 0;
        //digitsWrapper.setBackgroundDrawable(notEmpty ? digitsBackground : digitsEmptyBackground);
        callBar.setEnabled(notEmpty);

        if (!notEmpty && isDigit) {
            digits.setCursorVisible(false);
        }
        applyTextToAutoComplete();
    }
    
    private void applyTextToAutoComplete() {

        // If single pane for smartphone use autocomplete list
        if (hasAutocompleteList()) {
            //if (digits.length() >= 2) {
                autoCompleteAdapter.getFilter().filter(digits.getText().toString());
            //} else {
            //    autoCompleteAdapter.swapCursor(null);
            //}
        }
        // Dual pane : always use autocomplete list
        if (mDualPane && autoCompleteFragment != null) {
            autoCompleteFragment.filter(digits.getText().toString());
        }
    }

    /**
     * Set the mode of the text/digit input.
     * 
     * @param textMode True if text mode. False if digit mode
     */
    public void setTextDialing(boolean textMode) {
        Log.d(THIS_FILE, "Switch to mode " + textMode);
        setTextDialing(textMode, false);
    }
    

    /**
     * Set the mode of the text/digit input.
     * 
     * @param textMode True if text mode. False if digit mode
     */
    public void setTextDialing(boolean textMode, boolean forceRefresh) {
        if(!forceRefresh && isDigit == !textMode) {
            // Nothing to do
            return;
        }
        isDigit = !textMode;
        if(isDigit) {
            // We need to clear the field because the formatter will now 
            // apply and unapply to this field which could lead to wrong values when unapplied
            digits.getText().clear();
            digits.addTextChangedListener(digitFormater);
        }else {
            digits.removeTextChangedListener(digitFormater);
        }
        digits.setCursorVisible(!isDigit);
        digits.setIsDigit(isDigit, true);
        
        // Update views visibility
        dialPad.setVisibility(isDigit ? View.VISIBLE : View.GONE);
        autoCompleteList.setVisibility(hasAutocompleteList() ? View.VISIBLE : View.GONE);
        //switchTextView.setImageResource(isDigit ? R.drawable.ic_menu_switch_txt
        //        : R.drawable.ic_menu_switch_digit);

        // Invalidate to ask to require the text button to a digit button
        getSherlockActivity().invalidateOptionsMenu();
    }
    
    private boolean hasAutocompleteList() {
        if(!isDigit) {
            return true;
        }
        return dialerLayout.canShowList();
    }

    /**
     * Set the value of the text field and put caret at the end
     * 
     * @param value the new text to see in the text field
     */
    public void setTextFieldValue(CharSequence value) {
        digits.setText(value);
        // make sure we keep the caret at the end of the text view
        Editable spannable = digits.getText();
        Selection.setSelection(spannable, spannable.length());
    }

    // @Override
    public void onTrigger(int keyCode, int dialTone) {
        dialFeedback.giveFeedback(dialTone);
        keyPressed(keyCode);
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // Nothing to do here

    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        afterTextChanged(digits.getText());
        accountChooserButton.setChangeable(TextUtils.isEmpty(digits.getText().toString()));
    }

    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

       /* int action = MenuItem.SHOW_AS_ACTION_NEVER;//getResources().getBoolean(R.bool.menu_in_bar) ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER;
        MenuItem delMenu = menu.add(isDigit ? R.string.switch_to_text : R.string.switch_to_digit);
        delMenu.setIcon(
                isDigit ? R.drawable.ic_menu_switch_txt
                        : R.drawable.ic_menu_switch_digit).setShowAsAction( action );
        delMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                setTextDialing(isDigit);
                return true;
            }
        });*/
    }

    @Override
    public void placeCall() {
        placeCallWithOption(null);
    }

    @Override
    public void placeVideoCall() {
        Bundle b = new Bundle();
        b.putBoolean(SipCallSession.OPT_CALL_VIDEO, true);
        placeCallWithOption(b );
    }
    
    private void placeCallWithOption(Bundle b) {
        if (service == null) {
            return;
        }
        String toCall = "";
        Long accountToUse = SipProfile.INVALID_ID;
        // Find account to use
        SipProfile acc = accountChooserButton.getSelectedAccount();
        if (acc != null) {
            accountToUse = acc.id;
        }
        // Find number to dial
        if(isDigit) {
            toCall = PhoneNumberUtils.stripSeparators(digits.getText().toString());
        }else {
            toCall = digits.getText().toString();
        }
        
        if (TextUtils.isEmpty(toCall)) {
            return;
        }

        // Well we have now the fields, clear theses fields
        digits.getText().clear();

        // -- MAKE THE CALL --//
        if (accountToUse >= 0) {
            // It is a SIP account, try to call service for that
            try {
                service.makeCallWithOptions(toCall, accountToUse.intValue(), b);
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Service can't be called to make the call");
            }
        } else if (accountToUse != SipProfile.INVALID_ID) {
            // It's an external account, find correct external account
           // CallHandlerPlugin ch = new CallHandlerPlugin(getActivity());
           // String actualCall = toCall;
            if(accountToUse == -2)
            {
        		if(!isRoaming())
        		{
         	   CallHandlerPlugin ch = new CallHandlerPlugin(getActivity());
         		  if(actualCall != null)
         		  {
         			  if(!actualCall.equalsIgnoreCase(""))
         			  {
         			  	pDialog = new ProgressDialog(getActivity());
         			  	pDialog.setTitle("Sonetel call thru");
         			  	pDialog.setMessage("Authenticating...");
         			  	pDialog.setIndeterminate(true);
         			  	pDialog.show(); 
         			  	
                		   ch.loadFrom(accountToUse, actualCall,toCall, new OnLoadListener() {
                 			   @Override
                 			   public void onLoad(CallHandlerPlugin ch) {
                 				  // placePluginCall(ch);
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
        		else
        			Toast.makeText(getActivity(), "Call thru is not available when roaming. Please buy a local SIM card if you want to use Call thru in this country.", Toast.LENGTH_LONG).show();
            }
            else if(accountToUse == -3)
            {
         	   CallHandlerPlugin ch = new CallHandlerPlugin(getActivity());
         	   
 			  	pDialog = new ProgressDialog(getActivity());
 			  	pDialog.setTitle("Sonetel call back");
 			  	pDialog.setMessage("Please wait. You will receive a call in a moment.");
 			  	pDialog.setIndeterminate(true);
 			  	pDialog.show();
 			  	
 			  	ch.loadFrom(accountToUse, actualCall,toCall, new OnLoadListener() {
 					   @Override
 					   public void onLoad(CallHandlerPlugin ch) {
 						  // placePluginCall(ch);
 					   }
 				   });
             
            }
         /*  else
           {
            ch.loadFrom(accountToUse, toCall, actualCall, new OnLoadListener() {
                @Override
                public void onLoad(CallHandlerPlugin ch) {
                    placePluginCall(ch);
                }
            });
        }*/
        }
    }
    
    private String getLocalAccessNo(Context ctxt)
    {
 	   String calThruNbr = new String();
 	   String countryID = new String();
 	   String toCall = new String();
 	   
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
    public void placeVMCall() {
        Long accountToUse = SipProfile.INVALID_ID;
        SipProfile acc = null;
        acc = accountChooserButton.getSelectedAccount();
        if (acc != null) {
            accountToUse = acc.id;
        }

        if (accountToUse >= 0) {
            SipProfile vmAcc = SipProfile.getProfileFromDbId(getActivity(), acc.id, new String[] {
                    SipProfile.FIELD_VOICE_MAIL_NBR
            });
            if (!TextUtils.isEmpty(vmAcc.vm_nbr)) {
                // Account already have a VM number
                try {
                    service.makeCall(vmAcc.vm_nbr, (int) acc.id);
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Service can't be called to make the call");
                }
            } else {
                // Account has no VM number, propose to create one
                final long editedAccId = acc.id;
                LayoutInflater factory = LayoutInflater.from(getActivity());
                final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);

                missingVoicemailDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(acc.display_name)
                        .setView(textEntryView)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                if (missingVoicemailDialog != null) {
                                    TextView tf = (TextView) missingVoicemailDialog
                                            .findViewById(R.id.vmfield);
                                    if (tf != null) {
                                        String vmNumber = tf.getText().toString();
                                        if (!TextUtils.isEmpty(vmNumber)) {
                                            ContentValues cv = new ContentValues();
                                            cv.put(SipProfile.FIELD_VOICE_MAIL_NBR, vmNumber);

                                            int updated = getActivity().getContentResolver()
                                                    .update(ContentUris.withAppendedId(
                                                            SipProfile.ACCOUNT_ID_URI_BASE,
                                                            editedAccId),
                                                            cv, null, null);
                                            Log.d(THIS_FILE, "Updated accounts " + updated);
                                        }
                                    }
                                    missingVoicemailDialog.hide();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (missingVoicemailDialog != null) {
                                    missingVoicemailDialog.hide();
                                }
                            }
                        })
                        .create();

                // When the dialog is up, completely hide the in-call UI
                // underneath (which is in a partially-constructed state).
                missingVoicemailDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                missingVoicemailDialog.show();
            }
        }/** else if (accountToUse == CallHandlerPlugin.getAccountIdForCallHandler(getActivity(),
                (new ComponentName(getActivity(), com.sonetel.plugins.telephony.CallHandler.class).flattenToString()))) {
            // Case gsm voice mail
            TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(
                    Context.TELEPHONY_SERVICE);
            String vmNumber = tm.getVoiceMailNumber();

            if (!TextUtils.isEmpty(vmNumber)) {
                if(service != null) {
                    try {
                        service.ignoreNextOutgoingCallFor(vmNumber);
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Not possible to ignore next");
                    }
                }
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", vmNumber, null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {

                missingVoicemailDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.gsm)
                        .setMessage(R.string.no_voice_mail_configured)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (missingVoicemailDialog != null) {
                                    missingVoicemailDialog.hide();
                                }
                            }
                        })
                        .create();

                // When the dialog is up, completely hide the in-call UI
                // underneath (which is in a partially-constructed state).
                missingVoicemailDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                missingVoicemailDialog.show();
            }
        }*/
        // TODO : manage others ?... for now, no way to do so cause no vm stored
    }

    private void placePluginCall(CallHandlerPlugin ch) {
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
    protected boolean isRoaming()
    {
    	TelephonyManager tm = (TelephonyManager)getActivity().getSystemService(Context.TELEPHONY_SERVICE);
    	
    	return tm.isNetworkRoaming();
    	
    }
    void changeAccessNo(Context context)
    {
      	String AccessNo = null;
    	Location_Finder LocFinder = new Location_Finder(context);
    	PreferencesWrapper prefWrapper = new PreferencesWrapper(context) ;
    	
    	DBAdapter dbAdapt = new DBAdapter(context);
    	
    	AccessNo = dbAdapt.getCallthruNbr(LocFinder.getCurrentLocation(),context);
    	
    	if(AccessNo == null)
    		prefWrapper.setCallthruNumber("");
    	else
    		prefWrapper.setCallthruNumber(AccessNo);
    }
    protected boolean isLocationChanged(Context context)
    {
    	String CurrentLocation = null;
    	String StoredLocation = null;
    	PreferencesWrapper prefWrapper = new PreferencesWrapper(context) ;
    	StoredLocation = prefWrapper.getCurrentLocation();
    	TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    	
    	CurrentLocation = tm.getNetworkCountryIso();
    	
    	if(StoredLocation != null && CurrentLocation != null)
    	{
    		if(StoredLocation.equalsIgnoreCase(CurrentLocation))
    			return false;
    	}
    	return true;
    }
    protected boolean isInternetAvail(Context context)
    {
    	connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

    	if(ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI)
    	{
    		Log.d(THIS_FILE, "Wifi state is now "+ni.getState().name());
    		if (ni.getState() == NetworkInfo.State.CONNECTED) 
    		{
    				return true;
    		}
    	}
    	if(ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE) 
    	{
    		// Any mobile network connected
    			if (ni.getState() == NetworkInfo.State.CONNECTED) 
    			{
    				int subType = ni.getSubtype();
    				
    				// 3G (or better)
    				if (subType >= TelephonyManager.NETWORK_TYPE_UMTS) 
    				{
    					return true;
    				}
    				
    				// GPRS (or unknown)
    				if (subType == TelephonyManager.NETWORK_TYPE_GPRS || subType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
    				{
    					return true;
    				}
    				
    				// EDGE
    				if (subType == TelephonyManager.NETWORK_TYPE_EDGE) 
    				{
    					return true;
    				}
    			}
    	}
    	if (ni != null && ni.getType() != ConnectivityManager.TYPE_MOBILE && ni.getType() != ConnectivityManager.TYPE_WIFI)
    	{
    		if (ni.getState() == NetworkInfo.State.CONNECTED)
    		{
    				return true;
    		}
    	}
    	return false;
    	
    }
    @Override
    public void deleteChar() {
        keyPressed(KeyEvent.KEYCODE_DEL);
    }

    @Override
    public void deleteAll() {
        digits.getText().clear();
    }

    private final static String TAG_AUTOCOMPLETE_SIDE_FRAG = "autocomplete_dial_side_frag";

    @Override
    public void onVisibilityChanged(boolean visible) {
        if (visible && getResources().getBoolean(R.bool.use_dual_panes)) {
            // That's far to be optimal we should consider uncomment tests for reusing fragment
            // if (autoCompleteFragment == null) {
            autoCompleteFragment = new DialerAutocompleteDetailsFragment();

            if (digits != null) {
                Bundle bundle = new Bundle();
                bundle.putCharSequence(DialerAutocompleteDetailsFragment.EXTRA_FILTER_CONSTRAINT,
                        digits.getText().toString());

                autoCompleteFragment.setArguments(bundle);

            }
            // }
            // if
            // (getFragmentManager().findFragmentByTag(TAG_AUTOCOMPLETE_SIDE_FRAG)
            // != autoCompleteFragment) {
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, autoCompleteFragment, TAG_AUTOCOMPLETE_SIDE_FRAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commitAllowingStateLoss();
        
            // }
        }
    }

    @Override
    public boolean onKey(View arg0, int keyCode, KeyEvent arg2) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        
        return digits.onKeyDown(keyCode, event);
    }

}
