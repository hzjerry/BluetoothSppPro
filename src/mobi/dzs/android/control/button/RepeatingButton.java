package mobi.dzs.android.control.button;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

/**
 * 自定义按钮控件
 *   扩展长按，按下，抬起，三个事件
 *   @author jerryli lijian@dzs.mobi
 *   @see 对象被构造后，需要调用bindListener()绑定实例化的接口类，与长按的执行频率，
 *        与设置按钮的激活事件
 * */
public class RepeatingButton extends Button
{
	/**长按事件标志*/
	public static final byte mEVENT_REPEAT = 0x01;
    /**按钮释放事件标志*/
    public static final byte mEVENT_UP = 0x02;
    /**按钮按下事件标志*/
    public static final byte mEVENT_DOWN = 0x04;

	/**记录长按开始时间 */
    private long mStartTime;
    /**记录已经重复执行次数的计数器*/
    private int mRepeatCount = 0;
    /**按钮重复执行函数的接口对象*/
    private ButtonPassListener mListener = null;
    /**Timer触发间隔，按下按钮后，每500ms执行一次*/
    private long mInterval = 500;
    /**当前按钮工作的触发模式*/
    private byte mEventMode = mEVENT_UP;

	public RepeatingButton(Context context)
	{
		this(context, null);
	}

	public RepeatingButton(Context context, AttributeSet attrs)
	{
	    this(context, attrs, android.R.attr.buttonStyle);
	}

	public RepeatingButton(Context context, AttributeSet attrs, int defStyle)
	{
	    super(context, attrs, defStyle);
	    setFocusable(true); //允许获得焦点
	    setLongClickable(true); //启用长按事件
	}

	/**
	 * 绑定UP/Down两种事件的侦听函数
	 * @param ButtonPassListener l 按钮事件的侦听接口
	 * @param int bFlg 激活的事件属性
	 * @return void
	 * @see <p>bFlg：如果需要联合属性可以相加<br/>
	 * 		RepeatingButton.mEVENT_UP<br/>
	 * 		RepeatingButton.mEVENT_DOWN<br/>
	 *      </p>
	 * */
    public void bindListener(ButtonPassListener l, byte bFlg)
    {
        this.mListener = l;
        this.mEventMode = bFlg;
    }

	/**
	 * 绑定长按事件的侦听函数
	 * @param long interval 长按执行频率ms
	 * @param ButtonPassListener l 按钮事件的侦听接口
	 * @return void
	 * */
    public void bindListener(long interval, ButtonPassListener l)
    {
        this.mListener = l;
        this.mInterval = interval;
        this.mEventMode = mEVENT_REPEAT;
    }
    
    /**设定重复执行的频率*/
    public void setRepeatFreq(long interval)
    {
    	this.mInterval = interval;
    }
    
    /**
     * 获取当前按钮的事件模式
     * @return 	 RepeatingButton.mEVENT_UP = UP<br/>
	 * 		     RepeatingButton.mEVENT_DOWN = DOWN<br/>
	 * 			 RepeatingButton.mEVENT_REPEAT = REPEAT<br/>
     * */
    public byte getEventMode()
    {
    	return this.mEventMode;
    }

    /**
     * 启动长按绑定事件
     * @return boolean
     * */
    @Override
    public boolean performLongClick()
    {
    	if ((this.mEventMode & mEVENT_REPEAT) > 0x00)
    	{	//启用了长按按钮事件
	    	this.mStartTime = SystemClock.elapsedRealtime();
	    	this.mRepeatCount = 0;
	    	post(this.mRepeater);
	        return true;
    	}
    	else
    		return false;
    }

    /**
     * 这里是屏幕的处理事件
     * @param MotionEvent event 点击的事件类型
     * @return boolean
     * */
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
        	if ((this.mEventMode & mEVENT_REPEAT) != 0x00)
        	{	//启用长按事件后的处理
	            removeCallbacks(this.mRepeater);
	            if (this.mStartTime != 0)
	            {
	                doRepeat(true);
	                this.mStartTime = 0;
	            }
            }
        	else if ((this.mEventMode & mEVENT_UP) != 0x00)
        		this.doUp(); //按钮抬起时的处理
        }
        else if (event.getAction() == MotionEvent.ACTION_DOWN &&
        		 (this.mEventMode & mEVENT_DOWN) != 0x00
        		)
        {	//按钮被按下时的处理
        	this.doDown();
        }
        return super.onTouchEvent(event);
    }

    /**用于处理长按的线程部分*/
    private Runnable mRepeater = new Runnable()
    {  //在线程中判断重复
        public void run()
        {
            doRepeat(false);
            if (isPressed())
            	/*将要执行的线程对象, 第二个参数是long类型：延迟的时间*/
                postDelayed(this, mInterval); //计算长按后延迟下一次累加
        }
    };

    /**
     * 由线程控制的长按执行函数调用体
     * @param boolean last 是否启用重复执行次数
     * */
    public void doRepeat(boolean last)
    {
        if (this.mListener != null)
        	this.mListener.onRepeat
        	(
        			this,
        			SystemClock.elapsedRealtime() - this.mStartTime,
        			last ? -1 : this.mRepeatCount++
        	);
    }

    /**
     * 按钮被抬起的时处理调用体
     * */
    public void doUp()
    {
        if (this.mListener != null)
        	this.mListener.onUp(this);
    }

    /**
     * 按钮被按下时的处理调用体
     * */
    public void doDown()
    {
        if (this.mListener != null)
        	this.mListener.onDown(this);
    }
}
