package mobi.dzs.android.BLE_SPP_PRO;

import java.util.ArrayList;
import java.util.Hashtable;

import mobi.dzs.android.bluetooth.BluetoothCtrl;
import mobi.dzs.android.storage.CKVStorage;
import mobi.dzs.android.storage.CSharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * 主界面<br />
 * 维护蓝牙的连接与通信操作，首先进入后检查蓝牙状态，没有启动则开启蓝牙，然后立即进入搜索界面。<br/>
 * 得到需要连接的设备后，在主界面中建立配对与连接，蓝牙对象被保存在globalPool中，以便其他的不同通信模式的功能模块调用。
 * @author JerryLi
 *
 */
public class actMain extends Activity{
	/**CONST: scan device menu id*/
	public static final byte MEMU_RESCAN = 0x01;
	/**CONST: exit application*/
	public static final byte MEMU_EXIT = 0x02;
	/**CONST: about me*/
	public static final byte MEMU_ABOUT = 0x03;
	/**全局静态对象池*/
	private globalPool mGP = null;
	/**手机的蓝牙适配器*/
	private BluetoothAdapter mBT = BluetoothAdapter.getDefaultAdapter();
	/**蓝牙设备连接句柄*/
	private BluetoothDevice mBDevice = null;
	/**控件:Device Info显示区*/
	private TextView mtvDeviceInfo = null;
	/**控件:Service UUID显示区*/
	private TextView mtvServiceUUID = null;
	/**控件:设备信息显示区容器*/
	private LinearLayout mllDeviceCtrl = null;
	/**控件:选择连接成功后的设备通信模式面板*/
	private LinearLayout mllChooseMode = null;
	/**控件:配对按钮*/
	private Button mbtnPair = null;
	/**控件:通信按钮*/
	private Button mbtnComm = null;
	/**常量:搜索页面返回*/
	public static final byte REQUEST_DISCOVERY = 0x01;
	/**常量:从字符流模式返回*/
	public static final byte REQUEST_BYTE_STREAM = 0x02;
	/**常量:从命令行模式返回*/
	public static final byte REQUEST_CMD_LINE = 0x03;
	/**常量:从键盘模式返回*/
	public static final byte REQUEST_KEY_BOARD = 0x04;
	/**常量:从关于页面返回*/
	public static final byte REQUEST_ABOUT = 0x05;
	/**选定设备的配置信息*/
	private Hashtable<String, String> mhtDeviceInfo = new Hashtable<String, String>();
	/**蓝牙配对进程操作标志*/
	private boolean mbBonded = false;
	/**获取到的UUID Service 列表信息*/
	private ArrayList<String> mslUuidList = new ArrayList<String>();
	/**保存蓝牙进入前的开启状态*/
	private boolean mbBleStatusBefore = false;
	/**广播监听:获取UUID服务*/
	private BroadcastReceiver _mGetUuidServiceReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent intent){
			String action = intent.getAction();
			int iLoop = 0;
			if (BluetoothDevice.ACTION_UUID.equals(action)){
				Parcelable[] uuidExtra = 
					intent.getParcelableArrayExtra("android.bluetooth.device.extra.UUID");
				if (null != uuidExtra)
					iLoop = uuidExtra.length;
				/*uuidExtra should contain my service's UUID among his files, but it doesn't!!*/
				for(int i=0; i<iLoop; i++)
					mslUuidList.add(uuidExtra[i].toString());
			}
		}
	};
	/** 广播监听:蓝牙配对处理 */
	private BroadcastReceiver _mPairingRequest = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			BluetoothDevice device = null;
			if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){	//配对状态改变时，的广播处理
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() == BluetoothDevice.BOND_BONDED)
					mbBonded = true;//蓝牙配对设置成功
				else
					mbBonded = false;//蓝牙配对进行中或者配对失败
			}
		}
	};
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu)
//	{
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.act_main, menu);
//		return true;
//	}
	
	/**
	 * add top menu
	 * */
	@Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        //扫描设备
        MenuItem miScan = menu.add(0, MEMU_RESCAN, 0, getString(R.string.actMain_menu_rescan));
        miScan.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        //进入关于页面
        MenuItem miAbout = menu.add(0, MEMU_ABOUT, 1, getString(R.string.menu_about));
        miAbout.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //退出系统
        MenuItem miExit = menu.add(0, MEMU_EXIT, 2, getString(R.string.menu_close));
        miExit.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return super.onCreateOptionsMenu(menu);
    }
	
	/**
	 * 菜单点击后的执行指令
	 * */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {  
        switch(item.getItemId()) {  
	        case MEMU_RESCAN: //开始扫描
	        	this.mGP.closeConn();//关闭连接
	        	this.initActivityView(); //进入扫描时，显示界面初始化
	        	this.openDiscovery(); //进入搜索页面
	        	return true;
	        case MEMU_EXIT: //退出程序
	        	this.finish();
	        	return true;
	        case MEMU_ABOUT: //打开关于页面
	        	this.openAbout();
	        	return true;
	        default:
	        	return super.onMenuItemSelected(featureId, item);
        }
    }
	
	/**
	 * 页面构造
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_main);
		
		if (null == mBT){ //系统中不存在蓝牙模块
			Toast.makeText(this, "Bluetooth module not found", Toast.LENGTH_LONG).show();
			this.finish();
		}
		this.initFirstInstallTimestemp(); //记录首次安装的时间

		this.mtvDeviceInfo = (TextView)this.findViewById(R.id.actMain_tv_device_info);
		this.mtvServiceUUID = (TextView)this.findViewById(R.id.actMain_tv_service_uuid);
		this.mllDeviceCtrl = (LinearLayout)this.findViewById(R.id.actMain_ll_device_ctrl);
		this.mllChooseMode = (LinearLayout)this.findViewById(R.id.actMain_ll_choose_mode);
		this.mbtnPair = (Button)this.findViewById(R.id.actMain_btn_pair);
		this.mbtnComm = (Button)this.findViewById(R.id.actMain_btn_conn);
		
		this.initActivityView(); //初始化窗口控件的视图
		
		this.mGP = ((globalPool)this.getApplicationContext()); //得到全局对象的引用
		
		new startBluetoothDeviceTask().execute(""); //启动蓝牙设备
	}
	
	/**
	 * 初始化首次安装程序的时间
	 */
	private void initFirstInstallTimestemp(){
		CKVStorage oDS = new CSharedPreferences(this);
		if (oDS.getLongVal("SYSTEM", "FIRST_INSTALL_TIMESTEMP") == 0){
			oDS.setVal("SYSTEM", "FIRST_INSTALL_TIMESTEMP", System.currentTimeMillis()).saveStorage();
		}
	}
	
	/**
	 * 初始化显示界面的控件
	 * @return void
	 * */
	private void initActivityView(){
		this.mllDeviceCtrl.setVisibility(View.GONE); //隐藏 扫描到的设备信息
		this.mbtnPair.setVisibility(View.GONE); //隐藏 配对按钮
		this.mbtnComm.setVisibility(View.GONE); //隐藏 连接按钮
		this.mllChooseMode.setVisibility(View.GONE); //隐藏 通信模式选择
	}
	
	/**
	 * 析构处理
	 * */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		this.mGP.closeConn();//关闭连接
		
		//检查如果进入前蓝牙是关闭的状态，则退出时关闭蓝牙
		if (null != mBT && !this.mbBleStatusBefore)
			mBT.disable();
	}

	/**
	 * 进入搜索蓝牙设备列表页面
	 * */
	private void openDiscovery(){
		//进入蓝牙设备搜索界面
		Intent intent = new Intent(this, actDiscovery.class);
		this.startActivityForResult(intent, REQUEST_DISCOVERY); //等待返回搜索结果
	}
	
	/**
	 * 进入关于页面
	 * */
	private void openAbout(){
		Intent intent = new Intent(this, actAbout.class);
		this.startActivityForResult(intent, REQUEST_ABOUT); //等待返回搜索结果
	}
	
	/**
	 * 显示选中这被的信息
	 * @return void
	 * */
	private void showDeviceInfo(){
		/*显示需要连接的设备信息*/
		this.mtvDeviceInfo.setText(
			String.format(getString(R.string.actMain_device_info), 
				this.mhtDeviceInfo.get("NAME"),
				this.mhtDeviceInfo.get("MAC"),
				this.mhtDeviceInfo.get("COD"),
				this.mhtDeviceInfo.get("RSSI"),
				this.mhtDeviceInfo.get("DEVICE_TYPE"),
				this.mhtDeviceInfo.get("BOND"))
		);
	}
	/**
	 * 显示Service UUID信息
	 * @return void
	 * */
	private void showServiceUUIDs(){
		//对于4.0.3以上的系统支持获取UUID服务内容的操作
		if (Build.VERSION.SDK_INT >= 15){
			new GetUUIDServiceTask().execute("");
		}else{	//不支持获取uuid service信息
			this.mtvServiceUUID.setText(getString(R.string.actMain_msg_does_not_support_uuid_service));
		}
	}
	
	/**
	 * 蓝牙设备选择完后返回处理
	 * */
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (requestCode == REQUEST_DISCOVERY){
			if (Activity.RESULT_OK == resultCode){
				this.mllDeviceCtrl.setVisibility(View.VISIBLE); //显示设备信息区

				this.mhtDeviceInfo.put("NAME", data.getStringExtra("NAME"));
				this.mhtDeviceInfo.put("MAC", data.getStringExtra("MAC"));
				this.mhtDeviceInfo.put("COD", data.getStringExtra("COD"));
				this.mhtDeviceInfo.put("RSSI", data.getStringExtra("RSSI"));
				this.mhtDeviceInfo.put("DEVICE_TYPE", data.getStringExtra("DEVICE_TYPE"));
				this.mhtDeviceInfo.put("BOND", data.getStringExtra("BOND"));
				
				this.showDeviceInfo();//显示设备信息
				
				//如果设备未配对，显示配对操作
				if (this.mhtDeviceInfo.get("BOND").equals(getString(R.string.actDiscovery_bond_nothing))){
					this.mbtnPair.setVisibility(View.VISIBLE); //显示配对按钮
					this.mbtnComm.setVisibility(View.GONE); //隐藏通信按钮
					//提示要显示Service UUID先建立配对
					this.mtvServiceUUID.setText(getString(R.string.actMain_tv_hint_service_uuid_not_bond));
				}else{
					//已存在配对关系，建立与远程设备的连接
					this.mBDevice = this.mBT.getRemoteDevice(this.mhtDeviceInfo.get("MAC"));
					this.showServiceUUIDs();//显示设备的Service UUID列表
					this.mbtnPair.setVisibility(View.GONE); //隐藏配对按钮
					this.mbtnComm.setVisibility(View.VISIBLE); //显示通信按钮
				}
			}else if (Activity.RESULT_CANCELED == resultCode){
				//未操作，结束程序
				this.finish();
			}
		}
		else if (REQUEST_BYTE_STREAM == requestCode || REQUEST_CMD_LINE == requestCode ||
				 REQUEST_KEY_BOARD == requestCode)
		{	//从通信模式返回的处理
			if (null == this.mGP.mBSC || !this.mGP.mBSC.isConnect()){	//通信连接丢失，重新连接
				this.mllChooseMode.setVisibility(View.GONE); //隐藏 通信模式选择
				this.mbtnComm.setVisibility(View.VISIBLE); //显示 建立通信按钮
				this.mGP.closeConn();//释放连接对象
				Toast.makeText(this, //提示连接丢失
				   getString(R.string.msg_msg_bt_connect_lost),
				   Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	/**
	 * 配对按钮的单击事件
	 * @return void
	 * */
	public void onClickBtnPair(View v){
		new PairTask().execute(this.mhtDeviceInfo.get("MAC"));
		this.mbtnPair.setEnabled(false); //冻结配对按钮
	}

    /**
     * 建立设备的串行通信连接
     * 建立成功后出现通信模式的选择按钮
     * @return void
     * */
	public void onClickBtnConn(View v){
		new connSocketTask().execute(this.mBDevice.getAddress());
    }
	
	/**
	 * 通信模式选择-串行流模式
	 * @return void
	 * */
	public void onClickBtnSerialStreamMode(View v){
		//进入串行流模式
		Intent intent = new Intent(this, actByteStream.class);
		this.startActivityForResult(intent, REQUEST_BYTE_STREAM); //等待返回搜索结果
	}
	
	/**
	 * 通信模式选择-键盘模式
	 * @return void
	 * */
	public void onClickBtnKeyBoardMode(View v){
		//进入键盘模式界面
		Intent intent = new Intent(this, actKeyBoard.class);
		this.startActivityForResult(intent, REQUEST_KEY_BOARD); //等待返回搜索结果
	}
	
	/**
	 * 通信模式选择-命令行模式
	 * @return void
	 * */
	public void onClickBtnCommandLine(View v){
		//进入命令行模式界面
		Intent intent = new Intent(this, actCmdLine.class);
		this.startActivityForResult(intent, REQUEST_CMD_LINE); //等待返回搜索结果
	}

    //----------------
    /*多线程处理(开机时启动蓝牙)*/
    private class startBluetoothDeviceTask extends AsyncTask<String, String, Integer>{
    	/**常量:蓝牙已经启动*/
    	private static final int RET_BULETOOTH_IS_START = 0x0001;
    	/**常量:设备启动失败*/
    	private static final int RET_BLUETOOTH_START_FAIL = 0x04;
    	/**等待蓝牙设备启动的最长时间(单位S)*/
    	private static final int miWATI_TIME = 15;
    	/**每次线程休眠时间(单位ms)*/
    	private static final int miSLEEP_TIME = 150;
    	/**进程等待提示框*/
    	private ProgressDialog mpd;
    	
		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute(){
	    	/*定义进程对话框*/
			mpd = new ProgressDialog(actMain.this);
			mpd.setMessage(getString(R.string.actDiscovery_msg_starting_device));//蓝牙启动中
			mpd.setCancelable(false);//不可被终止
			mpd.setCanceledOnTouchOutside(false);//点击外部不可终止
			mpd.show();
			mbBleStatusBefore = mBT.isEnabled(); //保存进入前的蓝牙状态
		}
	
		/**异步的方式启动蓝牙，如果蓝牙已经启动则直接进入扫描模式*/
		@Override
		protected Integer doInBackground(String... arg0){
			int iWait = miWATI_TIME * 1000;//倒减计数器
			/* BT isEnable */
			if (!mBT.isEnabled()){
				mBT.enable(); //启动蓝牙设备
				//等待miSLEEP_TIME秒，启动蓝牙设备后再开始扫描
				while(iWait > 0){
					if (!mBT.isEnabled())
						iWait -= miSLEEP_TIME; //剩余等待时间计时
					else
						break; //启动成功跳出循环
					SystemClock.sleep(miSLEEP_TIME);
				}
				if (iWait < 0) //表示在规定时间内,蓝牙设备未启动
					return RET_BLUETOOTH_START_FAIL;
			}
			return RET_BULETOOTH_IS_START;
		}
			
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result){
			if (mpd.isShowing())
				mpd.dismiss();//关闭等待对话框
			
			if (RET_BLUETOOTH_START_FAIL == result){	//蓝牙设备启动失败
				AlertDialog.Builder builder = new AlertDialog.Builder(actMain.this); //对话框控件
    	    	builder.setTitle(getString(R.string.dialog_title_sys_err));//设置标题
    	    	builder.setMessage(getString(R.string.actDiscovery_msg_start_bluetooth_fail));
    	    	builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener(){
    	            @Override
    	            public void onClick(DialogInterface dialog, int which){
    	            	mBT.disable();
    	            	//蓝牙设备无法启动，直接终止程序
    	            	finish();
    	            }
    	    	}); 
    	    	builder.create().show();
			}
			else if (RET_BULETOOTH_IS_START == result){	//蓝牙启动成功
				openDiscovery(); //进入搜索页面
			}
		}
    }
    
    //----------------
    /*多线程处理(配对处理线程)*/
    private class PairTask extends AsyncTask<String, String, Integer>{
		/**常量:配对成功*/
		static private final int RET_BOND_OK = 0x00;
		/**常量: 配对失败*/
		static private final int RET_BOND_FAIL = 0x01;
		/**常量: 配对等待时间(10秒)*/
		static private final int iTIMEOUT = 1000 * 10; 
		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute(){
    		//提示开始建立配对
			Toast.makeText(actMain.this, 
					   getString(R.string.actMain_msg_bluetooth_Bonding),
					   Toast.LENGTH_SHORT).show();
    		/*蓝牙自动配对*/
    		//监控蓝牙配对请求
    		registerReceiver(_mPairingRequest, new IntentFilter(BluetoothCtrl.PAIRING_REQUEST));
    		//监控蓝牙配对是否成功
    		registerReceiver(_mPairingRequest, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
		}
		
		@Override
		protected Integer doInBackground(String... arg0){
    		final int iStepTime = 150;
    		int iWait = iTIMEOUT; //设定超时等待时间
    		try{	//开始配对
    			//获得远端蓝牙设备
    			mBDevice = mBT.getRemoteDevice(arg0[0]);
				BluetoothCtrl.createBond(mBDevice);
				mbBonded = false; //初始化配对完成标志
			}catch (Exception e1){	//配对启动失败
				Log.d(getString(R.string.app_name), "create Bond failed!");
				e1.printStackTrace();
				return RET_BOND_FAIL;
			}
			while(!mbBonded && iWait > 0){
				SystemClock.sleep(iStepTime);
				iWait -= iStepTime;
			}
			return (int) ((iWait > 0)? RET_BOND_OK : RET_BOND_FAIL);
		}
    	
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result){
			unregisterReceiver(_mPairingRequest); //注销监听
			
        	if (RET_BOND_OK == result){//配对建立成功
				Toast.makeText(actMain.this, 
						   getString(R.string.actMain_msg_bluetooth_Bond_Success),
						   Toast.LENGTH_SHORT).show();
				mbtnPair.setVisibility(View.GONE); //隐藏配对按钮
				mbtnComm.setVisibility(View.VISIBLE); //显示通信按钮
				mhtDeviceInfo.put("BOND", getString(R.string.actDiscovery_bond_bonded));//显示已绑定
				showDeviceInfo();//刷新配置信息
				showServiceUUIDs();//显示远程设备提供的服务
        	}else{	//在指定时间内未完成配对
				Toast.makeText(actMain.this, 
						   getString(R.string.actMain_msg_bluetooth_Bond_fail),
						   Toast.LENGTH_LONG).show();
				try{
					BluetoothCtrl.removeBond(mBDevice);
				}catch (Exception e){
					Log.d(getString(R.string.app_name), "removeBond failed!");
					e.printStackTrace();
				}
				mbtnPair.setEnabled(true); //解冻配对按钮
        	}
		}
    }
    
    //----------------
    /*多线程处理(读取UUID Service信息线程)*/
    private class GetUUIDServiceTask extends AsyncTask<String, String, Integer>{
    	/**延时等待时间*/
    	private static final int miWATI_TIME = 4 * 1000;
    	/**每次检测的时间*/
    	private static final int miREF_TIME = 200;
    	/**uuis find service is run*/
    	private boolean mbFindServiceIsRun = false;
		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute(){
			mslUuidList.clear();
			//提示UUID服务搜索中
			mtvServiceUUID.setText(getString(R.string.actMain_find_service_uuids));
			// Don't forget to unregister during onDestroy
			registerReceiver(_mGetUuidServiceReceiver,
							 new IntentFilter(BluetoothDevice.ACTION_UUID));// Register the BroadcastReceiver
			this.mbFindServiceIsRun = mBDevice.fetchUuidsWithSdp();
		}
		
		/**
		 * 线程异步处理
		 */
		@Override
		protected Integer doInBackground(String... arg0){
			int iWait = miWATI_TIME;//倒减计数器
			
			if (!this.mbFindServiceIsRun)
				return null; //UUID Service扫瞄服务器启动失败
			
			while(iWait > 0){
				if (mslUuidList.size() > 0 && iWait > 1500)
					iWait = 1500; //如果找到了第一个UUID则继续搜索N秒后结束
				SystemClock.sleep(miREF_TIME);
				iWait -= miREF_TIME;//每次循环减去刷新时间
			}
			return null;
		}    	
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result){
			StringBuilder sbTmp = new StringBuilder();
			unregisterReceiver(_mGetUuidServiceReceiver); //注销广播监听
			//如果存在数据，则自动刷新
			if (mslUuidList.size() > 0){
				for(int i=0; i<mslUuidList.size(); i++)
					sbTmp.append(mslUuidList.get(i) + "\n");
				mtvServiceUUID.setText(sbTmp.toString());
			}else//未发现UUIS服务列表
				mtvServiceUUID.setText(R.string.actMain_not_find_service_uuids);
		}
    }
    
    //----------------
    /*多线程处理(建立蓝牙设备的串行通信连接)*/
    private class connSocketTask extends AsyncTask<String, String, Integer>{
    	/**进程等待提示框*/
    	private ProgressDialog mpd = null;
    	/**常量:连接建立失败*/
    	private static final int CONN_FAIL = 0x01;
    	/**常量:连接建立成功*/
    	private static final int CONN_SUCCESS = 0x02;
    	
		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute(){
	    	/*定义进程对话框*/
			this.mpd = new ProgressDialog(actMain.this);
			this.mpd.setMessage(getString(R.string.actMain_msg_device_connecting));
			this.mpd.setCancelable(false);//可被终止
			this.mpd.setCanceledOnTouchOutside(false);//点击外部可终止
			this.mpd.show();
		}
		
		@Override
		protected Integer doInBackground(String... arg0){
			if (mGP.createConn(arg0[0]))
				return CONN_SUCCESS; //建立成功
			else
				return CONN_FAIL; //建立失败
		}
    	
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result){
			this.mpd.dismiss();
			
			if (CONN_SUCCESS == result){	//通信连接建立成功
				mbtnComm.setVisibility(View.GONE); //隐藏 建立通信按钮
				mllChooseMode.setVisibility(View.VISIBLE); //显示通信模式控制面板
				Toast.makeText(actMain.this, 
						   getString(R.string.actMain_msg_device_connect_succes),
						   Toast.LENGTH_SHORT).show();
			}else{	//通信连接建立失败
				Toast.makeText(actMain.this, 
						   getString(R.string.actMain_msg_device_connect_fail),
						   Toast.LENGTH_SHORT).show();
			}
		}
    }
}
