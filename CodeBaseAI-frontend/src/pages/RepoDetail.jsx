import React, { useState, useEffect, useRef } from 'react';
import { 
  Layout,
  Tree,
  Typography, 
  Button, 
  Space, 
  Input,
  Tag,
  Spin,
  Alert,
  Tooltip,
  Drawer,
  Affix,
  Empty,
  Divider,
  BackTop,
  ConfigProvider
} from 'antd';
import { 
  GithubOutlined, 
  StarOutlined, 
  HomeOutlined,
  ShareAltOutlined,
  FileTextOutlined,
  FolderOutlined,
  SearchOutlined,
  MenuOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SunOutlined,
  MoonOutlined,
  ArrowLeftOutlined,
  BookOutlined,
  UnorderedListOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  CopyOutlined,
  CheckOutlined
} from '@ant-design/icons';
import { useNavigate, useParams, useOutletContext } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github-dark.css';
import { TaskApi } from '../api/task';
import { formatDateTime } from '../utils/dateFormat';
import PageLoading from '../components/PageLoading';
import MermaidChart from '../components/MermaidChart';
import ChatWidget from '../components/ChatWidget';

const { Title, Text, Paragraph } = Typography;
const { Sider, Content, Header } = Layout;
const { Search } = Input;

const RepoDetail = () => {
  const navigate = useNavigate();
  const { taskId } = useParams();
  const { darkMode, toggleDarkMode } = useOutletContext();
  const [loading, setLoading] = useState(true);
  const [task, setTask] = useState(null);
  const [catalogueTree, setCatalogueTree] = useState([]);
  const [selectedContent, setSelectedContent] = useState('');
  const [selectedTitle, setSelectedTitle] = useState('');
  const [error, setError] = useState('');
  const [expandedKeys, setExpandedKeys] = useState([]);
  const [collapsed, setCollapsed] = useState(false);
  const [searchValue, setSearchValue] = useState('');
  const [mobileDrawerVisible, setMobileDrawerVisible] = useState(false);
  const [anchors, setAnchors] = useState([]);
  const [activeAnchor, setActiveAnchor] = useState('');
  const [copied, setCopied] = useState(false);
  const contentRef = useRef(null);

  // 获取任务详情和目录树
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [taskResponse, treeResponse] = await Promise.all([
          TaskApi.getTaskDetail(taskId),
          TaskApi.getCatalogueTree(taskId)
        ]);
        
        if (taskResponse.code === 200) {
          setTask(taskResponse.data);
        } else {
          setError(taskResponse.msg || '获取仓库详情失败');
          return;
        }
        
        if (treeResponse.code === 200) {
          const treeData = buildTreeData(treeResponse.data);
          setCatalogueTree(treeData);
          
          const firstLevelKeys = treeResponse.data
            .filter(item => item.children && item.children.length > 0)
            .map(item => item.catalogueId);
          setExpandedKeys(firstLevelKeys);
          
          const firstLeaf = findFirstLeafWithContent(treeResponse.data);
          if (firstLeaf) {
            setSelectedContent(firstLeaf.content || '');
            setSelectedTitle(firstLeaf.title || '');
          }
        }
        
        setError('');
      } catch (error) {
        console.error('获取数据失败:', error);
        setError('获取数据失败');
      } finally {
        setLoading(false);
      }
    };

    if (taskId) {
      fetchData();
    } else {
      setError('无效的仓库ID');
      setLoading(false);
    }
  }, [taskId]);

  // 提取文章大纲
  useEffect(() => {
    if (selectedContent) {
      const headings = [];
      const lines = selectedContent.split('\n');
      lines.forEach((line, index) => {
        const match = line.match(/^(#{1,3})\s+(.+)/);
        if (match) {
          const level = match[1].length;
          const title = match[2];
          const id = `heading-${index}`;
          headings.push({ id, title, level });
        }
      });
      setAnchors(headings);
    }
  }, [selectedContent]);

  // 构建树形数据结构
  const buildTreeData = (data) => {
    const buildNode = (item, level = 0) => ({
      title: item.name,
      key: item.catalogueId,
      icon: item.children && item.children.length > 0 ? <FolderOutlined /> : <FileTextOutlined />,
      content: item.content,
      name: item.name,
      level: level,
      isLeaf: !item.children || item.children.length === 0,
      isParent: item.children && item.children.length > 0,
      children: item.children ? item.children.map(child => buildNode(child, level + 1)) : []
    });

    return data.map(item => buildNode(item, 0));
  };

  // 查找第一个有内容的叶子节点
  const findFirstLeafWithContent = (data) => {
    for (const item of data) {
      if (item.children && item.children.length > 0) {
        const found = findFirstLeafWithContent(item.children);
        if (found) return found;
      } else if (item.content) {
        return item;
      }
    }
    return null;
  };

  // 查找节点内容
  const findNodeContent = (treeData, key) => {
    for (const node of treeData) {
      if (node.key === key) {
        return { content: node.content, title: node.title, isParent: node.isParent };
      }
      if (node.children && node.children.length > 0) {
        const found = findNodeContent(node.children, key);
        if (found) return found;
      }
    }
    return null;
  };

  // 处理节点选择
  const handleTreeSelect = (selectedKeys) => {
    if (selectedKeys.length > 0) {
      const nodeData = findNodeContent(catalogueTree, selectedKeys[0]);
      if (nodeData && !nodeData.isParent) {
        setSelectedContent(nodeData.content || '暂无内容');
        setSelectedTitle(nodeData.title || '');
        setMobileDrawerVisible(false);
        // 滚动到顶部
        if (contentRef.current) {
          contentRef.current.scrollTop = 0;
        }
      }
    }
  };

  // 处理节点展开收缩
  const handleTreeExpand = (expandedKeysValue) => {
    setExpandedKeys(expandedKeysValue);
  };

  // 搜索过滤树节点
  const filterTreeNode = (node) => {
    if (!searchValue) return true;
    return node.title.toLowerCase().includes(searchValue.toLowerCase());
  };

  // 复制链接
  const handleCopyLink = () => {
    navigator.clipboard?.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // 随机生成星星数
  const getRandomStars = () => {
    return Math.floor(Math.random() * 200) + 1;
  };

  // 渲染加载状态
  if (loading) {
    return <PageLoading tip="加载仓库详情..." />;
  }

  // 渲染错误状态
  if (error) {
    return (
      <div style={{ 
        height: '100vh', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        padding: '20px' 
      }}>
        <Alert
          message="错误"
          description={error}
          type="error"
          showIcon
          action={
            <Button type="primary" onClick={() => navigate('/')}>
              返回首页
            </Button>
          }
        />
      </div>
    );
  }

  // 提取仓库名称
  const repoName = task?.projectUrl?.includes('github.com') 
    ? task.projectUrl.split('github.com/')[1]?.split('/').slice(0, 2).join(' / ')
    : task?.projectName;

  // 侧边栏渲染
  const renderSidebar = () => (
    <div style={{ 
      height: '100%', 
      display: 'flex', 
      flexDirection: 'column',
      background: darkMode ? '#141414' : '#fff'
    }}>
      {/* 搜索框 */}
      <div style={{ padding: '16px' }}>
        <Search
          placeholder="搜索文档..."
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
          style={{ width: '100%' }}
          prefix={<SearchOutlined />}
          allowClear
        />
      </div>
      
      {/* 目录树 */}
      <div style={{ 
        flex: 1, 
        overflow: 'auto', 
        padding: '0 8px 16px'
      }}>
        {catalogueTree.length > 0 ? (
          <Tree
            expandedKeys={expandedKeys}
            onExpand={handleTreeExpand}
            onSelect={handleTreeSelect}
            treeData={catalogueTree}
            filterTreeNode={filterTreeNode}
            showIcon
            style={{ 
              fontSize: '14px',
              background: 'transparent'
            }}
            className="custom-wiki-tree"
          />
        ) : (
          <Empty 
            description="暂无目录数据" 
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        )}
      </div>
    </div>
  );

  return (
    <ConfigProvider
      theme={{
        token: {
          colorBgContainer: darkMode ? '#1f1f1f' : '#fff',
          colorBgLayout: darkMode ? '#141414' : '#f5f5f5',
          colorText: darkMode ? '#ffffffd9' : '#000000d9',
        }
      }}
    >
      <Layout style={{ height: '100vh' }}>
        {/* 顶部导航栏 */}
        <Header 
          style={{ 
            background: darkMode ? '#1f1f1f' : '#fff',
            borderBottom: darkMode ? '1px solid #303030' : '1px solid #f0f0f0',
            padding: '0 24px',
            height: '56px',
            lineHeight: '56px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            position: 'sticky',
            top: 0,
            zIndex: 100,
            boxShadow: '0 2px 8px rgba(0,0,0,0.06)'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            {/* 移动端菜单按钮 */}
            <Button
              type="text"
              icon={<MenuOutlined />}
              onClick={() => setMobileDrawerVisible(true)}
              style={{ display: window.innerWidth < 768 ? 'block' : 'none' }}
            />
            
            {/* Logo和标题 */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <BookOutlined style={{ fontSize: '20px', color: '#1890ff' }} />
              <Title level={5} style={{ margin: 0, fontWeight: 600 }}>
                {repoName || 'Wiki'}
              </Title>
            </div>

            {/* 桌面端折叠按钮 */}
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ display: window.innerWidth >= 768 ? 'block' : 'none' }}
            />
          </div>

          <Space>
            {/* 主题切换 */}
            <Tooltip title={darkMode ? '切换到亮色主题' : '切换到暗色主题'}>
              <Button
                type="text"
                icon={darkMode ? <SunOutlined /> : <MoonOutlined />}
                onClick={toggleDarkMode}
              />
            </Tooltip>
            
            {/* GitHub链接 */}
            <Tooltip title="查看源码">
              <Button
                type="text"
                icon={<GithubOutlined />}
                href={task?.projectUrl}
                target="_blank"
              />
            </Tooltip>
            
            {/* 分享按钮 */}
            <Tooltip title={copied ? '已复制' : '复制链接'}>
              <Button
                type="text"
                icon={copied ? <CheckOutlined /> : <ShareAltOutlined />}
                onClick={handleCopyLink}
              />
            </Tooltip>
            
            {/* 返回首页 */}
            <Tooltip title="返回首页">
              <Button
                type="text"
                icon={<HomeOutlined />}
                onClick={() => navigate('/')}
              />
            </Tooltip>
          </Space>
        </Header>

        <Layout>
          {/* 侧边栏 - 桌面端 */}
          <Sider 
            width={280}
            collapsible
            collapsed={collapsed}
            trigger={null}
            style={{ 
              background: darkMode ? '#141414' : '#fff',
              borderRight: darkMode ? '1px solid #303030' : '1px solid #f0f0f0',
              overflow: 'hidden',
              display: window.innerWidth < 768 ? 'none' : 'block'
            }}
          >
            {renderSidebar()}
          </Sider>

          {/* 侧边栏 - 移动端 */}
          <Drawer
            title="文档目录"
            placement="left"
            open={mobileDrawerVisible}
            onClose={() => setMobileDrawerVisible(false)}
            width={280}
            bodyStyle={{ padding: 0 }}
          >
            {renderSidebar()}
          </Drawer>

          {/* 主内容区 */}
          <Layout style={{ background: darkMode ? '#141414' : '#f5f5f5' }}>
            <Content 
              style={{ 
                padding: '24px',
                overflow: 'auto',
                position: 'relative'
              }}
              ref={contentRef}
            >
              <div style={{ 
                maxWidth: '1200px', 
                margin: '0 auto',
                display: 'flex',
                gap: '24px'
              }}>
                {/* 文章内容 */}
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.3 }}
                  style={{ flex: 1, minWidth: 0 }}
                >
                  <div style={{
                    background: darkMode ? '#1f1f1f' : '#fff',
                    borderRadius: '8px',
                    padding: '32px',
                    boxShadow: darkMode 
                      ? '0 2px 8px rgba(0,0,0,0.3)' 
                      : '0 2px 8px rgba(0,0,0,0.06)'
                  }}>
                    {/* 文章标题 */}
                    {selectedTitle && (
                      <div style={{ marginBottom: '24px' }}>
                        <Title level={2} style={{ marginBottom: '16px' }}>
                          {selectedTitle}
                        </Title>
                        <Space split={<Divider type="vertical" />}>
                          <Text type="secondary">
                            <ClockCircleOutlined /> {formatDateTime(task?.updateTime, 'YYYY-MM-DD')}
                          </Text>
                          <Text type="secondary">
                            <EyeOutlined /> {Math.floor(Math.random() * 1000) + 100} 阅读
                          </Text>
                          <Tag color="blue">
                            <StarOutlined /> {getRandomStars()} stars
                          </Tag>
                        </Space>
                        <Divider />
                      </div>
                    )}

                    {/* 文章内容 */}
                    {selectedContent ? (
                      <div className="markdown-body" style={{ fontSize: '15px', lineHeight: '1.8' }}>
                        <ReactMarkdown
                          remarkPlugins={[remarkGfm]}
                          rehypePlugins={[rehypeHighlight]}
                          components={{
                            h1: ({children, ...props}) => {
                              const id = `heading-${props.node?.position?.start?.line}`;
                              return <h1 id={id} style={{ 
                                fontSize: '28px', 
                                fontWeight: 600,
                                marginTop: '24px',
                                marginBottom: '16px',
                                paddingBottom: '8px',
                                borderBottom: darkMode ? '1px solid #303030' : '1px solid #e8e8e8'
                              }}>{children}</h1>;
                            },
                            h2: ({children, ...props}) => {
                              const id = `heading-${props.node?.position?.start?.line}`;
                              return <h2 id={id} style={{ 
                                fontSize: '22px',
                                fontWeight: 600,
                                marginTop: '20px',
                                marginBottom: '12px'
                              }}>{children}</h2>;
                            },
                            h3: ({children, ...props}) => {
                              const id = `heading-${props.node?.position?.start?.line}`;
                              return <h3 id={id} style={{ 
                                fontSize: '18px',
                                fontWeight: 600,
                                marginTop: '16px',
                                marginBottom: '8px'
                              }}>{children}</h3>;
                            },
                            p: ({children}) => (
                              <p style={{ 
                                marginBottom: '16px',
                                color: darkMode ? '#ffffffd9' : '#262626'
                              }}>{children}</p>
                            ),
                            code: ({node, inline, className = '', children, ...props}) => {
                              if (inline) {
                                return (
                                  <code style={{
                                    background: darkMode ? '#2d2d2d' : '#f6f8fa',
                                    padding: '2px 6px',
                                    borderRadius: '3px',
                                    fontSize: '14px',
                                    color: darkMode ? '#ff7875' : '#d73a49',
                                    fontFamily: 'SFMono-Regular, Consolas, monospace'
                                  }}>{children}</code>
                                );
                              }
                              return (
                                <code className={className} {...props}>
                                  {children}
                                </code>
                              );
                            },
                            pre: ({children}) => {
                              const textContent = children?.props?.children;
                              if (typeof textContent === 'string' && textContent.includes('graph') || textContent?.includes('flowchart')) {
                                return <MermaidChart chart={textContent} />;
                              }
                              return (
                                <pre style={{
                                  background: darkMode ? '#1e1e1e' : '#f6f8fa',
                                  padding: '16px',
                                  borderRadius: '6px',
                                  overflow: 'auto',
                                  fontSize: '14px',
                                  lineHeight: '1.5',
                                  marginBottom: '16px'
                                }}>{children}</pre>
                              );
                            },
                            blockquote: ({children}) => (
                              <blockquote style={{
                                borderLeft: '4px solid #1890ff',
                                paddingLeft: '16px',
                                margin: '16px 0',
                                background: darkMode ? '#1f1f1f' : '#f6f8fa',
                                padding: '16px',
                                borderRadius: '6px',
                                color: darkMode ? '#ffffffa6' : '#595959'
                              }}>{children}</blockquote>
                            ),
                            a: ({children, href}) => (
                              <a 
                                href={href} 
                                target="_blank" 
                                rel="noopener noreferrer"
                                style={{ 
                                  color: '#1890ff',
                                  textDecoration: 'none'
                                }}
                                onMouseEnter={(e) => e.target.style.textDecoration = 'underline'}
                                onMouseLeave={(e) => e.target.style.textDecoration = 'none'}
                              >{children}</a>
                            ),
                            ul: ({children}) => (
                              <ul style={{ 
                                paddingLeft: '20px',
                                marginBottom: '16px',
                                lineHeight: '1.8'
                              }}>{children}</ul>
                            ),
                            ol: ({children}) => (
                              <ol style={{ 
                                paddingLeft: '20px',
                                marginBottom: '16px',
                                lineHeight: '1.8'
                              }}>{children}</ol>
                            ),
                            li: ({children}) => (
                              <li style={{ marginBottom: '4px' }}>{children}</li>
                            ),
                            table: ({children}) => (
                              <div style={{ overflowX: 'auto', marginBottom: '16px' }}>
                                <table style={{
                                  width: '100%',
                                  borderCollapse: 'collapse',
                                  border: darkMode ? '1px solid #303030' : '1px solid #d9d9d9'
                                }}>{children}</table>
                              </div>
                            ),
                            th: ({children}) => (
                              <th style={{
                                border: darkMode ? '1px solid #303030' : '1px solid #d9d9d9',
                                padding: '12px',
                                background: darkMode ? '#262626' : '#fafafa',
                                fontWeight: 600
                              }}>{children}</th>
                            ),
                            td: ({children}) => (
                              <td style={{
                                border: darkMode ? '1px solid #303030' : '1px solid #d9d9d9',
                                padding: '12px'
                              }}>{children}</td>
                            )
                          }}
                        >
                          {selectedContent}
                        </ReactMarkdown>
                      </div>
                    ) : (
                      <Empty 
                        description="请从左侧选择文档查看"
                        style={{ padding: '60px 0' }}
                      />
                    )}
                  </div>
                </motion.div>

                {/* 文章大纲 - 桌面端显示 */}
                {anchors.length > 0 && window.innerWidth >= 1400 && (
                  <Affix offsetTop={80}>
                    <div style={{ width: '240px' }}>
                      <div style={{
                        background: darkMode ? '#1f1f1f' : '#fff',
                        borderRadius: '8px',
                        padding: '16px',
                        boxShadow: darkMode 
                          ? '0 2px 8px rgba(0,0,0,0.3)' 
                          : '0 2px 8px rgba(0,0,0,0.06)'
                      }}>
                        <div style={{ 
                          display: 'flex', 
                          alignItems: 'center',
                          marginBottom: '12px'
                        }}>
                          <UnorderedListOutlined style={{ marginRight: '8px' }} />
                          <Text strong>文章大纲</Text>
                        </div>
                        <div style={{ maxHeight: '400px', overflow: 'auto' }}>
                          {anchors.map((anchor) => (
                            <div
                              key={anchor.id}
                              style={{
                                paddingLeft: `${(anchor.level - 1) * 16}px`,
                                marginBottom: '8px',
                                cursor: 'pointer',
                                color: activeAnchor === anchor.id 
                                  ? '#1890ff' 
                                  : (darkMode ? '#ffffffa6' : '#595959'),
                                fontSize: anchor.level === 1 ? '14px' : '13px',
                                fontWeight: anchor.level === 1 ? 500 : 400,
                                transition: 'color 0.3s'
                              }}
                              onClick={() => {
                                const element = document.getElementById(anchor.id);
                                element?.scrollIntoView({ behavior: 'smooth' });
                                setActiveAnchor(anchor.id);
                              }}
                              onMouseEnter={(e) => e.target.style.color = '#1890ff'}
                              onMouseLeave={(e) => {
                                if (activeAnchor !== anchor.id) {
                                  e.target.style.color = darkMode ? '#ffffffa6' : '#595959';
                                }
                              }}
                            >
                              {anchor.title}
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  </Affix>
                )}
              </div>
            </Content>
          </Layout>
        </Layout>

        {/* 回到顶部 */}
        <BackTop target={() => contentRef.current} />
        
        {/* Chat Widget */}
        <ChatWidget darkMode={darkMode} />
      </Layout>

      {/* 自定义样式 */}
      <style jsx>{`
        .custom-wiki-tree .ant-tree-treenode {
          padding: 2px 0;
        }
        
        .custom-wiki-tree .ant-tree-node-content-wrapper {
          padding: 4px 8px;
          border-radius: 4px;
          transition: all 0.2s;
        }
        
        .custom-wiki-tree .ant-tree-node-content-wrapper:hover {
          background: ${darkMode ? '#262626' : '#f5f5f5'};
        }
        
        .custom-wiki-tree .ant-tree-node-content-wrapper.ant-tree-node-selected {
          background: #e6f7ff;
          border: 1px solid #91d5ff;
        }
        
        .markdown-body img {
          max-width: 100%;
          height: auto;
        }
        
        .markdown-body hr {
          margin: 24px 0;
          border: none;
          border-top: 1px solid ${darkMode ? '#303030' : '#e8e8e8'};
        }
        
        /* 响应式设计 */
        @media (max-width: 768px) {
          .ant-layout-header {
            padding: 0 16px !important;
          }
          
          .ant-layout-content {
            padding: 16px !important;
          }
          
          .markdown-body {
            font-size: 14px !important;
          }
        }
      `}</style>
    </ConfigProvider>
  );
};

export default RepoDetail;