package mobi.dzs.android.BLE_SPP_PRO;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mobi.dzs.android.bluetooth.BluetoothSppClient;
import mobi.dzs.android.util.DynamicStorage;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.TextView;

public class BaseCommActivity extends BaseActivity
{
	/**常量:菜单变量-清屏*/
	protected final static byte MEMU_CLEAR = 0x01;
	/**常量:菜单变量-IO模式设置*/
	protected final static byte MEMU_IO_MODE = 0x02;
	/**常量:菜单变量-保存到文件*/
	protected final static byte MEMU_SAVE_TO_FILE = 0x03;	
	/**常量:菜单变量-清除历史命令*/
	protected final static byte MEMU_CLEAR_CMD_HISTORY = 0x04;	
	/**常量:菜单变量-加载使用向导*/
	protected final static byte MEMU_HELPER = 0x05;	
	/**常量:动态存储存储对象的Key; subkey:input_mode/output_mode*/
	protected final static String KEY_IO_MODE = "key_io_mode";
	/**常量:结束符字符集*/
	protected final static String[] msEND_FLGS = {"\r\n", "\n"};
	
	/**常量:历史发送命令字符串分隔符(将命令历史保存到字符串中，使用这个分隔符进行数组切割)*/
	protected static final String HISTORY_SPLIT = "&#&";
	/**常量:历史发送命令字符保存关键字*/
	protected static final String KEY_HISTORY = "send_history";	
	/**输入自动完成列表*/
	protected ArrayList<String> malCmdHistory = new ArrayList<String>();
	
	/**线程终止标志(用于终止监听线程)*/
	protected boolean mbThreadStop = false;
	
	/**控件:发送数据量*/
	private TextView mtvTxdCount = null;
	/**控件:接收数据量*/
	private TextView mtvRxdCount = null;
	/**控件:连接保持时间*/
	private TextView mtvHoleRun = null;
	
	/** 输入模式 */
	protected byte mbtInputMode = BluetoothSppClient.IO_MODE_STRING;
	/** 输出模式 */
	protected byte mbtOutputMode = BluetoothSppClient.IO_MODE_STRING;
	
	/**对象:引用全局的蓝牙连接对象*/
	protected BluetoothSppClient mBSC = null;
	/**对象:引用全局的动态存储对象*/
	protected DynamicStorage mDS = null;
	
	/**未设限制的AsyncTask线程池(重要)*/
	protected static ExecutorService FULL_TASK_EXECUTOR;
	static{
		FULL_TASK_EXECUTOR = (ExecutorService) Executors.newCachedThreadPool();
	};
	
	/**
	 * 页面构造
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		this.mBSC = ((globalPool)this.getApplicationContext()).mBSC;
		this.mDS =  ((globalPool)this.getApplicationContext()).mDS;
		
		if (null == this.mBSC && !this.mBSC.isConnect()){	//当进入时，发现连接已丢失，则直接返回主界面
        	this.setResult(Activity.RESULT_CANCELED); //返回到主界面
        	this.finish();
        	return;
		}
	}
	
	/**
	 * 启用数据统计状态条
	 * @return void
	 * @see 使用时需要在布局文件中加入以下内容:<br/>
	    &lt;include <br/>
	        android:layout_width="match_parent"<br/>
	        android:layout_height="wrap_content"<br/>
	        layout="@layout/bar_data_count" /&gt;
	 * */
	protected void usedDataCount(){	//获取数据统计条
		this.mtvTxdCount = (TextView)this.findViewById(R.id.tv_txd_count);
		this.mtvRxdCount = (TextView)this.findViewById(R.id.tv_rxd_count);
		this.mtvHoleRun = (TextView)this.findViewById(R.id.tv_connect_hold_time);
		this.refreshTxdCount();
		this.refreshRxdCount();
	}
	
