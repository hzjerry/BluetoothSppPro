package mobi.dzs.android.bluetooth;

/**
 * PV操作锁的资源信号量
 * @version 1.0 2014-04-24
 * @author JerryLi (lijian@dzs.mobi)
 */
final public class CResourcePV {
	private int iCount=0;
	/** 
	 * 构造
	 * @param iResourceCount int 资源的数量
	 */
	public CResourcePV(int iResourceCount){
		this.iCount = iResourceCount;
	}
	/**
	 * 检查是否存在资源
	 * @return boolean
	 */
	public boolean isExist(){
		synchronized(this){
			return iCount == 0;
		}
	}
	/**
	 * 抢占资源操作
	 * @return
	 */
	public boolean seizeRes(){
		synchronized(this){
			if (this.iCount > 0){
				iCount--;
				return true;
			}else
				return false;
		}
	}
	/**
	 * 归还资源操作
	 * @return
	 */
	public void revert(){
		synchronized(this){
			iCount++;
		}
	}
}
