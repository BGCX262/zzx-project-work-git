package com.asd.rms;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BaseInfoActivity extends Activity {
	private final String TAG = "BaseInfoActivity";
	private TelephonyManager mTelephonyManager;
	Resources mRes;
	TextView mDeviceId;
	TextView operatorName;
	TextView gsmState;
	TextView roamingState;
	private Button pingTestButton;
	private String mPingIpAddrResult;
	private String mPingHostnameResult;
	private String mHttpClientTestResult;
	
	private TextView mPingIpAddr;
	private TextView mPingHostname;
	private TextView mHttpClientTest;
	
	private TextView dbm;
	private TextView asu;
	private TextView mLocation;
	private TextView mNeighboringCids;
	private Button mLockLacCid;
	
	private TextView gprsState;
	private TextView network;
	private TextView mMwi;
	private TextView mCfi;
	private boolean mMwiValue = false;
	private boolean mCfiValue = false;
	
	private TextView callState;
	private TextView resets;
	private TextView attempts;
	private TextView successes;
	
	private TextView sent;
	private TextView received;
	private TextView sentSinceReceived;
	
	private Spinner preferredNetworkType;
	
	private TextView oemInfoButton;
	
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
		/*@Override
		public void onSignalStrengthChanged(int asuValue) {//过时
			super.onSignalStrengthChanged(asuValue);
			asu.setText("  " + asuValue + getResources().getString(R.string.base_info_display_asu));
		}*/
		//信号状态
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			String val = "";
			Resources r = getResources();
			if(signalStrength.isGsm()){
				val = signalStrength.getGsmSignalStrength()*2-111 + " " + r.getString(R.string.base_info_display_dbm);
			}else{
				val = signalStrength.getCdmaDbm()*2-111 + " " + r.getString(R.string.base_info_display_dbm);
			}
			dbm.setText(val);
		}
		//小区状态
		public void onCellLocationChanged(CellLocation location) {
			Log.v(TAG, "onCellLocationChanged()");
			updateLocation(location);
		}
		//服务状态
		public void onServiceStateChanged(ServiceState serviceState) {
			super.onServiceStateChanged(serviceState);
			updateServiceState(serviceState);
		}
		//数据连接状态
		public void onDataConnectionStateChanged(int state) {
			updateDataState();//gprs
			updateDataStats(); //
			updateNetworkType();//network
		};
		//消息等待
		public void onMessageWaitingIndicatorChanged(boolean mwi) {
			mMwiValue = mwi;
			updateMessageWaiting();
		};
		//呼叫重定向
		public void onCallForwardingIndicatorChanged(boolean cfi) {
			mCfiValue = cfi;
			updateCallRedirect();
		};
		//呼叫状态
		public void onCallStateChanged(int state, String incomingNumber) {
			updatePhoneState(state,incomingNumber);
		};
		//PPP数据
		public void onDataActivity(int direction) {
			updateDataStats2();
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mRes = getResources();
		mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		setContentView(R.layout.base_info);
		mDeviceId = (TextView) findViewById(R.id.imei);
		operatorName = (TextView)findViewById(R.id.operator);
		gsmState = (TextView) findViewById(R.id.gsm);
		roamingState = (TextView)findViewById(R.id.roaming);
		
		pingTestButton = (Button)findViewById(R.id.ping_test);
		pingTestButton.setOnClickListener(mPingButtonHandler);
		
		mPingIpAddr = (TextView) findViewById(R.id.pingIpAddr);
        mPingHostname = (TextView) findViewById(R.id.pingHostname);
        mHttpClientTest = (TextView) findViewById(R.id.httpClientTest);
        
        dbm = (TextView)findViewById(R.id.dbm);
        //TODO---ASU
        asu = (TextView)findViewById(R.id.asu);
        
        mLocation = (TextView)findViewById(R.id.location);
        mNeighboringCids = (TextView) findViewById(R.id.neighboring);
        mLockLacCid = (Button)findViewById(R.id.lockLacCid);
        mLockLacCid.setOnClickListener(mLockButtonHandler);
        
        gprsState = (TextView)findViewById(R.id.gprs);
        network = (TextView)findViewById(R.id.network);
        mMwi = (TextView)findViewById(R.id.mwi);
        mCfi = (TextView)findViewById(R.id.cfi);
        
        callState = (TextView)findViewById(R.id.call);
        resets = (TextView)findViewById(R.id.resets);
        attempts = (TextView)findViewById(R.id.attempts);
        successes = (TextView)findViewById(R.id.successes);
        
        sent = (TextView)findViewById(R.id.sent);
        received = (TextView)findViewById(R.id.received);
        sentSinceReceived = (TextView)findViewById(R.id.sentSinceReceived);
        
        preferredNetworkType = (Spinner)findViewById(R.id.preferredNetworkType);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,mPreferredNetworkLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        preferredNetworkType.setAdapter(adapter);
//        preferredNetworkType.setOnItemSelectedListener(mPreferredNetworkHandler);
        
        oemInfoButton = (Button) findViewById(R.id.oem_info);
        oemInfoButton.setOnClickListener(mOemInfoButtonHandler);
        PackageManager pm = getPackageManager();
        Intent oemInfoIntent = new Intent("com.android.settings.OEM_RADIO_INFO");
        List<ResolveInfo> oemInfoIntentList = pm.queryIntentActivities(oemInfoIntent, 0);
        if(oemInfoIntentList.size() == 0){
        	oemInfoButton.setEnabled(false);
        }
        
//		CellLocation.requestLocationUpdate();
        //放在这里，不放在onResume()方法里，广播注册一样，地址改变了会发送广播给监听了这个信息的类接收，下面就监听了
//		CellLocation.requestLocationUpdate();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateProperties();
		
		updateNeighboringCids(mTelephonyManager);
		mTelephonyManager.listen(mPhoneStateListener,
					PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
				  | PhoneStateListener.LISTEN_SERVICE_STATE
				  | PhoneStateListener.LISTEN_CELL_LOCATION
				  | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
				  | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
				  | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
				  | PhoneStateListener.LISTEN_CALL_STATE
				  | PhoneStateListener.LISTEN_DATA_ACTIVITY);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(mTimer != null)
			mTimer.cancel();
	}
	// 锁屏按钮
	OnClickListener mLockButtonHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
