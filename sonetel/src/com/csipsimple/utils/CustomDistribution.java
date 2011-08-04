/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.utils;

import java.util.Locale;

import com.sonetel.R;
//import com.csipsimple.R;
import com.csipsimple.wizards.WizardUtils.WizardInfo;
import com.csipsimple.wizards.impl.Sonetel;

public class CustomDistribution {

	// CSipSimple trunk distribution
	
	public static boolean distributionWantsOtherAccounts() {
		return false;//yuva
	}
	
	public static boolean distributionWantsOtherProviders() {
		return false;//yuva
	}
	
	public static String getSupportEmail() {
		return "yuva@sonetel.com";//developers@csipsimple.com";
	}
	
	public static String getUserAgent() {
		return "Sonetel android client"; //yuva
	}
	
	public static WizardInfo getCustomDistributionWizard() {
		return new WizardInfo("SONETEL_APP", "Sonetel", 
				R.drawable.ic_wizard_sonetel, 1, 
				new Locale[]{  }, false, false, 
				Sonetel.class);
		//return null; //yuva
	}
	
	public static String getRootPackage() {
		return "com.sonetel";
	}
	
	public static boolean showIssueList() {
		return false;//yuva
	}
	
	public static String getFaqLink() {
		return "http://www.sonetel.com/site/pmwiki.php?n=Admin.Android";//http://code.google.com/p/csipsimple/wiki/FAQ#Summary";
	}
	
	public static boolean showFirstSettingScreen() {
		return false;//yuva
	}
	
	public static boolean supportMessaging() {
		return false;//yuva
	}
	
	public static boolean showContactTab() {
		return true;//yuva
	}

	public static boolean forceNoMultipleCalls() {
		return true;
	}
	
}
