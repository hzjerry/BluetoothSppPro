package mobi.dzs.android.BLE_SPP_PRO;

import mobi.dzs.android.bluetooth.BluetoothSppClient;
import mobi.dzs.android.util.CHexConver;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 通信模式：字节流模式
 * */
public class actByteStream extends BaseCommActivity
{
	/**控件:发送按钮*/
	private ImageButton mibtnSend = null;
	/**控件:输入框*/
	private AutoCompleteTextView mactvInput = null;
	/**控件:数据接收区*/
	private TextView mtvReceive = null;
	/**控件:卷屏控制控件*/
	private ScrollView msvCtl = null;
	
	/**
	 * 页面构造
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_byte_stream);
		
		/*控件引用*/
		this.mibtnSend = (ImageButton)this.findViewById(R.id.actByteStream_btn_send);
		this.mactvInput = (AutoCompleteTextView)this.findViewById(R.id.actByteStream_actv_input);
		this.mtvReceive = (TextView)this.findViewById(R.id.actByteStream_tv_receive);
		this.msvCtl = (ScrollView)this.findViewById(R.id.actByteStream_sv_Scroll);
		
		this.initCtl(); //初始化控件
		this.loadAutoComplateCmdHistory(this.getLocalClassName(), this.mactvInput); //载入自动完成输入框的内容
		
		this.enabledBack(); //激活回退按钮
		this.initIO_Mode(); //初始化输入输出模式
		this.usedDataCount(); //启用数据统计状态条
		
