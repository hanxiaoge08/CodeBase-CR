import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp, theme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import AdminLayout from './layouts/MainLayout';
import BasicLayout from './layouts/BasicLayout';
import TaskList from './pages/TaskList';
import TaskDetail from './pages/TaskDetail';
import TaskCreate from './pages/TaskCreate';
import TaskEdit from './pages/TaskEdit';
import HomePage from './pages/HomePage';
import RepoDetail from './pages/RepoDetail';
import ComingSoon from './components/ComingSoon';
import themeConfig from './theme/themeConfig';
import './App.css';

const App = () => {
  const [darkMode, setDarkMode] = useState(false);
  
  const toggleDarkMode = () => {
    setDarkMode(!darkMode);
  };

  // 合并暗色主题配置
  const mergedTheme = {
    ...themeConfig,
    algorithm: darkMode ? theme.darkAlgorithm : theme.defaultAlgorithm,
  };

  return (
    <ConfigProvider theme={mergedTheme} locale={zhCN}>
      <AntApp>
        <BrowserRouter>
          <Routes>
            {/* 前台路由 */}
            <Route path="/" element={<BasicLayout darkMode={darkMode} toggleDarkMode={toggleDarkMode} />}>
              <Route index element={<HomePage darkMode={darkMode} />} />
              <Route path="repo/:taskId" element={<RepoDetail darkMode={darkMode} />} />
            </Route>

            {/* 管理后台路由 */}
            <Route path="/admin" element={<AdminLayout darkMode={darkMode} toggleDarkMode={toggleDarkMode} />}>
              <Route index element={<Navigate to="/admin/tasks" replace />} />
              <Route path="tasks" element={<TaskList darkMode={darkMode} />} />
              <Route path="task/detail/:taskId" element={<TaskDetail darkMode={darkMode} />} />
              <Route path="task/create" element={<TaskCreate darkMode={darkMode} />} />
              <Route path="task/edit/:taskId" element={<TaskEdit darkMode={darkMode} />} />
              {/* 占位路由，防止404 */}
              <Route path="dashboard" element={<ComingSoon title="数据统计" description="数据统计功能正在开发中，敬请期待！" />} />
              <Route path="team" element={<ComingSoon title="团队管理" description="团队管理功能正在开发中，敬请期待！" />} />
              <Route path="deploy" element={<ComingSoon title="系统部署" description="系统部署功能正在开发中，敬请期待！" />} />
            </Route>

            {/* 重定向其他所有路径到首页 */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  );
};

export default App; 