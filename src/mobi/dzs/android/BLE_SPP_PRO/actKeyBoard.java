package mobi.dzs.android.BLE_SPP_PRO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import android.os.SystemClock;
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

public class actKeyBoard extends BaseCommActivity{
	/**当前使用的结束符*/
	private String msEndFlg = msEND_FLGS[0];
	/**常量:菜单变量-设置结束符*/
	private final static byte MEMU_SET_END_FLG = 0x21;
	/**常量:菜单变量-键盘设置*/
	private final static byte MENU_SET_KEY_BOARD = 0x22;
	/**常量:菜单变量-按钮长按的触发频率设置*/
	private final static byte MENU_SET_LONG_PASS_REPEAT = 0x24;
	/**常量:结束符 动态存储用子关键字*/
	private final static String SUB_KEY_END_FLG = "SUB_KEY_END_FLG";
	/**常量:常量:模块已经被使用过的标志(用于初始化)*/
	private final static String SUB_KEY_MODULE_IS_USED = "SUB_KEY_MODULE_IS_USED";
	/**常量:按钮子键-按钮显示名*/
	private final static String SUB_KEY_BTN_NAME = "SUB_KEY_BTN_NAME";
	/**常量:按钮子键-按钮按下的值*/
	private final static String SUB_KEY_BTN_DOWN_VAL = "SUB_KEY_BTN_VAL";
	/**常量:按钮子键-按钮长按的值*/
	private final static String SUB_KEY_BTN_HOLD_VAL = "SUB_KEY_BTN_HOLD_VAL";
	/**常量:按钮子键-按钮抬起的值*/
	private final static String SUB_KEY_BTN_UP_VAL = "SUB_KEY_BTN_UP_VAL";
	/**常量:按钮子键-按钮长按时的触发频率*/
	private final static String SUB_KEY_BTN_REPEAT_FREQ = "SUB_KEY_BTN_REPEAT_FREQ";
	/**常量:按钮激发频率最小值(ms)*/
	private final static int BTN_REPEAT_MIN_FREQ = 50;
	/** 按钮点击后的触发类型 DOWN按下，HOLD保持按住，UP抬起 */
	public enum TIRGGER_TYPE{DOWN, HOLD, UP};
	
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
	/** 按钮发送值数组列表 (HashMap('down'='', 'hold'='', 'up'=''))*/
	private final List<HashMap<String, String>> mlBtnSendVal = new ArrayList<HashMap<String, String>>();
	
	/**当前按钮是否处于设置模式*/
	private boolean mbSetMode = false;	
	/**当前长按时重复执行频率*/
	private int miRepateFreq = 500;
	/**当前是否隐藏发送区*/
	private boolean mbHideSendArea = false;
	/**定义按钮的动作函数*/
	private class CRL implements ButtonPassListener{
		@Override
		public void onRepeat(View v, long duration, int repeatcount){ //长按
			if(mbSetMode)
				return;//长按事件下不能进入按钮设置模式。
			else
				onBtnClick_Array(v, TIRGGER_TYPE.HOLD);
		}
		@Override
		public void onDown(View v){ //按下
			onBtnClick_Array(v, TIRGGER_TYPE.DOWN);
		}
		@Override
		public void onUp(View v){ //抬起
			if(mbSetMode)
				return;//长按事件下不能进入按钮设置模式。
			else
				onBtnClick_Array(v, TIRGGER_TYPE.UP);
		}
	};	
	public CRL mCRL = new CRL();
	
	/**
	 * 页面构造
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState){
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
		this.mtvRecAreaTitle.setOnClickListener(new TextView.OnClickListener(){ //处理发送区的显示与隐藏
			@Override
			public void onClick(View v){
				if (v.getId() == R.id.actKeyBoard_tv_receive_area_title){
					String sTitle = getString(R.string.tv_receive_area_title);
					TextView tv = ((TextView)v);
					if (mbHideSendArea){
						sTitle += "\t\t("+ getString(R.string.tips_click_to_hide);
						sTitle += ":"+ getString(R.string.tv_send_area_title) +")";
						tv.setText(sTitle);
						mrlSendArea.setVisibility(View.VISIBLE);
					}else{
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
		
		/*默认启动时隐藏发送区*/
		String sTitle = getString(R.string.tv_receive_area_title);
		sTitle += "\t\t("+ getString(R.string.tips_click_to_show);
		sTitle += ":"+ getString(R.string.tv_send_area_title) +")";
		this.mtvRecAreaTitle.setText(sTitle);
		this.mrlSendArea.setVisibility(View.GONE);
		this.mrlSendArea.refreshDrawableState(); //刷新发送区
		this.mbHideSendArea = true; //隐藏发送区
		
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
    public void onResume(){
    	super.onResume();
    	Display display = this.getWindowManager().getDefaultDisplay();
    	Point size = new Point();
    	display.getSize(size);
    	
    	int iHeight = (size.x / 3) * 2 / 3; //按钮高度;
    	
    	for(int i=0; i<miBTN_CNT; i++){
			LayoutParams btnPara = mbtns[i].getLayoutParams();
			btnPara.height = iHeight;//将按钮设置为长方形
			mbtns[i].setLayoutParams(btnPara);
    	}
    }
    
