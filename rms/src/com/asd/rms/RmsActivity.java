package com.asd.rms;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.widget.TabHost;
import android.widget.TextView;

public class RmsActivity extends TabActivity {
	
	TabHost mTabHost;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTabHost = getTabHost();
        mTabHost.addTab(mTabHost.newTabSpec("tab1")
        				.setIndicator("基本信息")
        				.setContent(new Intent(this,BaseInfoActivity.class)));
    }
   
}