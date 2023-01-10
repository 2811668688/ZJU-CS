# 计算机体系结构 - Exp5 Report

## 1 实验目的和要求

**实验目的**

-  Understand the algorithm of Scoreboard
-  Understand out-of-order execution of processor
-  Implement the pipeline with out-of-order execution of multi-cycle operation

**实验要求**

- Implement the algorithm of scoreboard, and incorporate it into pipeline.
2. Pass the simulation test and fpga verification.

## 2 实验内容和原理

### 2.1 数据通路图

![image-20221201212936847](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221201212936847.png)

以及一些重要的值：

```
`define BUSY    0
`define OP_L    1
`define OP_H    5
`define DST_L   6
`define DST_H   10
`define SRC1_L  11
`define SRC1_H  15
`define SRC2_L  16
`define SRC2_H  20
`define FU1_L   21 // i.e. Qj,Qk
`define FU1_H   23
`define FU2_L   24
`define FU2_H   26
`define RDY1    27 // i.e. Rj,Rk
`define RDY2    28
`define FU_DONE 29

`define FU_ALU      3'd1
`define FU_MEM      3'd2
`define FU_MUL      3'd3
`define FU_DIV      3'd4
`define FU_JUMP     3'd5
```

### 2.1 normal_stall

- 原理解释：normal_stall信号用于检测当前是否有结构竞争和WAW，因为scoreboard无法处理这两种情况所以遇 到这两种情况不能issue，需要stall。
- 结构冒险：因缺乏硬件支持而导致指令不能在预定的时钟周期内执行的情况。即硬件不支持多条指令在同一时钟周期执行。即查看要用的Fu是否为busy状态，busy状态则无法同时使用，必须等待。
- WAW：已存在要对某寄存器进行写，结果又遇到了要写，必须stall，scoreboard方法无法处理。即查看当前issue阶段指令的目的寄存器是否与FUS中存在的目的寄存器相同。

```c++
	assign normal_stall = (use_FU != `FU_BLANK && FUS[use_FU][`BUSY]) | (|RRS[dst]);
```

### 2.2 ensure WAR

- 写回处理时，要判断WAR问题。如果别的指令不需要读当前即将写入的寄存器，才可以进行写入。

- 实现而言：看某一条指令是否能写回（拿ALU指令为例），那么需要看FUS中除了ALU外所有指令的源寄存器：
  （1）如果源寄存器和ALU指令的目的寄存器不同就说明肯定不会出现WAR
  （2）如果某个源寄存器和ALU指令的目的寄存器相同，那么要看这个源寄存器的状态：
       （2.1）如果是ready状态，那么说明这个源寄存器还没有读取，此时ALU指令不能write back
       （2.2）如果不是ready状态，那么说明要么源寄存器读取结束，要么说明源寄存器就在等待这个ALU写回作为读入，此时可以write back
- 实现而言，剩下的几个部件同理进行判断

```c++
wire ALU_WAR = (//不会出现是1，会出现是0
       (FUS[`FU_MEM][`SRC1_H:`SRC1_L]  != FUS[`FU_ALU][`DST_H:`DST_L] | ~FUS[`FU_MEM][`RDY1])  &    // fill sth. here- 要么不等于，要么是不ready，这两个时候是可以进行写回的
       (FUS[`FU_MEM][`SRC2_H:`SRC2_L]  != FUS[`FU_ALU][`DST_H:`DST_L] | ~FUS[`FU_MEM][`RDY2])  &    // fill sth. here
       (FUS[`FU_MUL][`SRC1_H:`SRC1_L]  != FUS[`FU_ALU][`DST_H:`DST_L] | ~FUS[`FU_MUL][`RDY1])  &    // fill sth. here
       (FUS[`FU_MUL][`SRC2_H:`SRC2_L]  != FUS[`FU_ALU][`DST_H:`DST_L] | ~FUS[`FU_MUL][`RDY2])  &    // fill sth. here
       (FUS[`FU_DIV][`SRC1_H:`SRC1_L]  != FUS[`FU_ALU][`DST_H:`DST_L] | ~FUS[`FU_DIV][`RDY1])  &    // fill sth. here
       (FUS[`FU_DIV][`SRC2_H:`SRC2_L]  != FUS[`FU_ALU][`DST_H:`DST_L] | ~FUS[`FU_DIV][`RDY2])  &    // fill sth. here
       (FUS[`FU_JUMP][`SRC1_H:`SRC1_L] != FUS[`FU_ALU][`DST_H:`DST_L] | ~FUS[`FU_JUMP][`RDY1]) &    // fill sth. here
       (FUS[`FU_JUMP][`SRC2_H:`SRC2_L] != FUS[`FU_ALU][`DST_H:`DST_L] | ~FUS[`FU_JUMP][`RDY2])      // fill sth. here
     );
