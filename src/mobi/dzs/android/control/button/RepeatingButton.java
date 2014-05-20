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
public class RepeatingButton extends Button{
	/**记录长按开始时间 */
    private long mStartTime;
    /**记录已经重复执行次数的计数器*/
    private int mRepeatCount = 0;
    /**按钮重复执行函数的接口对象*/
    private ButtonPassListener mListener = null;
    /**Timer触发间隔，按下按钮后，每500ms执行一次*/
    private long mInterval = 500;

	public RepeatingButton(Context context)	{
		this(context, null);
	}

	public RepeatingButton(Context context, AttributeSet attrs)	{
	    this(context, attrs, android.R.attr.buttonStyle);
	}

	public RepeatingButton(Context context, AttributeSet attrs, int defStyle){
	    super(context, attrs, defStyle);
	    setFocusable(true); //允许获得焦点
	    setLongClickable(true); //启用长按事件
	}
	/**
	 * 绑定长按事件的侦听函数
	 * @param long interval 长按执行频率ms
	 * @param ButtonPassListener l 按钮事件的侦听接口
	 * @return void
	 * */
    public void bindListener(ButtonPassListener l, long lHold_feq){
        this.mListener = l;
        this.mInterval = lHold_feq;
    }
    
    /**设定重复执行的频率*/
    public void setRepeatFreq(long interval){
    	this.mInterval = interval;
    }

    /**
     * 启动长按绑定事件
     * @return boolean
     * */
    @Override
    public boolean performLongClick(){
    	this.mStartTime = SystemClock.elapsedRealtime(); //记录开始执行长按的时间
    	this.mRepeatCount = 0;
    	post(this.mRepeater); //绑定需奥执行的长按时回调线程
        return true;
    }

    /**
     * 这里是屏幕的处理事件
     * @param event MotionEvent  点击的事件类型
     * @return boolean
     * */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_UP){
        	removeCallbacks(this.mRepeater); //结束回调长按线程
        	this.doUp(); //按钮抬起时的处理
        }else if (event.getAction() == MotionEvent.ACTION_DOWN ){ //按钮被按下时的处理
        	this.doDown();
        }
        return super.onTouchEvent(event);
    }

    /**用于处理长按的线程部分*/
    private Runnable mRepeater = new Runnable(){  //在线程中判断重复
    	/** 首次调用长按事件的时间点（超过设定的长按频率时间后，执行长按事件） */
    	private long lFirstRunTime = mStartTime + mInterval;
    	/** 线程循环体 */
        public void run(){
        	if (SystemClock.elapsedRealtime() > lFirstRunTime)//超过设定的长按频率时间后，执行长按事件
        		doRepeat(false); //执行长安事件
            if (isPressed()) //执行完成后，如果按钮还保持按住，则延迟后再次执行
                postDelayed(this, mInterval); //计算长按后延迟下一次累加
        }
    };

    /**
     * 由线程控制的长按执行函数调用体
     * @param last boolean 是否启用重复执行次数
     * */
    public void doRepeat(boolean last){
        if (this.mListener != null)
        	this.mListener.onRepeat(
    			this,
    			SystemClock.elapsedRealtime() - this.mStartTime,
    			last ? -1 : this.mRepeatCount++
        	);
    }

    /**
     * 按钮被抬起的时处理调用体
     * */
    public void doUp(){
        if (this.mListener != null)
        	this.mListener.onUp(this);
    }

    /**
     * 按钮被按下时的处理调用体
     * */
    public void doDown(){
        if (this.mListener != null)
        	this.mListener.onDown(this);
    }
}
