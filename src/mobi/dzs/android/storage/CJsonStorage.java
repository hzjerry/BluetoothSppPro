package mobi.dzs.android.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;
/**
 * 数据存储对象<br/>
 * 默认将数据存储在外部存储SD卡的package_name/目录下
 * @author Jerry.Li(hzjerry@gmail.com)
 * @version 0.201404026
 * @see 需要权限<br/>
 * &lt;uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/&gt;
 * &lt;uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/&gt;
 */
final public class CJsonStorage extends CKVStorage{
	/** Android Activity对象容器 */
	private Context _c = null;
	/** 包名 */
	private String _sPkgName;
	/** 配置文件名（默认值） */
	private String _sPROFILES_NAME = "profiles.json";
	/** json 类对象 */
	private JSONObject _json = null;
	/**
	 * 构造函数
	 * @param C Context Activity的父类引用
	 */
	public CJsonStorage(Context C){
		this._c = C;
		try{
			this._sPkgName =  (_c.getPackageManager().getPackageInfo(_c.getPackageName(), 0)).packageName;
			this._bSrorageIsReady = this.readStorage(); //载入配置信息
		}catch (NameNotFoundException e){
			e.printStackTrace();
		}
	}
	/**
	 * 构造函数(指定外部存储的根目录)
	 * @param C Context Activity的父类引用
	 * @param sExtRootPath String 外部存储的根目录
	 */
	public CJsonStorage(Context C, String sExtRootPath){
		this._c = C;
		this._sPkgName =  sExtRootPath;
		this._bSrorageIsReady = this.readStorage(); //载入配置信息
	}
	/**
	 * 构造函数(指定外部存储的根目录)
	 * @param C Context Activity的父类引用
	 * @param sExtRootPath String 外部存储的根目录
	 * @param sConfigFile String 指定配置文件名(只需要文件名，不需要扩展名)
	 */
	public CJsonStorage(Context C, String sExtRootPath, String sConfigFile){
		this._c = C;
		this._sPkgName =  sExtRootPath;
		this._sPROFILES_NAME = sConfigFile.concat(".json");
		this._bSrorageIsReady = this.readStorage(); //载入配置信息
	}
	/**
	 * 获取文件操作句柄
	 * @return
	 */
	private File getFilehd() {
		File f = null;
		String sRoot = null;
		//查找SD卡是否存在
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			sRoot = Environment.getExternalStorageDirectory().getAbsolutePath();//获取跟目录
			f = new File(sRoot.concat("/").concat(_sPkgName).concat("/")); // 获取文件句柄
			if (!f.exists())//检查目录是否存在，不存在则创建目录
				f.mkdirs();
			//得到文件句柄
			f = new File(sRoot.concat("/").concat(_sPkgName).concat("/"), _sPROFILES_NAME);
			Log.v(_sPkgName, sRoot.concat("/").concat(_sPkgName).concat("/")+ _sPROFILES_NAME);
		}
		else{//不存在SD存储区，则存储到内部目录
			f = new File(_c.getFilesDir(), _sPROFILES_NAME);// 获取文件句柄
		}
		return f;
	}
	/**
	 * 读取存储文件
	 * @return boolean false:数据载入失败 / true:载入成功
	 */
	private boolean readStorage(){
		char[] cBuf = new char[512];
		StringBuilder sb = new StringBuilder();
		int iRet = 0;
		try {
			FileInputStream fis = new FileInputStream(this.getFilehd());
			InputStreamReader reader = new InputStreamReader(fis);
			while((iRet = reader.read(cBuf)) > 0)
				sb.append(cBuf, 0, iRet);
			reader.close();
			fis.close();
			//载入json数据
			String sTmp = sb.toString();
			if (sTmp.length() > 0)
				_json = new JSONObject(sTmp);
			else
				_json = new JSONObject();
			return true;
		} catch (FileNotFoundException e) { //文件不存在，创建空节点
			_json = new JSONObject();
			return true;
		} catch (IOException e) { //IO读取读取失败，创建空节点
			_json = new JSONObject();
			return true;
		} catch (JSONException e) { //json载入失败
			_json = new JSONObject(); //放弃载入，生成一个新对象
			return true;
		}
	}
	
	@Override
	public boolean saveStorage() {
		File f = getFilehd();
		if (f.exists())
			f.delete(); //文件存在，删除
		try {
//			f.createNewFile(); //创建新文件
			FileOutputStream fso = new FileOutputStream(f);
			fso.write(this._json.toString().getBytes());
			fso.close();
			fso = null;
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, String sVal) {
		if (this.isReady()){
			try {
				JSONObject jTmp = this._json.optJSONObject(sKey);
				if (null == jTmp){
					if (null == sVal)
						sVal = "";
					this._json.put(sKey, new JSONObject().put(sSubKey, sVal));
				}
				else
					jTmp.put(sSubKey, sVal);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, int iVal) {
		if (this.isReady()){
			try {
				JSONObject jTmp = this._json.optJSONObject(sKey);
				if (null == jTmp)
					this._json.put(sKey, new JSONObject().put(sSubKey, iVal));
				else
					jTmp.put(sSubKey, iVal);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, double dbVal) {
		if (this.isReady()){
			try {
				JSONObject jTmp = this._json.optJSONObject(sKey);
				if (null == jTmp)
					this._json.put(sKey, new JSONObject().put(sSubKey, dbVal));
				else
					jTmp.put(sSubKey, dbVal);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, long lVal) {
		if (this.isReady()){
			try {
				JSONObject jTmp = this._json.optJSONObject(sKey);
				if (null == jTmp)
					this._json.put(sKey, new JSONObject().put(sSubKey, lVal));
				else
					jTmp.put(sSubKey, lVal);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, boolean bVal) {
		if (this.isReady()){
			try {
				JSONObject jTmp = this._json.optJSONObject(sKey);
				if (null == jTmp)
					this._json.put(sKey, new JSONObject().put(sSubKey, bVal));
				else
					jTmp.put(sSubKey, bVal);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	@Override
	public String getStringVal(String sKey, String sSubKey) {
		JSONObject jsObj = null;
		if (this.isReady()){
			if (null != (jsObj = this._json.optJSONObject(sKey))){
				try {
					return jsObj.getString(sSubKey);
				} catch (JSONException e) {
					return "";
				}
			}
		}
		return "";
	}

	@Override
	public double getDoubleVal(String sKey, String sSubKey) {
		JSONObject jsObj = null;
		if (this.isReady()){
			if (null != (jsObj = this._json.optJSONObject(sKey))){
				try {
					return jsObj.getDouble(sSubKey);
				} catch (JSONException e) {
					return 0.0d;
				}
			}
		}
		return 0.0d;
	}

	@Override
	public int getIntVal(String sKey, String sSubKey) {
		JSONObject jsObj = null;
		if (this.isReady()){
			if (null != (jsObj = this._json.optJSONObject(sKey))){
				try {
					return jsObj.getInt(sSubKey);
				} catch (JSONException e) {
					return 0;
				}
			}
		}
		return 0;
	}

	@Override
	public long getLongVal(String sKey, String sSubKey) {
		JSONObject jsObj = null;
		if (this.isReady()){
			if (null != (jsObj = this._json.optJSONObject(sKey))){
				try {
					return jsObj.getLong(sSubKey);
				} catch (JSONException e) {
					return 0l;
				}
			}
		}
		return 0l;
	}

	@Override
	public boolean getBooleanVal(String sKey, String sSubKey) {
		JSONObject jsObj = null;
		if (this.isReady()){
			if (null != (jsObj = this._json.optJSONObject(sKey))){
				try {
					return jsObj.getBoolean(sSubKey);
				} catch (JSONException e) {
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public CKVStorage removeVal(String sKey, String sSubKey) {
		JSONObject jsObj = null;
		if (this.isReady()){
			if (null != (jsObj = this._json.optJSONObject(sKey))){
				jsObj.remove(sSubKey);
				if (jsObj.length() == 0) //删除无子Key的父Key
					jsObj.remove(sKey);
			}
		}
		return this;
	}

}
