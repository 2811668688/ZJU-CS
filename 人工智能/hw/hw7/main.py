import random
import numpy as np
import torch
from QRobot import QRobot
from ReplayDataSet import ReplayDataSet
from torch_py.MinDQNRobot import MinDQNRobot as TorchRobot # PyTorch版本
import matplotlib.pyplot as plt
from Maze import Maze
import time
from Runner import Runner

def my_search(maze):
    # 机器人移动方向
    move_map = {
        'u': (-1, 0), # up
        'r': (0, +1), # right
        'd': (+1, 0), # down
        'l': (0, -1), # left
    }
    # 迷宫路径搜索树
    class SearchTree(object):


        def __init__(self, loc=(), action='', parent=None):
            """
            初始化搜索树节点对象
            :param loc: 新节点的机器人所处位置
            :param action: 新节点的对应的移动方向
            :param parent: 新节点的父辈节点
            """

            self.loc = loc  # 当前节点位置
            self.to_this_action = action  # 到达当前节点的动作
            self.parent = parent  # 当前节点的父节点
            self.children = []  # 当前节点的子节点

        def add_child(self, child):
            """
            添加子节点
            :param child:待添加的子节点
            """
            self.children.append(child)

        def is_leaf(self):
            """
            判断当前节点是否是叶子节点
            """
            return len(self.children) == 0
    def expand(maze, is_visit_m, node):
        """
        拓展叶子节点，即为当前的叶子节点添加执行合法动作后到达的子节点
        :param maze: 迷宫对象
        :param is_visit_m: 记录迷宫每个位置是否访问的矩阵
        :param node: 待拓展的叶子节点
        """
        child_number = 0  # 记录叶子节点个数
        can_move = maze.can_move_actions(node.loc)
        for a in can_move:
            new_loc = tuple(node.loc[i] + move_map[a][i] for i in range(2))
            if not is_visit_m[new_loc]:
                child = SearchTree(loc=new_loc, action=a, parent=node)
                node.add_child(child)
                child_number+=1
        return child_number  # 返回叶子节点个数
                
    def back_propagation(node):
        """
        回溯并记录节点路径
        :param node: 待回溯节点
        :return: 回溯路径
        """
        path = []
        while node.parent is not None:
            path.insert(0, node.to_this_action)
            node = node.parent
        return path

    def myDFS(maze):
        """
        对迷宫进行深度
        :param maze: 待搜索的maze对象
        """
        start = maze.sense_robot()
        root = SearchTree(loc=start)
        queue = [root]  # 节点堆栈，用于层次遍历
        h, w, _ = maze.maze_data.shape
        is_visit_m = np.zeros((h, w), dtype=np.int)  # 标记迷宫的各个位置是否被访问过
        path = []  # 记录路径
        peek = 0
        while True:
            current_node = queue[peek]  # 栈顶元素作为当前节点
            #is_visit_m[current_node.loc] = 1  # 标记当前节点位置已访问

            if current_node.loc == maze.destination:  # 到达目标点
                path = back_propagation(current_node)
                break

            if current_node.is_leaf() and is_visit_m[current_node.loc] == 0:  # 如果该点存在叶子节点且未拓展
                is_visit_m[current_node.loc] = 1  # 标记该点已拓展
                child_number = expand(maze, is_visit_m, current_node)
                peek+=child_number  # 开展一些列入栈操作
                for child in current_node.children:
                    queue.append(child)  # 叶子节点入栈
            else:
                queue.pop(peek)  # 如果无路可走则出栈
                peek-=1
            # 出队
            #queue.pop(0)

        return path
    path = myDFS(maze)
    return path

class Robot(TorchRobot):

    def __init__(self, maze):
        """
        初始化 Robot 类
        :param maze:迷宫对象
        """
        super(Robot, self).__init__(maze)
        maze.set_reward(reward={
            "hit_wall": 5.0,
            "destination": -maze.maze_size ** 2.0,
            "default": 1.0,
        })
        self.maze = maze
        self.epsilon = 0
        """开启金手指，获取全图视野"""
        self.memory.build_full_view(maze=maze)
        self.loss_list = self.train()

    def train(self):
        loss_list = []
        batch_size = len(self.memory)

        while True:
            loss = self._learn(batch=batch_size)
            loss_list.append(loss)
            success = False
            self.reset()
            for _ in range(self.maze.maze_size ** 2 - 1):
                a, r = self.test_update()
                if r == self.maze.reward["destination"]:
                    return loss_list

    def train_update(self):
        def state_train():
            state=self.sense_state()
            return state
        def action_train(state):
            action=self._choose_action(state)
            return action
        def reward_train(action):
            reward=self.maze.move_robot(action)
            return reward
        state = state_train()
        action = action_train(state)
        reward = reward_train(action)
        return action, reward

    def test_update(self):
        def state_test():
            state = torch.from_numpy(np.array(self.sense_state(), dtype=np.int16)).float().to(self.device)
            return state
        state = state_test()
        self.eval_model.eval()
        with torch.no_grad():
            q_value = self.eval_model(state).cpu().data.numpy()
        def action_test(q_value):
            action=self.valid_action[np.argmin(q_value).item()]
            return action
        def reward_test(action):
            reward=self.maze.move_robot(action)
            return reward
        action = action_test(q_value)
        reward = reward_test(action)
        return action, reward