    /**
     * 刷新数据统计状态条-发送统计值<br/>
     *  备注：同时会刷新运行时间显示值
     * @return void
     * @see 必须使用 usedDataCount()以后才能使用这个函数
     * */
	protected void refreshTxdCount(){
		long lTmp = 0;
		if (null != this.mtvTxdCount)
		{
			lTmp = this.mBSC.getTxd();
	    	this.mtvTxdCount.setText(String.format(getString(R.string.templet_txd, lTmp)));
	    	lTmp = this.mBSC.getConnectHoldTime();
	    	this.mtvHoleRun.setText(String.format(getString(R.string.templet_hold_time, lTmp)));
		}
    }
	
    /**
     * 刷新数据统计状态条-接收统计值
     * @return void
     * @see 必须使用 usedDataCount()以后才能使用这个函数
     * */
	protected void refreshRxdCount(){
		long lTmp = 0;
		if (null != this.mtvRxdCount)
		{
			lTmp = this.mBSC.getRxd();
	    	this.mtvRxdCount.setText(String.format(getString(R.string.templet_rxd, lTmp)));
	    	lTmp = this.mBSC.getConnectHoldTime();
	    	this.mtvHoleRun.setText(String.format(getString(R.string.templet_hold_time, lTmp)));
		}
    }
	
    /**
     * 刷新数据统计状态条-运行时间<br/>
     *  备注：同时会刷新运行时间显示值
     * @return void
     * @see 必须使用 usedDataCount()以后才能使用这个函数
     * */
	protected void refreshHoldTime(){
		if (null != this.mtvHoleRun)
		{
			long lTmp = this.mBSC.getConnectHoldTime();
	    	this.mtvHoleRun.setText(String.format(getString(R.string.templet_hold_time, lTmp)));
		}
    }
	
	/**
	 * 初始化输入输出模式
	 * @return void
	 * */
	protected void initIO_Mode(){
		this.mbtInputMode = (byte)this.mDS.getIntVal(KEY_IO_MODE, "input_mode");
		if (this.mbtInputMode == 0)
			this.mbtInputMode = BluetoothSppClient.IO_MODE_STRING;
		
		this.mbtOutputMode = (byte)this.mDS.getIntVal(KEY_IO_MODE, "output_mode");
		if (this.mbtOutputMode == 0)
			this.mbtOutputMode = BluetoothSppClient.IO_MODE_STRING;
    	mBSC.setRxdMode(mbtInputMode);
    	mBSC.setTxdMode(mbtOutputMode);
	}
	
