# 计算机体系结构 - Exp2 Report

## 一、实验目的和要求

**实验目的**

- 了解RISC-V简单的异常和中断

- 了解如何在流水线中添加异常和中断机制

**实验要求**

- 实现csrrw, csrrs, csrrc, csrrwi, csrrsi, csrrci, ecall, mret指令

- 实现mstatus, mtvec, mepc, mcause, mtval寄存器

- 实现三种异常和外部中断，需要实现精确异常

- 通过仿真测试和上板验证

## 二、实验内容和原理

### 实验简介

**实验任务**

- 完善ExceptionUnit 模块，运行测试程序验证正确性

**数据通路图**

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023201713409.png" alt="image-20221023201713409" style="zoom:67%;" />

**输入信号**

ExceptionUnit的输入信号如下：

```vhdl
module ExceptionUnit(
         input clk, rst,
         //CSR
         input csr_rw_in,//csr系列6个指令判断信号，参考Ctrl，对应W S C三种指令
         //csrrc:清除，csrrw读写，csrr是设置
         input[1:0] csr_wsc_mode_in,//判断是WSC三种哪个指令
         input csr_w_imm_mux,//是否有立即数
         input[11:0] csr_rw_addr_in,//CSR寄存器地址
         input[31:0] csr_w_data_reg,//写入的数据
         input[4:0] csr_w_data_imm,//立即数值
         output[31:0] csr_r_data_out,//csr指令输出值
         //中断
         input interrupt,//
         input illegal_inst,//非法指令
         input l_access_fault,//l指令访问出错
         input s_access_fault,//S指令访问出错
         input ecall_m,//ecall

         input mret,//是否为MRET指令
         input[31:0] epc_cur,//错误原因信号
         input[31:0] epc_next,//地址储存信号
         output[31:0] PC_redirect,//重定向信号
         output redirect_mux,
         output reg_FD_flush, reg_DE_flush, reg_EM_flush, reg_MW_flush,
         output RegWrite_cancel
       );
```



### 实验原理

#### CSR指令

- 本次实验中需要支持以下六种CSR指令CSR

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023202237625.png" alt="image-20221023202237625" style="zoom:67%;" />

​	CSR寄存器在CSRRegs 模块内，而该模块位于ExceptionUnit 模块，因此除了异常指令外，需要在ExceptionUnit 模块中处理正常CSR指令的执行。以下输入信号为需要使用的寄存器值。

```vhdl
module CSRRegs(
         input clk, rst,
         input[11:0] raddr, waddr,
         input[31:0] wdata,
         input csr_w,
         input[1:0] csr_wsc_mode,
         output[31:0] rdata,
         output[31:0] mstatus,

         input is_trap,
         input is_mret,
         input[31:0] mepc,
         input[31:0] mcause,
         input[31:0] mtval,
         output[31:0] mtvec,
         output[31:0] mepc_o
       );
```

​	根据以上输入信号调用CSRRegs 模块，完成对相关CSR寄存器的读写操作



#### 异常检测

​	顶层的数据通路已经给ExceptionUnit 传入了异常检测信号，只需要根据输入信号就可以判断异常，然后进入异常处理部分即可。以下是四个异常检测的输入信号：

```vhdl
input illegal_inst,//非法指令
input l_access_fault,//l指令访问出错
input s_access_fault,//S指令访问出错
input ecall_m,//ecall
```



#### 异常处理

##### **处理要求**

本次实验中，我们需要处理3种异常：

- 访问错误异常：物理内存的地址不支持访问

- 环境调用异常：执行ecall指令时发生

- 非法指令异常：译码阶段发现无效操作码

发生异常/中断时，硬件状态转换如下：

​	  异常指令的PC被保存在mepc中，PC被设置为mtvec。mepc指向导致异常的指令；对于中断，它指向中断处理后应该恢复执行的位置
**=＞**根据异常来源设置mcause，并将mtval设置为出错的地址或者其它适用于特定异常的信息字
**=＞**把控制状态寄存器mstatus中的MIE位置零以禁用中断，并把先前的MIE值保留到MPIE中
**=＞**发生异常之前的权限模式保留在mstatus的MPP域中，再把权限模式更改为M

##### **寄存器地址说明**

地址名称描述

| 地址  | 名称    |                描述                |
| ----- | ------- | :--------------------------------: |
| 0x300 | mstatus |      Machine status register       |
| 0x305 | mtvec   | Machine trap-handler base address  |
| 0x341 | mepc    | Machine exception program counter  |
| 0x342 | mcause  |         Machine trap cause         |
| 0x343 | mtval   | Machine bad address or instruction |

