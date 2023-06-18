from kanren import run, eq, membero, var, conde        # kanren一个描述性Python逻辑编程系统
from kanren.core import lall                           # lall包用于定义规则
import time

###############################################################################
####             可在此处定义自己所需要用到的自定义函数(可选)                  #### 
####   提示：定义左邻近规则left(), 定义右邻近规则right(),定义邻近规则next()    ####
###############################################################################
#                                                                             #

def left(q, p, list):
    return membero((q, p), zip(list, list[1:]))
#左邻规则：q是在p左边的成员
#如果 list 是 [1, 2, 3, 4]，那么 zip(list, list[1:]) 的结果就是 [(1, 2), (2, 3), (3, 4)]。
#这句话也就是判断，（q,p）这个元组是否存在在列表元组中

def right(q, p, list):
    return membero((p, q), zip(list, list[1:]))
#右邻规则：q是在p右边的成员

def next_(q, p, list):
    return conde([left(p,q,list)], [right(p,q,list)])
#也就是要么左边要么右边，使用的是中括号

#                                                                             #
###############################################################################
#################                非必要性工作                 ################## 
###############################################################################

class Agent:
    """
    推理智能体.
    """
    
    def __init__(self):
        """
        智能体初始化.
        """
        
        self.units = var()              # 单个unit变量指代一座房子的信息(国家，工作，饮料，宠物，颜色) 
                                        # 例如('英国人', '油漆工', '茶', '狗', '红色')即为正确格式，但不是本题答案
                                        # 请基于给定的逻辑提示求解五条正确的答案
        self.rules_zebraproblem = None  # 用lall包定义逻辑规则
        self.solutions = None           # 存储结果
        
    def define_rules(self):
        """
        定义逻辑规则.
        """
        #lall是逻辑与作用
        self.rules_zebraproblem = lall(
            (eq, (var(), var(), var(), var(), var()), self.units),         # self.units共包含五个unit成员，即每一个unit对应的var都指代一座房子(国家，工作，饮料，宠物，颜色) 
                                                                           # 各个unit房子又包含五个成员属性: (国家，工作，饮料，宠物，颜色)
            
            ##############################################################################
            ####               请在以下区域中添加逻辑规则，感受逻辑约束问题               #### 
            ####     输出：五条房子匹配信息('英国人', '油漆工', '茶', '狗', '红色')       ####
            ##############################################################################
            #                                                                            #
            
            # 示例：把所有题面的消息拿出，另外有两个元素消息，斑马和矿泉水没提到，要补充进去
            # 另：对于房子的顺序，最后列表中的前后顺序，就是房子的顺序
            (membero,('英国人', var(), var(), var(), '红色'), self.units),
            (membero,('西班牙人', var(), var(), '狗', var()), self.units),
            (membero,('日本人', '油漆工', var(), var(), var()), self.units),
            (membero,('意大利人', var(), '茶', var(), var()), self.units),
            (membero((('挪威人', var(), var(), var(), var()),var(), var(), var(), var()), self.units)),
            (right, (var(), var(), var(), var(), '绿色'), (var(), var(), var(), var(), '白色'), self.units),
            (membero,(var(), '摄影师', var(), '蜗牛', var()), self.units),
            (membero,(var(), '外交官', var(), var(), '黄色'), self.units),
            (membero((var(), var(), (var(), var(), '牛奶', var(), var()), var(), var()), self.units)),
            #牛奶在答案中，必须在喝的东西的第三个->以此保证是第三个房子
            (membero,(var(), var(), '咖啡', var(), '绿色'), self.units),
            (next_, ('挪威人', var(), var(), var(), var()), (var(), var(), var(), var(), '蓝色'), self.units),
            (membero,(var(), '小提琴家', '橘子汁', var(), var()), self.units),
            (next_, (var(), var(), var(), '狐狸', var()), (var(), '医生', var(), var(), var()), self.units),
            (next_, (var(), var(), var(), '马', var()), (var(), '外交官', var(), var(), var()), self.units),
            (membero,(var(), var(), var(), '斑马', var()), self.units),   
            (membero,(var(), var(), '矿泉水', var(), var()), self.units),
                        
            #                                                                            #
            ##############################################################################
            #################             完成后请记得提交作业             ################# 
            ##############################################################################

        )
    
    def solve(self):
        """
        规则求解器(请勿修改此函数).
        return: 斑马规则求解器给出的答案，共包含五条匹配信息，解唯一.
        """
        
        self.define_rules()
        self.solutions = run(0, self.units, self.rules_zebraproblem)#找出一个答案就可以停下来
        return self.solutions
