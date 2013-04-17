package mobi.dzs.android.bluetooth;

import java.io.UnsupportedEncodingException;

import mobi.dzs.android.util.CHexConver;

/**
 * 蓝牙通信的SPP客户端
 * @version 1.0 2013-03-17
 * @author JerryLi (lijian@dzs.mobi)
 * */
public final class BluetoothSppClient extends BTSerialComm
{
	/**常量:输入输出模式为16进制值*/
	public final static byte IO_MODE_HEX = 0x01; 
	/**常量:输入输出模式为字符串*/
	public final static byte IO_MODE_STRING = 0x02;
	/**当前发送时的编码模式*/
	private byte mbtTxDMode = IO_MODE_STRING;
	/**当前接收时的编码模式*/
	private byte mbtRxDMode = IO_MODE_STRING;
	/**接收终止符*/
	private byte[] mbtEndFlg = null;
	/**指定:输入输出字符集 默认不指定(UTF-8:一个全角占3字节/GBK:一个全角占2字节)*/
	protected String msCharsetName = null;
	
	/**
	 * 创建蓝牙SPP客户端类
	 * @param String MAC 蓝牙MAC地址
	 * @return void
	 * */
	public BluetoothSppClient(String MAC)
	{
		super(MAC); //执行父类的构造函数
	}
	
	/**
	 * 设置发送时的字符串模式
	 * @param bOutIO_Mode 输出io模式 IO_MODE_HEX / IO_MODE_STRING
	 * @return void
	 * */
	public void setTxdMode(byte bOutputMode)
	{
		this.mbtTxDMode = bOutputMode;
	}
	
	/**
	 * 获取发送时的字符串模式
	 * @return byte 输出io模式 IO_MODE_HEX / IO_MODE_STRING
	 * */
	public byte getTxdMode()
	{
		return this.mbtTxDMode;
	}
	
	/**
	 * 设置接收时的字符串输出模式
	 * @param bOutIO_Mode 输出io模式 IO_MODE_HEX / IO_MODE_STRING
	 * */
	public void setRxdMode(byte bOutputMode)
	{
		this.mbtRxDMode = bOutputMode;
	}
	
	/**
	 * 发送数据给设备
	 * 
	 * @param byte btData[] 需要发送的数据位
	 * @return int >0 发送正常, 0未发送数据, -2:连接未建立; -3:连接丢失
	 * */
	public int Send(String sData)
	{
		if (IO_MODE_HEX == this.mbtTxDMode) //16进制字符串转换成byte值
		{
			if (CHexConver.checkHexStr(sData))
				return SendData(CHexConver.hexStr2Bytes(sData));
			else
				return 0; //无效的HEX值
		}
		else //将字符串直接变为char的byte送出
		{
			if (null != this.msCharsetName)
			{
				try
				{	//尝试做字符集转换
					return this.SendData(sData.getBytes(this.msCharsetName));
				}
				catch (UnsupportedEncodingException e)
				{	//字符集转换失败时使用默认字符集
					return this.SendData(sData.getBytes());
				}
			}
			else
				return this.SendData(sData.getBytes());
		}
		
		
	}
	
	/**
	 * 接收设备数据
	 * @return String null:未连接或连接中断 / String:数据
	 * */
	public String Receive()
	{
		byte[] btTmp = this.ReceiveData();
		
		if (null != btTmp)
		{
			if (IO_MODE_HEX == this.mbtRxDMode) //16进制字符串转换成byte值
				return (CHexConver.byte2HexStr(btTmp, btTmp.length)).concat(" ");
			else
				return new String(btTmp);
		}
		else
			return null;
	}
	
	/**
	 * 设置接收指令行的终止字符
	 * @return void
	 * @see 仅用于ReceiveStopFlg()函数
	 * */
	public void setReceiveStopFlg(String sFlg)
	{
		this.mbtEndFlg = sFlg.getBytes();
	}
	
	/**
	 * 设置处理字符集(默认为UTF-8)
	 * @param String sCharset 设置字符集 GBK/GB2312
	 * @return void
	 * @see 此设置仅对ReceiveStopFlg()与Send()函数有效
	 * */
	public void setCharset(String sCharset)
	{
		this.msCharsetName = sCharset;
	}
	
	/**
	 * 接收设备数据，指令行模式（阻塞模式）
	 *  备注：即在接收时遇到终止字符后，才会输出结果，并在输出结果中会剔除终止符
	 *  @return null:未连接或连接中断/String:取到数据
	 * */
	public String ReceiveStopFlg()
	{
		byte[] btTmp = null;
		
		if (null == this.mbtEndFlg)
			return new String(); //未设置终止符
		
		btTmp = this.ReceiveData_StopFlg(this.mbtEndFlg);
		if (null == btTmp)
			return null; //无效的接收
		else
		{
			if (null == this.msCharsetName)
				return new String(btTmp);
			else
			{
				try
				{	//尝试对取得的值做字符集转换
					return new String(btTmp, this.msCharsetName);
				}
				catch (UnsupportedEncodingException e)
				{	//转换失败时直接用UTF-8输出
					return new String(btTmp);
				}
			}
		}

	}
}
