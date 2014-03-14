package com.sonetel.plugins.sonetelcallthru;

//import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisPool;
//import redis.clients.jedis.JedisPoolConfig;
//import org.apache.commons.pool.impl.GenericObjectPool;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.content.ContextWrapper;


public class Location_Finder extends ContextWrapper{
	
	private static Context base1;
	public Location_Finder(Context base) {
		super(base);
		// TODO Auto-generated constructor stub
	}
	
	public Location_Finder() {
		super(base1);
	}

	public String getCurrentLocation()
	{
		String countryID = null;
		String countryCode = null;
		
		TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		countryCode = tm.getNetworkCountryIso();
		
		if(countryCode.equalsIgnoreCase("au"))
			countryID = "1";
		else if(countryCode.equalsIgnoreCase("at"))
			countryID = "2";
		else if(countryCode.equalsIgnoreCase("be"))
			countryID = "3";
		else if(countryCode.equalsIgnoreCase("br"))
			countryID = "4";
		else if(countryCode.equalsIgnoreCase("bg"))
			countryID = "5";
		else if(countryCode.equalsIgnoreCase("ca"))
			countryID = "6";
		else if(countryCode.equalsIgnoreCase("cl"))
			countryID = "7";
		else if(countryCode.equalsIgnoreCase("cy"))
			countryID = "8";
		else if(countryCode.equalsIgnoreCase("cz"))
			countryID = "9";
		else if(countryCode.equalsIgnoreCase("dk"))
			countryID = "10";
		else if(countryCode.equalsIgnoreCase("sv"))
			countryID = "11";
		else if(countryCode.equalsIgnoreCase("ee"))
			countryID = "12";
		else if(countryCode.equalsIgnoreCase("fi"))
			countryID = "13";
		else if(countryCode.equalsIgnoreCase("fr"))
			countryID = "14";
		else if(countryCode.equalsIgnoreCase("ge"))
			countryID = "15";
		else if(countryCode.equalsIgnoreCase("gr"))
			countryID = "16";
		else if(countryCode.equalsIgnoreCase("si"))//hungery removed from list slovnia added
		countryID = "17";
		//else if(countryCode.equalsIgnoreCase("hu"))//hungery removed from list
			//countryID = "17";
		else if(countryCode.equalsIgnoreCase("ie"))
			countryID = "18";
		else if(countryCode.equalsIgnoreCase("it"))
			countryID = "19";
		else if(countryCode.equalsIgnoreCase("jp"))
			countryID = "20";
		else if(countryCode.equalsIgnoreCase("lv"))
			countryID = "21";
		else if(countryCode.equalsIgnoreCase("lt"))
			countryID = "22";
		else if(countryCode.equalsIgnoreCase("lu"))
			countryID = "23";
		else if(countryCode.equalsIgnoreCase("mt"))
			countryID = "24";
		else if(countryCode.equalsIgnoreCase("mx"))
			countryID = "25";
		else if(countryCode.equalsIgnoreCase("nl"))
			countryID = "26";
		else if(countryCode.equalsIgnoreCase("nz"))
			countryID = "27";
		else if(countryCode.equalsIgnoreCase("pa"))
			countryID = "28";
		else if(countryCode.equalsIgnoreCase("pe"))
			countryID = "29";
		else if(countryCode.equalsIgnoreCase("pl"))
			countryID = "30";
		else if(countryCode.equalsIgnoreCase("pt"))
			countryID = "31";
		else if(countryCode.equalsIgnoreCase("ro"))
			countryID = "32";
		else if(countryCode.equalsIgnoreCase("sg"))
			countryID = "33";
		else if(countryCode.equalsIgnoreCase("sk"))
			countryID = "34";
		else if(countryCode.equalsIgnoreCase("za"))
			countryID = "35";
		else if(countryCode.equalsIgnoreCase("es"))
			countryID = "36";
		else if(countryCode.equalsIgnoreCase("se"))
			countryID = "37";
		else if(countryCode.equalsIgnoreCase("ch"))
			countryID = "38";
		else if(countryCode.equalsIgnoreCase("gb"))
			countryID = "39";
		else if(countryCode.equalsIgnoreCase("us"))
			countryID = "40";
		else if(countryCode .equalsIgnoreCase("vn"))
			countryID = "41";
		else if(countryCode.equalsIgnoreCase("ar"))
			countryID = "42";
		else if(countryCode.equalsIgnoreCase("bh"))
			countryID = "43";
		else if(countryCode.equalsIgnoreCase("do"))
			countryID = "44";
		else if(countryCode.equalsIgnoreCase("hk"))
			countryID = "45";
		else if(countryCode.equalsIgnoreCase("il"))
			countryID = "46";
		else if(countryCode.equalsIgnoreCase("pr"))
			countryID = "47";
		else if(countryCode.equalsIgnoreCase("th"))
			countryID = "48";
		else if(countryCode.equalsIgnoreCase("ua"))
			countryID = "49";
		else if(countryCode.equalsIgnoreCase("ru"))
			countryID = "50";
		else if(countryCode.equalsIgnoreCase("li"))
			countryID = "51";
		else if(countryCode.equalsIgnoreCase("dz"))
			countryID = "52";
		else if(countryCode.equalsIgnoreCase("in"))
			countryID = "53";
		
		
		return countryID;
	}
	