    /**
     * 设置输入输出模式（对话框）<br/>
     * 字符显示模式 IO_MODE_HEX / IO_MODE_STRING
     * @return void
     * */
	protected void setIOModeDialog(){
    	final RadioButton rbInChar, rbInHex;
    	final RadioButton rbOutChar, rbOutHex;

    	AlertDialog.Builder builder = new AlertDialog.Builder(this); //对话框控件
    	builder.setTitle(this.getString(R.string.dialog_title_io_mode_set));//设置标题
    	LayoutInflater inflater = LayoutInflater.from(this);
    	//布局显示初始化
    	final View view = inflater.inflate(R.layout.dialog_io_mode, null);
    	rbInChar =(RadioButton)view.findViewById(R.id.rb_io_mode_set_in_string);
    	rbInHex =(RadioButton)view.findViewById(R.id.rb_io_mode_set_in_hex);
    	rbOutChar =(RadioButton)view.findViewById(R.id.rb_io_mode_set_out_string);
    	rbOutHex =(RadioButton)view.findViewById(R.id.rb_io_mode_set_out_hex);

    	/*初始化输入模式值*/
    	if (BluetoothSppClient.IO_MODE_STRING == this.mbtInputMode)//输入设置
    		rbInChar.setChecked(true);
    	else
    		rbInHex.setChecked(true);
    	if (BluetoothSppClient.IO_MODE_STRING == this.mbtOutputMode)//输出设置
    		rbOutChar.setChecked(true);
    	else
    		rbOutHex.setChecked(true);

    	builder.setView(view);//绑定布局
    	builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
            	//设置输入输出的模式
            	mbtInputMode = (rbInChar.isChecked())? BluetoothSppClient.IO_MODE_STRING : BluetoothSppClient.IO_MODE_HEX;
            	mbtOutputMode = (rbOutChar.isChecked())? BluetoothSppClient.IO_MODE_STRING : BluetoothSppClient.IO_MODE_HEX;
            	//记住当前设置的模式
            	mDS.setVal(KEY_IO_MODE, "input_mode", mbtInputMode);
            	mDS.setVal(KEY_IO_MODE, "output_mode", mbtOutputMode);
            	mDS.saveStorage();
            	mBSC.setRxdMode(mbtInputMode);
            	mBSC.setTxdMode(mbtOutputMode);
            }
    	});
    	builder.create().show();
    }
	
    /**
     * 保存用于自动完成控件的命令历史字
     * @param String sClass 所属的类一般为this.getLocalClassName()
     * @return void
     * */
    protected void saveAutoComplateCmdHistory(String sClass){
    	if(malCmdHistory.isEmpty())
    		this.mDS.setVal(KEY_HISTORY, sClass, ""); //清除历史日志
    	else{	//保存输入提示历史
    		StringBuilder sbBuf = new StringBuilder();
    		String sTmp = null;
    		for(int i=0; i<malCmdHistory.size(); i++)
	    		sbBuf.append(malCmdHistory.get(i) + HISTORY_SPLIT);
    		sTmp = sbBuf.toString();
    		this.mDS.setVal(KEY_HISTORY, sClass, sTmp.substring(0, sTmp.length()-3));
    	}
    	this.mDS.saveStorage();
    }
    
    /**
     * 取出用于自动完成控件的命令历史字
     * @param String sClass 所属的类一般为this.getLocalClassName()
     * @param AutoCompleteTextView v 自动完成控件的引用
     * @return void
     * */
    protected void loadAutoComplateCmdHistory(String sClass, AutoCompleteTextView v){
    	String sTmp = this.mDS.getStringVal(KEY_HISTORY, sClass);
    	if(!sTmp.equals("")){	//保存输入提示历史
    		String[] sT = sTmp.split(HISTORY_SPLIT);
    		for (int i=0;i<sT.length; i++)
    			this.malCmdHistory.add(sT[i]);
			v.setAdapter(
				new ArrayAdapter<String>(this,  
                	android.R.layout.simple_dropdown_item_1line,sT)
			);
    	}
    }
    
    /**
     * 给自动完成控件增加一个命令历史字
     * @param String sData 要追加的数据
     * @param AutoCompleteTextView v 自动完成控件的引用
     * @return void
     * @see 1、必须在onDestroy()中使用saveAutoComplateCmdHistory()保存自动完成值，否则新增的内容会在下次启动时丢失；<br/>
     * 2、在启动时，用loadAutoComplateCmdHistory()载入之前保存的历史值；
     * */
    protected void addAutoComplateVal(String sData, AutoCompleteTextView v){
		//输入提示的处理
		if (this.malCmdHistory.indexOf(sData) == -1){	//不存在历史列表中，加入自动提示字段
			this.malCmdHistory.add(sData);
			v.setAdapter(
				new ArrayAdapter<String>(this,  
                	android.R.layout.simple_dropdown_item_1line,  
                	malCmdHistory.toArray(new String[malCmdHistory.size()]))
			);
		}
    }
    
    /**
     * 清除自动完成控件中的命令历史字内容
     * @param AutoCompleteTextView v 自动完成控件的引用
     * @return void
     * */
    protected void clearAutoComplate(AutoCompleteTextView v){
    	this.malCmdHistory.clear();
		v.setAdapter(
			new ArrayAdapter<String>(this,  
            	android.R.layout.simple_dropdown_item_1line)
		);
    }
}