	/**
	 * 析构
	 * */
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	this.mBSC.killReceiveData_StopFlg(); //强制终止接收函数
    }
    
	/**
	 * 屏幕旋转时的处理
	 * */
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
	}
	
	/**
	 * add top menu
	 * */
	@Override
    public boolean onCreateOptionsMenu(Menu menu){
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
    public boolean onMenuItemSelected(int featureId, MenuItem item){  
        switch(item.getItemId()){
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
	        	if (this.mbSetMode){	
	        		item.setTitle(R.string.menu_set_key_board_start);
	        		this.mtvSendView.setText(R.string.actKeyBoard_tv_Init);
	        	}else{	
	        		item.setTitle(R.string.menu_set_key_board_end);
	        		this.mtvSendView.setText(R.string.actKeyBoard_tv_set_keyboard_helper);
	        	}
	        	this.mbSetMode = !this.mbSetMode;//反转设置模式状态
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
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (KeyEvent.KEYCODE_BACK == keyCode){	//按回退键的处理
        	this.mbThreadStop = true; //终止接收线程
        	this.setResult(Activity.RESULT_CANCELED, null);
        	this.finish();
        	return true;
        }else
        	return super.onKeyDown(keyCode, event);
    }
    
    /**
     * 设置结束符（对话框）
     * @param byte bMode 字符显示模式 IO_MODE_HEX / IO_MODE_STRING
     * @return void
     * */
	private void selectEndFlg(){
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
    	builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
            	//设置输入输出的模式
            	String sHexEndFlg = etVal.getText().toString().trim();
            	if (sHexEndFlg.isEmpty()){	//未设置终止符
            		msEndFlg = new String();
            		mBSC.setReceiveStopFlg(msEndFlg); //设置结束符
	            	mDS.setVal(getLocalClassName(), SUB_KEY_END_FLG, sHexEndFlg).saveStorage();
	            	showEndFlg(); //显示当前结束符的设置信息
            	}else if (CHexConver.checkHexStr(sHexEndFlg)){
	            	msEndFlg = CHexConver.hexStr2Str(sHexEndFlg);
	            	mBSC.setReceiveStopFlg(msEndFlg); //设置结束符
	            	//记住当前设置的模式
	            	mDS.setVal(getLocalClassName(), SUB_KEY_END_FLG, sHexEndFlg).saveStorage();
	            	showEndFlg(); //显示当前结束符的设置信息
            	}else{
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
    	rgEndFlg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1){
				if (rb_rn.getId() == arg1){
					msEndFlg = msEND_FLGS[0];
					etVal.setEnabled(false); //不可修改
				}else if (rb_n.getId() == arg1){
					msEndFlg = msEND_FLGS[1];
					etVal.setEnabled(false); //不可修改
				}else
					etVal.setEnabled(true); //可修改
				etVal.setText(CHexConver.str2HexStr(msEndFlg));//输出HEX字符串
			}
    	});
    	/*结束符的输入框的监听*/
    	etVal.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0){
				String sEndFlg = etVal.getText().toString().trim();
				if (sEndFlg.isEmpty() || CHexConver.checkHexStr(sEndFlg)){
					etVal.setTextColor(android.graphics.Color.BLACK);
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
				}else{
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
    private void saveData2File(){
    	this.save2SD(this.mtvRecView.getText().toString().trim());
    }
	
	/**
	 * 载入按钮配置信息
	 * */
	private void loadBtnProfile(){
		String sTmp;
		final String sModel = this.getLocalClassName();
    	//键盘按钮值初始化
    	for(int i=0; i<miBTN_CNT; i++){
    		String sName = this.mDS.getStringVal(sModel, SUB_KEY_BTN_NAME.concat(String.valueOf(i)));
    		String sDown = this.mDS.getStringVal(sModel, SUB_KEY_BTN_DOWN_VAL.concat(String.valueOf(i)));
    		String sHold = this.mDS.getStringVal(sModel, SUB_KEY_BTN_HOLD_VAL.concat(String.valueOf(i)));
    		String sUp = this.mDS.getStringVal(sModel, SUB_KEY_BTN_UP_VAL.concat(String.valueOf(i)));
    		
    		if (!sName.isEmpty())//载入按钮名称
    			mbtns[i].setText(sName);
    		//载入按钮3状态的值
    		HashMap<String, String> mhBtnSend = new HashMap<String, String>();
    		mhBtnSend.put("DOWN", (sDown.isEmpty())?"":sDown);
    		mhBtnSend.put("HOLD", (sHold.isEmpty())?"":sHold);
    		mhBtnSend.put("UP", (sUp.isEmpty())?"":sUp);
    		this.mlBtnSendVal.add(mhBtnSend);
    	}
    	
    	for (int i=0; i<miBTN_CNT; i++)//绑定按钮的点击监听事件
			this.mbtns[i].bindListener(mCRL, 500);
		
		//保存按钮长按时的触发频率
		int iRepeat = this.mDS.getIntVal(this.getLocalClassName(), SUB_KEY_BTN_REPEAT_FREQ);
		if (0 == iRepeat) //默认频率
			this.setBtnRepeatFreq(500);
		else
			this.setBtnRepeatFreq(iRepeat);
		
		sTmp = String.format(getString(R.string.actKeyBoard_msg_repeat_freq_set)+"\n", iRepeat);
		this.mtvRecView.append(sTmp);//显示当前设定的促发频率
	}
    
    /**
     * 设定按钮长按的重复频率
     * @param long interval 毫秒
     * @return void
     * */
    private void setBtnRepeatFreq(int interval){
    	this.miRepateFreq = interval;
		for (int i=0; i<miBTN_CNT; i++)
			this.mbtns[i].setRepeatFreq(interval);
		this.mDS.setVal(this.getLocalClassName(), SUB_KEY_BTN_REPEAT_FREQ, interval).saveStorage();
    }
	
    /**
     * 载入终止符配置信息
     * @param String sModelName 模块名称
     * @return void
     * */
    private void loadProfile(){
    	String sHexEndFlg = this.mDS.getStringVal(this.getLocalClassName(), SUB_KEY_END_FLG);
    	//首次使用判断，默认第一次使用为false，取反则为true
    	boolean bModuleIsUsed = this.mDS.getBooleanVal(this.getLocalClassName(), SUB_KEY_MODULE_IS_USED);
    	if (!bModuleIsUsed){ //首次使用默认认为(\r\n)
    		this.msEndFlg = msEND_FLGS[0];
    		this.mDS.setVal(getLocalClassName(), SUB_KEY_MODULE_IS_USED, true) //标记已经使用过
    		.setVal(getLocalClassName(), SUB_KEY_END_FLG, CHexConver.str2HexStr(msEndFlg))//保存首次使用的终止符
    		.saveStorage();
    	}else if (sHexEndFlg.isEmpty())
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
    private void showEndFlg(){
    	if(msEndFlg.equals(msEND_FLGS[0])){
    		this.mtvRecView.append(
    				String.format(
    						this.getString(R.string.actKeyBoard_msg_helper_endflg), 
    						this.getString(R.string.dialog_end_flg_rn)));
    	}else if(msEndFlg.equals(msEND_FLGS[1])){
    		this.mtvRecView.append(
    				String.format(
    						this.getString(R.string.actKeyBoard_msg_helper_endflg), 
    						this.getString(R.string.dialog_end_flg_n)));
    	}else{
    		if (this.msEndFlg.isEmpty())
    			this.mtvRecView.append(getString(R.string.msg_helper_endflg_nothing));
    		else{
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
    public void onBtnClick_Array(View v, TIRGGER_TYPE tt){
    	int iBtnId = v.getId();
    	for(int i=0; i<miBTN_CNT; i++){
    		if (mbtns[i].getId() == iBtnId){ //找到了当前按下的按钮
    			if (mbSetMode) //设置模式
    				this.setBtnKeyboard(i);
    			else{
    				if (TIRGGER_TYPE.DOWN == tt && !this.mlBtnSendVal.get(i).get("DOWN").isEmpty()){
    					this.Send(this.mlBtnSendVal.get(i).get("DOWN"));//发送的处理
    				}else if (TIRGGER_TYPE.HOLD == tt && !this.mlBtnSendVal.get(i).get("HOLD").isEmpty()){
    					this.Send(this.mlBtnSendVal.get(i).get("HOLD"));//发送的处理
    				}else if (TIRGGER_TYPE.UP == tt && !this.mlBtnSendVal.get(i).get("UP").isEmpty()){
    					this.Send(this.mlBtnSendVal.get(i).get("UP"));//发送的处理
    				}
    			}
    			break;
    		}
    	}
    }
    
    /**
     * 数据的发送处理
     * @return void
     * */
    private void Send(String sData){
		if (!sData.equals("")){
			String sSend = sData;
			int iRet = 0;
			if (!this.msEndFlg.isEmpty()) //加入结束符的处理
				iRet = this.mBSC.Send(sSend.concat(this.msEndFlg));
			else
				iRet = this.mBSC.Send(sSend);
			
			if (iRet >= 0){ //检查通信状态
				if (View.VISIBLE == this.mrlSendArea.getVisibility()){ //发送区显示时才输出到TextView
					if (iRet == 0)
						this.mtvSendView.append(sSend.concat("(fail) "));
					else
						this.mtvSendView.append(sSend.concat("(succeed) "));
				}
			}else{	//链接丢失
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
     * @param int iId 按钮的ID序号
     * @return void
     * */
    private void setBtnKeyboard(int iId){
    	final AlertDialog adCtrl;
    	final String sModel = this.getLocalClassName();
    	final int iBtnSite = iId;

    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(this.getString(R.string.dialog_title_keyboard_set));//设置标题
    	LayoutInflater inflater = LayoutInflater.from(this);
    	//载入xml布局 必须为final 后面才能访问
    	final View view = inflater.inflate(R.layout.dialog_set_keyboard,null);
    	final EditText etBtnName =(EditText)view.findViewById(R.id.et_keyboard_set_BtnName);
    	final EditText etDownVal =(EditText)view.findViewById(R.id.et_keyboard_set_btn_down_value);
    	final EditText etHoldVal =(EditText)view.findViewById(R.id.et_keyboard_set_btn_hold_value);
    	final EditText etUpVal =(EditText)view.findViewById(R.id.et_keyboard_set_btn_up_value);
    	//初始化
    	etBtnName.setText(mbtns[iBtnSite].getText().toString());//设定初始化值
    	etDownVal.setText(this.mlBtnSendVal.get(iBtnSite).get("DOWN")); //按下的值
    	etHoldVal.setText(this.mlBtnSendVal.get(iBtnSite).get("HOLD")); //长按的值
    	etUpVal.setText(this.mlBtnSendVal.get(iBtnSite).get("UP")); //抬起的值
    	builder.setView(view);
    	builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener(){
    	    @Override
    	    public void onClick(DialogInterface dialog, int which){
    	    	String sBtnName = etBtnName.getText().toString().trim();
    	    	HashMap<String, String> hm = new HashMap<String, String>();
    	    	hm.put("DOWN", etDownVal.getText().toString().trim());
    	    	hm.put("HOLD", etHoldVal.getText().toString().trim());
    	    	hm.put("UP", etUpVal.getText().toString().trim());
    	    	mbtns[iBtnSite].setText(sBtnName);//设定按钮名字
    	    	mlBtnSendVal.set(iBtnSite, hm);//添加按钮的新值
    	    	/*保存新的按钮值*/
    	    	mDS.setVal(sModel, SUB_KEY_BTN_NAME.concat(String.valueOf(iBtnSite)), sBtnName)
    	    	.setVal(sModel, SUB_KEY_BTN_DOWN_VAL.concat(String.valueOf(iBtnSite)), hm.get("DOWN"))
    	    	.setVal(sModel, SUB_KEY_BTN_HOLD_VAL.concat(String.valueOf(iBtnSite)), hm.get("HOLD"))
    	    	.setVal(sModel, SUB_KEY_BTN_UP_VAL.concat(String.valueOf(iBtnSite)), hm.get("UP"))
    	    	.saveStorage();
    	    }
    	});
    	adCtrl = builder.create();
    	adCtrl.show();
    	
    	//对输入值加入验证
    	etDownVal.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s){
				String sSend = etDownVal.getText().toString().trim();
				if (sSend.length() == 0 && etUpVal.getText().toString().trim().isEmpty() &&
					etHoldVal.getText().toString().trim().isEmpty())
				{//全空时不能提交设置
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					return;
				}
				if (BluetoothSppClient.IO_MODE_HEX == mbtOutputMode){	//16进制值时对输入做验证
					if (CHexConver.checkHexStr(sSend)){
						etDownVal.setTextColor(android.graphics.Color.BLACK);
						adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					}else{	//HEX值无效，显示红色
						adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
						etDownVal.setTextColor(android.graphics.Color.RED);
					}
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
			}
    	});
    	etHoldVal.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s){
				String sSend = etHoldVal.getText().toString().trim();
				if (sSend.length() == 0 && etUpVal.getText().toString().trim().isEmpty() &&
					etDownVal.getText().toString().trim().isEmpty())
				{//全空时不能提交设置
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					return;
				}
				if (BluetoothSppClient.IO_MODE_HEX == mbtOutputMode){	//16进制值时对输入做验证
					if (CHexConver.checkHexStr(sSend)){
						etHoldVal.setTextColor(android.graphics.Color.BLACK);
						adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					}else{	//HEX值无效，显示红色
						adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
						etHoldVal.setTextColor(android.graphics.Color.RED);
					}
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
			}
    	});
    	etUpVal.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s){
				String sSend = etUpVal.getText().toString().trim();
				if (sSend.length() == 0 && etHoldVal.getText().toString().trim().isEmpty() &&
					etDownVal.getText().toString().trim().isEmpty())
				{//全空时不能提交设置
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					return;
				}
				if (BluetoothSppClient.IO_MODE_HEX == mbtOutputMode){	//16进制值时对输入做验证
					if (CHexConver.checkHexStr(sSend)){
						etUpVal.setTextColor(android.graphics.Color.BLACK);
						adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					}else{	//HEX值无效，显示红色
						adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
						etUpVal.setTextColor(android.graphics.Color.RED);
					}
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
			}
    	});
    }
    
    /**
     * 选择重复频率
     * @return void
     * */
    private void selectRepeatFreq(){
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
	    builder.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1){
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
	    etFreq.addTextChangedListener(new TextWatcher(){//对输入的重复频率进行有效性验证
			@Override
			public void afterTextChanged(Editable arg0){
				int iFreq;
				//取出设置的频率值
				if (etFreq.getText().toString().isEmpty())
					iFreq= 0;
				else
					iFreq = Integer.valueOf(etFreq.getText().toString());
				
				if (iFreq >= BTN_REPEAT_MIN_FREQ){
					etFreq.setTextColor(android.graphics.Color.BLACK);
					adCtrl.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
				}else{
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
    private void autoScroll(){
    	int iOffset = 0;
		//自动滚屏处理
		iOffset = this.mtvRecView.getMeasuredHeight() - this.msvRecView.getHeight();     
        if (iOffset > 0)
        	this.msvRecView.scrollTo(0, iOffset);
        
        if (this.mrlSendArea.getVisibility() == View.VISIBLE){	//当发送区显示的时候，才组刷新处理
			iOffset = this.mtvSendView.getMeasuredHeight() - this.msvSendView.getHeight();     
	        if (iOffset > 0)
	        	this.msvSendView.scrollTo(0, iOffset);
        }
    }
    
    //----------------
    /*多线程处理(建立蓝牙设备的串行通信连接)*/
    private class receiveTask extends AsyncTask<String, String, Integer>{
    	/**常量:连接丢失*/
    	private final static int CONNECT_LOST = 0x01;
    	/**常量:线程任务结束*/
    	private final static int THREAD_END = 0x02;
		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute(){
			mtvRecView.append(getString(R.string.msg_receive_data_wating));
			mbThreadStop = false;
		}
		/**
		  * 线程异步处理
		  */
		@Override
		protected Integer doInBackground(String... arg0){
			mBSC.Receive(); //首次启动调用一次以启动接收线程
			while(!mbThreadStop){
				if (!mBSC.isConnect())
					return CONNECT_LOST; //检查连接是否丢失
				else
					SystemClock.sleep(10);//接收等待延时，提高接收效率
				
				if (mBSC.getReceiveBufLen() > 0)
					this.publishProgress(mBSC.Receive());
			}
			return THREAD_END;
		}
		/**
		 * 线程内更新处理
		 */
		@Override
		public void onProgressUpdate(String... progress){
			mtvRecView.append(progress[0]); //显示区中追加数据
			autoScroll(); //自动卷屏处理
			refreshRxdCount(); //刷新接收数据统计值
		}
		/**
		  * 阻塞任务执行完后的清理工作
		  */
		@Override
		public void onPostExecute(Integer result){
			if (CONNECT_LOST == result) //通信连接丢失
				mtvRecView.append(getString(R.string.msg_msg_bt_connect_lost));
			else
				mtvRecView.append(getString(R.string.msg_receive_data_stop));//提示接收终止
			refreshHoldTime(); //刷新数据统计状态条-运行时间
		}
    }
}
