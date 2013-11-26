package mobi.dzs.android.BLE_SPP_PRO;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class actAbout extends BaseActivity
{
	private TextView mtvShow = null;
	/**
	 * 页面构造
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_about);
		
		this.enabledBack(); //激活返回按钮
		
		this.mtvShow = (TextView)this.findViewById(R.id.actAbout_tv_show);
		
    	if (this.getString(R.string.language).toString().equals("cn"))
    		this.mtvShow.setText(this.getStringFormRawFile(R.raw.about_cn) +"\n\n");
    	else
    		this.mtvShow.setText(this.getStringFormRawFile(R.raw.about_en) +"\n");
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
	        	this.setResult(Activity.RESULT_CANCELED); //返回到主界面
	        	this.finish();
	        	return true;
	        default:
	        	return super.onMenuItemSelected(featureId, item);
        }
    }
}
