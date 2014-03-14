package com.sonetel.plugins.sonetelcallthru;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;

import com.sonetel.api.ISipService;
import com.sonetel.api.SipProfile;
import com.sonetel.utils.Compatibility;
import com.sonetel.utils.Log;
import com.sonetel.utils.PreferencesProviderWrapper;
public class AccessNumbers extends Activity 
{

	//private DBAdapter database;
	//private static SQLiteDatabase dbObj;
	public static final String FIELD_ID = "id";
	public static final String COUNTRY_ID = "country_id";
	public static final String ACCESS_NO = "access_no";
	public static boolean authResp = false;
    public static ISipService service;
    

	
	public static ContentValues getContentValues(int countryid)
	{
		ContentValues initialValues = new ContentValues(); 
	
		String stAccessNumbers = new String();
		String stCountry_Code = new String();
	
		switch(countryid)	
		{
			case 1:
				stCountry_Code = "1"; //Australia
				stAccessNumbers = "0280155253";//+61280155253";
				break;
			case 2:
				stCountry_Code = "2"; //Austria
				stAccessNumbers = "0720881478";//"+43720881478";
				break;
			case 3:
				stCountry_Code = "3"; //Belgium
				stAccessNumbers = "+3225880224";
				break;
			case 4:
				stCountry_Code = "4"; //Brazil
				stAccessNumbers = "01139584965";//+551139584965";
				break;
			case 5:
				stCountry_Code = "5"; //Bulgaria
				stAccessNumbers = "024917079";//+35924917079";
				break;
			case 6:
				stCountry_Code = "6"; //Canada
				stAccessNumbers = "4388002487";//+14388002487";
				break;
			case 7:
				stCountry_Code = "7"; //Chile
				stAccessNumbers = "+5629381813";
				break;
			case 8:
				stCountry_Code = "8"; //Cyprus
				stAccessNumbers = "22022673";//+35722022673";
				break;
			case 9:
				stCountry_Code = "9"; //Czech republic
				stAccessNumbers = "228880644";//+420228880644";
				break;
			case 10:
				stCountry_Code = "10"; //Denmark
				stAccessNumbers = "69960351";//+4569960351";
				break;
			case 11:
				stCountry_Code = "11"; //El Salvador
				stAccessNumbers = "21133872";//+50321133872";
				break;
			case 12:
				stCountry_Code = "12"; //Estonia
				stAccessNumbers = "6680353";//+3726680353";
				break;
			case 13:
				stCountry_Code = "13"; //Finland
				stAccessNumbers = "0942599586";//+358942599586";
				break;
			case 14:
				stCountry_Code = "14"; //France
				stAccessNumbers = "0975180800";//+33975180800";
				break;
			case 15:
				stCountry_Code = "15"; //Georgia
				stAccessNumbers = "0706777314";//+99532473914";+995706777314
				break;
			case 16:
				stCountry_Code = "16"; //Greece
				stAccessNumbers = "2111980803";//+302111980803";
				break;
			case 17:
				stCountry_Code = "17"; // Slovenia + 
				stAccessNumbers = "018888395";//+38618888395";
				break;
			case 18:
				stCountry_Code = "18"; //Ireland
				stAccessNumbers = "0766801021";//+353766801021";
				break;
			case 19:
				stCountry_Code = "19"; //Italy
				stAccessNumbers = "0699367705";//+390699367705";
			case 20:
				stCountry_Code = "20"; //Japan
				stAccessNumbers = "0345782199";//+81345782199";
				break;
			case 21:
				stCountry_Code = "21"; //Latvia
				stAccessNumbers = "66163204";//+37166163204";
				break;
			case 22:
				stCountry_Code = "22"; //Lithuania
				stAccessNumbers = "052140248";//+37052140248";
				break;
			case 23:
				stCountry_Code = "23"; //Luxembourg
				stAccessNumbers = "20881159";//+35220881159";
				break;
			case 24:
				stCountry_Code = "24"; //Malta
				stAccessNumbers = "+35627780148";
				break;
			case 25:
				stCountry_Code = "25"; //Mexico
				stAccessNumbers = "015511689661";//+525511689661";
				break;
			case 26:
				stCountry_Code = "26"; //Netherlands
				stAccessNumbers = "0858881057";//+31858881057";
				break;
			case 27:
				stCountry_Code = "27"; //New Zealand
				stAccessNumbers = "099250321";//+6499250321";
				break;
			case 28:
				stCountry_Code = "28"; //Panama
				stAccessNumbers = "8365956";//+5078365956";
				break;
			case 29:
				stCountry_Code = "29"; //Peru
				stAccessNumbers = "017059739";//+5117059739";
				break;
			case 30:
				stCountry_Code = "30"; //Poland
				stAccessNumbers = "223970440";//+48223970440";
				break;
			case 31:
				stCountry_Code = "31"; //Portugal
				stAccessNumbers = "308801568";//+351308801568";
				break;
			case 32:
				stCountry_Code = "32"; //Romania
				stAccessNumbers = "0318142237";//+40318142237";
				break;
			case 33:
				stCountry_Code = "33";  //Singapore
				stAccessNumbers = "31582682";//+6531582682";
				break;
			case 34:
				stCountry_Code = "34"; //Slovakia
				stAccessNumbers = "0233215517";//+421233215517";
				break;
			case 35:
				stCountry_Code = "35"; //South Africa
				stAccessNumbers = "0105008759";//+27105008759";
				break;
			case 36:
				stCountry_Code = "36";  //Spain
				stAccessNumbers = "911231747";//+34911231747";
				break;
			case 37:
				stCountry_Code = "37"; //Sweden
				stAccessNumbers = "0852506065";//+46852506065";
				break;
			case 38:
				stCountry_Code = "38"; //Switzerland
				stAccessNumbers = "0225180134";//+41225180134";
				break;
			case 39:
				stCountry_Code = "39"; //United Kingdom
				stAccessNumbers = "02033182922";//+442033182922";
				break;
			case 40:
				stCountry_Code = "40"; //United States
				stAccessNumbers = "2026815905";
				break;
			case 41:
				stCountry_Code = "41"; //Vietnam
				stAccessNumbers = "0473054028";//+84473054028";
				break;
			case 42:
				stCountry_Code = "42"; //Argentina 
				stAccessNumbers = " 01152391365";//+541152391365";
				break;	
			case 43:
				stCountry_Code = "43"; //Bahrain 
				stAccessNumbers = "16199849";//+97316199849";
				break;	
			case 44:
				stCountry_Code = "44"; //Dominican Republic +
				stAccessNumbers = "8296072144";//+18296072144";
				break;	
			case 45:
				stCountry_Code = "45"; //Hong Kong +
				stAccessNumbers = "58083139";//+85258083139";
				break;	
			case 46:
				stCountry_Code = "46"; //Israel +
				stAccessNumbers = "037630192";//+97237630192";
				break;	
			case 47:
				stCountry_Code = "47"; //Puerto Rico + 
				stAccessNumbers = "+17879317232";
				break;	
			case 48:
				stCountry_Code = "48"; //Thailand +66600035063
				stAccessNumbers = "0600035063";
				break;	
			case 49:
				stCountry_Code = "49"; //Ukraine +380443607309
				stAccessNumbers = "0443607309";
				break;
			case 50:
				stCountry_Code = "50"; //Russia  +74999184251
				stAccessNumbers = "+74999184251";
				break;
			case 51:
				stCountry_Code = "51"; //Liechtenstein 
				stAccessNumbers = "+4233769219";
				break;
			case 52:
				stCountry_Code = "52"; //Algeria 
				stAccessNumbers = "+213983201245";
				break;
			case 53:
				stCountry_Code = "53"; //India 
				stAccessNumbers = "04040153284,2";
				break;
				 
				
		}

	initialValues.put(COUNTRY_ID,stCountry_Code ); 
	initialValues.put(ACCESS_NO, stAccessNumbers); 
	
	return initialValues;

	}
	public static boolean sendPostReq() throws ClientProtocolException, IOException
	{
		URI updateURI;
		try {
			//updateURI = new URI("https://sonetelindia.dyndns.org:2020/servlets/apacheServer");
			//updateURI = new URI("https://192.168.2.123:2020/servlets/apacheServer");
			//updateURI = new URI("https://eu.test.sonetel.net:2020/servlets/apacheServer");
			updateURI = new URI("https://sonetel.net:2020/servlets/apacheServer");
			//updateURI = new URI("https://192.168.2.152:2020/servlets/apacheServer");
	
			HttpPost httpPost = new HttpPost(updateURI);

			HttpResponse httpResp;
			HttpClient httpClient = new DefaultHttpClient();
		
			List<NameValuePair> pairs = new ArrayList<NameValuePair>(2);
		
			pairs.add(new BasicNameValuePair("ACCESS_NOS", "GLOBAL_CALLTHRU_NUMBERS"));//"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><ADMIN><OPERATION>GET</OPERATION><ENTITY>ACCESSNUMBERS</ENTITY></ADMIN>"));//key,""));

			httpPost.setEntity(new UrlEncodedFormEntity(pairs));
			
		
			httpResp = httpClient.execute(httpPost);
			
			//HttpEntity respEntity =  httpResp.getEntity();
			
			InputStream is = httpResp.getEntity().getContent();
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
		    byte[] buf;
		    int ByteRead;
		    buf = new byte[1024];

		    String xmldata = null;
		    while ((ByteRead = is.read(buf, 0, buf.length)) != -1) {
		        os.write(buf, 0, ByteRead);
		       // totalSize += ByteRead;                      
		    }

		    xmldata =  os.toString();//.replaceAll(" ", "");
		    os.close();
		    is.close();
			return true;
		
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d("AccessNumbers", e.getMessage());
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static SipProfile sendPostReq(SipProfile account, String number,String callthrunum,Context context,PendingIntent callthruIntent) throws ClientProtocolException, IOException
	{
		URI updateURI;
		try {
			//updateURI = new URI("https://208.51.64.179:2020/servlets/apacheServer");
			updateURI = new URI("https://sonetel.net:2020/servlets/apacheServer");
			//updateURI = new URI("https://eu.test.sonetel.net:2020/servlets/apacheServer");
			//updateURI = new URI("https://192.168.2.152:2020/servlets/apacheServer");
			//updateURI = new URI("https://sonetelindia.dyndns.org:2020/servlets/apacheServer");
			//updateURI = new URI("https://192.168.2.123:2020/servlets/apacheServer");
			
			
			
			HttpPost httpPost = new HttpPost(updateURI);

			HttpResponse httpResp;
			//ResponseHandler<String> responseHandler = new BasicResponseHandler();
			HttpClient httpClient = getHttpClient();// new DefaultHttpClient();
		
			List<NameValuePair> pairs = new ArrayList<NameValuePair>(3);
			
			if(number=="1")
			{
					pairs.add(new BasicNameValuePair("UID", account.getSipUserName()+"@"+account.getSipDomain()));//"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><ADMIN><OPERATION>GET</OPERATION><ENTITY>ACCESSNUMBERS</ENTITY></ADMIN>"));//key,""));
					//	pairs.add(new BasicNameValuePair("DATA", MD5.MD5Hash(account.getPassword())));
					pairs.add(new BasicNameValuePair("DATA", account.getPassword()));
					pairs.add(new BasicNameValuePair("MNO", account.mobile_nbr));
			}
			else if(number=="2")
			{
				pairs.add(new BasicNameValuePair("MNO", account.mobile_nbr));
				pairs.add(new BasicNameValuePair("UID", account.getSipUserName()+"@"+account.getSipDomain()));
				pairs.add(new BasicNameValuePair("DATA", account.getPassword()));
							
			}
			else
			{
				if(callthruIntent==null)
				{
					pairs.add(new BasicNameValuePair("CBNO", number));
					Log.d("Request for call back - ",number);
				}
				else
				{
					pairs.add(new BasicNameValuePair("DNO", number));
					Log.d("Request for call thru - ",number);
				}
				
				pairs.add(new BasicNameValuePair("UID", account.usrid));//account.getSipUserName()+"@"+account.getSipDomain()));
				pairs.add(new BasicNameValuePair("DATA", account.getPassword()));
				pairs.add(new BasicNameValuePair("PIN", account.pin));
				pairs.add(new BasicNameValuePair("CLI",account.mobile_nbr));
				
				if(callthruIntent==null)
				{
					if(callthrunum != null)
					{
						if(!callthrunum.equalsIgnoreCase(""))
						{
							pairs.add(new BasicNameValuePair("CallerCLIType","1"));
							pairs.add(new BasicNameValuePair("CallerCLI",callthrunum));
						}
						else
						{
							pairs.add(new BasicNameValuePair("CallerCLIType","1"));
							pairs.add(new BasicNameValuePair("CallerCLI","+12026815905"));
						}
					}
					else
					{
						pairs.add(new BasicNameValuePair("CallerCLIType","1"));
						pairs.add(new BasicNameValuePair("CallerCLI","+12026815905"));
					}
						
					pairs.add(new BasicNameValuePair("CalleeCLIType","2"));
					pairs.add(new BasicNameValuePair("CalleeCLI",account.mobile_nbr));
				}
				
				if(callthruIntent!=null)
				{
					pairs.add(new BasicNameValuePair("APP", "1"));
					
					PackageInfo pinfo = PreferencesProviderWrapper.getCurrentPackageInfos(context);
					String userAgent = android.os.Build.BRAND + "-" + android.os.Build.DEVICE + "-" + android.os.Build.VERSION.RELEASE + "-" + Compatibility.getApiLevel()
		                    + "-" + pinfo.versionCode;
					
					pairs.add(new BasicNameValuePair("DEVICE", userAgent));
				}

			}
			//CBNO=00447544599516&UID=2000004141&DATA=sonetel1&PIN=5001&CLI=%2B919177095550&CallerCLIType=1&CallerCLI=%2B919177095550&CalleeCLIType=2&CalleeCLI=919177095550
			httpPost.setEntity(new UrlEncodedFormEntity(pairs));
		
			httpResp = httpClient.execute(httpPost);
			
			//HttpEntity respEntity =  httpResp.getEntity();
			
			InputStream is = httpResp.getEntity().getContent();
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
		    byte[] buf;
		    int ByteRead;
		    buf = new byte[1024];

		    String xmldata = null;
		    while ((ByteRead = is.read(buf, 0, buf.length)) != -1) {
		        os.write(buf, 0, ByteRead);
		       // totalSize += ByteRead;                      
		    }

		    xmldata =  os.toString();//.replaceAll(" ", "");
		    os.close();
		    is.close();
		    
		    if(number=="1" && !xmldata.equalsIgnoreCase("FALSE") && xmldata.length()>4)
		    {
		    	
		    	String[] tokens = null;
		    
		    	tokens = xmldata.split("&");
		    	
		    	if(tokens != null)
		    	{
		    		account.entid = tokens[0];
		    		
		    		account.extension = tokens[1];
		    		
		    		account.pin = tokens[2];
		    		
		    		account.usrid = tokens[3];
		    		
		    		Log.d("Yuva_Sucess",account.pin);
		    		
		    	}
		    	
		    	return account;
		    }
		    else if(xmldata.equalsIgnoreCase("TRUE"))
		    {
		    	if(callthruIntent != null)
		    		callthruIntent.send();
		    }

		    
			return account;
		
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d("AccessNumbers", e.getMessage());
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return account;
	}
    
	public static HttpClient getHttpClient() {
	    try {
	        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
	        trustStore.load(null, null);

	        SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
	        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

	        HttpParams params = new BasicHttpParams();
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

	        SchemeRegistry registry = new SchemeRegistry();
	        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        registry.register(new Scheme("https", sf, 443));

	        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

	        return new DefaultHttpClient(ccm, params);
	    } catch (Exception e) {
	        return new DefaultHttpClient();
	    }
	}
}