	public String getTelContryCode()
	{
		String countryID = null;
		String countryCode = null;
		
		TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		countryCode = tm.getNetworkCountryIso();
		
		if(countryCode.equalsIgnoreCase("au"))
			countryID = "61";
		else if(countryCode.equalsIgnoreCase("at"))
			countryID = "43";
		else if(countryCode.equalsIgnoreCase("be"))
			countryID = "32";
		else if(countryCode.equalsIgnoreCase("br"))
			countryID = "55";
		else if(countryCode.equalsIgnoreCase("bg"))
			countryID = "359";
		else if(countryCode.equalsIgnoreCase("ca"))
			countryID = "1";
		else if(countryCode.equalsIgnoreCase("cl"))
			countryID = "56";
		else if(countryCode.equalsIgnoreCase("cy"))
			countryID = "357";
		else if(countryCode.equalsIgnoreCase("cz"))
			countryID = "420";
		else if(countryCode.equalsIgnoreCase("dk"))
			countryID = "45";
		else if(countryCode.equalsIgnoreCase("sv"))
			countryID = "503";
		else if(countryCode.equalsIgnoreCase("ee"))
			countryID = "372";
		else if(countryCode.equalsIgnoreCase("fi"))
			countryID = "358";
		else if(countryCode.equalsIgnoreCase("fr"))
			countryID = "33";
		else if(countryCode.equalsIgnoreCase("ge"))
			countryID = "995";
		else if(countryCode.equalsIgnoreCase("gr"))
			countryID = "30";
		else if(countryCode.equalsIgnoreCase("si"))
			countryID = "386";
		else if(countryCode.equalsIgnoreCase("ie"))
			countryID = "353";
		else if(countryCode.equalsIgnoreCase("it"))
			countryID = "39";
		else if(countryCode.equalsIgnoreCase("jp"))
			countryID = "81";
		else if(countryCode.equalsIgnoreCase("lv"))
			countryID = "371";
		else if(countryCode.equalsIgnoreCase("lt"))
			countryID = "370";
		else if(countryCode.equalsIgnoreCase("lu"))
			countryID = "352";
		else if(countryCode.equalsIgnoreCase("mt"))
			countryID = "356";
		else if(countryCode.equalsIgnoreCase("mx"))
			countryID = "52";
		else if(countryCode.equalsIgnoreCase("nl"))
			countryID = "31";
		else if(countryCode.equalsIgnoreCase("nz"))
			countryID = "64";
		else if(countryCode.equalsIgnoreCase("pa"))
			countryID = "507";
		else if(countryCode.equalsIgnoreCase("pe"))
			countryID = "51";
		else if(countryCode.equalsIgnoreCase("pl"))
			countryID = "48";
		else if(countryCode.equalsIgnoreCase("pt"))
			countryID = "351";
		else if(countryCode.equalsIgnoreCase("ro"))
			countryID = "40";
		else if(countryCode.equalsIgnoreCase("sg"))
			countryID = "65";
		else if(countryCode.equalsIgnoreCase("sk"))
			countryID = "421";
		else if(countryCode.equalsIgnoreCase("za"))
			countryID = "27";
		else if(countryCode.equalsIgnoreCase("es"))
			countryID = "34";
		else if(countryCode.equalsIgnoreCase("se"))
			countryID = "46";
		else if(countryCode.equalsIgnoreCase("ch"))
			countryID = "41";
		else if(countryCode.equalsIgnoreCase("gb"))
			countryID = "44";
		else if(countryCode.equalsIgnoreCase("us"))
			countryID = "1";
		else if(countryCode .equalsIgnoreCase("vn"))
			countryID = "84";
		else if(countryCode.equalsIgnoreCase("ar"))
			countryID = "54";
		else if(countryCode.equalsIgnoreCase("bh"))
			countryID = "973";
		else if(countryCode.equalsIgnoreCase("do"))
			countryID = "1";
		else if(countryCode.equalsIgnoreCase("hk"))
			countryID = "852";
		else if(countryCode.equalsIgnoreCase("il"))
			countryID = "972";
		else if(countryCode.equalsIgnoreCase("pr"))
			countryID = "1";
		else if(countryCode.equalsIgnoreCase("pk"))
			countryID = "92";
		else if(countryCode.equalsIgnoreCase("cn"))
			countryID = "86";
		else if(countryCode.equalsIgnoreCase("ng"))
			countryID = "234";
		else if(countryCode.equalsIgnoreCase("in"))
			countryID = "91";
		else
			countryID = "+";
		
		
		return countryID;
	}
	
