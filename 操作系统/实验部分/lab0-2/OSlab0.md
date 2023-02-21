# 实验 0: Rinux 环境搭建和内核编译

## 0  实验简介

搭建实验虚拟机、docker运行环境。通过在QEMU上运行Linux来熟悉如何从源代码开始将内核运行在QEMU模拟器上，并且掌握使用gdb协同QEMU进行联合调试，为后续实验打下基础。

## 1  实验目的

- 了解容器的使用

- 使用交叉编译工具, 完成Linux内核代码编译
- 使用QEMU运行内核
- 熟悉GDB和QEMU联合调试

## 2  操作方法和实验步骤

### 2.1 搭建 Docker环境

#### 下载并导入docker镜像

```shell
### 导入docker镜像
$ cat oslab.tar | docker import - oslab:2022
### 执行命令后若出现以下错误提示
### ERROR: Got permission denied while trying to connect to the Docker daemon socket atunix:///var/run/docker.sock### 可以使用下面命令为该文件添加权限来解决
### $ sudo chmod a+rw /var/run/docker.sock
### 查看docker镜像
$ docker images
```

![image-20220915103601878](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915103601878.png)

#### 从镜像中创建⼀个容器并进入该容器

```shell
### 从镜像创建一个容器
$ docker run --name oslab -it oslab:2022 /bin/bash # --name:容器名称 -i:交互式操作 -t:终端 
### 提示符变为 '#' 表明成功进入容器后面的字符串根据容器而生成，为容器
### exit (or CTRL+D)  
```

![image-20220915104312539](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915104312539.png)

```shell
### 启动处于停止状态的容器
$ docker start oslab
# oslab为容器名称
$ docker ps# 可看到容器已经启动
### 从终端连入 docker 容器
$ docker exec -it oslab /bin/bash
```

![image-20220915104337946](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915104337946.png)



### 2.2 获取 Linux 源码和已经编译好的文件系统

1. 进入home目录。

2. 使用git 工具 clone 本仓库。其中已经准备好了根文件系统的镜像。根文件系统为Linux Kenrel 提供了基础的文件服务，在启动

Linux Kernel 时是必要的。

```
# git clone https://gitee.com/zju_xiayingjie/os22fall-stu
# cd os22fall-stu/src/lab0
# ls 
```

![image-20220915105113402](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915105113402.png)

![image-20220915105228609](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915105228609.png)

3. 在当前目录下，从https://www.kernel.org下载最新稳定版本的 Linux 源码。

![image-20220915105347274](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915105347274.png)

​	4. 使用解压缩Linux 源码包至 /home/os22fall-stu/src/lab0 目录下

![image-20220915105518673](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915105518673.png)



### 2.3 使用QEMU运行内核

```shell
# pwd
/home/os22fall-stu/src/lab0/
#qemu-system-riscv64 -nographic -machine virt -kernel ./linux-5.19.8/arch/riscv/boot/Image \
-device virtio-blk-device,drive=hd0 -append "root=/dev/vda ro console=ttyS0" \
-bios default -drive file=rootfs.img,format=raw,id=hd0
```

![image-20220915145137456](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915145137456.png)

![image-20220915145323776](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915145323776.png)



### 2.4 使用gdb调试内核

对于终端1

```shell
### Terminal 1
# pwd
/home/os22fall-stu/src/lab0/
# export RISCV=/opt/riscv
### 设置环境变量
# export PATH=$PATH:$RISCV/bin
# qemu-system-riscv64 -nographic -machine virt -kernel ./linux-5.19.8/arch/riscv/boot/Image \
-device virtio-blk-device,drive=hd0 -append "root=/dev/vda ro console=ttyS0" \
-bios default -drive file=rootfs.img,format=raw,id=hd0 -S -s
```

![image-20220915145846535](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220915145846535.png)

对于终端2

```shell
### Terminal 2
# export RISCV=/opt/riscv
### 设置环境变量
# export PATH=$PATH:$RISCV/bin
# riscv64-unknown-linux-gnu-gdb ./linux-5.19.8/vmlinux
```

![image-20221014140555052](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221014140555052.png)

当连接成功后，尝试以下的命令等：

```shell
(gdb) target remote localhost:1234 ### 连接 qemu
(gdb) b start_kernel ### 设置断点
(gdb) continue ### 继续执⾏
(gdb) quit ### 退出 gdb
```

以下执行的指令为设置断点，显示断点，单步执行，跳过函数的单步执行

首先：远程连接

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221014140742823.png" alt="image-20221014140742823" style="zoom:50%;" />

其次：设置断点，显示断点和删除断点

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221014140757583.png" alt="image-20221014140757583" style="zoom:50%;" />

最后，运行以及单步调试

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221014140827838.png" alt="image-20221014140827838" style="zoom:50%;" />

## 思考题

1. 使⽤ riscv64-unknown-elf-gcc 编译单个 .c ⽂件 

![image-20220916220342063](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220916220342063.png)

2. 使⽤ riscv64-unknown-elf-objdump 反汇编 1 中得到的编译产物

![image-20220916220845169](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220916220845169.png)

3. 调试 Linux 时:

​		在 GDB 中查看汇编代码：使用layout asm

![image-20220916204651395](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20220916204651395.png)

​       	 2.在 0x80000000 处下断点（如三中步骤）

​            3.查看所有已下的断点 

​            4.在 0x80200000 处下断点

​			5.清除 0x80000000 处的断点

  		  6.继续运⾏直到触发 0x80200000 处的断点 

 		   7.单步调试⼀次 

​		    8. 退出 QEMU

​	5.vmlinux 和 Image 的关系和区别是什么？

​	vmlinux是Linux内核编译出来的原始的内核文件，而Image是Linux内核编译时，处理vmlinux后生成的二进制内核映像



## 心得

​	在这次实验中的重点在于对需要使用的docker，gdb等有一个初步的了解，下面我来描述一下我碰到了什么困难。第一是实验中给出的命令参考，不能不加思考的照搬，这里面最明显的就是经常出现的path/to，当然这个由于每个人系统的不同肯定也会不尽相同，我就在使用gdb的时候对vmlinux吃了亏，另外，由于绝对路径有时过长，可以使用./的方式引出一个相对路径，这样表示的话会更加清楚；第二是对于使用gdb调试时，我们需要对qemu和gdb中都 配置好riscv的环境，在一次实验中我由于忘了这点造成了一些麻烦；第三是对于这些命令，基本我还处于一个慢慢学的状态，只能说对于基础的调试语句有了基本的认识，希望能在之后的实验中一边做一边熟练掌握这个gdb调试的工具；最后是在于makefile里需要加上额外参数的事情，由于我一开始没有仔细读文档，导致重新编译。