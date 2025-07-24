import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Tag, 
  Modal, 
  message, 
  Input, 
  Tooltip
} from 'antd';
import { 
  PlusOutlined, 
  SearchOutlined, 
  EditOutlined, 
  DeleteOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { TaskApi } from '../api/task';
import { formatDateTime, getStatusColor } from '../utils/dateFormat';
import EmptyState from '../components/EmptyState';

const TaskList = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [searchParams, setSearchParams] = useState({
    projectName: '',
    taksId: '', // 注意：这里是后端传参的原样拼写，有拼写错误
  });

  // 加载任务列表数据
  const fetchTasks = async (params = {}) => {
    setLoading(true);
    try {
      const requestParams = {
        pageIndex: params.current || pagination.current,
        pageSize: params.pageSize || pagination.pageSize,
        ...searchParams,
      };
      
      const response = await TaskApi.getTasksByPage(requestParams);
      if (response.code === 200) {
        setData(response.data.records);
        setPagination({
          ...pagination,
          current: response.data.current,
          pageSize: response.data.size,
          total: response.data.total,
        });
      } else {
        message.error(response.msg || '获取任务列表失败');
      }
    } catch (error) {
      console.error('获取任务列表出错:', error);
      message.error('获取任务列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks();
  }, []);

  // 处理表格变化
  const handleTableChange = (pagination) => {
    fetchTasks(pagination);
  };

  // 处理搜索
  const handleSearch = () => {
    fetchTasks({ current: 1 });
  };

  // 处理搜索参数变化
  const handleSearchParamChange = (e, key) => {
    setSearchParams({
      ...searchParams,
      [key]: e.target.value,
    });
  };

  // 处理删除任务
  const handleDelete = (taskId) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除此任务吗？此操作不可恢复。',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await TaskApi.deleteTask(taskId);
          if (response.code === 200) {
            message.success('删除成功');
            fetchTasks();
          } else {
            message.error(response.msg || '删除失败');
          }
        } catch (error) {
          console.error('删除任务出错:', error);
          message.error('删除失败');
        }
      },
    });
  };

  // 表格列定义
  const columns = [
    {
      title: '任务ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 180,
    },
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      width: 200,
    },
    {
      title: '项目URL',
      dataIndex: 'projectUrl',
      key: 'projectUrl',
      ellipsis: {
        showTitle: false,
      },
      render: (text) => (
        <Tooltip placement="topLeft" title={text}>
          {text}
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => (
        <Tag color={getStatusColor(status)}>{status}</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (text) => formatDateTime(text),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space size="middle">
          <Button 
            type="primary"
            icon={<InfoCircleOutlined />} 
            size="small"
            onClick={() => navigate(`/admin/task/detail/${record.taskId}`)}
          >
            详情
          </Button>
          <Button 
            icon={<EditOutlined />} 
            size="small"
            onClick={() => navigate(`/admin/task/edit/${record.taskId}`)}
          >
            编辑
          </Button>
          <Button 
            danger 
            icon={<DeleteOutlined />} 
            size="small"
            onClick={() => handleDelete(record.taskId)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
    >
      <Card
        title="任务列表"
        extra={
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={() => navigate('/admin/task/create')}
          >
            新建任务
          </Button>
        }
      >
        <Space style={{ marginBottom: 16 }}>
          <Input
            placeholder="项目名称"
            value={searchParams.projectName}
            onChange={(e) => handleSearchParamChange(e, 'projectName')}
            style={{ width: 200 }}
          />
          <Input
            placeholder="任务ID"
            value={searchParams.taksId}
            onChange={(e) => handleSearchParamChange(e, 'taksId')}
            style={{ width: 200 }}
          />
          <Button 
            type="primary" 
            icon={<SearchOutlined />} 
            onClick={handleSearch}
          >
            搜索
          </Button>
        </Space>
        
        {data.length === 0 && !loading ? (
          <EmptyState description="暂无任务数据" />
        ) : (
          <Table
            columns={columns}
            dataSource={data}
            rowKey="taskId"
            pagination={pagination}
            loading={loading}
            onChange={handleTableChange}
            scroll={{ x: 1200 }}
          />
        )}
      </Card>
    </motion.div>
  );
};

export default TaskList; 