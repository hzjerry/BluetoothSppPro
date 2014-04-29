package mobi.dzs.android.storage;

/**
 * KV键值存储类
 * key -&gt; val 模式存储方式 key由主键与子键联合组成
 * @author JerryLi(lijian@dzs.mobi)
 * @version
 * <li>1.0 (2014-04-25)create</li>
 * @see<pre>
 * CKVStorage ds = new CSharedPreferences(this);
 * if (ds.isReady())
 * 	//存储区准备完成可以存储
 * else
 * 	//无法存储数据
 * </pre>
 */
public abstract class CKVStorage {
	/** 存储区准备完成 标记*/
	protected boolean _bSrorageIsReady = false;
	/**
	 * 存储区是否准备完成
	 * @return false:不能存储 / true:可以存储
	 */
	public boolean isReady(){
		return this._bSrorageIsReady;
	}
	/**
	 * 保存数据到文件
	 * @return boolean false:存储失败
	 * @see setVal()后，需要使用这个函数保存值
	 * */
	public abstract boolean saveStorage();
	/**
	 * 保存K-V键值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @param sVal String 保存的字符串值
	 * @see 必须用saveStorage()保存数据到存储区，否则设置值将不会生效
	 * @return CKVStorage
	 * */
	public abstract CKVStorage setVal(String sKey, String sSubKey, String sVal);
	/**
	 * 保存K-V键值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @param iVal int 保存的整形值
	 * @see 必须用saveStorage()保存数据到存储区，否则设置值将不会生效
	 * @return CKVStorage
	 * */
	public abstract CKVStorage setVal(String sKey, String sSubKey, int iVal);
	/**
	 * 保存K-V键值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @param dbVal getDoubleVal 保存的浮点值
	 * @see 必须用saveStorage()保存数据到存储区，否则设置值将不会生效
	 * @return CKVStorage
	 * */
	public abstract CKVStorage setVal(String sKey, String sSubKey, double dbVal);
	/**
	 * 保存K-V键值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @param lVal 保存的长整形
	 * @see 必须用saveStorage()保存数据到存储区，否则设置值将不会生效
	 * @return CKVStorage
	 * */
	public abstract CKVStorage setVal(String sKey, String sSubKey, long lVal);
	/**
	 * 保存K-V键值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @param bVal 保存的布尔值
	 * @see 必须用saveStorage()保存数据到存储区，否则设置值将不会生效
	 * @return CKVStorage
	 * */
	public abstract CKVStorage setVal(String sKey, String sSubKey, boolean bVal);
	/**
	 * 获取K键的值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @return String 不存在时返回  ""
	 * */
	public abstract String getStringVal(String sKey, String sSubKey);
	/**
	 * 获取K键的值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @return float /不存在时返回 0.0f
	 * */
	public abstract double getDoubleVal(String sKey, String sSubKey);
	/**
	 * 获取K键的值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @return int /不存在时返回0
	 * */
	public abstract int getIntVal(String sKey, String sSubKey);
	/**
	 * 获取K键的值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @return long /不存在时返回0
	 * */
	public abstract long getLongVal(String sKey, String sSubKey);
	/**
	 * 获取K键的值
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @return boolean /不存在时返回 false
	 * */
	public abstract boolean getBooleanVal(String sKey, String sSubKey);
	/**
	 * 移除KV键
	 * @param sKey String 主关键字
	 * @param sSubKey String 子关键字
	 * @see 必须用saveStorage()保存数据到存储区，否则设置值将不会生效
	 * @return CKVStorage
	 */
	public abstract CKVStorage removeVal(String sKey, String sSubKey);
}
