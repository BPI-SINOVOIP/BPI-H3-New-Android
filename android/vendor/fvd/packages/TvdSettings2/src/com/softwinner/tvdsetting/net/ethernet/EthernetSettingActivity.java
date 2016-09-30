package com.softwinner.tvdsetting.net.ethernet;

import com.softwinner.tvdsetting.R;
import com.softwinner.tvdsetting.widget.ItemSelectView;
import com.softwinner.tvdsetting.widget.ItemSelectView.ItemSwitch;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.softwinner.tvdsetting.widget.ExtImageButton;
import android.net.ConnectivityManager;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDevInfo;
import android.widget.EditText;
import android.widget.Button;

public class EthernetSettingActivity extends Activity implements View.OnClickListener, View.OnFocusChangeListener,CheckBox.OnCheckedChangeListener{

    private static final String DHCP_MODE = EthernetManager.ETHERNET_CONNECT_MODE_DHCP;
    private static final String STATIC_MODE = EthernetManager.ETHERNET_CONNECT_MODE_MANUAL;
    private static final String PPPOE_MODE = EthernetManager.ETHERNET_CONNECT_MODE_PPPOE;
	private static final String TAG = "EthernetSettingActivity";

	private Context mContext;
	private ItemSelectView mSelector;
	private int currentMode = 0;

	private LinearLayout mDHCPLayout;
	private LinearLayout mStaticIPLayout;
	private LinearLayout mPPPoELayout;
	private LinearLayout mConnectModeLayout;
	private LinearLayout mDHCPUsernameLayout;
	private LinearLayout mDHCPPasswdLayout;
	private LinearLayout mIPAddressLayout;
	private LinearLayout mNetmaskLayout;
	private LinearLayout mDnsLayout;
	private LinearLayout mGatewayLayout;
	private LinearLayout mPPPoEUsernameLayout;
	private LinearLayout mPPPoEPasswdLayout;
	private Button mDhcpSubmit;
	private Button mStaticSubmit;
	private Button mPppoeSubmit;
	private EditText mDHCPUsername;
	private EditText mDHCPPasswd;
	private EditText mIPAddress;
	private EditText mNetmask;
	private EditText mDns;
	private EditText mGateway;
	private EditText mPPPoEUsername;
	private EditText mPPPoEPasswd;
	private CheckBox mSupportIPOE;

	private ConnectivityManager mService;
	private EthernetManager mEthManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		this.setContentView(R.layout.ethernetsetting);
		mSelector = (ItemSelectView)this.findViewById(R.id.selector);
		mSelector.setOnFocusChangeListener(this);
		mDHCPLayout = (LinearLayout)this.findViewById(R.id.dhcp_mode);
		mStaticIPLayout = (LinearLayout)this.findViewById(R.id.static_ip_mode);
		mPPPoELayout = (LinearLayout)this.findViewById(R.id.pppoe_mode);
		mDhcpSubmit = (Button)this.findViewById(R.id.dhcp_submit);
		mDhcpSubmit.setOnClickListener(this);
		mStaticSubmit = (Button)this.findViewById(R.id.static_submit);
		mStaticSubmit.setOnClickListener(this);
		mPppoeSubmit = (Button)this.findViewById(R.id.pppoe_submit);
		mPppoeSubmit.setOnClickListener(this);
		mConnectModeLayout = (LinearLayout)this.findViewById(R.id.connect_mode_layout);
		mDHCPUsernameLayout = (LinearLayout)this.findViewById(R.id.dhcp_username_layout);
		mDHCPUsernameLayout.setVisibility(View.GONE);
		mDHCPPasswdLayout = (LinearLayout)this.findViewById(R.id.dhcp_passwd_layout);
		mDHCPPasswdLayout.setVisibility(View.GONE);
		mIPAddressLayout = (LinearLayout)this.findViewById(R.id.ip_address_layout);
		mNetmaskLayout = (LinearLayout)this.findViewById(R.id.netmask_layout);
		mDnsLayout = (LinearLayout)this.findViewById(R.id.dns_layout);
		mGatewayLayout = (LinearLayout)this.findViewById(R.id.gateway_layout);
		mPPPoEUsernameLayout = (LinearLayout)this.findViewById(R.id.pppoe_username_layout);
		mPPPoEPasswdLayout = (LinearLayout)this.findViewById(R.id.pppoe_passwd_layout);
		mDHCPUsername = (EditText)this.findViewById(R.id.dhcp_username);
		mDHCPUsername.setOnFocusChangeListener(this);
		mDHCPUsername.setEnabled(false);
		mDHCPPasswd = (EditText)this.findViewById(R.id.dhcp_passwd);
		mDHCPPasswd.setOnFocusChangeListener(this);
		mDHCPPasswd.setEnabled(false);
		mSupportIPOE = (CheckBox)this.findViewById(R.id.supportipoe);
		mSupportIPOE.setOnCheckedChangeListener(this);
		mSupportIPOE.setChecked(false);
		mIPAddress = (EditText)this.findViewById(R.id.ip);
		mIPAddress.setOnFocusChangeListener(this);
		mNetmask = (EditText)this.findViewById(R.id.netmask);
		mNetmask.setOnFocusChangeListener(this);
		mDns = (EditText)this.findViewById(R.id.dns1);
		mDns.setOnFocusChangeListener(this);
		mGateway = (EditText)this.findViewById(R.id.gateway);
		mGateway.setOnFocusChangeListener(this);
		mPPPoEUsername = (EditText)this.findViewById(R.id.pppoe_username);
		mPPPoEUsername.setOnFocusChangeListener(this);
		mPPPoEPasswd = (EditText)this.findViewById(R.id.pppoe_passwd);
		mPPPoEPasswd.setOnFocusChangeListener(this);


