import React from 'react';
import { Result, Button } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';

const ComingSoon = ({ title, description }) => {
  const navigate = useNavigate();
  const location = useLocation();
  
  const isAdminPage = location.pathname.includes('/admin');
  const returnPath = isAdminPage ? '/admin/tasks' : '/';

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
    >
      <Result
        status="403"
        title={title || "功能开发中"}
        subTitle={description || "该功能正在开发中，敬请期待！"}
        extra={
          <Button type="primary" onClick={() => navigate(returnPath)}>
            {isAdminPage ? '返回任务列表' : '返回首页'}
          </Button>
        }
      />
    </motion.div>
  );
};

export default ComingSoon; 