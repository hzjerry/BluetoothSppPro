package mobi.dzs.android.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

final public class CSharedPreferences extends CKVStorage{
	private static final String msDELIMITER = "|_|"; 
	/** Android Activity对象容器 */
	private Context mc = null;
	/** 包名 */
	private String msPkgName;
	/** SharedPreferences的Editor接口对象 */
	private Editor meSaveData = null;
	/** android内部存储对象，存储位置:/data/data/<package name>/shared_prefs */
	private SharedPreferences mSP = null;
	/**
	 * 构造函数
	 * @param C Context Activity的父类引用
	 */
	public CSharedPreferences(Context C){
		this.mc = C;
		
		PackageManager manager = mc.getPackageManager();
		PackageInfo info;
		try{
			info = manager.getPackageInfo(mc.getPackageName(), 0);
			this.msPkgName = info.packageName;
			this.mSP = mc.getSharedPreferences(this.msPkgName, Context.MODE_PRIVATE);
			this._bSrorageIsReady = true; //存储区准备完成
		}catch (NameNotFoundException e){
			e.printStackTrace();
			this.msPkgName = "";
			this._bSrorageIsReady = false;
		}
	}
	
	/**
	 * 自动创建一个新的存储区<br/>
	 * (如果不存在存储区对象的话)
	 * */
	private void newStorage(){
		if (null == this.meSaveData)
			this.meSaveData = 
				mc.getSharedPreferences(this.msPkgName, Context.MODE_PRIVATE).edit(); //本地存储区
	}
	
	/**
	 * 生成关键字索引名
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @return String 合并后的关键字索引名
	 * */
	private String getIdxKey(String sKey, String sSubKey){
		return sKey + msDELIMITER + sSubKey;
	}
	
	@Override
	public boolean saveStorage() {
		if (null != this.meSaveData){
			this.meSaveData.commit();//提交数据保存
			this.meSaveData = null; //保存后释放存储区
			return true;
		}
		else
			return false;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, String sVal) {
		this.newStorage();
		this.meSaveData.putString(this.getIdxKey(sKey, sSubKey), sVal);
		return this;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, int iVal) {
		this.newStorage();
		this.meSaveData.putInt(this.getIdxKey(sKey, sSubKey), iVal);
		return this;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, double dbVal) {
		this.newStorage();
		this.meSaveData.putFloat(this.getIdxKey(sKey, sSubKey), (float)dbVal);
		return this;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, long lVal) {
		this.newStorage();
		this.meSaveData.putLong(this.getIdxKey(sKey, sSubKey), lVal);
		return this;
	}

	@Override
	public CKVStorage setVal(String sKey, String sSubKey, boolean bVal) {
		this.newStorage();
		this.meSaveData.putBoolean(this.getIdxKey(sKey, sSubKey), bVal);
		return this;
	}

	@Override
	public String getStringVal(String sKey, String sSubKey) {
		return this.mSP.getString(this.getIdxKey(sKey, sSubKey), "");
	}

	@Override
	public double getDoubleVal(String sKey, String sSubKey) {
		return (double)this.mSP.getFloat(this.getIdxKey(sKey, sSubKey), 0.0f);
	}

	@Override
	public int getIntVal(String sKey, String sSubKey) {
		return this.mSP.getInt(this.getIdxKey(sKey, sSubKey), 0);
	}

	@Override
	public long getLongVal(String sKey, String sSubKey) {
		return this.mSP.getLong(this.getIdxKey(sKey, sSubKey), 0);
	}

	@Override
	public boolean getBooleanVal(String sKey, String sSubKey) {
		return this.mSP.getBoolean(this.getIdxKey(sKey, sSubKey), false);
	}

	@Override
	public CKVStorage removeVal(String sKey, String sSubKey) {
		this.newStorage();
		this.meSaveData.remove(this.getIdxKey(sKey, sSubKey));
		return this;
	}

}