		mService = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mEthManager = (EthernetManager) getSystemService(Context.ETHERNET_SERVICE);
        String mode = mEthManager.getEthernetMode();
        if(mode.equals(DHCP_MODE)) {
            mSelector.setPosition(0);
            updateLinkModeUI(0);
        } else if(mode.equals(STATIC_MODE)) {
            mSelector.setPosition(1);
            updateLinkModeUI(1);
        } else if(mode.equals(PPPOE_MODE)) {
            mSelector.setPosition(2);
            updateLinkModeUI(2);
        }
		ItemSwitch mItemSwitch = new ItemSwitch() {
		    @Override
		    public void onItemSwitchPrev(int pos,String text) {
		        updateLinkModeUI(pos);
		    }
		    @Override
		    public void onItemSwitchNext(int pos,String text) {
		        updateLinkModeUI(pos);
		    }
		    @Override
		    public void onItemSwitchCenter(int pos,String text) {}
		};
		mSelector.setItemSwitch(mItemSwitch);
	}

	private void updateLinkModeUI(int mode) {
	    currentMode = mode;
		switch(mode){
		case 0:
			mDHCPLayout.setVisibility(View.VISIBLE);
			mStaticIPLayout.setVisibility(View.GONE);
			mPPPoELayout.setVisibility(View.GONE);
			break;
		case 1:
			mDHCPLayout.setVisibility(View.GONE);
			mStaticIPLayout.setVisibility(View.VISIBLE);
			mPPPoELayout.setVisibility(View.GONE);
			updateStaticUI();
			break;
		case 2:
			mDHCPLayout.setVisibility(View.GONE);
			mStaticIPLayout.setVisibility(View.GONE);
			mPPPoELayout.setVisibility(View.VISIBLE);
			updatePppoeUI();
			break;
		default:
			mDHCPLayout.setVisibility(View.VISIBLE);
			mStaticIPLayout.setVisibility(View.GONE);
			mPPPoELayout.setVisibility(View.GONE);
			break;
		}
	}

	private void updateDhcpUI() {
	    EditText mUsername = (EditText)this.findViewById(R.id.dhcp_username);
	    EditText mPassword = (EditText)this.findViewById(R.id.dhcp_passwd);

        EthernetDevInfo devInfo = mEthManager.getLoginInfo(DHCP_MODE);

        if(devInfo != null) {
            mUsername.setText(devInfo.getUsername());
            mPassword.setText(devInfo.getPasswd());
        }
	}

    private EthernetDevInfo updateDhcpConfig(EthernetDevInfo info) {
	    EditText mUsername = (EditText)this.findViewById(R.id.dhcp_username);
	    EditText mPassword = (EditText)this.findViewById(R.id.dhcp_passwd);

	    EthernetDevInfo devInfo = info;
        devInfo.setMode(EthernetManager.ETHERNET_CONNECT_MODE_DHCP);
        devInfo.setUsername(mUsername.getText().toString());
        devInfo.setPasswd(mPassword.getText().toString());
        return devInfo;
    }

	private void updateStaticUI() {
	    EditText mIp = (EditText)this.findViewById(R.id.ip);
	    EditText mNetmask = (EditText)this.findViewById(R.id.netmask);
	    EditText mDns = (EditText)this.findViewById(R.id.dns1);
	    EditText mGateway = (EditText)this.findViewById(R.id.gateway);
	    EthernetDevInfo devInfo = mEthManager.getStaticConfig();
	    if(devInfo.getIpAddress() == null) {
            devInfo = mEthManager.getDevInfo();
        }

        if(devInfo != null) {
            mIp.setText(devInfo.getIpAddress());
            mNetmask.setText(devInfo.getNetMask());
            mGateway.setText(devInfo.getGateWay());
            mDns.setText(devInfo.getDns1());
        }
	}

	private EthernetDevInfo updateStaticConfig(EthernetDevInfo info) {
	    EditText mIp = (EditText)this.findViewById(R.id.ip);
	    EditText mNetmask = (EditText)this.findViewById(R.id.netmask);
	    EditText mDns = (EditText)this.findViewById(R.id.dns1);
	    EditText mGateway = (EditText)this.findViewById(R.id.gateway);

	    EthernetDevInfo devInfo = info;
        devInfo.setMode(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL);
        devInfo.setIpAddress(mIp.getText().toString());
        devInfo.setNetMask(mNetmask.getText().toString());
        devInfo.setDns1(mDns.getText().toString());
        devInfo.setGateWay(mGateway.getText().toString());
        return devInfo;
    }

	private void updatePppoeUI() {
	    EditText mUsername = (EditText)this.findViewById(R.id.pppoe_username);
	    EditText mPassword = (EditText)this.findViewById(R.id.pppoe_passwd);

        EthernetDevInfo devInfo = mEthManager.getLoginInfo(PPPOE_MODE);

        if(devInfo != null) {
            mUsername.setText(devInfo.getUsername());
            mPassword.setText(devInfo.getPasswd());
        }
	}

    private EthernetDevInfo updatePppoeConfig(EthernetDevInfo info) {
	    EditText mUsername = (EditText)this.findViewById(R.id.pppoe_username);
	    EditText mPassword = (EditText)this.findViewById(R.id.pppoe_passwd);

	    EthernetDevInfo devInfo = info;
        devInfo.setMode(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE);
        devInfo.setUsername(mUsername.getText().toString());
        devInfo.setPasswd(mPassword.getText().toString());
        return devInfo;
    }

	@Override
	public void onFocusChange(View arg0, boolean hasFocus) {
		switch(arg0.getId()){
		case R.id.selector:
			if (hasFocus) {
				mConnectModeLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mConnectModeLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		case R.id.dhcp_username:
			if (hasFocus) {
				mDHCPUsernameLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mDHCPUsernameLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		case R.id.dhcp_passwd:
			if (hasFocus) {
				mDHCPPasswdLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mDHCPPasswdLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		case R.id.ip:
			if (hasFocus) {
				mIPAddressLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mIPAddressLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		case R.id.netmask:
			if (hasFocus) {
				mNetmaskLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mNetmaskLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		case R.id.dns1:
			if (hasFocus) {
				mDnsLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mDnsLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		case R.id.gateway:
			if (hasFocus) {
				mGatewayLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mGatewayLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		case R.id.pppoe_username:
			if (hasFocus) {
				mPPPoEUsernameLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mPPPoEUsernameLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		case R.id.pppoe_passwd:
			if (hasFocus) {
				mPPPoEPasswdLayout.setBackgroundResource(R.drawable.input_btn_focus);
			} else {
				mPPPoEPasswdLayout.setBackgroundResource(R.drawable.list_btn_nor);
			}
			break;
		default:
			break;
		}
	}
	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		if(view == mDhcpSubmit) {
		    mEthManager.setEthernetMode(DHCP_MODE, updateDhcpConfig(mEthManager.getLoginInfo(DHCP_MODE)));
		}
		else if(view == mStaticSubmit) {
		    mEthManager.setEthernetMode(STATIC_MODE, updateStaticConfig(mEthManager.getStaticConfig()));
		} else if(view == mPppoeSubmit) {
		    mEthManager.setEthernetMode(PPPOE_MODE, updatePppoeConfig(mEthManager.getLoginInfo(PPPOE_MODE)));
		}
	}
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_ethernet_nor);
			break;
		}
		return false;
	}

	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_ethernet_nor);
				finish();
			break;
		}
		return false;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Log.d(TAG,"currentMode = " + currentMode + " isChecked = " + isChecked);
		// TODO Auto-generated method stub
		if(currentMode == 0){
			if(isChecked){
				mDHCPUsernameLayout.setVisibility(View.VISIBLE);
				mDHCPPasswdLayout.setVisibility(View.VISIBLE);
			}else{
				mDHCPUsernameLayout.setVisibility(View.GONE);
				mDHCPPasswdLayout.setVisibility(View.GONE);
			}
		}
	}
}
