import copy
import random
import math
from func_timeout import FunctionTimedOut, func_set_timeout

class TreeNode:
    def __init__(self, action, board, color):
        self.father = None
        self.children = []
        self.Q = 0
        self.N = 0
        self.action = action
        self.board = board
        self.color = color
        self.action_list = list(board.get_legal_actions(color))
        self.action_left = self.action_list
        self.Leaf = len(self.action_list)==0
    
    def getFather(self):
        return self.father

    def getChildren(self):
        return self.children
    
    def getN(self):
        return self.N

    def getQ(self):
        return self.Q
    
    def getAction(self):
        return self.action

    def Is_Leaf(self):
        return self.Leaf

    def Fully(self):
        return len(self.action_left) == 0
    
    def addChild(self, node):
        self.children.append(node)
        node.father=self

    def Action_once(self):
        action = random.choice(self.action_left)
        self.action_left.remove(action)
        return action

    def UCB(self):
        if self.father is None:
            return 0
        a = self.Q / self.N
        b = math.log(self.father.getN())
        b = math.sqrt(2*b/self.N)
        return a + 20 * b

class SearchTree:
    def __init__(self, board, color):
        self.root = TreeNode('top', board, color)
        #设置初始节点
        self.player_color = color

    def bestChild(self, node):
        bestUCB = -999999
        bestChild = []
        #检索所有UCB值，随机挑选一个
        for n in node.getChildren():
            thisUCB = n.UCB()
            if thisUCB > bestUCB:
                bestUCB = thisUCB
                bestChild = [n]
            elif thisUCB == bestUCB:
                bestChild.append(n)
        return bestChild[random.randrange(0, len(bestChild))]
    
    def search(self):
        try:
            self._search()
        except FunctionTimedOut:
            pass
        return self.bestChild(self.root).getAction()

    @func_set_timeout(58) # 总共运行时间
    def _search(self):
        while True:
            select_node = self.select()
            reward = self.simulate(select_node)
            self.backPropagate(select_node, reward)


    def select(self):
        node = self.root
        while not node.Is_Leaf():
            #若当前节点非叶
            if not node.Fully():
                return self.expand(node)
            #拓展一个点，返回
            else:
                node = self.bestChild(node)
            #随机选择一个ucb值最好的点，继续做
        return node

    def expand(self, node):
        action = node.Action_once()
        tmp_board = copy.deepcopy(node.board)
        #要进行深拷贝，不能影响棋盘
        tmp_board._move(action, node.color)
        #对当前棋盘进行这一次action，得到新的棋盘
        if node.color == 'O':
            next_color = 'X'
        elif node.color == 'X':
            next_color = 'O'
        child = TreeNode(action, tmp_board, next_color)
        node.addChild(child)
        #建立新节点后，添加当父亲节点的孩子中
        return child
    

    def simulate(self, node):
        #进行模拟，随机落子，查看结果
        tmp_board = copy.deepcopy(node.board)
        #为了不影响棋盘同样深拷贝
        tmp_color = node.color
        while True:
            action_list = list(tmp_board.get_legal_actions(tmp_color))
            if len(action_list) == 0:
                #如果自己不能落子
                if tmp_color == 'O':
                    oppo_color = 'X' 
                else:
                    oppo_color = 'O'
                if len(list(tmp_board.get_legal_actions(oppo_color)))==0:
                    # 对手也无法落子,结束
                    winner, score = tmp_board.get_winner() 
                    # 0-黑棋赢,1-白旗赢,2-表示平局
                    if winner == 0:
                        return score
                    elif winner == 1:
                        return -score
                    elif winner == 2:
                        return 0
                else:
                    tmp_color = oppo_color
            else:
                action = random.choice(action_list)
                tmp_board._move(action, tmp_color)
                if tmp_color == 'O':
                    tmp_color = 'X' 
                else:
                    tmp_color = 'O'

    def backPropagate(self, node, reward):
        while node is not None:
            node.N += 1
            if node.color == 'O': 
                node.Q += reward
            else:
                node.Q -= reward
            #递归，去找父亲继续更新
            node = node.father



class AIPlayer:
    """
    AI 玩家
    """

    def __init__(self, color):
        """
        玩家初始化
        :param color: 下棋方,'X' - 黑棋,'O' - 白棋
        """
        self.searchTree = None
        self.color = color

    def get_move(self, board):
        """
        根据当前棋盘状态获取最佳落子位置
        :param board: 棋盘
        :return: action 最佳落子位置, e.g. 'A1'
        """
        if self.color == 'X':
            player_name = '黑棋'
        else:
            player_name = '白棋'
        print("请等一会,对方 {}-{} 正在思考中...".format(player_name, self.color))

        # -----------------请实现你的算法代码--------------------------------------

        self.searchTree = SearchTree(board, self.color)
        action = self.searchTree.search()
        return action