//			CellLocation cellLocation = mTelephonyManager.getCellLocation();
//			GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
//			Bundle m = new Bundle();
//			m.putInt("lac", -1);
//			m.putInt("cid", -1);
//			m.putInt("psc", gsmCellLocation.getPsc());
//			gsmCellLocation.fillInNotifierBundle(m);
//			gsmCellLocation.setLacAndCid(-1, -1);
//			CellLocation.requestLocationUpdate();
			List<NeighboringCellInfo> cellList = mTelephonyManager.getNeighboringCellInfo();
			int size = cellList.size();
			final String[] items = new String[size];
			NeighboringCellInfo cell;
			for(int i = 0; i < size; i++){
				cell = cellList.get(i);
				items[i] = cell.getLac() + " & " + cell.getCid(); 
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(BaseInfoActivity.this);
			String title = mRes.getString(R.string.neighbor_dialog_title);
			builder.setTitle(title)
				   .setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
							CellLocation cellLocation = mTelephonyManager.getCellLocation();
							String[] item = items[which].split("&");
							int lac = Integer.valueOf(item[0].trim());
							int cid = Integer.valueOf(item[1].trim());
							if(cellLocation instanceof GsmCellLocation){
								GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
	//							gsmCellLocation.setStateInvalid();
								gsmCellLocation.setLacAndCid(-1,-1);
								CellLocation.requestLocationUpdate();
								Toast.makeText(BaseInfoActivity.this, "设置成功 lac:"+lac+",cid:"+cid, 3000).show();
							}
					}
				});
			AlertDialog alert = builder.create();
			alert.show();
		}
	};
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			
		};
	};
	//
	AdapterView.OnItemSelectedListener mPreferredNetworkHandler = new AdapterView.OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
