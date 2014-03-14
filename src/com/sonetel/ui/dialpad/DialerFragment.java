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



import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

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
import com.sonetel.R;
import com.sonetel.api.ISipService;
import com.sonetel.api.SipCallSession;
import com.sonetel.api.SipConfigManager;
import com.sonetel.api.SipManager;
import com.sonetel.api.SipProfile;
import com.sonetel.db.DBAdapter;

import com.sonetel.plugins.sonetelcallback.CallBack;
import com.sonetel.plugins.sonetelcallthru.AccessNumbers;
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

public class DialerFragment extends SherlockFragment implements OnClickListener, OnLongClickListener,
        OnDialKeyListener, TextWatcher, OnDialActionListener, ViewPagerVisibilityListener, OnKeyListener {

    private static final String THIS_FILE = "DialerFragment";
    private EditSipUri sipTextUri;
    private static ProgressDialog pDialog = null;
    protected static final int PICKUP_PHONE = 0;
    //private Drawable digitsBackground, digitsEmptyBackground;
    private DigitsEditText digits;
    private ConnectivityManager connectivityManager;
    private ImageView countryView;
    private TextView countryName;
    private String actualCall; 
   // SipProfile SipAccount = new SipProfile(); 
    //private View digitDialer;

    private AccountChooserButton accountChooserButton;
    private boolean isDigit;
    /* , isTablet */
    
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

    public ISipService service;
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
			//if(!isInternetAvail(context))
			//	Toast.makeText(context, "No Internet connection! VoIP calling and Automatic Call Thru disabled.", Toast.LENGTH_LONG).show();
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
  
  /*  void changeAccessNo()
    {
    	String AccessNo = null;
    	Location_Finder LocFinder = new Location_Finder(this);
    	PreferencesWrapper prefWrapper = new PreferencesWrapper(this) ;
    	DBAdapter dbAdapt = new DBAdapter(Context);
    	if(!dbAdapt.isOpen())
    		dbAdapt.open();
    	
    	AccessNo = dbAdapt.getCallthruNbr(LocFinder.getCurrentLocation());
    	
    	if(AccessNo == null)
    		prefWrapper.setCallthruNumber("");
    	else
    		prefWrapper.setCallthruNumber(AccessNo);
    }
    protected boolean isLocationChanged()
    {
    	String CurrentLocation = null;
    	String StoredLocation = null;
    	PreferencesWrapper prefWrapper = new PreferencesWrapper(this.getBaseContext()) ;
    	StoredLocation = prefWrapper.getCurrentLocation();
    	TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    	
    	CurrentLocation = tm.getNetworkCountryIso();
    	
    	if(StoredLocation != null && CurrentLocation != null)
    	{
    		if(StoredLocation.equalsIgnoreCase(CurrentLocation))
    			return false;
    	}
    	return true;
    }*/
        @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);
        digitFormater = new PhoneNumberFormattingTextWatcher();
        // Auto complete list in case of text
        autoCompleteAdapter = new ContactsSearchAdapter(getActivity());
        autoCompleteListItemListener = new OnAutoCompleteListItemClicked(autoCompleteAdapter);

       // if(isDigit == null) {
            isDigit = true;//!prefsWrapper.getPreferenceBooleanValue(SipConfigManager.START_WITH_TEXT_DIALER);
       // }
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
        countryView = (ImageView)v.findViewById(R.id.imageView1);
        countryName = (TextView)v.findViewById(R.id.textView1);
        
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
		TextUtils.isEmpty(digits.getText().toString());
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
           // else
            //	accId = -2;
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

    private void checkDialingCountry(String dialedNum1) {
		// TODO Auto-generated method stub
    	
    	String dialedNum = PhoneNumberUtils.stripSeparators(dialedNum1);

    	if(dialedNum.startsWith("+91") || dialedNum.startsWith("0091")|| dialedNum.startsWith("01191"))
    	{
    		countryView.setImageResource(R.drawable.flag_ind);
    		countryName.setText("India");
    	}//1
    	else if(dialedNum.startsWith("+46") || dialedNum.startsWith("0046")|| dialedNum.startsWith("01146"))
    	{
    		countryView.setImageResource(R.drawable.flag_swe);
    		countryName.setText("Sweden");
    	}//2
    	else if(dialedNum.startsWith("+61") || dialedNum.startsWith("0061")|| dialedNum.startsWith("01161"))
    	{
    		countryView.setImageResource(R.drawable.flag_aus);
    		countryName.setText("Australia");
    	}//3
    	else if(dialedNum.startsWith("+92") || dialedNum.startsWith("0092")|| dialedNum.startsWith("01192"))
    	{
    		countryView.setImageResource(R.drawable.flag_pak);
    		countryName.setText("Pakisthan");
    	}//4
    	else if(dialedNum.startsWith("+93") || dialedNum.startsWith("0093")|| dialedNum.startsWith("01193"))
    	{
    		countryView.setImageResource(R.drawable.flag_afg);
    		countryName.setText("Afghanistan");
    	}//5
    	else if(dialedNum.startsWith("+355") || dialedNum.startsWith("00355")|| dialedNum.startsWith("011355"))
    	{
    		countryView.setImageResource(R.drawable.flag_alb);
    		countryName.setText("Albania");
    	}//6
    	else if(dialedNum.startsWith("+1264") || dialedNum.startsWith("001264")|| dialedNum.startsWith("0111264"))
    	{
    		countryView.setImageResource(R.drawable.flag_aia);
    		countryName.setText("Anguilla");
    	}//7
    	else if(dialedNum.startsWith("+1242") || dialedNum.startsWith("001242")|| dialedNum.startsWith("0111242"))
    	{
    		countryView.setImageResource(R.drawable.flag_bah);
    		countryName.setText("Bahamas");
    	}//8
    	else if(dialedNum.startsWith("+1268") || dialedNum.startsWith("001268")|| dialedNum.startsWith("0111268"))
    	{
    		countryView.setImageResource(R.drawable.flag_agt);
    		countryName.setText("Antigua and Barbuda");
    	}//18
    	else if(dialedNum.startsWith("+1242") || dialedNum.startsWith("001242")|| dialedNum.startsWith("0111242"))
    	{
    		countryView.setImageResource(R.drawable.flag_bah);
    		countryName.setText("Bahamas");
    	}//24
    	else if(dialedNum.startsWith("+971") || dialedNum.startsWith("00971")|| dialedNum.startsWith("011971"))
    	{
    		countryView.setImageResource(R.drawable.flag_uae);
    		countryName.setText("UAE");
    	}//10
    	else if(dialedNum.startsWith("+966") || dialedNum.startsWith("00966")|| dialedNum.startsWith("011966"))
    	{
    		countryView.setImageResource(R.drawable.flag_ksa);
    		countryName.setText("Saudi Arabia");
    	}//11
    	else if(dialedNum.startsWith("+65") || dialedNum.startsWith("0065")|| dialedNum.startsWith("01165"))
    	{
    		countryView.setImageResource(R.drawable.flag_sin);
    		countryName.setText("Singapore");
    	}//12
    	else if(dialedNum.startsWith("+213") || dialedNum.startsWith("00213")|| dialedNum.startsWith("011213"))
    	{
    		countryView.setImageResource(R.drawable.flag_alg);
    		countryName.setText("Algeria");
    	}//13
    	else if(dialedNum.startsWith("+1684") || dialedNum.startsWith("001684")|| dialedNum.startsWith("0111684"))
    	{
    		countryView.setImageResource(R.drawable.flag_asa);
    		countryName.setText("American Samoa");
    	}//14
    	else if(dialedNum.startsWith("+376") || dialedNum.startsWith("00376")|| dialedNum.startsWith("011376"))
    	{
    		countryView.setImageResource(R.drawable.flag_and);
    		countryName.setText("Andorra");
    	}//15
    	else if(dialedNum.startsWith("+244") || dialedNum.startsWith("00244")|| dialedNum.startsWith("011244"))
    	{
    		countryView.setImageResource(R.drawable.flag_ang);
    		countryName.setText("Angola");
    	}//16
    	else if(dialedNum.startsWith("+672") || dialedNum.startsWith("00672")|| dialedNum.startsWith("011672"))
    	{
    		countryView.setImageResource(R.drawable.flag_ata);
    		countryName.setText("Antarctica");
    	}//17
    	else if(dialedNum.startsWith("+54") || dialedNum.startsWith("0054")|| dialedNum.startsWith("01154"))
    	{
    		countryView.setImageResource(R.drawable.flag_arg);
    		countryName.setText("Argentina");
    	}//19
    	else if(dialedNum.startsWith("+374") || dialedNum.startsWith("00374")|| dialedNum.startsWith("011374"))
    	{
    		countryView.setImageResource(R.drawable.flag_arg);
    		countryName.setText("Armenia");
    	}//20
    	else if(dialedNum.startsWith("+297") || dialedNum.startsWith("00297")|| dialedNum.startsWith("011297"))
    	{
    		countryView.setImageResource(R.drawable.flag_aru);
    		countryName.setText("Aruba");
    	}//21
    	else if(dialedNum.startsWith("+43") || dialedNum.startsWith("0043")|| dialedNum.startsWith("01143"))
    	{
    		countryView.setImageResource(R.drawable.flag_aut);
    		countryName.setText("Austria");
    	}//22
    	else if(dialedNum.startsWith("+994") || dialedNum.startsWith("00994")|| dialedNum.startsWith("011994"))
    	{
    		countryView.setImageResource(R.drawable.flag_aze);
    		countryName.setText("Azerbaijan");
    	}//23
    	else if(dialedNum.startsWith("+973") || dialedNum.startsWith("00973")|| dialedNum.startsWith("011973"))
    	{
    		countryView.setImageResource(R.drawable.flag_brn);
    		countryName.setText("Bahrain");
    	}//25
    	else if(dialedNum.startsWith("+880") || dialedNum.startsWith("00880")|| dialedNum.startsWith("011880"))
    	{
    		countryView.setImageResource(R.drawable.flag_ban);
    		countryName.setText("Bangladesh");
    	}//26
    	else if(dialedNum.startsWith("+1246") || dialedNum.startsWith("001246")|| dialedNum.startsWith("0111246"))
    	{
    		countryView.setImageResource(R.drawable.flag_bar);
    		countryName.setText("Barbados");
    	}//27
    	else if(dialedNum.startsWith("+375") || dialedNum.startsWith("00375")|| dialedNum.startsWith("011375"))
    	{
    		countryView.setImageResource(R.drawable.flag_blr);
    		countryName.setText("Belarus");
    	}//28
    	else if(dialedNum.startsWith("+375") || dialedNum.startsWith("00375")|| dialedNum.startsWith("011375"))
    	{
    		countryView.setImageResource(R.drawable.flag_blr);
    		countryName.setText("Belarus");
    	}//28
    	else if(dialedNum.startsWith("+32") || dialedNum.startsWith("0032")|| dialedNum.startsWith("01132"))
    	{
    		countryView.setImageResource(R.drawable.flag_bel);
    		countryName.setText("Belgium");
    	}//29
    	else if(dialedNum.startsWith("+501") || dialedNum.startsWith("00501")|| dialedNum.startsWith("011501"))
    	{
    		countryView.setImageResource(R.drawable.flag_biz);
    		countryName.setText("Belize");
    	}//30
    	else if(dialedNum.startsWith("+229") || dialedNum.startsWith("00229")|| dialedNum.startsWith("011229"))
    	{
    		countryView.setImageResource(R.drawable.flag_ben);
    		countryName.setText("Benin");
    	}//31
    	else if(dialedNum.startsWith("+1441") || dialedNum.startsWith("001441")|| dialedNum.startsWith("0111441"))
    	{
    		countryView.setImageResource(R.drawable.flag_ber);
    		countryName.setText("Bermuda");
    	}//32
    	else if(dialedNum.startsWith("+975") || dialedNum.startsWith("00975")|| dialedNum.startsWith("011975"))
    	{
    		countryView.setImageResource(R.drawable.flag_bhu);
    		countryName.setText("Bhutan");
    	}//33
    	else if(dialedNum.startsWith("+591") || dialedNum.startsWith("00591")|| dialedNum.startsWith("011591"))
    	{
    		countryView.setImageResource(R.drawable.flag_bol);
    		countryName.setText("Bolivia");
    	}//34
    	else if(dialedNum.startsWith("+387") || dialedNum.startsWith("00387")|| dialedNum.startsWith("011387"))
    	{
    		countryView.setImageResource(R.drawable.flag_bih);
    		countryName.setText("Bosnia and Herzegovina");
    	}//35
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//36
    	///////////////////////////////////////*******************************///////////////////////////////////////////////////////////////////////
    	else if(dialedNum.startsWith("+55") || dialedNum.startsWith("0055")|| dialedNum.startsWith("01155"))
    	{
    		countryView.setImageResource(R.drawable.flag_bra);
    		countryName.setText("Brazil");
    	}//37
    	else if(dialedNum.startsWith("+673") || dialedNum.startsWith("00673")|| dialedNum.startsWith("011673"))
    	{
    		countryView.setImageResource(R.drawable.flag_bru);
    		countryName.setText("Brunei");
    	}//38
    	else if(dialedNum.startsWith("+359") || dialedNum.startsWith("00359")|| dialedNum.startsWith("011359"))
    	{
    		countryView.setImageResource(R.drawable.flag_bul);
    		countryName.setText("Bulgaria");
    	}//39
    	else if(dialedNum.startsWith("+226") || dialedNum.startsWith("00226")|| dialedNum.startsWith("011226"))
    	{
    		countryView.setImageResource(R.drawable.flag_bur);
    		countryName.setText("Burkina Faso");
    	}//40
    	else if(dialedNum.startsWith("+95") || dialedNum.startsWith("0095")|| dialedNum.startsWith("01195"))
    	{
    		countryView.setImageResource(R.drawable.flag_mya);
    		countryName.setText("Myanmar");
    	}//41
    	else if(dialedNum.startsWith("+257") || dialedNum.startsWith("00257")|| dialedNum.startsWith("011257"))
    	{
    		countryView.setImageResource(R.drawable.flag_bdi);
    		countryName.setText("Burundi");
    	}//42
    	else if(dialedNum.startsWith("+855") || dialedNum.startsWith("00855")|| dialedNum.startsWith("011855"))
    	{
    		countryView.setImageResource(R.drawable.flag_cam);
    		countryName.setText("Cambodia");
    	}//43
    	else if(dialedNum.startsWith("+237") || dialedNum.startsWith("00237")|| dialedNum.startsWith("011237"))
    	{
    		countryView.setImageResource(R.drawable.flag_cmr);
    		countryName.setText("Cameroon");
    	}//44
    	else if(dialedNum.startsWith("+238") || dialedNum.startsWith("00238")|| dialedNum.startsWith("011238"))
    	{
    		countryView.setImageResource(R.drawable.flag_cpv);
    		countryName.setText("Cape Verde");
    	}//45
    	else if(dialedNum.startsWith("+236") || dialedNum.startsWith("00236")|| dialedNum.startsWith("011236"))
    	{
    		countryView.setImageResource(R.drawable.flag_caf);
    		countryName.setText("CAR");
    	}//46
    	else if(dialedNum.startsWith("+235") || dialedNum.startsWith("00235")|| dialedNum.startsWith("011235"))
    	{
    		countryView.setImageResource(R.drawable.flag_cha);
    		countryName.setText("Chad");
    	}//47
    	else if(dialedNum.startsWith("+56") || dialedNum.startsWith("0056")|| dialedNum.startsWith("01156"))
    	{
    		countryView.setImageResource(R.drawable.flag_chi);
    		countryName.setText("Chile");
    	}//48
    	else if(dialedNum.startsWith("+86") || dialedNum.startsWith("0086")|| dialedNum.startsWith("01186"))
    	{
    		countryView.setImageResource(R.drawable.flag_chn);
    		countryName.setText("China");
    	}//49
    	else if(dialedNum.startsWith("+57") || dialedNum.startsWith("0057")|| dialedNum.startsWith("01157"))
    	{
    		countryView.setImageResource(R.drawable.flag_col);
    		countryName.setText("Colombia");
    	}//50
    	else if(dialedNum.startsWith("+269") || dialedNum.startsWith("00269")|| dialedNum.startsWith("011269"))
    	{
    		countryView.setImageResource(R.drawable.flag_com);
    		countryName.setText("Comoros");
    	}//51
    	else if(dialedNum.startsWith("+682") || dialedNum.startsWith("00682")|| dialedNum.startsWith("011682"))
    	{
    		countryView.setImageResource(R.drawable.flag_cok);
    		countryName.setText("Cook Islands");
    	}//52
    	else if(dialedNum.startsWith("+506") || dialedNum.startsWith("00506")|| dialedNum.startsWith("011506"))
    	{
    		countryView.setImageResource(R.drawable.flag_crc);
    		countryName.setText("Costa Rica");
    	}//53
    	else if(dialedNum.startsWith("+385") || dialedNum.startsWith("00385")|| dialedNum.startsWith("011385"))
    	{
    		countryView.setImageResource(R.drawable.flag_cro);
    		countryName.setText("Croatia");
    	}//54
    	else if(dialedNum.startsWith("+357") || dialedNum.startsWith("00357")|| dialedNum.startsWith("011357"))
    	{
    		countryView.setImageResource(R.drawable.flag_cyp);
    		countryName.setText("Cyprus");
    	}//55
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//56
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//57
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//58
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//59
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//60
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//61
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//62
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//63
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//64
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//65
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//66
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//67
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//68
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//69
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//70
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//71
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//72
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//73
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//74
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//75
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//76
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//77
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//78
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//79
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//80
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//81
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//82
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//83
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//84
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//85
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//86
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//87
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//88
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//89
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//90
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//91
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//92
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//93
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//94
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//95
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//96
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//97
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//98
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//99
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//100
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//101
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//102
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//103
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//104
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//105
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//106
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//107
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//108
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//109
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//110
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//111
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//112
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//113
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//114
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//115
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//116
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//117
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//118
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//119
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//120
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//121
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//122
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//123
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//124
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//125
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//126
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//127
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//128
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//129
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//130
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//131
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//132
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//133
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//134
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//135
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//136
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//137
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//138
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//139
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//140
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//141
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//142
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//143
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//144
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//145
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//146
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//147
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//148
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//149
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//150
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//151
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//152
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//153
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//154
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//155
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//156
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//157
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//158
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//159
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//160
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//36
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//36
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//36
    	else if(dialedNum.startsWith("+267") || dialedNum.startsWith("00267")|| dialedNum.startsWith("011267"))
    	{
    		countryView.setImageResource(R.drawable.flag_bot);
    		countryName.setText("Botswana");
    	}//36
    	///////////////////////////////////////******************************//////////////////////////////////////////////////////////////////////
    	else if((dialedNum.length()>= 5 && dialedNum.startsWith("+1")) || (dialedNum.length()>= 6 && dialedNum.startsWith("001")))
    	{
    		countryView.setImageResource(R.drawable.flag_usa);
    		countryName.setText("USA");
    	}//9
    	else
    	{
    		countryView.setImageResource(0);
    		countryName.setText("");
    	}
    	
    	if(dialedNum.length()>= 6)
    	{
    		
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
        checkDialingCountry(input.toString());
       
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
        //if(digits.length() == 4)
          //  checkDialingCountry(digits.getText().toString());
    }

    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

      /*  int action = MenuItem.SHOW_AS_ACTION_NEVER;;//getResources().getBoolean(R.bool.menu_in_bar) ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER;
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
        } 
        else if (accountToUse != SipProfile.INVALID_ID) {
            // It's an external account, find correct external account
            
           // String actualCall = toCall;
           if(accountToUse == -2)
           {
        		if(!isRoaming())
        		{
        	   
        		  if(actualCall != null)
        		  {
        			  if(!actualCall.equalsIgnoreCase(""))
        			  {
        			  	pDialog = new ProgressDialog(getActivity());
        			  	pDialog.setTitle("Sonetel call thru");
        			  	pDialog.setMessage("Authenticating...");
        			  	pDialog.setIndeterminate(true);
        			  	pDialog.show(); 
        			  	
        			  	CallHandlerPlugin ch = new CallHandlerPlugin(getActivity());
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
       					return;
       				}
        		  

        		  }
        		
        		 else
  				{
  					Location_Finder LocFinder = new Location_Finder(getActivity());
  					if(LocFinder.getContryName().startsWith("your"))
  						Toast.makeText(getActivity(), "Country of your current location unknown. Call thru not available.", Toast.LENGTH_LONG).show();
  					else
  						Toast.makeText(getActivity(), "No Call thru access number available in "+LocFinder.getContryName(), Toast.LENGTH_LONG).show();						
  					return;
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
        } /**else if (accountToUse == CallHandlerPlugin.getAccountIdForCallHandler(getActivity(),
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
