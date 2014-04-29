package mobi.dzs.android.BLE_SPP_PRO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mobi.dzs.android.util.LocalIOTools;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Environment;
import android.widget.Toast;

public class BaseActivity extends Activity{
	/**
	 * 激活Action Bar的回退按钮
	 * @return void
	 * */
	protected void enabledBack(){
		/*设置程序可以点击图标返回主界面*/
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
	/**
	 * 保存数据到SD卡
	 * @param sData String 待保存的数据
	 * @return void
	 * */
	protected void save2SD(String sData){
		String sRoot = null;
		String sFileName = null;
		String sPath = null;
		//判断sd卡是否存在,并取出根目录(末尾不带'/')
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			sRoot = Environment.getExternalStorageDirectory().toString();//获取跟目录
		else
			return;

		//生成文件名
		sFileName = (new SimpleDateFormat("MMddHHmmss", Locale.getDefault())).format(new Date()) + ".txt";
		//生成最终的保存路径
		sPath = sRoot.concat("/").concat(this.getString(R.string.app_name));
		if (LocalIOTools.coverByte2File(sPath, sFileName, sData.getBytes())){
			String sMsg = ("save to:").concat(sPath).concat("/").concat(sFileName);
			Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();//提示 文件保存成功
		}else{
			Toast.makeText(this, //提示 文件保存失败
			   getString(R.string.msg_save_file_fail),
			   Toast.LENGTH_SHORT).show();
		}
	}
	
    /**
     * 读取文本型资源文件的内容
     * @param int iRawID 资源文件ID
     * @return String / null
     */
    public String getStringFormRawFile(int iRawID){
    	InputStream is = this.getResources().openRawResource(iRawID);
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	int i;
		try{
			i = is.read();
	    	while(i != -1){
	    		baos.write(i);
	    		i = is.read();
	    	}
	    	is.close();
	    	return baos.toString().trim();
		}catch (IOException e){
			return null;
		}
    }
}
