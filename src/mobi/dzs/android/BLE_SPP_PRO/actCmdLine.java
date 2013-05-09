package mobi.dzs.android.BLE_SPP_PRO;

import mobi.dzs.android.util.CHexConver;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class actCmdLine extends BaseCommActivity
{
	/**常量:类型标识-接收*/
	private final static byte TYPE_RXD = 0x01;
	/**常量:类型标识-发送*/
	private final static byte TYPE_TXD = 0x02;
	/**常量:菜单变量-设置结束符*/
	private final static byte MEMU_SET_END_FLG = 0x21;
	/**常量:结束符 动态存储用子关键字*/
	private final static String SUB_KEY_END_FLG = "SUB_KEY_END_FLG";
	/**常量:模块已经被使用过的标志(用于初始化)*/
	private final static String SUB_KEY_MODULE_IS_USED = "SUB_KEY_MODULE_IS_USED";
	/**当前使用的结束符*/
	private String msEndFlg = msEND_FLGS[0];
	/**控件:输入框*/
	private AutoCompleteTextView mactvInput = null;
	/**控件:数据显示区*/
	private TextView mtvDataView = null;
	/**控件:卷屏控制控件*/
	private ScrollView msvCtl = null;

	/**
	 * 页面构造
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_cmd_line);
		
		/*控件引用*/
		this.mactvInput = (AutoCompleteTextView)this.findViewById(R.id.actCmdLine_actv_input);
		this.mtvDataView = (TextView)this.findViewById(R.id.actCmdLine_tv_data_view);
		this.msvCtl = (ScrollView)this.findViewById(R.id.actCmdLine_sv_Scroll);
		
		/*监听输入框的按钮事件*/
		this.mactvInput.setOnEditorActionListener(new OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
			{
				/*当点击发送按钮时发送指令*/
				if (EditorInfo.IME_ACTION_SEND == arg1 ||
					EditorInfo.IME_ACTION_DONE == arg1 ||
					EditorInfo.IME_ACTION_UNSPECIFIED == arg1
					)
				{
					String sCmd = mactvInput.getText().toString().trim();
					if (sCmd.length() > 0)
					{
						mactvInput.setText(""); //清除输入框
				    	if (mBSC.Send(sCmd.concat(msEndFlg)) >= 0)
				    	{
				    		append2DataView(TYPE_TXD, sCmd); //显示数据
				    		addAutoComplateVal(sCmd, mactvInput); //追加自动完成值
				    	}
				    	else
				    	{
							Toast.makeText(actCmdLine.this, //提示 连接丢失
							   getString(R.string.msg_msg_bt_connect_lost),
							   Toast.LENGTH_LONG).show();
							mtvDataView.append(getString(R.string.msg_msg_bt_connect_lost));
							mactvInput.setEnabled(false);//禁用命令输入行
				    	}
				    	refreshTxdCount(); //刷新接收数据统计值
					}
					return true;
				}
				else
					return false;
			}
		});
		
		this.initCtl(); //初始化控件
		this.loadAutoComplateCmdHistory(this.getLocalClassName(), this.mactvInput); //载入自动完成输入框的内容
		this.loadProfile(); //载入终止符
		
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
    	this.mBSC.killReceiveData_StopFlg(); //强制终止接收函数
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
        //清屏
        MenuItem miClear = menu.add(0, MEMU_CLEAR, 0, getString(R.string.menu_clear));
        miClear.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        //设置终止符
        MenuItem miSetStopFlg = menu.add(0, MEMU_SET_END_FLG, 0, getString(R.string.menu_set_stop_flg));
        miSetStopFlg.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //保存到文件
        MenuItem miSaveFile = menu.add(0, MEMU_SAVE_TO_FILE, 0, getString(R.string.menu_save_to_file));
        miSaveFile.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); //一直隐藏
        //清除历史记录
        MenuItem miClearHistory = menu.add(0, MEMU_CLEAR_CMD_HISTORY, 0, getString(R.string.menu_clear_cmd_history));
        miClearHistory.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); //一直隐藏
        //使用向导
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
	        	this.mBSC.killReceiveData_StopFlg(); //强制终止接收函数
	        	this.mbThreadStop = true; //终止接收线程
	        	this.setResult(Activity.RESULT_CANCELED); //返回到主界面
	        	this.finish();
	        	return true;
	        case MEMU_CLEAR: //清除屏幕
	        	this.mtvDataView.setText("");
	        	return true;
	        case MEMU_SET_END_FLG: //设定终止符
	        	this.selectEndFlg();
	        	return true;
	        case MEMU_SAVE_TO_FILE: //保存到文件
	        	this.saveData2File();
	        	return true;
	        case MEMU_CLEAR_CMD_HISTORY: //清除历史命令
	        	this.clearAutoComplate(this.mactvInput);
	        	return true;
	        case MEMU_HELPER: //显示使用向导
	        	if (this.getString(R.string.language).toString().equals("cn"))
	        		this.mtvDataView.setText(this.getStringFormRawFile(R.raw.cmd_line_cn) +"\n\n");
	        	else
	        		this.mtvDataView.setText(this.getStringFormRawFile(R.raw.cmd_line_en) +"\n");
	        	return true;
	        default:
	        	return super.onMenuItemSelected(featureId, item);
        }
    }
    
    /**
     * 按键监听处理
     * @param keyCode
     * @param event
     * @param data
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (KeyEvent.KEYCODE_BACK == keyCode)
        {	//按回退键的处理
        	this.mbThreadStop = true; //终止接收线程
        	this.setResult(Activity.RESULT_CANCELED, null);
        	this.finish();
        	return true;
        }
        else
        	return super.onKeyDown(keyCode, event);
    }
	
	/**
	 * 界面的控件初始化
	 * @return void
	 * */
	private void initCtl()
	{
		this.refreshRxdCount();
		this.refreshTxdCount();
	}
	
    /**
     * 保存收到的数据到SD卡文件中
     * @return void
     * */
    private void saveData2File()
    {
    	if (this.mtvDataView.length() > 0)
    		this.save2SD(this.mtvDataView.getText().toString().trim());
    }
    
	/**
	 * 追加数据到数据显示区
	 * @param byte b 追加的数据类型 TYPE_RXD:接收 / TYPE_TXD:发送
	 * @param String sData 需要显示的数据
	 * @return void
	 * */
	private void append2DataView(byte b, String sData)
	{
		StringBuilder sbTmp = new StringBuilder();
		if (TYPE_RXD == b)
			sbTmp.append("Rxd>");
		else
			sbTmp.append("Txd>");
		sbTmp.append(sData);
		sbTmp.append("\t(");
		sbTmp.append(sData.length() + this.msEndFlg.length());
		sbTmp.append("B)");
		sbTmp.append("\n");
		this.mtvDataView.append(sbTmp.toString());
	}
	
    /**
     * 设置结束符（对话框）
     * @param byte bMode 字符显示模式 IO_MODE_HEX / IO_MODE_STRING
     * @return void
     * */
	private void selectEndFlg()
    {
		final AlertDialog adCtrl;
    	final RadioGroup rgEndFlg;
    	final RadioButton rb_rn, rb_n, rb_other;
    	final EditText etVal;
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this); //对话框控件
    	builder.setTitle(this.getString(R.string.dialog_title_end_flg));//设置标题
    	LayoutInflater inflater = LayoutInflater.from(this);
    	//布局显示初始化
    	final View view = inflater.inflate(R.layout.dialog_end_flg, null);
    	rgEndFlg = (RadioGroup)view.findViewById(R.id.rg_end_flg);
    	rb_rn =(RadioButton)view.findViewById(R.id.rb_end_flg_set_rn);
    	rb_n =(RadioButton)view.findViewById(R.id.rb_end_flg_set_n);
    	rb_other =(RadioButton)view.findViewById(R.id.rb_end_flg_set_other);
    	etVal =(EditText)view.findViewById(R.id.et_end_flg_val);

    	builder.setView(view);//绑定布局
    	builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener()
    	{
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            	//设置输入输出的模式
            	String sHexEndFlg = etVal.getText().toString().trim();
            	if (sHexEndFlg.isEmpty())
            	{
	            	msEndFlg = new String();
	            	mBSC.setReceiveStopFlg(msEndFlg); //设置结束符
	            	mDS.setVal(getLocalClassName(), SUB_KEY_END_FLG, sHexEndFlg);
	            	mDS.saveStorage();
	            	showEndFlg(); //显示当前结束符的设置信息
            	}
            	else if (CHexConver.checkHexStr(sHexEndFlg))
            	{
	            	msEndFlg = CHexConver.hexStr2Str(sHexEndFlg);
	            	mBSC.setReceiveStopFlg(msEndFlg); //设置结束符
	            	//记住当前设置的模式
	            	mDS.setVal(getLocalClassName(), SUB_KEY_END_FLG, sHexEndFlg);
	            	mDS.saveStorage();
	            	showEndFlg(); //显示当前结束符的设置信息
            	}
            	else
            	{
					Toast.makeText(actCmdLine.this,
						   getString(R.string.msg_not_hex_string),
						   Toast.LENGTH_SHORT).show();
            	}
            }
    	});
    	adCtrl = builder.create();
    	adCtrl.show();
    	
    	/*初始化输入模式值*/
    	etVal.setText(CHexConver.str2HexStr(msEndFlg)); //初始化输入默认值
    	if (msEndFlg.equals(msEND_FLGS[0]))
    		rb_rn.setChecked(true);
    	else if (msEndFlg.equals(msEND_FLGS[1]))
    		rb_n.setChecked(true);
    	else
    		rb_other.setChecked(true);
    	
    	/*设置按钮组的监听事件*/
    	rgEndFlg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
    	{
			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1)
			{
				if (rb_rn.getId() == arg1)
				{
					msEndFlg = msEND_FLGS[0];
					etVal.setEnabled(false); //不可修改
				}
				else if (rb_n.getId() == arg1)
				{
					msEndFlg = msEND_FLGS[1];
					etVal.setEnabled(false); //不可修改
				}
				else
					etVal.setEnabled(true); //可修改
				etVal.setText(CHexConver.str2HexStr(msEndFlg));//输出HEX字符串
			}
    	});
    	/*结束符的输入框的监听*/
    	etVal.addTextChangedListener(new TextWatcher()
    	{
			@Override
			public void afterTextChanged(Editable arg0)
			{
				String sEndFlg = etVal.getText().toString().trim();
				if (sEndFlg.isEmpty() || CHexConver.checkHexStr(sEndFlg))
				{
					etVal.setTextColor(android.graphics.Color.BLACK);
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
				}
				else
				{
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					etVal.setTextColor(android.graphics.Color.RED);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
    	});
    }
    
    /**
     * 载入终止符配置信息
     * @param String sModelName 模块名称
     * @return void
     * */
    private void loadProfile()
    {
    	String sHexEndFlg = this.mDS.getStringVal(this.getLocalClassName(), SUB_KEY_END_FLG);
    	//首次使用判断，默认第一次使用为false，取反则为true
    	boolean bModuleIsUsed = this.mDS.getBooleanVal(this.getLocalClassName(), SUB_KEY_MODULE_IS_USED);
    	if (!bModuleIsUsed) //首次使用默认认为(\r\n)
    	{
    		this.msEndFlg = msEND_FLGS[0];
    		this.mDS.setVal(this.getLocalClassName(), SUB_KEY_MODULE_IS_USED, true); //标记已经使用过
    		this.mDS.saveStorage();
    	}
    	else if (sHexEndFlg.isEmpty())
    		this.msEndFlg = ""; //未设置结束符
    	else
    		this.msEndFlg = CHexConver.hexStr2Str(sHexEndFlg);
    	this.showEndFlg(); //显示当前结束符的设置信息
    	this.mBSC.setReceiveStopFlg(this.msEndFlg); //设置结束符
    }
    
    /**
     * 显示当前结束符的设置信息
     * @return void
     * */
    private void showEndFlg()
    {
    	if(msEndFlg.equals(msEND_FLGS[0]))
    	{
    		this.mtvDataView.setText(
    				String.format(
    						this.getString(R.string.actCmdLine_msg_helper), 
    						this.getString(R.string.dialog_end_flg_rn)));
    	}
    	else if(msEndFlg.equals(msEND_FLGS[1]))
    	{
    		this.mtvDataView.setText(
    				String.format(
    						this.getString(R.string.actCmdLine_msg_helper), 
    						this.getString(R.string.dialog_end_flg_n)));
    	}
    	else
    	{
    		String sTmp = null;
    		if(msEndFlg.isEmpty())
    			sTmp = getString(R.string.msg_helper_endflg_nothing);
    		else
    		{
    			sTmp = String.format(getString(R.string.actCmdLine_msg_helper), 
    					"("+ CHexConver.str2HexStr(msEndFlg) +")");
    		}
    		this.mtvDataView.setText(sTmp);
    	}
    }
    
    /**
     * 自动滚屏的处理
     * @return void
     * */
    private void autoScroll()
    {
		//自动滚屏处理
		int iOffset = this.mtvDataView.getMeasuredHeight() - this.msvCtl.getHeight();     
        if (iOffset > 0)
        	this.msvCtl.scrollTo(0, iOffset);
    }
    
    //----------------
    /*多线程处理(接收数据线程)*/
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
			mtvDataView.append(getString(R.string.msg_receive_data_wating));
			mtvDataView.append("\n");
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
				
				this.publishProgress(mBSC.ReceiveStopFlg());
			}
			return THREAD_END;
		}
		
		/**
		 * 线程内更新处理
		 */
		@Override
		public void onProgressUpdate(String... progress)
		{
			if (null != progress[0])
			{	//输入不为空时，刷新列表
				append2DataView(TYPE_RXD, progress[0]); //显示区中追加数据
				autoScroll(); //自动卷屏处理
				refreshRxdCount(); //刷新接收数据统计值
			}
		}
		
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result)
		{
			if (CONNECT_LOST == result) //通信连接丢失
				mtvDataView.append(getString(R.string.msg_msg_bt_connect_lost));
			else
				mtvDataView.append(getString(R.string.msg_receive_data_stop));//提示接收终止
			
			mactvInput.setEnabled(false);
			refreshHoldTime(); //刷新数据统计状态条-运行时间
		}
    }
}