```

### 2.3 maintain table

- 这一部分的主要任务：维护FUS和RRS
- 根据此ppt可以更好理解

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221201221902155.png" alt="image-20221201221902155" style="zoom:80%;" />

#### 2.3.1 IS

- 如果指令能够issue，更新FUS和RRS两张表格

```c++
if (RO_en)
    begin
    // not busy, no WAW, write info to FUS and RRS
        if (|dst)
            RRS[dst] <= use_FU;
        // fill sth. here.
        FUS[use_FU][`BUSY] <= 1'b1;//设置为忙状态(Busy)
        FUS[use_FU][`OP_H:`OP_L] <= op;//设置为当前指令的值（Op）
        FUS[use_FU][`DST_H:`DST_L] <= dst;
        FUS[use_FU][`SRC1_H:`SRC1_L] <= src1;
        FUS[use_FU][`SRC2_H:`SRC2_L] <= src2;//设置寄存器（F）
        FUS[use_FU][`FU1_H:`FU1_L] <= fu1;
        FUS[use_FU][`FU2_H:`FU2_L] <= fu2;//记录不能读取使用的寄存器在哪个fu部分（Q）
        FUS[use_FU][`RDY1] <= rdy1;
        FUS[use_FU][`RDY2] <= rdy2;//记录是否已经可以读取使用(R)
        FUS[use_FU][`FU_DONE] <= 1'b0;//标记ok

        IMM[use_FU] <= imm;
        PCR[use_FU] <= PC;
	end
```

#### 2.3.2 RO

- 在RO阶段，如果某条指令的两个源寄存器都ready了那么说明RO完毕，将其ready都设置为0，代表自己不再需要占有了，别的可以用了。并且自然也要把这个源寄存器原来在哪个部分被占有着给设置为0.
- 实现上，不断判断每个部位是否都ready了，是的话则可以进行调整。

```C++
    // RO
    if (FUS[`FU_JUMP][`RDY1] & FUS[`FU_JUMP][`RDY2])
        begin
        // JUMP
        	FUS[`FU_JUMP][`RDY1] <= 1'b0;
    		FUS[`FU_JUMP][`RDY2] <= 1'b0;
            FUS[`FU_JUMP][`FU1_H:`FU1_L] <= 3'b0;
            FUS[`FU_JUMP][`FU2_H:`FU2_L] <= 3'b0;
    	end
        // fill sth. here.
     else if (FUS[`FU_ALU][`RDY1] & FUS[`FU_ALU][`RDY2])
        begin
        // ALU
           	FUS[`FU_ALU][`RDY1] <= 1'b0;
        	FUS[`FU_ALU][`RDY2] <= 1'b0;
            FUS[`FU_ALU][`FU1_H:`FU1_L] <= 3'b0;
            FUS[`FU_ALU][`FU2_H:`FU2_L] <= 3'b0;
        end
     else if (FUS[`FU_MEM][`RDY1] & FUS[`FU_MEM][`RDY2])
         begin
         // MEM
            FUS[`FU_MEM][`RDY1] <= 1'b0;
    		FUS[`FU_MEM][`RDY2] <= 1'b0;
            FUS[`FU_MEM][`FU1_H:`FU1_L] <= 3'b0;
            FUS[`FU_MEM][`FU2_H:`FU2_L] <= 3'b0;
    	end
     else if (FUS[`FU_MUL][`RDY1] & FUS[`FU_MUL][`RDY2])
         begin
         // MUL
            FUS[`FU_MUL][`RDY1] <= 1'b0;
        	FUS[`FU_MUL][`RDY2] <= 1'b0;
            FUS[`FU_MUL][`FU1_H:`FU1_L] <= 3'b0;
            FUS[`FU_MUL][`FU2_H:`FU2_L] <= 3'b0;
        end
     else if (FUS[`FU_DIV][`RDY1] & FUS[`FU_DIV][`RDY2])
         begin
         // DIV
            FUS[`FU_DIV][`RDY1] <= 1'b0;
    		FUS[`FU_DIV][`RDY2] <= 1'b0;
            FUS[`FU_DIV][`FU1_H:`FU1_L] <= 3'b0;
            FUS[`FU_DIV][`FU2_H:`FU2_L] <= 3'b0;    
    	end
