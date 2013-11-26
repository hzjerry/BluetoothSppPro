Bluetooth spp tools, Android Project 简介
===================================
Bluetooth Spp tools pro 是 Android平台的蓝牙串口通信开发调试工具，是一个免费的开源项目。

使用说明
-----------------------------------
你可以使用本工具作为研究之用，但不建议将其用于商业项目，如果由此引起的任何损失或法律问题与本开发者无任何关系。
如果你引用了本项目中的任何文件，就默许您已接受上面的这个条款。
[点击查看LICENSE信息](https://github.com/hzjerry/BluetoothSppPro/blob/master/LICENSE.txt)

基础结构
-----------------------------------
### 系统包含了基本模块：
> * application中的全局蓝牙通信对象可被子模块引用（建立连接后，可共用连接给其他子模块使用）
> * 公共的蓝牙设备搜索模块，可扫描周围的蓝牙设备（如果手机支持还能扫描到蓝牙低功耗设备，蓝牙4.0）
> * 公共蓝牙配对与uuid service扫描界面以及蓝牙串行连接，建立连接后，可分别切换不同的通信模式进行操作
> * 通信模式分为：1、字符流模式；2、键盘模式；3、命令行模式；（未来会加入传感器模式）
-----------------------------------
### 结构图
![Alt structure](https://github.com/hzjerry/BluetoothSppPro/blob/master/readme/structure.png "structure")