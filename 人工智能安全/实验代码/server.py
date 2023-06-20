import models, torch

class Server(object):

    def __init__(self, conf, eval_dataset):
        """
        初始化Server对象
        """
        self.conf = conf    # 设置的超参数配置
        self.global_model = models.get_model(self.conf["model_name"])   # 实例化全局模型
        self.eval_loader = torch.utils.data.DataLoader(
            eval_dataset, 
            batch_size=self.conf["batch_size"], 
            shuffle=True
        )   # 生成用于评估的DataLoader

    def model_aggregate(self, weight_accumulator):
        """
        计算全局模型的参数更新
        """
        for name, data in self.global_model.state_dict().items():
			#weight_accumulator是一个字典，包含每个客户端的参数差异
   			#update_per_layer则是每一层模型参数的平均更新量
            update_per_layer = weight_accumulator[name] * self.conf["lambda"]

			#保证更新量和模型参数的类型一致
            if data.type() != update_per_layer.type():
                data.add_(update_per_layer.to(torch.int64))
            else:
                data.add_(update_per_layer)

    def model_eval(self):
        """
        评估全局模型的效果
        """
        self.global_model.eval()

        total_loss = 0.0        #总损失
        correct = 0         #正确的预测数量
        dataset_size = 0        #数据集总大小
        for batch_id, batch in enumerate(self.eval_loader):
            data, target = batch 
            dataset_size += data.size()[0]  #统计数据集大小

            if torch.cuda.is_available():
                data = data.cuda()
                target = target.cuda()

            output = self.global_model(data)    # 前向传播
            total_loss += torch.nn.functional.cross_entropy(output, target,
                                              reduction='sum').item() #计算总损失（CrossEntropy）
            pred = output.data.max(1)[1]  # get the index of the max log-probability
            correct += pred.eq(target.data.view_as(pred)).cpu().sum().item()    # 统计正确的预测数量

        # 计算准确率和平均损失
        acc = 100.0 * (float(correct) / float(dataset_size))
        total_l = total_loss / dataset_size
        return acc, total_l