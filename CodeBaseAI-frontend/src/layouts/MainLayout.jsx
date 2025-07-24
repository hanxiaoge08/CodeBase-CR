import React from 'react';
import { Layout, Menu, theme, Typography } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { 
  FolderOutlined, 
  AppstoreOutlined, 
  TeamOutlined, 
  RocketOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import HeaderNav from '../components/HeaderNav';

const { Content, Sider } = Layout;
const { Title } = Typography;

const AdminLayout = ({ darkMode, toggleDarkMode }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  // 菜单项
  const menuItems = [
    {
      key: '/admin/tasks',
      icon: <FolderOutlined />,
      label: '任务管理',
    },
    {
      key: '/admin/dashboard',
      icon: <AppstoreOutlined />,
      label: '数据统计',
    },
    {
      key: '/admin/team',
      icon: <TeamOutlined />,
      label: '团队管理',
    },
    {
      key: '/admin/deploy',
      icon: <RocketOutlined />,
      label: '系统部署',
    },
  ];

  // 处理菜单点击
  const handleMenuClick = (e) => {
    navigate(e.key);
  };

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
      transition: { type: 'spring', stiffness: 300, damping: 24 }
    }
  };

  // 确定当前选中的菜单项
  const selectedKeys = [location.pathname];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <HeaderNav darkMode={darkMode} toggleDarkMode={toggleDarkMode} />
      <Layout>
        <Sider width={200} style={{ background: colorBgContainer }}>
          <Menu
            mode="inline"
            selectedKeys={selectedKeys}
            style={{ height: '100%', borderRight: 0 }}
            items={menuItems}
            onClick={handleMenuClick}
          />
        </Sider>
        <Layout style={{ padding: '24px' }}>
          <Content
            style={{
              padding: 24,
              margin: 0,
              background: colorBgContainer,
              borderRadius: borderRadiusLG,
            }}
          >
            <motion.div
              variants={containerVariants}
              initial="hidden"
              animate="visible"
              style={{ height: '100%' }}
            >
              <motion.div variants={itemVariants}>
                <Outlet />
              </motion.div>
            </motion.div>
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
};

export default AdminLayout; 