import argparse, json
import datetime
import os
import logging
import torch, random

from server import *
from client import *
import models, datasets

	
# 本次上机实验为本地模拟联邦学习过程,需一至两小时的训练时间
# run with configuration
# python main.py -c ./utils/conf.json

# 各个文件的简要介绍
# ./utils/conf.json 提供参数配置,如数据集选择、模型选择、客户端数量、抽样数量等
# datasets.py 提供mnist和cifar数据集(已经下载在data文件夹中)
# models.py 提供若干模型
# server.py 为服务端,即整合各客户端模型
# client.py 为客户端,即本地训练模型

""" 
这是一个联邦学习的主程序,负责调用client.py和server.py实现联邦学习过程。
主要包含以下文件: 
- conf.json:提供参数配置,如数据集选择、模型选择、客户端数量、抽样数量等 
- datasets.py:提供mnist和cifar数据集(已经下载在data文件夹中) 
- models.py:提供若干模型
- server.py:为服务端,即整合各客户端模型 
- client.py:为客户端,即本地训练模型
"""
if __name__ == '__main__':
	# 使用 argparse 获取命令行输入的配置文件,获取conf路径,例如:python main.py -c ./utils/conf.json
	parser = argparse.ArgumentParser(description='Federated Learning')
	parser.add_argument('-c', '--conf', dest='conf')
	args = parser.parse_args()

	# 读取conf.json文件
	with open(args.conf, 'r') as f:
		conf = json.load(f)

	# 读取训练集和测试集数据
	train_datasets, eval_datasets = datasets.get_dataset("./data/", conf["type"])

	# 初始化服务器对象
	server = Server(conf, eval_datasets)
	clients = []

	# 根据客户端数量创建客户端对象
	for c in range(conf["no_models"]):
		clients.append(Client(conf, server.global_model, train_datasets, c))

	# 开始联邦学习过程
	print("\n\n")
	for e in range(conf["global_epochs"]):

		# 随机抽取k个客户端参与本轮训练
		candidates = random.sample(clients, conf["k"])

		# 初始化权重累加器
		weight_accumulator = {}

		for name, params in server.global_model.state_dict().items():
			weight_accumulator[name] = torch.zeros_like(params)

		# 所有候选客户端参与联邦学习过程
		for c in candidates:
			# 执行本地训练并返回更新后的权重值
			diff = c.local_train(server.global_model)

			# 将每个客户端更新的权重进行求和并存入累加器
			for name, params in server.global_model.state_dict().items():
				weight_accumulator[name].add_(diff[name])

		# 使用所有客户端更新后的权重计算全局模型的平均权重
		server.model_aggregate(weight_accumulator)

		# 使用模型进行验证并打印结果
		acc, loss = server.model_eval()
		print("Epoch %d, acc: %f, loss: %f\n" % (e, acc, loss))
		
		
	