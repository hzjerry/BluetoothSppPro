/**
 * 
 */
package mobi.dzs.android.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

/**
 * 本地IO操作类
 * @author JerryLi(hzjerry@gmail.com)
 * @version 1.0
 * @date 2010-06-25
 */
public class LocalIOTools{
	/**
	 * 通过本地文件载入字符串内容
	 * 
	 * @param String path 文件路徑
	 * @return String 读取成功后輸出文件内容
	 */
	public static String LoadFromFile(String path){
		StringBuffer sbOutBuf = new StringBuffer();
		String read;
		BufferedReader bufread;
		try	{
			File fhd = new File(path); // 获取文件句柄
			bufread = new BufferedReader(new FileReader(fhd)); // 打开缓存
			/* 循环方式逐行读入 */
			while ((read = bufread.readLine()) != null)
				sbOutBuf.append(read);
			bufread.close();
		}catch (Exception d){
//			System.out.println(d.getMessage());
			return null;
		}
		return sbOutBuf.toString(); // 输出文件内容
	}
	
	/**
	 * byte内容追加到本地文件
	 *   如果文件不存在，则创建
	 * 
	 * @param String path 文件路徑(末尾不要带“\”符号)
	 * @param String sFile 文件名
	 * @param byte[] bData 需要写入的数据
	 * 
	 * @return boolean
	 * @see android.permission.WRITE_EXTERNAL_STORAGE
	 */
	public static boolean appendByte2File(String sPath, String sFile, byte[] bData){
		try	{
			/*检查目录是否存在*/
			File fhd = new File(sPath); // 获取文件句柄
			if (!fhd.exists())
				if (!fhd.mkdirs())//目录不存在，创建之
					return false; //目录创建失败，退出
			
			/*检查文件是否存在*/
			fhd = new File(sPath +"\\"+ sFile); // 获取文件句柄
			if (!fhd.exists())
				if (!fhd.createNewFile()) //文件不存在，创建之
					return false; //文件创建失败，退出
			
			//追加方式写入文件
			FileOutputStream fso = new FileOutputStream(fhd, true);
			fso.write(bData);
			fso.close();
			return true;
		}catch (Exception d){
//			System.out.println(d.getMessage());
			return false;
		}
	}/*Output:
		if (LocalIOTools.appendByte2File("F:/temp", "javatest.txt", "this is test.\r\nbuf write".getBytes()))
			System.out.println("write ok.");
		else
			System.out.println("write fail.");
	 *///:~
	
	/**
	 * byte内容写入本地文件
	 *   如果文件存在则覆盖之
	 * 
	 * @param String path 文件路徑(末尾不要带“\”符号)
	 * @param String sFile 文件名
	 * @param byte[] bData 需要写入的数据
	 * 
	 * @return boolean
	 * @see android.permission.WRITE_EXTERNAL_STORAGE
	 */
	public static boolean coverByte2File(String sPath, String sFile, byte[] bData){
		try	{
			/*检查目录是否存在*/
			File fhd = new File(sPath); // 获取文件句柄
			if (!fhd.exists())
				if (!fhd.mkdirs())//目录不存在，创建之
					return false; //目录创建失败，退出
			
			/*检查文件是否存在*/
			fhd = new File(sPath +"/"+ sFile); // 获取文件句柄
			if (fhd.exists())
				fhd.delete(); //文件存在，删除
			
			//追加方式写入文件
			FileOutputStream fso = new FileOutputStream(fhd);
			fso.write(bData);
			fso.close();
			return true;
		}catch (Exception d){
			System.out.println(d.getMessage());
			return false;
		}		
	}/*Output:
		if (LocalIOTools.appendByte2File("F:/temp", "javatest.txt", "this is test.\r\nbuf write".getBytes()))
			System.out.println("write ok.");
		else
			System.out.println("write fail.");
	 *///:~  
}
