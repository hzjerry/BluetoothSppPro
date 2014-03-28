package mobi.dzs.android.control.button;

import android.view.View;
/**
 * 按钮事件处理侦听接口
 * @author JerryLi
 * @see 用于 class RepeatingButton，在使用时，需要在运行类中，实例化接口的所有函数
 * */
public interface ButtonPassListener{
	/**长按按钮事件
	 * @param View v 按钮对象
	 * @param long duration 延迟毫秒数
	 * @param int repeatcount 重复调用次数
	 * */
    void onRepeat(View v, long duration, int repeatcount);

    /**
     * 按钮被按下的处理事件
     * */
    void onUp(View v);

    /**
     * 被按下的按钮抬起时的事件
     * */
    void onDown(View v);
}