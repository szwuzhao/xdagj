# XDAGJ测试网接入教程

现阶段XDAGJ只提供矿池功能，用户可以利用原有C语言版本的XDAG客户端钱包参与到测试环节中。本教程提供的环境非必需，用户可以根据自身操作系统执行对应的步骤。

[TOC]



## 钱包接入教程

### MacOS/Linux命令行钱包

MacOS 和 LInux平台目前没有可视化钱包，用户需要根据自身环境编译对应的客户端

**须知：由于RanndomX算法对系统内存要求较大，运行命令行钱包需要确保系统可用内存大于5G**

#### MacOS

系统版本：MacOS BigSur 11.2.3

- 安装依赖项

  ```shell
  brew install cmake openssl libtool gmp autoconf 
  ```

- RandomX依赖(首次编译该项目)

  ```shell
  git clone https://github.com/tevador/RandomX.git
  cd RandomX
  mkdir build && cd build
  cmake -DARCH=native ..
  make
  sudo make install
  ```

- 下载源码

  ```shell
  git clone https://github.com/XDagger/xdag.git
  ```

- 编译libsecp256k1(首次编译该项目)

  ```shell
  cd xdag/secp256k1
  ./autogen.sh
  ./configure
  make
  ./tests
  sudo make install
  ```

- 构建XDAG客户端

  ```shell
  mkdir build && cd build
  cmake .. -DBUILD_TAG=mac
  make
  ```

#### Ubuntu

系统版本 : Ubuntu20.04 LTS

- 安装依赖项

  ```shell
  apt-get install cmake gcc build-essential pkg-config libssl-dev libgmp-dev libtool libsecp256k1-dev librandomx-dev
  ```

- 为RandomX算法打开hugepage功能

  - 临时开启

  ```shell
  sudo sysctl -w vm.nr_hugepages=2560
  ```

  - 永久开启

  ```shell
  sudo bash -c "echo vm.nr_hugepages=2560 >> /etc/sysctl.conf"
  ```

- 下载源码

  ```shell
  git clone https://github.com/XDagger/xdag.git
  ```

- 构建XDAG客户端

  ```shell
  cd xdag
  mkdir build && cd build
  cmake .. 
  make
  ```

### XDAG命令行客户端用法

- 链接矿池

  ```shell
  ./xdag -t -randomx f -m <挖矿线程> <矿池地址>:<矿池端口>
  ```

  -m 为可选项目，表示挖矿线程，默认为0，即不进行挖矿操作

- 第一次运行

  - 第一次运行系统会提示您`set password`，该密码用于转账以及解锁钱包信息，请务必牢记，密码一旦遗失将无法找回
  - `enter random characters`该字段为随机数种子，用于加强文件的随机性，不是密码
  - 生成您的钱包地址需要花费一些时间，请耐心等待直到`xdag>`字段出现

- 查看链接到矿池的状态

  ```shell
  xdag> state
  [展示目前网络状态]
  ```

- 查询余额

  ```she
  xdag> balance
  [显示您账户的所有余额]
  ```

- 显示XDAG地址

  ```sh
  xdag> account
  [显示该账户下所拥有的xdag地址]
  ```

- 转账操作

  ```shell
  xdag> xfer
  [xfer  金额  地址]
  ```

- 退出

  ```she
  xdag> terminate
  ```

- 更过命令行指令

  ```shell
  ./xdag -h
  #或
  xdag> help
  ```

  更多详细的信息，可以参考[XDAG]()

### Windows 可视化钱包

官网下载可视化钱包使用，[下载地址](https://xdag.io/zh/)

解压后打开`wallet-config.json`文件，修改`pool_address`为测试网矿池地址，并将`is_test_net`修改为`true`

## 钱包备份与还原

### 备份

- 建议您备份整个工作目录，并且以只读的方式存储
- 工作目录下的`wallet.dat`和`dnet_key.dat`为钱包文件，对其进行单独的备份以防意外丢失

**请务必牢记第一次运行时设置的密码，若密码遗失将无法正确解密钱包文件，无法找回对应的账户**

### 还原

- 请将上述备份文件存入编译后客户端所在同目录下，启动程序后系统会自动识别并且通过密码还原



## 挖矿教程

建议下载XDAG的专用挖矿软件[XdagRandomXMiner]()

**须知1：使用挖矿软件，一个矿工需要占用2.5G的运行内存，该内存与矿工数量呈线性关系增长，若使用多个矿工，需要确保开启的内存页为1280*对应矿工数量**

**须知2：请确保钱包地址已经在XDAG网络上被确认，否则无法进行挖矿操作**

### Linux系统为RandomX算法打开hugepage功能

- 临时开启

  ```shell
  sudo sysctl -w vm.nr_hugepages=1280
  ```

- 永久开启

  ```shell
  sudo bash -c "echo vm.nr_hugepages=1280 >> /etc/sysctl.conf"
  ```

### MacOS / Linux

- 启动命令

  ```shell
  DaggerMiner -cpu  -test -p <矿池地址:端口> -t <挖矿线程数> -a <钱包地址>
  ```

### Windows

请参考[Enable the Lock Pages in Memory Option (Windows)](https://msdn.microsoft.com/en-gb/library/ms190730.aspx)打开hugepage

- 启动命令

  ```shell
  DaggerMiner.exe -cpu -test -p <矿池地址:端口> -t <挖矿线程数> -a <钱包地址> 
  ```



## 矿池地址

南京: 146.56.240.230:8882

新加坡: 51.79.222.25:8882



## 其他

现在您已经可以接入XDAGJ测试网络并进行转账功能了，同时可以在浏览器中查看一下你想要知道的信息

我们欢迎您将使用过程发生的错误或者其他一切可以帮助我们完善项目的信息，您可以在【Issues】向我们反馈

如果您有其他疑问，或者希望我们提供更多的教程，也可以在[Issues]()中进行提问。