	public String getContryName()
	{
		String countryID = null;
		String countryCode = null;
		
		TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		countryCode = tm.getNetworkCountryIso();
		
		if(countryCode.equalsIgnoreCase("au"))
			countryID = "Australia";
		else if(countryCode.equalsIgnoreCase("at"))
			countryID = "Austria";
		else if(countryCode.equalsIgnoreCase("be"))
			countryID = "Belgium";
		else if(countryCode.equalsIgnoreCase("br"))
			countryID = "Brazil";
		else if(countryCode.equalsIgnoreCase("bg"))
			countryID = "Bulgaria";
		else if(countryCode.equalsIgnoreCase("ca"))
			countryID = "Canada";
		else if(countryCode.equalsIgnoreCase("cl"))
			countryID = "Chile";
		else if(countryCode.equalsIgnoreCase("cy"))
			countryID = "Cyprus";
		else if(countryCode.equalsIgnoreCase("cz"))
			countryID = "Czech republic";
		else if(countryCode.equalsIgnoreCase("dk"))
			countryID = "Denmark";
		else if(countryCode.equalsIgnoreCase("sv"))
			countryID = "El Salvador";
		else if(countryCode.equalsIgnoreCase("ee"))
			countryID = "Estonia";
		else if(countryCode.equalsIgnoreCase("fi"))
			countryID = "Finland";
		else if(countryCode.equalsIgnoreCase("fr"))
			countryID = "France";
		else if(countryCode.equalsIgnoreCase("ge"))
			countryID = "Georgia";
		else if(countryCode.equalsIgnoreCase("gr"))
			countryID = "Greece";
		else if(countryCode.equalsIgnoreCase("si"))
			countryID = "Slovenia";
		else if(countryCode.equalsIgnoreCase("ie"))
			countryID = "Ireland";
		else if(countryCode.equalsIgnoreCase("it"))
			countryID = "Italy";
		else if(countryCode.equalsIgnoreCase("jp"))
			countryID = "Japan";
		else if(countryCode.equalsIgnoreCase("lv"))
			countryID = "Latvia";
		else if(countryCode.equalsIgnoreCase("lt"))
			countryID = "Lithuania";
		else if(countryCode.equalsIgnoreCase("lu"))
			countryID = "Luxembourg";
		else if(countryCode.equalsIgnoreCase("mt"))
			countryID = "Malta";
		else if(countryCode.equalsIgnoreCase("mx"))
			countryID = "Mexico";
		else if(countryCode.equalsIgnoreCase("nl"))
			countryID = "Netherlands";
		else if(countryCode.equalsIgnoreCase("nz"))
			countryID = "New Zealand";
		else if(countryCode.equalsIgnoreCase("pa"))
			countryID = "Panama";
		else if(countryCode.equalsIgnoreCase("pe"))
			countryID = "Peru";
		else if(countryCode.equalsIgnoreCase("pl"))
			countryID = "Poland";
		else if(countryCode.equalsIgnoreCase("pt"))
			countryID = "Portugal";
		else if(countryCode.equalsIgnoreCase("ro"))
			countryID = "Romania";
		else if(countryCode.equalsIgnoreCase("sg"))
			countryID = "Singapore";
		else if(countryCode.equalsIgnoreCase("sk"))
			countryID = "Slovakia";
		else if(countryCode.equalsIgnoreCase("za"))
			countryID = "South Africa";
		else if(countryCode.equalsIgnoreCase("es"))
			countryID = "Spain";
		else if(countryCode.equalsIgnoreCase("se"))
			countryID = "Sweden";
		else if(countryCode.equalsIgnoreCase("ch"))
			countryID = "Switzerland";
		else if(countryCode.equalsIgnoreCase("gb"))
			countryID = "United Kingdom";
		else if(countryCode.equalsIgnoreCase("us"))
			countryID = "United States";
		else if(countryCode .equalsIgnoreCase("vn"))
			countryID = "Vietnam";
		else if(countryCode.equalsIgnoreCase("ar"))
			countryID = "Argentina";
		else if(countryCode.equalsIgnoreCase("bh"))
			countryID = "Bahrain";
		else if(countryCode.equalsIgnoreCase("do"))
			countryID = "Dominican Republic";
		else if(countryCode.equalsIgnoreCase("hk"))
			countryID = "Hong Kong";
		else if(countryCode.equalsIgnoreCase("il"))
			countryID = "Israel";
		else if(countryCode.equalsIgnoreCase("pr"))
			countryID = "Puerto Rico";
		else if(countryCode.equalsIgnoreCase("pk"))
			countryID = "Pakisthan";
		else if(countryCode.equalsIgnoreCase("cn"))
			countryID = "China";
		else if(countryCode.equalsIgnoreCase("ng"))
			countryID = "Nigeria";
		else if(countryCode.equalsIgnoreCase("in"))
			countryID = "India";
		else
			countryID = "your country";
		
		
		return countryID;
	}
	//JedisPoolConfig config = new JedisPoolConfig(); 
	 //config.setMaxActive(200); 
	 //config.setMaxIdle(200); 
	 //pool = new JedisPool(config,"localhost",6379,-1,jedisPasswd); 

/**	boolean setKeyToRedis(String key)
	{
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		
		JedisPool pool = new JedisPool(poolConfig, "192.168.2.152");
		
		Jedis jedis = pool.getResource();

		jedis.set("foo", "bar");
		String foobar = jedis.get("foo");
		assert foobar.equals("bar");

		pool.returnResource(jedis);
		pool.destroy();
		return false;
	}*/
	String getMyPhoneNumber(Context context)
	{
		TelephonyManager tMgr =(TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
		return tMgr.getLine1Number();
		
	}

}