```

#### 2.3.3 EX

- 在指令执行完毕后，我们需要将FUS中对应的FD_DONE位置1
- 实现上，通过看每个组件是否为1，是的话也置相应的为1

```C++
    if(ALU_done) begin
        FUS[`FU_ALU][`FU_DONE] <= 1'b1;
    end
    if(MEM_done) begin
        FUS[`FU_MEM][`FU_DONE] <= 1'b1;
    end
    if(MUL_done) begin
        FUS[`FU_MUL][`FU_DONE] <= 1'b1;
    end
    if(DIV_done) begin
        FUS[`FU_DIV][`FU_DONE] <= 1'b1;
    end
    if(JUMP_done) begin
        FUS[`FU_JUMP][`FU_DONE] <= 1'b1;
    end
```

#### 2.3.4 WB

- WB阶段，需要将当前这条指令对应的FU在FUS中清零，并且若有其它指令的源寄存器在等待当前指令写回的值，要把相应的FUS的FU位设0，不再标注要从哪里拿，并且它的ready设1，表示拿到了。
- 实现上：以JUMP类指令为例，假设当前JUMP指令准备write back，那么首先将FUS[JUMP]和
  RRS[FUS [ FU_JUMP ] [ DST_H:DST_L ] ]清零，然后检查FUS中除JUMP以外的其它FU。如果某个FU需要的源寄存器和JUMP指令的目的寄存器相同，那么此时JUMP指令准备写回，该源寄存器就即将拿到想要的值，所以FUS中对应的FU位需要清零并且ready位需要置1

```c++
if (FUS[`FU_JUMP][`FU_DONE] & JUMP_WAR) begin
    FUS[`FU_JUMP] <= 32'b0;//清0
    RRS[FUS[`FU_JUMP][`DST_H:`DST_L]] <= 3'b0;//清0
    // ensure RAW
    if (FUS[`FU_ALU][`FU1_H:`FU1_L] == `FU_JUMP) begin//如果F等的是JUMP部分，且等待着
        FUS[`FU_ALU][`FU1_H:`FU1_L] <= `FU_BLANK;//清空
        FUS[`FU_ALU][`RDY1] <= 1'b1;//拥有了，置为1
    end
    if (FUS[`FU_MEM][`FU1_H:`FU1_L] == `FU_JUMP) begin
        FUS[`FU_MEM][`FU1_H:`FU1_L] <= `FU_BLANK;
        FUS[`FU_MEM][`RDY1] <= 1'b1;
    end
    if (FUS[`FU_MUL][`FU1_H:`FU1_L] == `FU_JUMP) begin
        FUS[`FU_MUL][`FU1_H:`FU1_L] <= `FU_BLANK;
        FUS[`FU_MUL][`RDY1] <= 1'b1;
    end
    if (FUS[`FU_DIV][`FU1_H:`FU1_L] == `FU_JUMP) begin
        FUS[`FU_DIV][`FU1_H:`FU1_L] <= `FU_BLANK;
        FUS[`FU_DIV][`RDY1] <= 1'b1;
    end
    if (FUS[`FU_ALU][`FU2_H:`FU2_L] == `FU_JUMP) begin
        FUS[`FU_ALU][`FU2_H:`FU2_L] <= `FU_BLANK;
        FUS[`FU_ALU][`RDY2] <= 1'b1;
    end
    if (FUS[`FU_MEM][`FU2_H:`FU2_L] == `FU_JUMP) begin
        FUS[`FU_MEM][`FU2_H:`FU2_L] <= `FU_BLANK;
        FUS[`FU_MEM][`RDY2] <= 1'b1;
    end
    if (FUS[`FU_MUL][`FU2_H:`FU2_L] == `FU_JUMP) begin
        FUS[`FU_MUL][`FU2_H:`FU2_L] <= `FU_BLANK;
        FUS[`FU_MUL][`RDY2] <= 1'b1;
    end
    if (FUS[`FU_DIV][`FU2_H:`FU2_L] == `FU_JUMP) begin
        FUS[`FU_DIV][`FU2_H:`FU2_L] <= `FU_BLANK;
        FUS[`FU_DIV][`RDY2] <= 1'b1;
    end
end
//ALU, MEM, MUL, DIV同理
```



## 3 实验过程和数据记录

### 3.1 仿真部分

**指令描述**

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205171900086.png" />

**仿真说明**

- 指令：PC=8时，lw x4, 8(x0），这一条指令会导致的问题是结构冒险，由于PC=4的那一拍使用了对应的处理部件，因此无法使用这个结构，因此这里的normal_stall为0（**我特意标注出了结构冒险和WAW冲突，以便更好查看仿真**）

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205173858899.png" alt="image-20221205173858899" style="zoom:50%;" />

​	并且，由于这一步的stall，PC=4的指令在此时进行读数一拍，mem花费两拍，wb花费一拍，最后终于使得8的这一拍成功发射，等待读数。

![image-20221205190726229](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205190726229.png)

- 指令：PC=10时，addi x1, x1, -1，这一步会触发WAW和结构冒险两种情况，既是因为add部件正在被占用，并且现在要读的正是之前部件中的目标寄存器。这里要等7拍，才能不存在此问题可以发射，原因是因为之前的add还要等前一个lw给x4。

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205183523676.png" alt="image-20221205183523676" style="zoom:50%;" />

​	具体展开分析，这里首先要等3拍的lw指令，然后两个寄存器都已经ready，可以看到27、28显示也都为1，那么可以继续进行，再恢复成0（EX），再一拍WB，接着就释放了，然后下一拍就可以成功实现IS。

![image-20221205194206166](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205194206166.png)

- PC=28、2C

  首先28占用部件，2C结构冲突，等28需要3拍完成之后，IS成功，然后进行2拍发现要跳转，更改指令到label

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221209163657787.png" alt="image-20221209163657787" style="zoom:50%;" />

- PC=5C时，mul x9, x8, x2，这一步同时触发结构冒险/WAW/RAW等相应的问题。

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205185709126.png" alt="image-20221205185709126" style="zoom:50%;" />

​	相应的，需要等的拍数也是正常分析。

​	这里是为了等待x8，可见x8在除法中等待了许多拍最终得到运算结果。

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205200721422.png" alt="image-20221205200721422" style="zoom:50%;" />

​	而最终，等到了参数的也相应的变为两个ready

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205200853908.png" alt="image-20221205200853908" style="zoom:67%;" />

### 上板验证

![image-20221205190007602](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205190007602.png)

## 4、实验心得和体会

思考题：
1、为什么记分牌算法不使用前递？

答：因为记分牌算法和流水线不同，指令是乱序的并且不一定是一个周期执行完成。比如div需要 40clk，那么此时记分牌其他模块正常工作。此时做完后 div提交，其他模块使用对应的参数，前递意义不大。同时，记分牌 FU众多，如果要前递需要很复杂的前递结构， 不符合设计要求。

2、如果使用分支预测，能否只让 CPU执行预测结果，如果预测错误如何？
答：记分牌算法可以执行分支预测：

<img src="C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20221205201146958.png" alt="image-20221205201146958" style="zoom:50%;" />

​	在此处加上BHT和 BTB表即可，将预测到的指令递交到IS继续执行。 但是分支预测如果预测失败会很难处理，因为指令的乱序执行无法确定需要清除那条指令需要额外的寄存器来记录。最好使用 ROB等 算法 实现 分支预测

3、指出乱序发生在何处

​	如图，在PC=24排队等待时，此时 lw指令和 sub指令都 done。但是 sub指令优先提交，之后lw指令才发生提交， 产生了乱序。

4、分析记分牌算法的优缺点
	优点：记分牌算法实现了乱序执行指令，解决了乱序执行的时候的hazard。同时实现不复杂，相比于五级流水线，实现了数据的流式运行。
	缺点：记分牌在 WAR,WAW会产生阻塞，后续指令无法发射。阻塞较多的指令；指令提交不是顺序的，可能对程序调试提出挑战。