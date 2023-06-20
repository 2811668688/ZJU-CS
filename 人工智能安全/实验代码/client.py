import models, torch, copy


class Client(object):
	#用传入的模型参数moedel进行初始化，然后使用本地的SGD优化器进行local_model模型训练
 	#最后返回一个包含model与local_model参数差异的字典。
    def __init__(self, conf, model, train_dataset, id=-1):

        self.conf = conf            # 联邦学习的超参数配置
        self.local_model = models.get_model(self.conf["model_name"])        # 实例化本地模型
        self.client_id = id     # 客户端id
        self.train_dataset = train_dataset  # 客户端的训练数据集
        all_range = list(range(len(self.train_dataset)))    # 训练数据集的索引范围
        data_len = int(len(self.train_dataset) / self.conf['no_models'])    # 每个模型所需的数据集长度
        train_indices = all_range[id * data_len: (id + 1) * data_len]       # 获得当前客户端的训练数据集的索引范围
        #因为这里是要进行模拟,所以每个模型分到自己的一部分数据,就类似于联邦学习里大家各自拥有一部分数据

        # 实例化dataloader，用于客户端的模型训练
        self.train_loader = torch.utils.data.DataLoader(
            self.train_dataset, 
            batch_size=conf["batch_size"], 
            sampler=torch.utils.data.sampler.SubsetRandomSampler(train_indices) #打乱以便训练
        )

    def local_train(self, model):

        # 将模型参数复制到本地模型中
        for name, param in model.state_dict().items():
            self.local_model.state_dict()[name].copy_(param.clone())

        # 实例化优化器（随机梯度下降SGD）
        optimizer = torch.optim.SGD(
            self.local_model.parameters(), 
            lr=self.conf['lr'],
            momentum=self.conf['momentum']
        )

        # 开始训练本地模型
        self.local_model.train()
        for e in range(self.conf["local_epochs"]):
            for batch_id, batch in enumerate(self.train_loader):
                data, target = batch

                if torch.cuda.is_available():
                    data = data.cuda()
                    target = target.cuda()

                optimizer.zero_grad()
                output = self.local_model(data)     # 前向传播
                loss = torch.nn.functional.cross_entropy(output, target)    # 计算损失
                loss.backward()                     # 反向传播
                optimizer.step()                        # 更新参数
            print("Epoch %d done." % e) # 每个epoch结束后，在控制台打印一条信息

        # 计算本地模型与传入模型之间的参数差异，并返回
        diff = dict()
        for name, data in self.local_model.state_dict().items():
            diff[name] = (data - model.state_dict()[name])
        return diff