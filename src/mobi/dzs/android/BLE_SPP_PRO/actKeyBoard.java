package mobi.dzs.android.BLE_SPP_PRO;

import java.util.Hashtable;

import mobi.dzs.android.bluetooth.BluetoothSppClient;
import mobi.dzs.android.control.button.ButtonPassListener;
import mobi.dzs.android.control.button.RepeatingButton;
import mobi.dzs.android.util.CHexConver;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class actKeyBoard extends BaseCommActivity
{
	/**当前使用的结束符*/
	private String msEndFlg = msEND_FLGS[0];
	/**常量:菜单变量-设置结束符*/
	private final static byte MEMU_SET_END_FLG = 0x21;
	/**常量:菜单变量-键盘设置*/
	private final static byte MENU_SET_KEY_BOARD = 0x22;
	/**常量:菜单变量-按钮激发模式*/
	private final static byte MENU_SET_BUTTON_EVENT = 0x23;
	/**常量:菜单变量-按钮长按的触发频率设置*/
	private final static byte MENU_SET_LONG_PASS_REPEAT = 0x24;
	/**常量:结束符 动态存储用子关键字*/
	private final static String SUB_KEY_END_FLG = "SUB_KEY_END_FLG";
	/**常量:按钮子键-按钮显示名*/
	private final static String SUB_KEY_BTN_NAME = "SUB_KEY_BTN_NAME";
	/**常量:按钮子键-按钮值*/
	private final static String SUB_KEY_BTN_VAL = "SUB_KEY_BTN_VAL";
	/**常量:按钮子键-按钮激发模式*/
	private final static String SUB_KEY_BTN_EVENT = "SUB_KEY_BTN_EVENT";
	/**常量:按钮子键-按钮长按时的触发频率*/
	private final static String SUB_KEY_BTN_REPEAT_FREQ = "SUB_KEY_BTN_REPEAT_FREQ";
	/**常量:按钮激发频率最小值(ms)*/
	private final static int BTN_REPEAT_MIN_FREQ = 50;
	
	/**发送数据视图*/
	private TextView mtvSendView = null;
	/**接收数据视图*/
	private TextView mtvRecView = null;
	/**发送区标题对象*/
	private TextView mtvRecAreaTitle = null;
	/**发送视图滚屏*/
	private ScrollView msvSendView = null;
	/**接收视图滚屏*/
	private ScrollView msvRecView = null;
	/**接收区域控制区*/
	private RelativeLayout mrlSendArea= null;
	
	/**按钮数组数量*/
	private final static int miBTN_CNT = 12;
	/**自定义按钮控件数组*/
	private RepeatingButton[] mbtns =  new RepeatingButton[miBTN_CNT];
	private Hashtable<Integer, String> mhtSendVal = new Hashtable<Integer, String>();
	
	/**当前按钮是否处于设置模式*/
	private boolean mbSetMode = false;	
	/**当前长按时重复执行频率*/
	private int miRepateFreq = 500;
	/**当前是否隐藏发送区*/
	private boolean mbHideSendArea = false;
	/**定义重复执行按钮的动作函数*/
	private class CRL implements ButtonPassListener
	{
		@Override
		public void onRepeat(View v, long duration, int repeatcount)
		{
			if(mbSetMode)
				return;//长按事件下不能进入按钮设置模式。
			else
				onBtnClick_Array(v);
		}

		@Override
		public void onDown(View v)
		{
			onBtnClick_Array(v);
		}

		@Override
		public void onUp(View v)
		{
			onBtnClick_Array(v);
		}
	};	
	public CRL mCRL = new CRL();
	
	/**
	 * 页面构造
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_keyboard);
		
		this.mtvSendView = (TextView)this.findViewById(R.id.actKeyBoard_tv_send_data_show);
		this.mtvRecView = (TextView)this.findViewById(R.id.actKeyBoard_tv_receive_show);
		this.mtvRecAreaTitle = (TextView)this.findViewById(R.id.actKeyBoard_tv_receive_area_title);
		this.msvSendView = (ScrollView)this.findViewById(R.id.actKeyBoard_sv_send_data_scroll);
		this.msvRecView = (ScrollView)this.findViewById(R.id.actKeyBoard_sv_receive_data_scroll);
		this.mrlSendArea = (RelativeLayout)this.findViewById(R.id.actKeyBoard_rl_send_area);
		this.mtvRecView.setText(""); //初始化接收区内容
		this.mtvRecAreaTitle.append("\t\t(");//设置接收区标题
		this.mtvRecAreaTitle.append(getString(R.string.tips_click_to_hide));//设置接收区标题
		this.mtvRecAreaTitle.append(":"+ getString(R.string.tv_send_area_title));//设置接收区标题
		this.mtvRecAreaTitle.append(")");//设置发送区标题
		this.mtvRecAreaTitle.setOnClickListener(new TextView.OnClickListener()
		{	//处理发送区的显示与隐藏
			@Override
			public void onClick(View v)
			{
				if (v.getId() == R.id.actKeyBoard_tv_receive_area_title)
				{
					String sTitle = getString(R.string.tv_receive_area_title);
					TextView tv = ((TextView)v);
					if (mbHideSendArea)
					{
						sTitle += "\t\t("+ getString(R.string.tips_click_to_hide);
						sTitle += ":"+ getString(R.string.tv_send_area_title) +")";
						tv.setText(sTitle);
						mrlSendArea.setVisibility(View.VISIBLE);
					}
					else
					{
						sTitle += "\t\t("+ getString(R.string.tips_click_to_show);
						sTitle += ":"+ getString(R.string.tv_send_area_title) +")";
						tv.setText(sTitle);
						mrlSendArea.setVisibility(View.GONE);
					}
					mrlSendArea.refreshDrawableState(); //刷新发送区
					mbHideSendArea = !mbHideSendArea;
				}
			}
		});
		
    	//绑定键盘数组
    	mbtns[0] = (RepeatingButton)findViewById(R.id.btn_keyboard_1);//取得RepeatingButton对象
    	mbtns[1] = (RepeatingButton)findViewById(R.id.btn_keyboard_2);//取得RepeatingButton对象
    	mbtns[2] = (RepeatingButton)findViewById(R.id.btn_keyboard_3);//取得RepeatingButton对象
    	mbtns[3] = (RepeatingButton)findViewById(R.id.btn_keyboard_4);//取得RepeatingButton对象
    	mbtns[4] = (RepeatingButton)findViewById(R.id.btn_keyboard_5);//取得RepeatingButton对象
    	mbtns[5] = (RepeatingButton)findViewById(R.id.btn_keyboard_6);//取得RepeatingButton对象
    	mbtns[6] = (RepeatingButton)findViewById(R.id.btn_keyboard_7);//取得RepeatingButton对象
    	mbtns[7] = (RepeatingButton)findViewById(R.id.btn_keyboard_8);//取得RepeatingButton对象
    	mbtns[8] = (RepeatingButton)findViewById(R.id.btn_keyboard_9);//取得RepeatingButton对象
    	mbtns[9] = (RepeatingButton)findViewById(R.id.btn_keyboard_10);//取得RepeatingButton对象
    	mbtns[10] = (RepeatingButton)findViewById(R.id.btn_keyboard_11);//取得RepeatingButton对象
    	mbtns[11] = (RepeatingButton)findViewById(R.id.btn_keyboard_12);//取得RepeatingButton对象
    	this.loadBtnProfile(); //载入按钮配置信息
    	
		this.enabledBack(); //激活回退按钮
		this.initIO_Mode(); //初始化输入输出模式
		this.usedDataCount(); //启用数据统计状态条
		this.loadProfile(); //载入终止符
		
		//初始化结束，启动接收线程
		new receiveTask()
			.executeOnExecutor(FULL_TASK_EXECUTOR);
	}
	
    /**初始化控件的大小*/
    @Override
    public void onResume()
    {
    	super.onResume();
    	Display display = this.getWindowManager().getDefaultDisplay();
    	Point size = new Point();
    	display.getSize(size);
    	
    	int iHeight = (size.x / 3) * 2 / 3; //按钮高度;
    	
    	for(int i=0; i<miBTN_CNT; i++)
    	{
			LayoutParams btnPara = mbtns[i].getLayoutParams();
			btnPara.height = iHeight;//将按钮设置为长方形
			mbtns[i].setLayoutParams(btnPara);
    	}
    }
    
	/**
	 * 析构
	 * */
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	this.mBSC.killReceiveData_StopFlg(); //强制终止接收函数
    }
    
	/**
	 * 屏幕旋转时的处理
	 * */
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
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
        //设置键盘
        MenuItem miSetKeyboard = menu.add(1, MENU_SET_KEY_BOARD, 0, getString(R.string.menu_set_key_board_start));
        miSetKeyboard.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //设置指令结束符
        MenuItem miSetStopFlg = menu.add(1, MEMU_SET_END_FLG, 1, getString(R.string.menu_set_stop_flg));
        miSetStopFlg.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //设置按钮激发模式
        MenuItem miSetBtnEvent = menu.add(1, MENU_SET_BUTTON_EVENT, 2, getString(R.string.menu_set_key_board_event));
        miSetBtnEvent.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //设置按钮长按的触发频率
        MenuItem miSetBtnLongPassFreq = menu.add(1, MENU_SET_LONG_PASS_REPEAT,3, getString(R.string.menu_set_button_long_pass_freq));
        miSetBtnLongPassFreq.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //设定IO模式
        MenuItem miIoMode = menu.add(0, MEMU_IO_MODE, 4, getString(R.string.menu_io_mode));
        miIoMode.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); //一直隐藏
        //保存到文件
        MenuItem miSaveFile = menu.add(1, MEMU_SAVE_TO_FILE, 5, getString(R.string.menu_save_to_file));
        miSaveFile.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); //一直隐藏
        //使用向导
        MenuItem miHelper = menu.add(1, MEMU_HELPER, 6, getString(R.string.menu_helper));
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
	        	this.mtvSendView.setText("");
	        	this.mtvRecView.setText("");
	        	return true;
	        case MENU_SET_KEY_BOARD: //设置键盘
	        	if (this.mbSetMode)
	        	{	
	        		item.setTitle(R.string.menu_set_key_board_start);
	        		this.mtvSendView.setText(R.string.actKeyBoard_tv_Init);
	        	}
	        	else
	        	{	
	        		item.setTitle(R.string.menu_set_key_board_end);
	        		this.mtvSendView.setText(R.string.actKeyBoard_tv_set_keyboard_helper);
	        	}
	        	this.mbSetMode = !this.mbSetMode;//反转设置模式状态
	        	return true;
	        case MENU_SET_BUTTON_EVENT: //设定按钮激发模式
	        	this.selectButtonEvent();
	        	return true;
	        case MENU_SET_LONG_PASS_REPEAT: //设定按钮长按促发频率
	        	this.selectRepeatFreq();
	        	return true;
	        case MEMU_SET_END_FLG: //设定终止符
	        	this.selectEndFlg();
	        	return true;
	        case MEMU_IO_MODE: //设定IO模式
	        	this.setIOModeDialog();
	        	return true;
	        case MEMU_SAVE_TO_FILE: //保存到文件
	        	this.saveData2File();
	        	return true;
	        case MEMU_HELPER: //显示使用向导
	        	if (this.getString(R.string.language).toString().equals("cn"))
	        		this.mtvRecView.setText(this.getStringFormRawFile(R.raw.key_board_cn) +"\n\n");
	        	else
	        		this.mtvRecView.setText(this.getStringFormRawFile(R.raw.key_board_en) +"\n");
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
            	{	//未设置终止符
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
					Toast.makeText(actKeyBoard.this, //提示 连接丢失
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
     * 保存收到的数据到SD卡文件中
     * @return void
     * */
    private void saveData2File()
    {
    	StringBuilder sb = new StringBuilder();
    	if (this.mtvRecView.length() > 0)
    	{
    		sb.append("Receive Data:\n");
    		sb.append("--------------------------\n");
    		sb.append(this.mtvRecView.getText().toString().trim());
    		sb.append("\n\n");
    	}
    	if (this.mtvRecView.length() > 0)
    	{
    		sb.append("Send Data:\n");
    		sb.append("--------------------------\n");
    		sb.append(this.mtvSendView.getText().toString().trim());
    	}
    	this.save2SD(sb.toString());
    }
	
	/**
	 * 载入按钮配置信息
	 * */
	private void loadBtnProfile()
	{
		String sTmp;
		final String sModel = this.getLocalClassName();
    	//键盘按钮值初始化
    	for(int i=0; i<miBTN_CNT; i++)
    	{
    		String sName = this.mDS.getStringVal(sModel, SUB_KEY_BTN_NAME.concat(String.valueOf(i)));
    		String sValue = this.mDS.getStringVal(sModel, SUB_KEY_BTN_VAL.concat(String.valueOf(i)));
    		
    		if (!sName.isEmpty())
    			mbtns[i].setText(sName);//载入按钮名称
    		
    		if (!sValue.isEmpty())
    			mhtSendVal.put(i, sValue); //载入按钮值
    		else
    			mhtSendVal.put(i, "");
    	}
		//按钮激发模式初始化
		byte btBtnEvent = (byte)this.mDS.getIntVal(this.getLocalClassName(), SUB_KEY_BTN_EVENT);
		if (0 == btBtnEvent)
		{	//默认为按钮抬起时触发
			this.setBtnBindEvent(RepeatingButton.mEVENT_UP);
			this.mtvRecView.append(getString(R.string.menu_button_event_up) + "\n");
		}
		else
		{	//载入之前保存的激发模式
			this.setBtnBindEvent(btBtnEvent);
			if (RepeatingButton.mEVENT_UP == btBtnEvent)
				this.mtvRecView.append(getString(R.string.menu_button_event_up) + "\n");
			else if (RepeatingButton.mEVENT_DOWN == btBtnEvent)
				this.mtvRecView.append(getString(R.string.menu_button_event_down) + "\n");
			else
				this.mtvRecView.append(getString(R.string.menu_button_event_repeat) + "\n");
		}
		
		//保存按钮长按时的触发频率
		int iRepeat = this.mDS.getIntVal(this.getLocalClassName(), SUB_KEY_BTN_REPEAT_FREQ);
		if (0 == iRepeat) //默认频率
			this.setBtnRepeatFreq(500);
		else
			this.setBtnRepeatFreq(iRepeat);
		
		if (RepeatingButton.mEVENT_REPEAT == btBtnEvent)
		{	//当为长按模式才会显示触发频率的提示
			sTmp = String.format(getString(R.string.actKeyBoard_msg_repeat_freq_set)+"\n", iRepeat);
			this.mtvRecView.append(sTmp);//显示当前设定的促发频率
		}
	}
	
    /**
     * 批量的设置按钮的事件属性
     * @param byte bFlg 按钮激发的类型 RepeatingButton.mEVENT_UP/ mEVENT_DOWN / mEVENT_REPEAT
     * @return void
     * */
    private void setBtnBindEvent(byte btFlg)
    {
    	if (RepeatingButton.mEVENT_UP == btFlg)
    	{
	    	for (int i=0; i<miBTN_CNT; i++)
	    		this.mbtns[i].bindListener(mCRL, RepeatingButton.mEVENT_UP);
    	}
    	else if (RepeatingButton.mEVENT_DOWN == btFlg)
    	{
	    	for (int i=0; i<miBTN_CNT; i++)
	    		this.mbtns[i].bindListener(mCRL, RepeatingButton.mEVENT_DOWN);
    	}
    	else
    	{
    		/**设置按钮的重复执行的频率500ms*/
    		for (int i=0; i<miBTN_CNT; i++)
    			this.mbtns[i].bindListener(500, mCRL);
    	}
    	//保存设置
    	this.mDS.setVal(this.getLocalClassName(), SUB_KEY_BTN_EVENT, btFlg);
    	this.mDS.saveStorage();
    }
    
    /**
     * 设定按钮长按的重复频率
     * @param long interval 毫秒
     * @return void
     * */
    private void setBtnRepeatFreq(int interval)
    {
    	this.miRepateFreq = interval;
		for (int i=0; i<miBTN_CNT; i++)
			this.mbtns[i].setRepeatFreq(interval);
		this.mDS.setVal(this.getLocalClassName(), SUB_KEY_BTN_REPEAT_FREQ, interval);
		this.mDS.saveStorage();
    }
    
    /**按钮激发模式选择*/
    private void selectButtonEvent()
    {
	    /*菜单列表初始化*/
	    String sList[] = new String[] {this.getString(R.string.menu_button_event_up),
	    							   this.getString(R.string.menu_button_event_down),
	    							   this.getString(R.string.menu_button_event_repeat)
	    							  };
	    /*构造选择框*/
	    AlertDialog.Builder builder = new AlertDialog.Builder(this); //对话框控件
	    builder.setTitle(this.getString(R.string.menu_set_key_board_event));//设置标题
	    builder.setItems(sList, new DialogInterface.OnClickListener()
	    {  
    		public void onClick(DialogInterface dialog, int which)
    		{  
    			switch (which)
    			{
    				case 0:
    					setBtnBindEvent(RepeatingButton.mEVENT_UP); //绑定促发事件
    					mtvSendView.setText(getString(R.string.menu_button_event_up) +"\n");
    					break;
    				case 1:
    					setBtnBindEvent(RepeatingButton.mEVENT_DOWN); //绑定促发事件
    					mtvSendView.setText(getString(R.string.menu_button_event_down) +"\n");
    					break;
    				case 2:
    					setBtnBindEvent(RepeatingButton.mEVENT_REPEAT); //绑定促发事件
    					mtvSendView.setText(getString(R.string.menu_button_event_repeat) +"\n");
    					break;
    			}
    		}
	    });
	    builder.setCancelable(false);
	    builder.create().show(); 
    }
	
    /**
     * 载入终止符配置信息
     * @param String sModelName 模块名称
     * @return void
     * */
    private void loadProfile()
    {
    	String sHexEndFlg = this.mDS.getStringVal(this.getLocalClassName(), SUB_KEY_END_FLG);
    	if (sHexEndFlg.isEmpty()) //默认为(\r\n)
    		this.msEndFlg = msEND_FLGS[0];
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
    		this.mtvRecView.append(
    				String.format(
    						this.getString(R.string.actKeyBoard_msg_helper_endflg), 
    						this.getString(R.string.dialog_end_flg_rn)));
    	}
    	else if(msEndFlg.equals(msEND_FLGS[1]))
    	{
    		this.mtvRecView.append(
    				String.format(
    						this.getString(R.string.actKeyBoard_msg_helper_endflg), 
    						this.getString(R.string.dialog_end_flg_n)));
    	}
    	else
    	{
    		if (this.msEndFlg.isEmpty())
    			this.mtvRecView.append(getString(R.string.actKeyBoard_msg_helper_endflg_nothing));
    		else
    		{
    			this.mtvRecView.append(
    				String.format(
    						getString(R.string.actKeyBoard_msg_helper_endflg),
    						"("+ CHexConver.str2HexStr(msEndFlg) +")"
    						)
    					);
    		}
    	}
    }
    
    /**
     * 统一处理按钮的单击事件
     * @param View v 按钮的单机事件处理
     * @return void
     * */
    public void onBtnClick_Array(View v)
    {
    	int iBtnId = v.getId();
    	for(int i=0; i<miBTN_CNT; i++)
    	{
    		if (mbtns[i].getId() == iBtnId)
    		{	//找到了当前按下的按钮
    			if (mbSetMode)
    				this.setBtnKeyboard(i);
    			else
    				this.Send(this.mhtSendVal.get(i));//发送的处理
    			break;
    		}
    	}
    }
    
    /**
     * 数据的发送处理
     * @return void
     * */
    private void Send(String sData)
    {
		if (!sData.equals(""))
		{
			String sSend = sData;
			int iRet = 0;
			if (!this.msEndFlg.isEmpty()) //加入结束符的处理
				iRet = this.mBSC.Send(sSend.concat(this.msEndFlg));
			else
				iRet = this.mBSC.Send(sSend);
			
			if (iRet >= 0) //检查通信状态
			{	//通信正常
				if (iRet == 0)
					this.mtvSendView.append(sSend.concat("(fail) "));
				else
					this.mtvSendView.append(sSend.concat("(succeed) "));
			}
			else
			{	//链接丢失
				Toast.makeText(actKeyBoard.this, //提示 连接丢失
					   getString(R.string.msg_msg_bt_connect_lost),
					   Toast.LENGTH_LONG).show();
				this.mtvRecView.append(this.getString(R.string.msg_msg_bt_connect_lost) + "\n");
			}
			this.refreshTxdCount();//刷新发送值
			this.autoScroll(); //滚屏处理
		}
    }
    
    /**
     * 设置按钮键盘值的处理
     * @param final int iId 按钮的ID序号
     * @return void
     * */
    private void setBtnKeyboard(final int iId)
    {
    	final AlertDialog adCtrl;
    	final EditText tvBtnName, tvSendVal;
    	final String sModel = this.getLocalClassName();

    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(this.getString(R.string.dialog_title_keyboard_set));//设置标题
    	LayoutInflater inflater = LayoutInflater.from(this);
    	//载入xml布局 必须为final 后面才能访问
    	final View view = inflater.inflate(R.layout.dialog_set_keyboard,null);
    	tvBtnName =(EditText)view.findViewById(R.id.et_keyboard_set_BtnName);
    	tvSendVal =(EditText)view.findViewById(R.id.et_keyboard_set_SendValue);
    	tvBtnName.setText(mbtns[iId].getText().toString());//设定初始化值
    	tvSendVal.setText(mhtSendVal.get(iId));
    	builder.setView(view);
    	builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener()
    	{
    	    @Override
    	    public void onClick(DialogInterface dialog, int which)
    	    {
    	    	String sBtnName = tvBtnName.getText().toString().trim();
    	    	String sBtnVal = tvSendVal.getText().toString().trim();
    	    	mbtns[iId].setText(sBtnName);//设定按钮名字
    	    	mhtSendVal.remove(iId);//移除原始项目
    	    	mhtSendVal.put(iId, sBtnVal);//添加按钮的新值
    	    	/*保存新的按钮值*/
    	    	mDS.setVal(sModel, SUB_KEY_BTN_NAME + String.valueOf(iId), sBtnName);
    	    	mDS.setVal(sModel, SUB_KEY_BTN_VAL + String.valueOf(iId), sBtnVal);
    	    	mDS.saveStorage();
    	    }
    	});
    	adCtrl = builder.create();
    	adCtrl.show();
    	
    	//对输入值加入验证
    	tvSendVal.addTextChangedListener(new TextWatcher()
    	{
			@Override
			public void afterTextChanged(Editable s)
			{
				String sSend = tvSendVal.getText().toString().trim();
				if (BluetoothSppClient.IO_MODE_HEX == mbtOutputMode)
				{	//16进制值时对输入做验证
					if (CHexConver.checkHexStr(sSend))
					{
						tvSendVal.setTextColor(android.graphics.Color.BLACK);
						adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					}
					else
					{	//HEX值无效，显示红色
						adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
						tvSendVal.setTextColor(android.graphics.Color.RED);
					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}
    		
    	});
    }
    
    /**
     * 选择重复频率
     * @return void
     * */
    private void selectRepeatFreq()
    {
    	final AlertDialog adCtrl;
	    /*输入框初始化*/
	    final EditText etFreq = new EditText(this);
	    etFreq.setHint(
    		String.format(
	    		getString(R.string.actKeyBoard_tv_long_pass_freq_hint),
	    			BTN_REPEAT_MIN_FREQ)); //设置提示
	    etFreq.setInputType(InputType.TYPE_CLASS_NUMBER); //限制只能输入数字
	    etFreq.setText(String.valueOf(this.miRepateFreq));
	    /*构造选择框*/
	    AlertDialog.Builder builder = new AlertDialog.Builder(this); //对话框控件
	    builder.setTitle(this.getString(R.string.dialog_title_keyboard_long_pass_frea));//设置标题
	    builder.setView(etFreq);
	    builder.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener()
	    {
			@Override
			public void onClick(DialogInterface arg0, int arg1)
			{
				int iFreq;
				//取出设置的频率值
				if (etFreq.getText().toString().isEmpty())
					iFreq= 0;
				else
					iFreq = Integer.valueOf(etFreq.getText().toString());
				setBtnRepeatFreq(iFreq); //设置触发频率
				String sTmp = String.format(getString(R.string.actKeyBoard_msg_repeat_freq_set)+"\n", iFreq);
				mtvSendView.setText(sTmp);//显示当前设定的促发频率
				
			}
	    });
	    builder.setCancelable(false);
	    adCtrl = builder.create();
	    adCtrl.show();
	    etFreq.addTextChangedListener(new TextWatcher()
	    {	//对输入的重复频率进行有效性验证
			@Override
			public void afterTextChanged(Editable arg0)
			{
				int iFreq;
				//取出设置的频率值
				if (etFreq.getText().toString().isEmpty())
					iFreq= 0;
				else
					iFreq = Integer.valueOf(etFreq.getText().toString());
				
				if (iFreq >= BTN_REPEAT_MIN_FREQ)
				{
					etFreq.setTextColor(android.graphics.Color.BLACK);
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
				}
				else
				{
					etFreq.setTextColor(android.graphics.Color.RED);
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
			}
	    	
	    });
    }
    
    /**
     * 自动滚屏的处理
     * @return void
     * */
    private void autoScroll()
    {
    	int iOffset = 0;
		//自动滚屏处理
		iOffset = this.mtvRecView.getMeasuredHeight() - this.msvRecView.getHeight();     
        if (iOffset > 0)
        	this.msvRecView.scrollTo(0, iOffset);
        
        if (this.mrlSendArea.getVisibility() == View.VISIBLE)
        {	//当发送区显示的时候，才组刷新处理
			iOffset = this.mtvSendView.getMeasuredHeight() - this.msvSendView.getHeight();     
	        if (iOffset > 0)
	        	this.msvSendView.scrollTo(0, iOffset);
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
			mtvRecView.append(getString(R.string.msg_receive_data_wating));
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
			mtvRecView.append(progress[0]); //显示区中追加数据
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
				mtvRecView.append(getString(R.string.msg_msg_bt_connect_lost));
			else
				mtvRecView.append(getString(R.string.msg_receive_data_stop));//提示接收终止
			refreshHoldTime(); //刷新数据统计状态条-运行时间
		}
    }
}
