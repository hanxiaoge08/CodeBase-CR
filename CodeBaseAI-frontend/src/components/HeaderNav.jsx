import React from 'react';
import { Layout, Typography, Button, Space, Switch } from 'antd';
import { GithubOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';

const { Header } = Layout;
const { Title } = Typography;

const HeaderNav = ({ darkMode, toggleDarkMode }) => {
  const navigate = useNavigate();
  const location = useLocation();
  
  const isAdminPage = location.pathname.includes('/admin');

  return (
    <Header 
      style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        padding: '0 24px',
        background: isAdminPage ? '#001529' : (darkMode ? '#1f1f1f' : '#fff'),
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
        position: 'sticky',
        top: 0,
        zIndex: 1000,
        height: 64
      }}
    >
      <motion.div
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.5, ease: 'easeOut' }}
        style={{ display: 'flex', alignItems: 'center' }}
      >
        <Title 
          level={3} 
          style={{ 
            margin: 0, 
            cursor: 'pointer',
            color: isAdminPage ? 'white' : (darkMode ? '#ffffff' : 'inherit')
          }}
          onClick={() => navigate(isAdminPage ? '/admin' : '/')}
        >
          {isAdminPage ? 'CodeBaseAI 管理后台' : 'CodeBaseAI'}
        </Title>
      </motion.div>

      <Space>
        <Switch 
          checkedChildren="🌙" 
          unCheckedChildren="☀️" 
          checked={darkMode}
          onChange={toggleDarkMode}
        />
        
        {isAdminPage ? (
          <Button 
            type="primary" 
            onClick={() => navigate('/')}
          >
            返回前台
          </Button>
        ) : (
          <Button 
            icon={<UserOutlined />}
            onClick={() => navigate('/admin/tasks')}
          >
            管理后台
          </Button>
        )}
        
        <Button 
          icon={<GithubOutlined />} 
          href="https://github.com" 
          target="_blank"
        >
          GitHub
        </Button>
      </Space>
    </Header>
  );
};

export default HeaderNav; 