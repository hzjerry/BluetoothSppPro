package mobi.dzs.android.BLE_SPP_PRO;

import mobi.dzs.android.bluetooth.BluetoothSppClient;
import mobi.dzs.android.util.CHexConver;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
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
 * Communication modes: byte-stream mode
 * @author JerryLi
 * */
public class actByteStream extends BaseCommActivity{
	/**Control: the Send button*/
	private ImageButton mibtnSend = null;
	/**Controls: input box*/
	private AutoCompleteTextView mactvInput = null;
	/**Controls: data receive area*/
	private TextView mtvReceive = null;
	/**Control: Scroll screen control*/
	private ScrollView msvCtl = null;
	
	/**
	 * Page construction
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_byte_stream);
		
		/*Control reference*/
		this.mibtnSend = (ImageButton)this.findViewById(R.id.actByteStream_btn_send);
		this.mactvInput = (AutoCompleteTextView)this.findViewById(R.id.actByteStream_actv_input);
		this.mtvReceive = (TextView)this.findViewById(R.id.actByteStream_tv_receive);
		this.msvCtl = (ScrollView)this.findViewById(R.id.actByteStream_sv_Scroll);
		
		this.initCtl(); //Initialize controls
		//Loading the contents of the input box automatically
		this.loadAutoComplateCmdHistory(this.getLocalClassName(), this.mactvInput);
		
		this.enabledBack(); //激活回退按钮
		this.initIO_Mode(); //初始化输入输出模式
		this.usedDataCount(); //启用数据统计状态条
		
		//Start receiving thread
		new receiveTask()
			.executeOnExecutor(FULL_TASK_EXECUTOR);
	}
	
	/**
	 * 析构
	 * */
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	//Save for auto-complete control word command history
    	this.saveAutoComplateCmdHistory(this.getLocalClassName());
    }
    
	/**
	 * Screen rotation processing
	 * */
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		this.mactvInput.setInputType(InputType.TYPE_NULL); //Close soft keyboard
	}
	
	/**
	 * add top menu
	 * */
	@Override
    public boolean onCreateOptionsMenu(Menu menu){
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
	 * Menu click execute instructions
	 * */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {  
        switch(item.getItemId())  {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	        	this.mbThreadStop = true; //Termination of the receiving thread
	        	this.setResult(Activity.RESULT_CANCELED); //Return to the main interface
	        	this.finish();
	        	return true;
	        case MEMU_CLEAR: //Clear the screen
	        	this.mtvReceive.setText("");
	        	return true;
	        case MEMU_IO_MODE: //Set the IO mode
	        	this.setIOModeDialog();
	        	return true;
	        case MEMU_SAVE_TO_FILE: //Saved to file
	        	this.saveData2File();
	        	return true;
	        case MEMU_CLEAR_CMD_HISTORY: //Clear History command
	        	this.clearAutoComplate(this.mactvInput);
	        	return true;
	        case MEMU_HELPER: //Display using the wizard
	        	if (this.getString(R.string.language).toString().substring(0, 2).equals("zh"))
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
	private void initCtl(){
		this.mibtnSend.setEnabled(false);
		this.refreshRxdCount();
		this.refreshTxdCount();
		
		/*监听：输入框没有内容时，发送按钮不可用*/
		this.mactvInput.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0){
				if (arg0.length() > 0)
					mibtnSend.setEnabled(true);
				else
					mibtnSend.setEnabled(false);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3){
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3){
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
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (KeyEvent.KEYCODE_BACK == keyCode){
        	this.mbThreadStop = true; //终止接收线程
        	this.setResult(Activity.RESULT_CANCELED, null);
        	this.finish();
        	return true;
        }
        else
        	return super.onKeyDown(keyCode, event);
    }
    
    /**
     * Auto scroll processing
     * @return void
     * */
    private void autoScroll(){
		//自动滚屏处理
		int iOffset = this.mtvReceive.getMeasuredHeight() - this.msvCtl.getHeight();     
        if (iOffset > 0)
        	this.msvCtl.scrollTo(0, iOffset);
    }
    
    /**
     * 保存收到的数据到SD卡文件中
     * */
    private void saveData2File(){
    	if (this.mtvReceive.length() > 0)
    		this.save2SD(this.mtvReceive.getText().toString().trim());
    }
    
    /**
     * Send button event handler
     * */
    public void onClickBtnSend(View c){
    	String sSend = this.mactvInput.getText().toString().trim();
    	if (BluetoothSppClient.IO_MODE_HEX == this.mbtOutputMode){	//当使用HEX发送时，对发送内容做检查
    		if (!CHexConver.checkHexStr(sSend)){
    			Toast.makeText(this, //提示 本次发送失败
				   getString(R.string.msg_not_hex_string),
				   Toast.LENGTH_SHORT).show();
    			return;
    		}
    	}
    	
    	this.mibtnSend.setEnabled(false);// 禁用发送按钮
//    	sSend += "\r\n"; 
    	if (this.mBSC.Send(sSend) >= 0){
    		this.refreshTxdCount(); //刷新发送数据计值
    		this.mibtnSend.setEnabled(true); //发送成功恢复发送按钮
    		this.addAutoComplateVal(sSend, this.mactvInput); //追加自动完成值
    	}else{
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
    	/**Constant: the connection is lost*/
    	private final static byte CONNECT_LOST = 0x01;
    	/**Constant: the end of the thread task*/
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
		protected Integer doInBackground(String... arg0){
			mBSC.Receive(); //首次启动调用一次以启动接收线程
			while(!mbThreadStop){
				if (!mBSC.isConnect())//检查连接是否丢失
					return (int)CONNECT_LOST; 
				
				if (mBSC.getReceiveBufLen() > 0){
					SystemClock.sleep(20); //先延迟让缓冲区填满
					this.publishProgress(mBSC.Receive());
				}
			}
			return (int)THREAD_END;
		}
		
		/**
		 * 线程内更新处理
		 */
		@Override
		public void onProgressUpdate(String... progress){
			if (null != progress[0]){
				mtvReceive.append(progress[0]); //显示区中追加数据
				autoScroll(); //自动卷屏处理
				refreshRxdCount(); //刷新接收数据统计值
			}
		}
		
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result){
			if (CONNECT_LOST == result) //connection is lost
				mtvReceive.append(getString(R.string.msg_msg_bt_connect_lost));
			else
				mtvReceive.append(getString(R.string.msg_receive_data_stop));//Tip receive termination
			mibtnSend.setEnabled(false); //Disable the Send button
			refreshHoldTime(); //刷新数据统计状态条-运行时间
		}
    }
}
