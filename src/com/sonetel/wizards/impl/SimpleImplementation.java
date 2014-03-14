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

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.widget.Toast;

import com.sonetel.R;
import com.sonetel.api.SipProfile;
import com.sonetel.api.SipUri;
import com.sonetel.api.SipUri.ParsedSipContactInfos;

import java.util.HashMap;

public abstract class SimpleImplementation extends BaseImplementation {
	//private static final String THIS_FILE = "SimplePrefsWizard";
	protected EditTextPreference accountDisplayName;
	protected EditTextPreference accountUsername;
	protected EditTextPreference accountPassword;
	protected CheckBoxPreference accountUseTcp;
	public EditTextPreference mobileNumber;
	
	protected static String DISPLAY_NAME = "display_name";
	protected static String USER_NAME = "username";
	protected static String PASSWORD = "password";
	protected static String USE_TCP = "use_tcp";
	protected static String MOBILE_NUMBER = "mobilenumber";


	protected void bindFields() {
		accountDisplayName = (EditTextPreference) findPreference(DISPLAY_NAME);
		accountUsername = (EditTextPreference) findPreference(USER_NAME);
		accountPassword = (EditTextPreference) findPreference(PASSWORD);
		accountUseTcp = (CheckBoxPreference) findPreference(USE_TCP);
		mobileNumber = (EditTextPreference) findPreference(MOBILE_NUMBER);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void fillLayout(final SipProfile account) {
		bindFields();
		
		String display_name = account.display_name;
		if(TextUtils.isEmpty(display_name)) {
			display_name = getDefaultName();
		}
		hidePreference(null, DISPLAY_NAME); //yuva
		
		if(accountDisplayName != null)
			accountDisplayName.setText(display_name);

		
		ParsedSipContactInfos parsedInfo = SipUri.parseSipContact(account.acc_id);
		
		accountUsername.setText(parsedInfo.userName);
		accountPassword.setText(account.data);
		mobileNumber.setText(account.mobile_nbr);
		
		if(canTcp()) {
			accountUseTcp.setChecked(account.transport == SipProfile.TRANSPORT_TCP);
		}else {
			hidePreference(null, USE_TCP);
		}
	}

	/**
	 * Set descriptions for fields managed by the simple implementation.
	 * 
	 * {@inheritDoc}
	 */
	public void updateDescriptions() {
		setStringFieldSummary(DISPLAY_NAME);
		setStringFieldSummary(USER_NAME);
		setPasswordFieldSummary(PASSWORD);
		setStringFieldSummary(MOBILE_NUMBER);
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = -5743705263738203615L;

	{
		put(DISPLAY_NAME, R.string.w_common_display_name_desc);
		put(USER_NAME, R.string.w_common_username_desc);
		put(PASSWORD, R.string.w_common_password_desc);
		put(MOBILE_NUMBER,R.string.w_common_mobileno_desc);
	}};
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if(res != null) {
			return parent.getString( res );
		}
		return "";
	}

	public boolean canSave() {
		boolean isValid = true;
		
		//isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));
		isValid &= checkField(accountUsername, isEmpty(accountUsername));
		
		if(isValid &=checkField(mobileNumber, isEmpty(mobileNumber)))
		{
			if(mobileNumber.getText().length()>= 8 && mobileNumber.getText().startsWith("+"))
			{
				isValid &= true;
			}
			else
			{
				isValid &= false;
				Toast.makeText(this.parent,"Invalid mobile number. Please enter +MOBILENUMBER in the international format", Toast.LENGTH_LONG).show();
			}
		}

		return isValid;
	}

	/**
     * Basic implementation of the account building based on simple implementation fields.
     * A specification of this class could extend and add its own post processing here.
     * 
     * {@inheritDoc}
	 */
	public SipProfile buildAccount(SipProfile account) {
		
		if(accountDisplayName != null)
			account.display_name = accountDisplayName.getText().trim();
		else
			account.display_name = "VoIP";
		
		account.acc_id = "<sip:"
				+ SipUri.encodeUser(accountUsername.getText().trim()) + "@"+getDomain()+">";
		
		String regUri = "sip:"+getDomain();
		account.reg_uri = regUri;
		account.proxies = new String[] { regUri } ;

		
		account.realm = "*";
		account.username = getText(accountUsername).trim();
		account.data = getText(accountPassword);
		account.scheme = SipProfile.CRED_SCHEME_DIGEST;
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;

		account.reg_timeout = 1800;
		account.mobile_nbr = getText(mobileNumber).trim();
		
		if(canTcp()) {
			account.transport = accountUseTcp.isChecked() ? SipProfile.TRANSPORT_TCP : SipProfile.TRANSPORT_UDP;
		}else {
			account.transport = SipProfile.TRANSPORT_UDP;
		}
		
		return account;
	}
	
	/**
	 * Get the server domain to use by default for registrar, proxy and user domain. 
	 * @return The server name / ip of the sip domain
	 */
	protected abstract String getDomain();

    /**
     * Get the default display name for this account.
     * 
     * @return The display name to use by default for this account
     */
	protected abstract String getDefaultName();
	
    /**
     * Does the sip provider allows TCP connection. And support it correctly. If
     * so the application will propose a checkbox to use TCP transportation.
     * This method may be overriden by a implementation.
     * @return True if TCP is available.
     */
	protected boolean canTcp() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_simple_preferences;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean needRestart() {
		return false;
	}
	
	public void setUsername(String username) {
		if(!TextUtils.isEmpty(username)) {
			accountUsername.setText(username);
		}
	}
	
	public void setPassword(String password) {
		if(!TextUtils.isEmpty(password)) {
			accountPassword.setText(password);
		}
	}
	
	public void setMobileNumber(String mobilenbr) {
		if(!TextUtils.isEmpty(mobilenbr)) {
			mobileNumber.setText(mobilenbr);
		}
	}
}

