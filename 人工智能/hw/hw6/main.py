import warnings
# 忽视警告
warnings.filterwarnings('ignore')
import os
import matplotlib
import cv2 as cv
import numpy as np
import matplotlib.pyplot as plt
from tensorflow.keras.callbacks import ModelCheckpoint, ReduceLROnPlateau, EarlyStopping
from tensorflow.keras.applications.imagenet_utils import preprocess_input
from tensorflow.keras import backend as K
from tensorflow.keras.optimizers import Adam
from keras.utils import np_utils,get_file

K.image_data_format() == 'channels_last'

from keras_py.utils import get_random_data
from keras_py.face_rec import mask_rec
from keras_py.face_rec import face_rec
from keras_py.mobileNet import MobileNet

# 导入图片生成器
from tensorflow.keras.preprocessing.image import ImageDataGenerator

def letterbox_image(image, size):
    """
    调整图片尺寸
    :param image: 用于训练的图片
    :param size: 需要调整到网络输入的图片尺寸
    :return: 返回经过调整的图片
    """
    new_image = cv2.resize(image, size, interpolation=cv2.INTER_AREA)
    return new_image

def processing_data(data_path, height, width, batch_size=32, test_split=0.1):
    """
    数据处理
    :param data_path: 带有子目录的数据集路径
    :param height: 图像形状的行数
    :param width: 图像形状的列数
    :param batch_size: batch 数据的大小，整数，默认32。
    :param test_split: 在 0 和 1 之间浮动。用作测试集的训练数据的比例，默认0.1。
    :return: train_generator, test_generator: 处理后的训练集数据、验证集数据
    """

    train_data = ImageDataGenerator(
            # 对图片的每个像素值均乘上这个放缩因子，把像素值放缩到0和1之间有利于模型的收敛
            rescale=1. / 255,  
            # 浮点数，剪切强度（逆时针方向的剪切变换角度）
            shear_range=0.1,  
            # 随机缩放的幅度，若为浮点数，则相当于[lower,upper] = [1 - zoom_range, 1+zoom_range]
            zoom_range=0.1,
            # 浮点数，图片宽度的某个比例，数据提升时图片水平偏移的幅度
            width_shift_range=0.1,
            # 浮点数，图片高度的某个比例，数据提升时图片竖直偏移的幅度
            height_shift_range=0.1,
            # 布尔值，进行随机水平翻转
            horizontal_flip=True,
            # 布尔值，进行随机竖直翻转
            vertical_flip=True,
            # 在 0 和 1 之间浮动。用作验证集的训练数据的比例
            validation_split=test_split  
    )

    # 接下来生成测试集，可以参考训练集的写法
    test_data = ImageDataGenerator(
            rescale=1. / 255,
            validation_split=test_split)

    train_generator = train_data.flow_from_directory(
            # 提供的路径下面需要有子目录
            data_path, 
            # 整数元组 (height, width)，默认：(256, 256)。 所有的图像将被调整到的尺寸。
            target_size=(height, width),
            # 一批数据的大小
            batch_size=batch_size,
            # "categorical", "binary", "sparse", "input" 或 None 之一。
            # 默认："categorical",返回one-hot 编码标签。
            class_mode='categorical',
            # 数据子集 ("training" 或 "validation")
            subset='training', 
            seed=0)
    test_generator = test_data.flow_from_directory(
            data_path,
            target_size=(height, width),
            batch_size=batch_size,
            class_mode='categorical',
            subset='validation',
            seed=0)

    return train_generator, test_generator

