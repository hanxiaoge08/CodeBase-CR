import React, { useState, useEffect } from 'react';
import { 
  Input, 
  Card, 
  Typography, 
  Space, 
  Tag, 
  Tooltip, 
  Row, 
  Col,
  Empty,
  Spin
} from 'antd';
import { 
  SearchOutlined, 
  StarOutlined, 
  ArrowRightOutlined,
  GithubOutlined,
  PlusOutlined
} from '@ant-design/icons';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { motion } from 'framer-motion';
import { TaskApi } from '../api/task';
import { formatDateTime } from '../utils/dateFormat';
import AddRepoModal from '../components/AddRepoModal';

const { Title, Text, Paragraph } = Typography;

const HomePage = () => {
  const navigate = useNavigate();
  const { darkMode } = useOutletContext();
  const [searchValue, setSearchValue] = useState('');
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [addModalVisible, setAddModalVisible] = useState(false);

  // 获取已完成的任务列表
  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    setLoading(true);
    try {
      const response = await TaskApi.getTasksByPage({
        pageIndex: 1,
        pageSize: 100,
        status: '已完成'
      });
      
      if (response.code === 200) {
        setTasks(response.data.records || []);
      }
    } catch (error) {
      console.error('获取任务列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 过滤任务
  const filteredTasks = tasks.filter(task => 
    task.projectName?.toLowerCase().includes(searchValue.toLowerCase()) ||
    (task.projectUrl && task.projectUrl.toLowerCase().includes(searchValue.toLowerCase()))
  );

  // 动画变量
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: { 
      opacity: 1,
      transition: { 
        staggerChildren: 0.1 
      } 
    },
  };

  const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { 
      y: 0, 
      opacity: 1,
      transition: { type: 'spring', stiffness: 200, damping: 20 }
    }
  };

  const handleCardClick = (taskId) => {
    navigate(`/repo/${taskId}`);
  };

  // 显示添加仓库弹窗
  const showAddModal = () => {
    setAddModalVisible(true);
  };

  // 关闭添加仓库弹窗
  const hideAddModal = () => {
    setAddModalVisible(false);
  };

  // 添加仓库成功后的回调
  const handleAddSuccess = (newTask) => {
    // 刷新任务列表
    fetchTasks();
  };

  // 获取随机星星数
  const getRandomStars = () => {
    return (Math.floor(Math.random() * 200) + 1) + 'k';
  };

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: '20px' }}>
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        style={{ textAlign: 'center', marginBottom: 60 }}
      >
        <Title level={2} style={{ fontSize: 32, marginBottom: 10 }}>
          CodeBaseAI
        </Title>
        <Paragraph style={{ fontSize: 18, marginBottom: 30 }}>
          哪个仓库你想了解?
        </Paragraph>
        
        <Input
          size="large"
          placeholder="搜索仓库 (或粘贴链接)"
          prefix={<SearchOutlined />}
          style={{ 
            maxWidth: 800, 
            borderRadius: 8,
            padding: '12px 16px',
            boxShadow: '0 2px 10px rgba(0, 0, 0, 0.05)'
          }}
          value={searchValue}
          onChange={e => setSearchValue(e.target.value)}
        />
      </motion.div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin size="large" tip="加载中..." />
        </div>
      ) : (
        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          <Row gutter={[16, 16]}>
            {/* 添加项目卡片 - 始终显示，并且颜色不同 */}
            <Col xs={24} sm={12} md={8} lg={8} xl={8}>
              <motion.div variants={itemVariants}>
                <Card 
                  hoverable
                  style={{ 
                    height: '100%', 
                    background: darkMode 
                      ? 'linear-gradient(135deg, #1f3a5f 0%, #2d4a6b 100%)'
                      : 'linear-gradient(135deg, #e0f7ff 0%, #d0f1ff 100%)',
                    borderRadius: 8,
                    boxShadow: '0 2px 10px rgba(0, 0, 0, 0.05)'
                  }}
                  onClick={showAddModal}
                >
                  <div style={{ 
                    display: 'flex', 
                    flexDirection: 'column', 
                    alignItems: 'center', 
                    justifyContent: 'center',
                    height: 130
                  }}>
                    <div style={{ 
                      fontSize: 28, 
                      marginBottom: 16, 
                      width: 50, 
                      height: 50, 
                      borderRadius: '50%', 
                      background: darkMode ? '#2c3e50' : '#f0f9ff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
                    }}>
                      <PlusOutlined />
                    </div>
                    <Text strong>添加仓库</Text>
                  </div>
                </Card>
              </motion.div>
            </Col>

            {filteredTasks.length > 0 ? (
              filteredTasks.map(task => (
                <Col xs={24} sm={12} md={8} lg={8} xl={8} key={task.taskId}>
                  <motion.div variants={itemVariants}>
                    <Card 
                      hoverable
                      style={{ 
                        height: '100%',
                        borderRadius: 8,
                        boxShadow: '0 2px 10px rgba(0, 0, 0, 0.05)'
                      }}
                      onClick={() => handleCardClick(task.taskId)}
                    >
                      <Space direction="vertical" size="small" style={{ width: '100%' }}>
                        <div style={{ display: 'flex', alignItems: 'center' }}>
                          <GithubOutlined style={{ marginRight: 8 }} />
                          <Text strong style={{ fontSize: 16 }}>
                            {task.projectUrl && task.projectUrl.includes('github.com') 
                              ? task.projectUrl.split('github.com/')[1]?.split('/').slice(0, 2).join(' / ')
                              : task.projectName}
                          </Text>
                        </div>
                        
                        <Paragraph ellipsis={{ rows: 2 }} style={{ marginTop: 8, height: 44 }}>
                          {task.projectName} - 在 {formatDateTime(task.createTime, 'YYYY-MM-DD')} 创建
                        </Paragraph>
                        
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
                          <Tag icon={<StarOutlined />} color="default">
                            {getRandomStars()}
                          </Tag>
                          <ArrowRightOutlined />
                        </div>
                      </Space>
                    </Card>
                  </motion.div>
                </Col>
              ))
            ) : (
              <Col span={24}>
                <Empty
                  description="暂无匹配的项目"
                  style={{ margin: '40px 0' }}
                />
              </Col>
            )}
          </Row>
        </motion.div>
      )}

      {/* 添加仓库弹窗 */}
      <AddRepoModal 
        visible={addModalVisible} 
        onCancel={hideAddModal} 
        onSuccess={handleAddSuccess}
      />
    </div>
  );
};

export default HomePage; 