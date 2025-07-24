import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Descriptions, 
  Button, 
  Tag, 
  Space, 
  message, 
  Alert,
  Typography,
  Divider
} from 'antd';
import { 
  EditOutlined, 
  DeleteOutlined, 
  RollbackOutlined, 
  LinkOutlined
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { TaskApi } from '../api/task';
import { formatDateTime, getStatusColor } from '../utils/dateFormat';
import PageLoading from '../components/PageLoading';

const { Title, Paragraph } = Typography;

const TaskDetail = () => {
  const navigate = useNavigate();
  const { taskId } = useParams();
  const [loading, setLoading] = useState(true);
  const [task, setTask] = useState(null);
  const [error, setError] = useState('');

  // 获取任务详情
  const fetchTaskDetail = async () => {
    setLoading(true);
    try {
      const response = await TaskApi.getTaskDetail(taskId);
      if (response.code === 200) {
        setTask(response.data);
        setError('');
      } else {
        setError(response.msg || '获取任务详情失败');
      }
    } catch (error) {
      console.error('获取任务详情出错:', error);
      setError('获取任务详情失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (taskId) {
      fetchTaskDetail();
    } else {
      setError('无效的任务ID');
      setLoading(false);
    }
  }, [taskId]);

  // 处理删除任务
  const handleDelete = async () => {
    try {
      const response = await TaskApi.deleteTask(taskId);
      if (response.code === 200) {
        message.success('删除成功');
        navigate('/admin/tasks');
      } else {
        message.error(response.msg || '删除失败');
      }
    } catch (error) {
      console.error('删除任务出错:', error);
      message.error('删除失败');
    }
  };

  // 动画配置
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: { 
      opacity: 1,
      transition: { 
        duration: 0.6,
        staggerChildren: 0.1
      }
    }
  };

  const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { 
      y: 0, 
      opacity: 1,
      transition: { type: 'spring', stiffness: 300, damping: 24 }
    }
  };

  // 渲染加载状态
  if (loading) {
    return <PageLoading tip="加载任务详情..." />;
  }

  // 渲染错误状态
  if (error) {
    return (
      <Alert
        message="错误"
        description={error}
        type="error"
        showIcon
        action={
          <Button size="small" type="primary" onClick={() => navigate('/admin/tasks')}>
            返回列表
          </Button>
        }
      />
    );
  }

  // 渲染详情内容
  return (
    <motion.div
      variants={containerVariants}
      initial="hidden"
      animate="visible"
    >
      <Card
        title={
          <Space>
            <span>任务详情</span>
            {task && <Tag color={getStatusColor(task.status)}>{task.status}</Tag>}
          </Space>
        }
        extra={
          <Space>
            <Button 
              icon={<EditOutlined />} 
              onClick={() => navigate(`/admin/task/edit/${taskId}`)}
            >
              编辑
            </Button>
            <Button 
              danger 
              icon={<DeleteOutlined />} 
              onClick={handleDelete}
            >
              删除
            </Button>
            <Button 
              icon={<RollbackOutlined />} 
              onClick={() => navigate('/admin/tasks')}
            >
              返回列表
            </Button>
          </Space>
        }
      >
        {task && (
          <>
            <motion.div variants={itemVariants}>
              <Title level={4}>{task.projectName}</Title>
              <Paragraph type="secondary">
                <LinkOutlined /> {task.projectUrl}
              </Paragraph>
            </motion.div>

            <Divider />

            <motion.div variants={itemVariants}>
              <Descriptions bordered column={2}>
                <Descriptions.Item label="任务ID" span={2}>
                  {task.taskId}
                </Descriptions.Item>
                <Descriptions.Item label="项目名称">
                  {task.projectName}
                </Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag color={getStatusColor(task.status)}>{task.status}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="创建时间">
                  {formatDateTime(task.createTime)}
                </Descriptions.Item>
                <Descriptions.Item label="更新时间">
                  {formatDateTime(task.updateTime)}
                </Descriptions.Item>
                <Descriptions.Item label="项目URL" span={2}>
                  <a href={task.projectUrl} target="_blank" rel="noopener noreferrer">
                    {task.projectUrl}
                  </a>
                </Descriptions.Item>
                {task.failReason && (
                  <Descriptions.Item label="失败原因" span={2}>
                    <Alert message={task.failReason} type="error" />
                  </Descriptions.Item>
                )}
              </Descriptions>
            </motion.div>
          </>
        )}
      </Card>
    </motion.div>
  );
};

export default TaskDetail; 