def save_model(model, checkpoint_save_path, model_dir):  # 保存模型
    """
    保存模型，每迭代3次保存一次
    :param model: 训练的模型
    :param checkpoint_save_path: 加载历史模型
    :param model_dir: 
    :return: 
    """
    if os.path.exists(checkpoint_save_path):
        print("模型加载中")
        model.load_weights(checkpoint_save_path)
        print("模型加载完毕")
    checkpoint_period = ModelCheckpoint(
        # 模型存储路径
        model_dir + 'ep{epoch:03d}-loss{loss:.3f}-val_loss{val_loss:.3f}.h5',
        # 检测的指标
        monitor='val_acc',
        # ‘auto’，‘min’，‘max’中选择
        mode='max',
        # 是否只存储模型权重
        save_weights_only=False,
        # 是否只保存最优的模型
        save_best_only=True,
        # 检测的轮数是每隔2轮
        period=2
    )
    return checkpoint_period

# 数据集路径
basic_path = "./datasets/5f680a696ec9b83bb0037081-momodel/data/"

# 数据路径
data_path = basic_path + 'image'

# 图像数据的行数和列数
height, width = 160, 160

# 获取训练数据和验证数据集
train_generator, test_generator = processing_data(data_path, height, width)

# 通过属性class_indices可获得文件夹名与类的序号的对应字典。
labels = train_generator.class_indices
print(labels)

# 转换为类的序号与文件夹名对应的字典
labels = dict((v, k) for k, v in labels.items())
print(labels)

pnet_path = "./datasets/5f680a696ec9b83bb0037081-momodel/data/keras_model_data/pnet.h5"
rnet_path = "./datasets/5f680a696ec9b83bb0037081-momodel/data/keras_model_data/rnet.h5"
onet_path = "./datasets/5f680a696ec9b83bb0037081-momodel/data/keras_model_data/onet.h5"

# 加载 MobileNet 的预训练模型权重
weights_path = basic_path + 'keras_model_data/mobilenet_1_0_224_tf_no_top.h5'

# 图像数据的行数和列数
height, width = 160, 160
model = MobileNet(input_shape=[height,width,3],classes=2)
model.load_weights(weights_path,by_name=True)
print('加载完成...')

checkpoint_save_path = "./results/last_one88.h5"
model_dir = "./results/"
checkpoint_period = save_model(model, checkpoint_save_path, model_dir)

# 学习率下降的方式，acc三次不下降就下降学习率继续训练
reduce_lr = ReduceLROnPlateau(
                        monitor='accuracy',  # 检测的指标
                        factor=0.5,     # 当acc不下降时将学习率下调的比例
                        patience=3,     # 检测轮数是每隔三轮
                        verbose=2       # 信息展示模式
                    )

early_stopping = EarlyStopping(
                            monitor='val_accuracy',  # 检测的指标
                            min_delta=0.0001,         # 增大或减小的阈值
                            patience=3,         # 检测的轮数频率
                            verbose=1            # 信息展示的模式
                        )

# 一次的训练集大小
batch_size = 32
# 图片数据路径
data_path = basic_path + 'image'
# 图片处理
train_generator, test_generator = processing_data(data_path, height=160, width=160, batch_size=batch_size, test_split=0.1)

# 编译模型
model.compile(loss='binary_crossentropy',  # 二分类损失函数   
              optimizer=Adam(lr=0.001),   # 优化器
              metrics=['accuracy'])        # 优化目标
# 训练模型
history = model.fit(train_generator,    
                    epochs=30, # epochs: 整数，数据的迭代总轮数。
                    # 一个epoch包含的步数,通常应该等于你的数据集的样本数量除以批量大小。
                    steps_per_epoch=637 // batch_size,
                    validation_data=test_generator,
                    validation_steps=70 // batch_size,
                    initial_epoch=0, # 整数。开始训练的轮次（有助于恢复之前的训练）。
                    callbacks=[checkpoint_period, reduce_lr])

# 保存模型
model.save_weights(model_dir + 'temp.h5')

plt.plot(history.history['loss'],label = 'train_loss')
plt.plot(history.history['val_loss'],'r',label = 'val_loss')
plt.legend()
plt.show()

plt.plot(history.history['accuracy'],label = 'acc')
plt.plot(history.history['val_accuracy'],'r',label = 'val_acc')
plt.legend()
plt.show()
