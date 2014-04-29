package mobi.dzs.android.BLE_SPP_PRO;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class actDiscovery extends Activity{
	/**CONST: scan device menu id*/
	public static final int MEMU_SCAN = 1;
	/**CONST: quit system*/
	public static final int MEMU_QUIT = 2;
	/**CONST: device type bltetooth 2.1*/
	public static final int DEVICE_TYPE_BREDR = 0x01;
	/**CONST: device type bltetooth 4.0 ble*/
	public static final int DEVICE_TYPE_BLE = 0x02;
	/**CONST: device type bltetooth double mode*/
	public static final int DEVICE_TYPE_DUMO = 0x03;
	
	public final static String EXTRA_DEVICE_TYPE = "android.bluetooth.device.extra.DEVICE_TYPE";
	
	/** Discovery is Finished */
	private boolean _discoveryFinished;
	
	/**手机的蓝牙适配器*/
	private BluetoothAdapter mBT = BluetoothAdapter.getDefaultAdapter();
	/**bluetooth List View*/
	private ListView mlvList = null;
	/**
	 * Storage the found bluetooth devices
	 * format:<MAC, <Key,Val>>;Key=[RSSI/NAME/COD(class od device)/BOND/UUID]
	 * */
	private Hashtable<String, Hashtable<String, String>> mhtFDS = null;
	
	/**ListView的动态数组对象(存储用于显示的列表数组)*/
	private ArrayList<HashMap<String, Object>> malListItem = null;
	/**SimpleAdapter对象(列表显示容器对象)*/
	private SimpleAdapter msaListItemAdapter = null;

	/**
	 * Scan for Bluetooth devices. (broadcast listener)
	 */
	private BroadcastReceiver _foundReceiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent){
			/* bluetooth device profiles*/
			Hashtable<String, String> htDeviceInfo = new Hashtable<String, String>();
			
			Log.d(getString(R.string.app_name), ">>Scan for Bluetooth devices");
			
			/* get the search results */
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			/* create found device profiles to htDeviceInfo*/
			Bundle b = intent.getExtras();
			htDeviceInfo.put("RSSI", String.valueOf(b.get(BluetoothDevice.EXTRA_RSSI)));
			if (null == device.getName())
				htDeviceInfo.put("NAME", "Null");
			else
				htDeviceInfo.put("NAME", device.getName());
			
			htDeviceInfo.put("COD",  String.valueOf(b.get(BluetoothDevice.EXTRA_CLASS)));
			if (device.getBondState() == BluetoothDevice.BOND_BONDED)
				htDeviceInfo.put("BOND", getString(R.string.actDiscovery_bond_bonded));
			else
				htDeviceInfo.put("BOND", getString(R.string.actDiscovery_bond_nothing));
			//TODO:内容为空
			String sDeviceType = String.valueOf(b.get(EXTRA_DEVICE_TYPE));
			if (!sDeviceType.equals("null"))
				htDeviceInfo.put("DEVICE_TYPE", sDeviceType);
			else
				htDeviceInfo.put("DEVICE_TYPE", "-1"); //不存在设备号

			/*adding scan to the device profiles*/
			mhtFDS.put(device.getAddress(), htDeviceInfo);
			
			/*Refresh show list*/
			showDevices();
		}
	};	
	/**
	 * Bluetooth scanning is finished processing.(broadcast listener)
	 */
	private BroadcastReceiver _finshedReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			Log.d(getString(R.string.app_name), ">>Bluetooth scanning is finished");
			_discoveryFinished = true; //set scan is finished
			unregisterReceiver(_foundReceiver);
			unregisterReceiver(_finshedReceiver);
			
			/* 提示用户选择需要连接的蓝牙设备 */
			if (null != mhtFDS && mhtFDS.size()>0){	//找到蓝牙设备
				Toast.makeText(actDiscovery.this, 
							   getString(R.string.actDiscovery_msg_select_device),
							   Toast.LENGTH_SHORT).show();
			}else{	//未找到蓝牙设备
				Toast.makeText(actDiscovery.this, 
						   getString(R.string.actDiscovery_msg_not_find_device),
						   Toast.LENGTH_LONG).show();
			}
		}
	};
	
	/**
	 * start run
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_discovery);
		
		this.mlvList = (ListView)this.findViewById(R.id.actDiscovery_lv);
		
    	/* 选择项目后返回给调用页面 */
    	this.mlvList.setOnItemClickListener(new OnItemClickListener(){  
            @Override  
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3){  
                String sMAC = ((TextView)arg1.findViewById(R.id.device_item_ble_mac)).getText().toString();
        		Intent result = new Intent();
        		result.putExtra("MAC", sMAC);
        		result.putExtra("RSSI", mhtFDS.get(sMAC).get("RSSI"));
        		result.putExtra("NAME", mhtFDS.get(sMAC).get("NAME"));
        		result.putExtra("COD", mhtFDS.get(sMAC).get("COD"));
        		result.putExtra("BOND", mhtFDS.get(sMAC).get("BOND"));
        		result.putExtra("DEVICE_TYPE", toDeviceTypeString(mhtFDS.get(sMAC).get("DEVICE_TYPE")));
        		setResult(Activity.RESULT_OK, result);
        		finish();
            }  
        });
    	//立即启动扫描线程
		new scanDeviceTask().execute("");
	}
	
	/**
	 * add top menu
	 * */
	@Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        MenuItem miScan = menu.add(0, MEMU_SCAN, 0, getString(R.string.actDiscovery_menu_scan));
        MenuItem miClose = menu.add(0, MEMU_QUIT, 1, getString(R.string.menu_close));
        miScan.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        miClose.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }
	
	/**
	 * 菜单点击后的执行指令
	 * */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {  
        switch(item.getItemId()){  
	        case MEMU_SCAN: //开始扫描
	        	new scanDeviceTask().execute("");
	        	return true;
	        case MEMU_QUIT: //结束程序
	        	this.setResult(Activity.RESULT_CANCELED, null);
	        	this.finish();
	        	return true;
	        default:
	        	return super.onMenuItemSelected(featureId, item);
        }
    }
	
	/**
	 * 析构处理
	 *   退出时，强制终止搜索
	 * */
	@Override
	protected void onDestroy(){
		super.onDestroy();
		
		if (mBT.isDiscovering())
			mBT.cancelDiscovery();
	}
	
	/**
	 * 开始扫描周围的蓝牙设备<br/>
	 *  备注:进入这步前必须保证蓝牙设备已经被启动
	 *  @return void
	 * */
	private void startSearch(){
		_discoveryFinished = false; //标记搜索未结束
		
		//如果找到的设别对象为空，则创建这个对象。
		if (null == mhtFDS)
			this.mhtFDS = new Hashtable<String, Hashtable<String, String>>();
		else
			this.mhtFDS.clear();
		
		/* Register Receiver*/
		IntentFilter discoveryFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(_finshedReceiver, discoveryFilter);
		IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(_foundReceiver, foundFilter);
		mBT.startDiscovery();//start scan

		this.showDevices(); //the first scan clear show list
	}
	
	/**
	 * 将设备类型ID，转换成设备解释字符串
	 * @return String
	 * */
	private String toDeviceTypeString(String sDeviceTypeId){
		Pattern pt = Pattern.compile("^[-\\+]?[\\d]+$");
		if (pt.matcher(sDeviceTypeId).matches()){
	        switch(Integer.valueOf(sDeviceTypeId)){
	        	case DEVICE_TYPE_BREDR:
	        		return getString(R.string.device_type_bredr);
	        	case DEVICE_TYPE_BLE:
	        		return getString(R.string.device_type_ble);
	        	case DEVICE_TYPE_DUMO:
	        		return getString(R.string.device_type_dumo);
	        	default: //默认为蓝牙2.0
	        		return getString(R.string.device_type_bredr);
	        }
		}
		else
			return sDeviceTypeId; //如果不是数字，则直接输出
	}

	/* Show devices list */
	protected void showDevices(){
		if (null == this.malListItem) //数组容器不存在时，创建
			this.malListItem = new ArrayList<HashMap<String, Object>>();
		
		//如果列表适配器未创建则创建之
        if (null == this.msaListItemAdapter){
	        //生成适配器的Item和动态数组对应的元素  
	        this.msaListItemAdapter = new SimpleAdapter(this,malListItem,//数据源   
	            R.layout.list_view_item_devices,//ListItem的XML实现  
	            //动态数组与ImageItem对应的子项          
	            new String[] {"NAME","MAC", "COD", "RSSI", "DEVICE_TYPE", "BOND"},   
	            //ImageItem的XML文件里面的一个ImageView,两个TextView ID  
	            new int[] {R.id.device_item_ble_name,
	        			   R.id.device_item_ble_mac,
	        			   R.id.device_item_ble_cod,
	        			   R.id.device_item_ble_rssi,
	        			   R.id.device_item_ble_device_type,
	        			   R.id.device_item_ble_bond
	        			  }
	        );
	      	//添加并且显示  
	    	this.mlvList.setAdapter(this.msaListItemAdapter);
        }
        
		//构造适配器的数据
        this.malListItem.clear();//清除历史项
        Enumeration<String> e = this.mhtFDS.keys();
        /*重新构造数据*/
        while (e.hasMoreElements()){
            HashMap<String, Object> map = new HashMap<String, Object>();
            String sKey = e.nextElement();
            map.put("MAC", sKey);
            map.put("NAME", this.mhtFDS.get(sKey).get("NAME"));
            map.put("RSSI", this.mhtFDS.get(sKey).get("RSSI"));
            map.put("COD", this.mhtFDS.get(sKey).get("COD"));
            map.put("BOND", this.mhtFDS.get(sKey).get("BOND"));
            map.put("DEVICE_TYPE", toDeviceTypeString(this.mhtFDS.get(sKey).get("DEVICE_TYPE")));
            this.malListItem.add(map);
        }
		this.msaListItemAdapter.notifyDataSetChanged(); //通知适配器内容发生变化自动跟新
	}
    
    //----------------
    /*多线程处理:设备扫描监管线程*/
    private class scanDeviceTask extends AsyncTask<String, String, Integer>{
    	/**常量:蓝牙未开启*/
    	private static final int RET_BLUETOOTH_NOT_START = 0x0001;
    	/**常量:设备搜索完成*/
    	private static final int RET_SCAN_DEVICE_FINISHED = 0x0002;
    	/**等待蓝牙设备启动的最长时间(单位S)*/
    	private static final int miWATI_TIME = 10;
    	/**每次线程休眠时间(单位ms)*/
    	private static final int miSLEEP_TIME = 150;
    	/**进程等待提示框*/
    	private ProgressDialog mpd = null;
    	
		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute(){
	    	/*定义进程对话框*/
			this.mpd = new ProgressDialog(actDiscovery.this);
			this.mpd.setMessage(getString(R.string.actDiscovery_msg_scaning_device));
			this.mpd.setCancelable(true);//可被终止
			this.mpd.setCanceledOnTouchOutside(true);//点击外部可终止
			this.mpd.setOnCancelListener(new DialogInterface.OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog){	//按下取消按钮后，终止搜索等待线程
					_discoveryFinished = true;
				}
			});
			this.mpd.show();
			
			startSearch(); //执行蓝牙扫描
		}
		
		@Override
		protected Integer doInBackground(String... params){
			if (!mBT.isEnabled()) //蓝牙未启动
				return RET_BLUETOOTH_NOT_START;
			
			int iWait = miWATI_TIME * 1000;//倒减计数器
			//等待miSLEEP_TIME秒，启动蓝牙设备后再开始扫描
			while(iWait > 0){
				if (_discoveryFinished)
					return RET_SCAN_DEVICE_FINISHED; //蓝牙搜索结束
				else
					iWait -= miSLEEP_TIME; //剩余等待时间计时
				SystemClock.sleep(miSLEEP_TIME);;
			}
			return RET_SCAN_DEVICE_FINISHED; //在规定时间内，蓝牙设备未启动
		}
		/**
		 * 线程内更新处理
		 */
		@Override
		public void onProgressUpdate(String... progress){
		}
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result){
			if (this.mpd.isShowing())
				this.mpd.dismiss();//关闭等待对话框
			
			if (mBT.isDiscovering())
				mBT.cancelDiscovery();
			
			if (RET_SCAN_DEVICE_FINISHED == result){//蓝牙设备搜索结束
				
			}else if (RET_BLUETOOTH_NOT_START == result){	//提示蓝牙未启动
				Toast.makeText(actDiscovery.this, getString(R.string.actDiscovery_msg_bluetooth_not_start), 
	 					   Toast.LENGTH_SHORT).show();
			}
		}
    }
}

