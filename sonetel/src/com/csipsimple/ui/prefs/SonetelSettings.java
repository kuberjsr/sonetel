package com.csipsimple.ui.prefs;

import android.telephony.TelephonyManager;

import com.sonetel.R;
//import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.PreferencesWrapper;



public class SonetelSettings extends GenericPrefs {
	

	//private static final String THIS_FILE = "Prefs Network";

	@Override
	protected int getXmlPreferences() {
		return R.xml.prefs_network;
		
	}
	
	@Override
	protected void afterBuildPrefs() {
		super.afterBuildPrefs();
		TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
		
		//if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
			hidePreference("for_incoming", "use_gprs_in");
			hidePreference("for_outgoing", "use_gprs_out");
			hidePreference("for_incoming", "use_edge_in");
			hidePreference("for_outgoing", "use_edge_out");
			//hidePreference("for_outgoing", "use_wifi_out");
			//hidePreference("for_incoming", "use_wifi_in");
		//}
		
		PreferencesWrapper pfw = new PreferencesWrapper(this);
		

		if(!Compatibility.isCompatible(9)) {
			hidePreference("perfs", SipConfigManager.LOCK_WIFI_PERFS);
		}
		
		if(!pfw.isAdvancedUser()) {
			hidePreference(null, "perfs");
			hidePreference(null,"secure_transport");
			hidePreference(null,"nat_traversal");
			
		/**	hidePreference("nat_traversal", SipConfigManager.ENABLE_TURN);
			hidePreference("nat_traversal", SipConfigManager.TURN_SERVER);
			hidePreference("nat_traversal", SipConfigManager.TURN_USERNAME);
			hidePreference("nat_traversal", SipConfigManager.TURN_PASSWORD);
			hidePreference("nat_traversal", SipConfigManager.ENABLE_ICE);
			hidePreference("nat_traversal", SipConfigManager.ENABLE_STUN);
			hidePreference("nat_traversal", SipConfigManager.STUN_SERVER);*/
			
			hidePreference("transport", SipConfigManager.ENABLE_TCP);
			hidePreference("transport", SipConfigManager.ENABLE_UDP);
			hidePreference("transport", SipConfigManager.TCP_TRANSPORT_PORT);
			hidePreference("transport", SipConfigManager.UDP_TRANSPORT_PORT);
			hidePreference("transport", SipConfigManager.RTP_PORT);
			hidePreference("transport", SipConfigManager.USE_IPV6);
			hidePreference("transport", SipConfigManager.OVERRIDE_NAMESERVER);
			hidePreference("transport", SipConfigManager.ENABLE_DNS_SRV);
			hidePreference("transport", SipConfigManager.USE_COMPACT_FORM);
			//hidePreference("transport", SipConfigManager.USE_SRTP);
			
			hidePreference("transport", SipConfigManager.ENABLE_QOS);
			hidePreference("transport", SipConfigManager.DSCP_VAL);
			hidePreference("transport", SipConfigManager.USER_AGENT);
			
		}
	}

	@Override
	protected void updateDescriptions() {
		setStringFieldSummary(SipConfigManager.STUN_SERVER);
	}
	
}
