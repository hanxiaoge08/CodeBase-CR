import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Form, 
  Input, 
  Button, 
  Space, 
  message, 
  Alert,
  Select,
  Divider
} from 'antd';
import { 
  SaveOutlined, 
  RollbackOutlined,
  CheckCircleOutlined
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { TaskApi } from '../api/task';
import PageLoading from '../components/PageLoading';

const { Option } = Select;

const TaskEdit = () => {
  const navigate = useNavigate();
  const { taskId } = useParams();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  // 获取任务详情
  const fetchTaskDetail = async () => {
    setLoading(true);
    try {
      const response = await TaskApi.getTaskDetail(taskId);
      if (response.code === 200) {
        form.setFieldsValue(response.data);
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

  // 提交表单
  const handleSubmit = async (values) => {
    setSubmitting(true);
    try {
      const response = await TaskApi.updateTask(values);
      if (response.code === 200) {
        message.success('更新任务成功');
        navigate(`/admin/task/detail/${taskId}`);
      } else {
        message.error(response.msg || '更新任务失败');
      }
    } catch (error) {
      console.error('更新任务出错:', error);
      message.error('更新任务失败');
    } finally {
      setSubmitting(false);
    }
  };

  // 动画配置
  const containerVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { 
      opacity: 1, 
      y: 0,
      transition: { duration: 0.5, ease: "easeOut" }
    }
  };

  // 渲染加载状态
  if (loading) {
    return <PageLoading tip="加载任务数据..." />;
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

  return (
    <motion.div
      variants={containerVariants}
      initial="hidden"
      animate="visible"
    >
      <Card
        title="编辑任务"
        extra={
          <Button 
            icon={<RollbackOutlined />} 
            onClick={() => navigate(`/admin/task/detail/${taskId}`)}
          >
            返回详情
          </Button>
        }
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="taskId"
            label="任务ID"
          >
            <Input disabled />
          </Form.Item>
          
          <Form.Item
            name="projectName"
            label="项目名称"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input placeholder="请输入项目名称" />
          </Form.Item>
          
          <Form.Item
            name="projectUrl"
            label="项目URL"
            rules={[{ required: true, message: '请输入项目URL' }]}
          >
            <Input placeholder="请输入项目URL" />
          </Form.Item>
          
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Option value="进行中">进行中</Option>
              <Option value="已完成">已完成</Option>
              <Option value="处理失败">处理失败</Option>
            </Select>
          </Form.Item>
          
          <Form.Item
            name="failReason"
            label="失败原因"
          >
            <Input.TextArea 
              placeholder="如果任务失败，请描述失败原因" 
              rows={4}
            />
          </Form.Item>
          
          <Divider />
          
          <Form.Item>
            <Space>
              <Button 
                type="primary" 
                htmlType="submit" 
                loading={submitting}
                icon={<SaveOutlined />}
              >
                保存修改
              </Button>
              <Button 
                onClick={() => fetchTaskDetail()}
                disabled={submitting || loading}
                icon={<CheckCircleOutlined />}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </motion.div>
  );
};

export default TaskEdit; 