##### **异常处理步骤**

发生异常时：

mstatus：

- 将MIE置零，禁用中断，并把原来的MIE存到MPIE

mtvec：

- 将当前PC设为mtvec

mepc：

- 保存导致异常的指令地址（异常）

mcause：

- 对于异常，最高位置0，低位如下

- 非法指令异常：低位设置为2

- Load访问异常：低位设置为5

- Store访问异常：低位设置为7

- Ecall环境调用异常：低位设置为11

mtval：

- 访问异常：出错的地址

- 非法指令异常：非法指令本身

- 其他异常：置0

异常处理完后：

当我们检测到mret 指令时，执行如下步骤：

- 将PC 寄存器设置为mepc （这里的mepc 保存的已经不是异常指令位置，而是自增4之后的地址）

- 将mstatus 的MPIE 域复制到MIE 域，恢复之前的中断使能设置（这里是异常触发时操作的逆操作）



## 三、实验过程和数据记录

### 核心代码

```vhdl
    if (interrupt & mstatus[3])//能发生中断且为外设中断
      begin
        mepc <= epc_next;//mepc存放中断发生时的PC的下一个
        mcause <= 32'h8000000B;  // 外部中断类型的设置
        mtval <= 0;//异常信息字，外设中断不需要设置
      end
    else if (illegal_inst & mstatus[3])
      begin
        mepc <= epc_cur;//之后回到此处
        mcause <= 2;//非法指令原因设置
        mtval <= inst;
      end
    else if (l_access_fault & mstatus[3])
      begin
        mepc <= epc_cur;
        mcause <= 5;//ld指令地址错误设置
        mtval <= addr;
      end
    else if (s_access_fault & mstatus[3])
      begin
        mepc <= epc_cur;
        mcause <= 7;//save指令地址错误设置
        mtval <= 0;
      end
    else if (ecall_m & mstatus[3])
      begin
        mepc <= epc_cur;
        mcause <= 11;//ecall错误地址设置
        mtval <= 0;
      end
    else if (mret)//返回正常的程序
      begin
        mepc <= 0;
        mcause <= 0;
        mtval <= 0;
      end
    else//普通指令，不做异常
      begin
        mepc <= 0;
        mcause <= 0;
        mtval <= 0;
      end
```

​	其中，为了保证mtval的正确，这里修改core

​	其目的是为了保证，处理异常时mtval 的写入值可以只考虑两种情况

- 访问错误异常时，写入错误的地址

- 非法指令异常时，写入错误的指令

​	也就是传入inst_WB和ALUout_WB作为地址以及指令的输出

```vdhl
.mret(mret_MEM),.inst(inst_WB),.addr(ALUout_WB),
```



### 仿真验证

#### CSR指令验证

**csrrwi & csrr**

验证第一次出现的csrrwi和csrr

![image-20221023205834289](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023205834289.png)

指令要实现的是：第一条指令将0x306 原来的值（0）赋给x1 ，并将16写入0x306 （0x306对应寄存器mcounteren ，地址映射后对应CSR[6] ）。第二条指令，将0x306 的值读出并赋值给x1 寄存器

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023210720682.png" alt="image-20221023210720682" style="zoom:50%;" />

倒数两栏分别是CSR[6]和x1寄存器，可以看到当PC为16时，x1赋值为0x306原来的值0，0x306则是被写入了16

PC为18时，x1的值变化为0x306的当前值16

**csrw**

![image-20221023220812323](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023220812323.png)

验证csrw质量，该条指令将寄存器x1的值写入CSR寄存器0x305，即mtvec

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023220914290.png" alt="image-20221023220914290" style="zoom:50%;" />

可以看到，PC_WB执行到 0x30 的 csrw 0x305, x1 时，0x305处的mtvec寄存器被赋值为x1的当前值
120(0x78)

#### 异常处理验证

第一次： 0x38 处的ecall 指令，环境调用异常

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023221124395.png" alt="image-20221023221124395" style="zoom:50%;" />

第二次： 0x40 处的00000012 指令，非法指令异常

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023221433459.png" alt="image-20221023221433459" style="zoom:50%;" />

第三次： 0x4C 处的lw x1, 128(x0) 指令，Load地址访问异常

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023221449393.png" alt="image-20221023221449393" style="zoom:50%;" />