//			Message msg = mHandler.obtainMessage();
		}
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};
	//Oem按钮Handler
	OnClickListener mOemInfoButtonHandler = new OnClickListener(){
		public void onClick(View v) {
			Intent intent = new Intent("com.android.settings.OEM_RADIO_INFO");
			try{
				startActivity(intent);
			}catch(android.content.ActivityNotFoundException ex){
				Log.d(TAG,"OEM-specific Info/Settings Activity Not Found : " + ex);
			}
		}
	};
	
	private final void updateDataStats2(){
		Resources r = getResources();
		
		long txPackets = TrafficStats.getMobileTxPackets();
		long rxPackets = TrafficStats.getMobileRxPackets();
		long txBytes = TrafficStats.getMobileTxBytes();
		long rxBytes = TrafficStats.getMobileRxBytes();
		
		String packets = r.getString(R.string.base_info_display_packets);
		String bytes = r.getString(R.string.base_info_display_bytes);
		
		sent.setText(txPackets + " " + packets + ", " + txBytes + " " + bytes);
		received.setText(rxPackets + " " + packets + ", " + rxBytes + " " + bytes);
	}
	/*
	 * 更新无线通信重置 数据尝试次数  数据成功
	 */
	private final void updateDataStats(){
		Object obj;
		try {
			Class<?> sys = Class.forName("android.os.SystemProperties");
			Method method = sys.getMethod("get", String.class,String.class);
			//静态方法，则不需要构造实例的方式了
			obj = method.invoke(sys, "net.gsm.radio-reset","0");
			resets.setText(obj.toString());
			
			obj = method.invoke(sys, "net.gsm.attempt-gprs","0");
			attempts.setText(obj.toString());
			
			obj = method.invoke(sys, "net.ppp.reset-by-timeout","0");
			successes.setText(obj.toString());
			
			obj = method.invoke(sys, "net.ppp.reset-by-timeout","0");
			sentSinceReceived.setText(obj.toString());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 更新呼叫状态
	 * @param state
	 * @param incomingNumber
	 */
	private final void updatePhoneState(int state, String incomingNumber){
		String display = mRes.getString(R.string.base_info_unknown);
		switch(state){
		case TelephonyManager.CALL_STATE_IDLE:
			display = mRes.getString(R.string.base_info_phone_idle);
			break;
		case TelephonyManager.CALL_STATE_RINGING:
			display = mRes.getString(R.string.base_info_phone_ringing);
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			display = mRes.getString(R.string.base_info_phone_offhook);
			break;
		}
		callState.setText(display);
	}
	/**
	 * 更新呼叫重定向
	 */
	private final void updateCallRedirect(){
		mCfi.setText(String.valueOf(mCfiValue));
	}
	/**
	 * 更新消息等待
	 */
	private final void updateMessageWaiting(){
		mMwi.setText(String.valueOf(mMwiValue));
	}
	/**
	 * 更新网络类型
	 */
	private final void updateNetworkType(){
		int networkType = mTelephonyManager.getNetworkType();
		String display = mRes.getString(R.string.base_info_unknown);
		switch(networkType){
		case TelephonyManager.NETWORK_TYPE_CDMA:
			display = mRes.getString(R.string.base_info_network_type_cdma);
			break;
		case TelephonyManager.NETWORK_TYPE_EDGE:
			display = mRes.getString(R.string.base_info_network_type_edge);
			break;
		case TelephonyManager.NETWORK_TYPE_GPRS:
			display = mRes.getString(R.string.base_info_network_type_gprs);
			break;
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
			display = mRes.getString(R.string.base_info_network_type_evdo_0);
			break;
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
			display = mRes.getString(R.string.base_info_network_type_evdo_A);
			break;
		case TelephonyManager.NETWORK_TYPE_EVDO_B:
			display = mRes.getString(R.string.base_info_network_type_evdo_B);
			break;
		}
		network.setText(display);
	}
	/**
	 * 更新Gprs状态
	 */
	private final void updateDataState(){
		int state = mTelephonyManager.getDataState();
		String display = mRes.getString(R.string.base_info_unknown);
		switch(state){
			case TelephonyManager.DATA_CONNECTED:
					display = mRes.getString(R.string.base_info_data_connected);
					break;
			case TelephonyManager.DATA_CONNECTING:
				display = mRes.getString(R.string.base_info_data_connecting);
				break;
			case TelephonyManager.DATA_DISCONNECTED:
				display = mRes.getString(R.string.base_info_data_suspended);
				break;
		}
		gprsState.setText(display);
	}
	
	Timer mTimer ;
	final StringBuilder sb = new StringBuilder();
	final Handler handler = new Handler();
	/**
	 * 更新邻小区
	 * @param tm
	 */
	private final void updateNeighboringCids(TelephonyManager tm){
		final TelephonyManager telephony = tm;
		final Runnable updateNeiCids = new Runnable(){
			@Override
			public void run() {
				mNeighboringCids.setText(sb.toString());
			}
		};
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				List<NeighboringCellInfo> cellList = telephony.getNeighboringCellInfo();
//				Toast.makeText(BaseInfoActivity.this, "size" + cellList.size(), 1000).show();
				sb.delete(0, sb.length());
				for(NeighboringCellInfo ci : cellList){
					sb.append("LAC:").append(ci.getLac()).append(",CID:").append(ci.getCid()).append("\n");
				}
				handler.post(updateNeiCids);
			}
		}, 0, 3000);
	}
	
	/**
	 *  更新主小区地区信息  LAC CID
	 * @param location
	 */
	private final void updateLocation(CellLocation location){
		if(location instanceof GsmCellLocation){
			GsmCellLocation loc = (GsmCellLocation)location;
			int lac = loc.getLac();
			int cid = loc.getCid();
			mLocation.setText(mRes.getString(R.string.base_info_lac) + " = "
							+ ((lac == -1) ? "unknown" : lac)
							+ "  "
							+ mRes.getString(R.string.base_info_cid) + " = "
							+ ((cid == -1) ? "unknown" : cid));
		}else if(location instanceof CdmaCellLocation){
			CdmaCellLocation loc = (CdmaCellLocation)location;
			int bid = loc.getBaseStationId();
			int sid = loc.getSystemId();
			int nid = loc.getNetworkId();
			int lat = loc.getBaseStationLatitude();
			int lon = loc.getBaseStationLongitude();
			mLocation.setText("BID = " 
							+ ((bid == -1) ? "unknown" : Integer.toHexString(bid))
							+ "  "
							+ "SID = "
							+ ((sid == -1) ? "unknown" : Integer.toHexString(sid))
							+ "NID = "
							+ ((nid == -1) ? "unknown" : Integer.toHexString(nid))
							+ "\n"
							+ "LAT = "
							+ ((lat == -1) ? "unknown" : Integer.toHexString(lat))
							+ "  "
							+ "LONG = "
							+ ((lon == -1) ? "unknown" : Integer.toHexString(lon)));
		}else{
			mLocation.setTag("unknown");
		}
	}
	//Ping Button
	OnClickListener mPingButtonHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			updatePingState();
		}
	};
	/**
	 * 更新Ping有关的状态
	 */
	private final void updatePingState() {
//		final Handler handler = new Handler();
		// Set all to unknown since the threads will take a few secs to update.
		mPingIpAddrResult = mRes.getString(R.string.base_info_unknown);
        mPingHostnameResult = mRes.getString(R.string.base_info_unknown);
        mHttpClientTestResult = mRes.getString(R.string.base_info_unknown);
        
        mPingIpAddr.setText(mPingIpAddrResult);
        mPingHostname.setText(mPingHostnameResult);
        mHttpClientTest.setText(mHttpClientTestResult);
        
        final Runnable updatePingResults = new Runnable(){
			public void run() {
				mPingIpAddr.setText(mPingIpAddrResult);
				mPingHostname.setText(mPingHostnameResult);
				mHttpClientTest.setText(mHttpClientTestResult);
			}
        };
        Thread ipAddr = new Thread(){
        	@Override
        	public void run() {
        		pingIpAddr();
        		handler.post(updatePingResults);
        	}
        };
        ipAddr.start();
        
        Thread hostname = new Thread(){
        	public void run() {
        		pingHostname();
        		handler.post(updatePingResults);
        	}
        };
        hostname.start();
        
        Thread httpClient = new Thread(){
        	public void run() {
        		httpClientTest();
        		handler.post(updatePingResults);
        	}
        };
        httpClient.start();
	}
	/**
	 * HttpClient测试
	 */
	private void httpClientTest(){
		HttpClient client = new DefaultHttpClient();
		try {
			HttpGet request = new HttpGet("http://www.baidu.com/");
			HttpResponse response = client.execute(request);
			if(response.getStatusLine().getStatusCode() == 200){
				mHttpClientTestResult = "Pass";
			}else{
				mHttpClientTestResult = "Fail: Code " + String.valueOf(response);
			}
			request.abort();
		} catch (IOException e) {
			mHttpClientTestResult = "Fail: IOException";
			e.printStackTrace();
		}finally{
			client.getConnectionManager().shutdown();
		}
	}
	/**
	 * 主机地址测试(移动网络可行  wifi不行)
	 */
	private final void pingHostname(){
			try {
				Process p = Runtime.getRuntime().exec("ping -c 1 www.baidu.com");
				int status = p.waitFor();
				if(status == 0){
					mPingHostnameResult = "Pass";
				}else{
					mPingHostnameResult = "Fail: Host unreachable";
				}
			} catch (IOException e) {
				mPingHostnameResult = "Fail:IOException";
			} catch (InterruptedException e) {
				mPingHostnameResult = "Fail: Host unreachable";
			}
	};
	/**
	 * Ip地址测试（移动网络可行  wifi不行）
	 */
	private final void pingIpAddr(){
		try {
			String ipAddress = "220.181.111.86";//baidu.com
			Process p = Runtime.getRuntime().exec("ping -c 1 " + ipAddress);
			int status = p.waitFor();
			if(status == 0){
				mPingIpAddrResult = "Pass";
			}else{
				mPingIpAddrResult = "Fail: IP addr not reachable";
			}
		} catch (IOException e) {
			mPingIpAddrResult = "Fail: IOException";
		} catch (InterruptedException e) {
			mPingIpAddrResult = "Fail: InterruptedException";
		}
	}
	

	private void updateProperties() {
		String lineNumber = mTelephonyManager.getLine1Number();
		String s = mTelephonyManager.getDeviceId();
		mDeviceId.setText(s + ":" + lineNumber);
	}
	
	private void updateServiceState(ServiceState serviceState) {
		int state = serviceState.getState();
		String display = mRes.getString(R.string.base_info_unknown);
		switch(state){
			case ServiceState.STATE_IN_SERVICE:
				display = mRes.getString(R.string.base_info_service_in);
				break;
			case ServiceState.STATE_OUT_OF_SERVICE:
			case ServiceState.STATE_EMERGENCY_ONLY:
				display = mRes.getString(R.string.base_info_service_emergency);
				break;
			case ServiceState.STATE_POWER_OFF:
				display = mRes.getString(R.string.base_info_service_off);
				break;
		}
		gsmState.setText(display);
		
		if(serviceState.getRoaming()){
			roamingState.setText(R.string.base_Info_roaming_in);
		}else{
			roamingState.setText(R.string.base_Info_roaming_not);
		}
		
		operatorName.setText(serviceState.getOperatorAlphaLong());
	}
	
	private String[] mPreferredNetworkLabels = {
			"WCDMA preferred",
            "GSM only",
            "WCDMA only",
            "GSM auto (PRL)",
            "CDMA auto (PRL)",
            "CDMA only",
            "EvDo only",
            "GSM/CDMA auto (PRL)",
            "Unknown"};
}
