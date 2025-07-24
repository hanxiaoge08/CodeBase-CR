import React from 'react';
import { Empty, Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';

const EmptyState = ({ description, showAction = true }) => {
  const navigate = useNavigate();
  const location = useLocation();
  
  const isAdminPage = location.pathname.includes('/admin');
  const createPath = isAdminPage ? '/admin/task/create' : '/admin/task/create';

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      style={{ 
        display: 'flex', 
        flexDirection: 'column', 
        alignItems: 'center', 
        justifyContent: 'center',
        padding: '40px 0' 
      }}
    >
      <Empty 
        description={description || '暂无数据'} 
        image={Empty.PRESENTED_IMAGE_SIMPLE}
      />
      
      {showAction && isAdminPage && (
        <Button 
          type="primary" 
          icon={<PlusOutlined />}
          onClick={() => navigate(createPath)}
          style={{ marginTop: 16 }}
        >
          创建新任务
        </Button>
      )}
    </motion.div>
  );
};

export default EmptyState; 