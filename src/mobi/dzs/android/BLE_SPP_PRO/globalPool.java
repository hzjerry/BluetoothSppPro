package mobi.dzs.android.BLE_SPP_PRO;

import mobi.dzs.android.bluetooth.BluetoothSppClient;
import mobi.dzs.android.util.DynamicStorage;
import android.app.Application;

public class globalPool extends Application
{
	/**蓝牙SPP通信连接对象*/
	public BluetoothSppClient mBSC = null;
	/**动态公共存储对象*/
	public DynamicStorage mDS = null;
	/**
	 * 覆盖构造
	 * */
	@Override
	public void onCreate()
	{
		this.mDS = new DynamicStorage(this);
	}
	
	/**
	 * 建立蓝牙连接
	 * @param String sMac 蓝牙硬件地址
	 * @return boolean
	 * */
	public boolean createConn(String sMac)
	{
		if (null == this.mBSC)
		{
			this.mBSC = new BluetoothSppClient(sMac);
			if (this.mBSC.createConn())
				return true;
			else
			{
				this.mBSC = null;
				return false;
			}
		}
		else
			return true;
	}
	
	/**
	 * 关闭并释放连接
	 * @return void
	 * */
	public void closeConn()
	{
		if (null != this.mBSC)
		{
			this.mBSC.closeConn();
			this.mBSC = null;
		}
	}
}