第四次： 0x54 处的sw x1, 128(x0) 指令，Store地址访问异常

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023221502327.png" alt="image-20221023221502327" style="zoom:50%;" />

分析：综合来看，都实现了PC的成功跳转到0x78实现异常处理的指令

以0x40 处的非法指令异常00000012 为例，检测相关CSR寄存器的值：

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023221433459.png" alt="image-20221023221433459" style="zoom:50%;" />

mstatus：异常触发时从0x88 到0x80 ，异常返回时恢复为0x88 ，符合预期

mepc：异常触发时被存为异常指令地址0x40 ，在Trap程序中（异常返回前）自增4变为0x44 ，符合预期

mcause：此处为非法指令异常，mcause应该设置为0x00000002，符合预期

mtval：非法指令异常，mtval应该设为非法指令本身，此处变为0x00000012，符合预期



### 上板验证

#### CSR指令

同仿真验证，这里还是选择以地址0x1C 处的csrr x1, 0x306 指令为例，将0x306 处的CSR寄存器（此时的值为16）读出，并赋值给x1。因此在PC_WB到达1C 时，寄存器x1的值应该变为0x10，如下图所示，符合预期

![image-20221023205834289](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221023205834289.png)

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221025174123079.png" alt="image-20221025174123079" style="zoom:67%;" />



#### 异常触发验证

​	0x38 处的ecall 指令，触发第一次异常，后一个周期PC_IF变为0x78 ，跳转到异常处理程序

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221025174226220.png" alt="image-20221025174226220" style="zoom:67%;" />



#### 异常处理和返回验证

​	观察x25 （ mepc 的值）和x27 （ mcause 的值）x29（mvtal）的值，这里选取了第2次异常返回前： mepc 应为异常指令地址0x40 ， mcause 应为0x2 ，mtval应该为0x12，对应非法指令异常。这里显示的值都正确

​	<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221026145006054.png" alt="image-20221026145006054" style="zoom:90%;" />





## 四.实验心得

心得：这次的实验主要做的是触发异常之后的处理程序，涉及到的是一些特定寄存器的修改。实验2需要我们将流水线CPU添加支持异常和中断处理的功能（中断最后又不需要做了），主要分为三个部分，首先是支持CSR类的RISC-V指令的正常执行，这一部分不包括异常处理，因此我们做完这部分可以先用Trap测试程序中， 0x00-0x34 位置的指令测试，因为这一部分的指令都是正常指令；第二部分是异常检测，需要支持四种异常，根据异常处理的一般步骤读写CSRRegs即可；最后一部分是异常返回，检测到mret 信号时，读取mepc+4 ，跳转即可。实验2的整体思路很清晰，和os的lab2有很紧密的联系。不过我们班的实验和其他班的对于x29有所不一样的设计，后来在助教的提醒下我才发现。感谢助教的耐心答疑和指导，我们成功解决了这个问题，完成了这次实验

思考题：

1.精确异常和非精确异常的区别是什么？

​	非精确异常：在多发射乱序执行的流水线 CPU 上，从指令进入流水线到异常事件的发生，期间要经过若干流水级，此时 PC 的值已指向其后的某条指令，在实现非精确异常的 CPU 上就把此时的 PC 值作为引起异常指令的所在，指向了真正的引起异常的指令后面的某条指令所在。

​	精确异常：精确异常的 CPU 能够指向了真正的引起异常的指令的所在。

2.阅读测试代码，第一次导致trap的指令是哪条？trap之后的指令做了什么？如果实现了U mode，并以U mode从头开始执行测试指令，会出现什么新的异常？

​	第一次导致trap的是ecall指令。trap后，首先flush四个寄存器，取消后面指令的读写操作。之后跳转到mtvec这个系统目标寄存器的地址。并同时设置对应CSR的值，将mepc，mcause，mtval存入对应寄存器，修改MIE和MPP的值为对应值。

3.为什么异常要传到最后一段即WB段后，才送入异常处理模块？可不可以一旦在某一段流水线发现了异常就送入异常处理模块，如果可以请说明异常处理模块应该如何处理异常；如果不可以，请说明理由。

异常传到最后一段WB段后，是为了保证异常发生前的指令都已经被正确执行了。

如果要实现在某一段流水线发现异常就送入异常处理模块，需要添加其他后援寄存器去保存流水线个指令的状态包括寄存器堆、流水段寄存器等等。	