		//初始化结束，启动接收线程
		new receiveTask()
			.executeOnExecutor(FULL_TASK_EXECUTOR);
	}
	
	/**
	 * 析构
	 * */
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	this.saveAutoComplateCmdHistory(this.getLocalClassName()); //保存用于自动完成控件的命令历史字
    }
    
	/**
	 * 屏幕旋转时的处理
	 * */
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		this.mactvInput.setInputType(InputType.TYPE_NULL); //旋转时关闭软键盘
	}
	
	/**
	 * add top menu
	 * */
	@Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuItem miClear = menu.add(0, MEMU_CLEAR, 0, getString(R.string.menu_clear));
        miClear.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        MenuItem miIoMode = menu.add(0, MEMU_IO_MODE, 0, getString(R.string.menu_io_mode));
        miIoMode.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); //一直隐藏
        MenuItem miSaveFile = menu.add(0, MEMU_SAVE_TO_FILE, 0, getString(R.string.menu_save_to_file));
        miSaveFile.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); //一直隐藏
        MenuItem miClearHistory = menu.add(0, MEMU_CLEAR_CMD_HISTORY, 0, getString(R.string.menu_clear_cmd_history));
        miClearHistory.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); //一直隐藏
        MenuItem miHelper = menu.add(0, MEMU_HELPER, 0, getString(R.string.menu_helper));
        miHelper.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); //一直隐藏
        return super.onCreateOptionsMenu(menu);
    }
	
	/**
	 * 菜单点击后的执行指令
	 * */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) 
    {  
        switch(item.getItemId())  
        {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	        	this.mbThreadStop = true; //终止接收线程
	        	this.setResult(Activity.RESULT_CANCELED); //返回到主界面
	        	this.finish();
	        	return true;
	        case MEMU_CLEAR: //清除屏幕
	        	this.mtvReceive.setText("");
	        	return true;
	        case MEMU_IO_MODE: //设定IO模式
	        	this.setIOModeDialog();
	        	return true;
	        case MEMU_SAVE_TO_FILE: //保存到文件
	        	this.saveData2File();
	        	return true;
	        case MEMU_CLEAR_CMD_HISTORY: //清除历史命令
	        	this.clearAutoComplate(this.mactvInput);
	        	return true;
	        case MEMU_HELPER: //显示使用向导
	        	if (this.getString(R.string.language).toString().equals("cn"))
	        		this.mtvReceive.setText(this.getStringFormRawFile(R.raw.byte_stream_cn) +"\n\n");
	        	else
	        		this.mtvReceive.setText(this.getStringFormRawFile(R.raw.byte_stream_en) +"\n");
	        	return true;
	        default:
	        	return super.onMenuItemSelected(featureId, item);
        }
    }
	
	/**
	 * 界面的控件初始化
	 * @return void
	 * */
	private void initCtl()
	{
		this.mibtnSend.setEnabled(false);
		this.refreshRxdCount();
		this.refreshTxdCount();
		
		/*监听：输入框没有内容时，发送按钮不可用*/
		this.mactvInput.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void afterTextChanged(Editable arg0)
			{
				if (arg0.length() > 0)
					mibtnSend.setEnabled(true);
				else
					mibtnSend.setEnabled(false);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{
			}
			
		});
	}
	
    /**
     * 用户按返回按钮的处理
     * @param keyCode
     * @param event
     * @param data
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (KeyEvent.KEYCODE_BACK == keyCode)
        {
        	this.mbThreadStop = true; //终止接收线程
        	this.setResult(Activity.RESULT_CANCELED, null);
        	this.finish();
        	return true;
        }
        else
        	return super.onKeyDown(keyCode, event);
    }
    
    /**
     * 自动滚屏的处理
     * @return void
     * */
    private void autoScroll()
    {
		//自动滚屏处理
		int iOffset = this.mtvReceive.getMeasuredHeight() - this.msvCtl.getHeight();     
        if (iOffset > 0)
        	this.msvCtl.scrollTo(0, iOffset);
    }
    
    /**
     * 保存收到的数据到SD卡文件中
     * */
    private void saveData2File()
    {
    	if (this.mtvReceive.length() > 0)
    		this.save2SD(this.mtvReceive.getText().toString().trim());
    }
    
    /**
     * 发送按钮
     * */
    public void onClickBtnSend(View c)
    {
    	String sSend = this.mactvInput.getText().toString().trim();
    	if (BluetoothSppClient.IO_MODE_HEX == this.mbtOutputMode)
    	{	//当使用HEX发送时，对发送内容做检查
    		if (!CHexConver.checkHexStr(sSend))
    		{
    			Toast.makeText(this, //提示 本次发送失败
				   getString(R.string.msg_not_hex_string),
				   Toast.LENGTH_SHORT).show();
    			return;
    		}
    	}
    	
    	this.mibtnSend.setEnabled(false);// 禁用发送按钮
//    	sSend += "\r\n"; 
    	if (this.mBSC.Send(sSend) >= 0)
    	{
    		this.refreshTxdCount(); //刷新发送数据计值
    		this.mibtnSend.setEnabled(true); //发送成功恢复发送按钮
    		this.addAutoComplateVal(sSend, this.mactvInput); //追加自动完成值
    	}
    	else
    	{
			Toast.makeText(this, //提示 连接丢失
					   getString(R.string.msg_msg_bt_connect_lost),
					   Toast.LENGTH_LONG).show();
			this.mactvInput.setEnabled(false); //禁用输入框
    	}
    		
    }
    
    //----------------
    /*多线程处理(建立蓝牙设备的串行通信连接)*/
    private class receiveTask extends AsyncTask<String, String, Integer>
    {
    	/**常量:连接丢失*/
    	private final static byte CONNECT_LOST = 0x01;
    	/**常量:线程任务结束*/
    	private final static byte THREAD_END = 0x02;
		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute()
		{
			mtvReceive.setText(getString(R.string.msg_receive_data_wating));
			mbThreadStop = false;
		}
		
		/**
		  * 线程异步处理
		  */
		@Override
		protected Integer doInBackground(String... arg0)
		{
			mBSC.Receive(); //首次启动调用一次以启动接收线程
			while(!mbThreadStop)
			{
				if (!mBSC.isConnect())
					return CONNECT_LOST; //检查连接是否丢失
				
				if (mBSC.getReceiveBufLen() > 0)
					this.publishProgress(mBSC.Receive());
				
				try
				{
					Thread.sleep(20);//接收等待延时，提高接收效率
				}
				catch (InterruptedException e)
				{
					return THREAD_END;
				} 
			}
			return THREAD_END;
		}
		
		/**
		 * 线程内更新处理
		 */
		@Override
		public void onProgressUpdate(String... progress)
		{
			mtvReceive.append(progress[0]); //显示区中追加数据
			autoScroll(); //自动卷屏处理
			refreshRxdCount(); //刷新接收数据统计值
		}
		
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result)
		{
			if (CONNECT_LOST == result) //通信连接丢失
				mtvReceive.append(getString(R.string.msg_msg_bt_connect_lost));
			else
				mtvReceive.append(getString(R.string.msg_receive_data_stop));//提示接收终止
			mibtnSend.setEnabled(false); //禁用发送按钮
			refreshHoldTime(); //刷新数据统计状态条-运行时间
		}
    }
}
