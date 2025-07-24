import React, { useState, useEffect } from 'react';
import { 
  Layout,
  Tree,
  Typography, 
  Button, 
  Space, 
  Breadcrumb,
  Tag,
  Spin,
  Alert,
  Card,
  Divider
} from 'antd';
import { 
  GithubOutlined, 
  StarOutlined, 
  HomeOutlined,
  ShareAltOutlined,
  FileTextOutlined,
  FolderOutlined
} from '@ant-design/icons';
import { useNavigate, useParams, useOutletContext } from 'react-router-dom';
import { motion } from 'framer-motion';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github.css'; // 代码高亮样式
import { TaskApi } from '../api/task';
import { formatDateTime } from '../utils/dateFormat';
import PageLoading from '../components/PageLoading';
import MermaidChart from '../components/MermaidChart';

const { Title, Text, Paragraph } = Typography;
const { Sider, Content } = Layout;

// Markdown 自定义样式
const markdownStyles = {
  container: {
    lineHeight: 1.8,
    fontSize: '14px',
    color: '#262626'
  },
  heading: {
    marginTop: '24px',
    marginBottom: '16px',
    borderBottom: '1px solid #f0f0f0',
    paddingBottom: '8px'
  },
  blockquote: {
    borderLeft: '4px solid #1890ff',
    paddingLeft: '16px',
    margin: '16px 0',
    background: '#f6f8fa',
    padding: '16px',
    borderRadius: '6px'
  },
  inlineCode: {
    background: '#f6f8fa',
    padding: '2px 6px',
    borderRadius: '3px',
    fontSize: '13px',
    color: '#d73a49',
    fontFamily: '"SFMono-Regular", "Consolas", "Liberation Mono", "Menlo", "monospace"',
    border: '1px solid #e1e4e8',
    verticalAlign: 'baseline'
  },
  codeBlock: {
    background: '#f6f8fa',
    padding: '16px',
    borderRadius: '6px',
    overflow: 'auto',
    fontSize: '13px',
    fontFamily: '"SFMono-Regular", "Consolas", "Liberation Mono", "Menlo", "monospace"',
    border: '1px solid #e1e4e8'
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    border: '1px solid #d9d9d9',
    marginTop: '16px',
    marginBottom: '16px'
  },
  th: {
    border: '1px solid #d9d9d9',
    padding: '12px 16px',
    background: '#fafafa',
    fontWeight: 'bold',
    textAlign: 'left'
  },
  td: {
    border: '1px solid #d9d9d9',
    padding: '12px 16px'
  },
  list: {
    paddingLeft: '20px',
    margin: '16px 0'
  },
  listItem: {
    marginBottom: '6px'
  }
};

const RepoDetail = () => {
  const navigate = useNavigate();
  const { taskId } = useParams();
  const { darkMode } = useOutletContext();
  const [loading, setLoading] = useState(true);
  const [task, setTask] = useState(null);
  const [catalogueTree, setCatalogueTree] = useState([]);
  const [selectedContent, setSelectedContent] = useState('');
  const [selectedTitle, setSelectedTitle] = useState('');
  const [error, setError] = useState('');
  const [treeLoading, setTreeLoading] = useState(false);
  const [expandedKeys, setExpandedKeys] = useState([]);

  // 获取任务详情和目录树
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // 并行获取任务详情和目录树
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
          
          // 初始时展开所有一级节点（只展开第一层）
          const firstLevelKeys = treeResponse.data
            .filter(item => item.children && item.children.length > 0)
            .map(item => item.catalogueId);
          setExpandedKeys(firstLevelKeys);
          
          // 默认选择第一个有内容的叶子节点
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

  // 构建树形数据结构
  const buildTreeData = (data) => {
    const buildNode = (item, level = 0) => ({
      title: item.name,
      key: item.catalogueId,
      icon: item.children && item.children.length > 0 ? <FolderOutlined /> : <FileTextOutlined />,
      content: item.content,
      name: item.name,
      level: level, // 添加层级信息
      isLeaf: !item.children || item.children.length === 0,
      isParent: item.children && item.children.length > 0, // 是否为父节点
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

  // 处理节点选择 - 只有叶子节点才显示内容
  const handleTreeSelect = (selectedKeys) => {
    if (selectedKeys.length > 0) {
      const nodeData = findNodeContent(catalogueTree, selectedKeys[0]);
      if (nodeData && !nodeData.isParent) {
        setSelectedContent(nodeData.content || '暂无内容');
        setSelectedTitle(nodeData.title || '');
      }
    }
  };

  // 处理节点展开收缩
  const handleTreeExpand = (expandedKeysValue) => {
    setExpandedKeys(expandedKeysValue);
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
      <div style={{ padding: '20px' }}>
        <Alert
          message="错误"
          description={error}
          type="error"
          showIcon
          action={
            <Button size="small" type="primary" onClick={() => navigate('/')}>
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

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* 页面头部 */}
      <div style={{ 
        padding: '16px 24px', 
        borderBottom: darkMode ? '1px solid #303030' : '1px solid #f0f0f0',
        background: darkMode ? '#1f1f1f' : '#fff',
        flexShrink: 0
      }}>
        <Breadcrumb style={{ marginBottom: 16 }}>
          <Breadcrumb.Item href="/" onClick={(e) => {
            e.preventDefault();
            navigate('/');
          }}>
            <HomeOutlined /> 首页
          </Breadcrumb.Item>
          <Breadcrumb.Item>{repoName}</Breadcrumb.Item>
        </Breadcrumb>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space direction="vertical" size="small">
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <GithubOutlined style={{ fontSize: 20, marginRight: 8 }} />
              <Title level={4} style={{ margin: 0 }}>{repoName}</Title>
            </div>
            <Space size="large">
              <Tag icon={<StarOutlined />} color="default">
                {getRandomStars()}k stars
              </Tag>
              <Text type="secondary">
                更新于 {formatDateTime(task?.updateTime, 'YYYY-MM-DD')}
              </Text>
            </Space>
          </Space>
          
          <Space>
            <Button 
              type="primary" 
              icon={<ShareAltOutlined />}
              onClick={() => navigator.clipboard?.writeText(window.location.href)}
            >
              分享
            </Button>
            <Button
              href={task?.projectUrl}
              target="_blank"
              icon={<GithubOutlined />}
            >
              查看源码
            </Button>
          </Space>
        </div>
      </div>

      {/* 主体布局 */}
      <Layout style={{ flex: 1, minHeight: 0 }}>
        {/* 左侧目录树 */}
        <Sider 
          width={280} 
          style={{ 
            background: darkMode ? '#1f1f1f' : '#fff',
            borderRight: darkMode ? '1px solid #303030' : '1px solid #f0f0f0',
            height: '100%',
            overflow: 'hidden'
          }}
        >
          <div style={{ padding: '12px 8px', height: '100%', display: 'flex', flexDirection: 'column' }}>
            <div style={{ flex: 1, overflow: 'auto', paddingRight: '4px' }}>
              {catalogueTree.length > 0 ? (
                <>
                  <style>
                    {`
                      /* 基础树节点样式 */
                      .custom-tree .ant-tree-treenode {
                        padding: 2px 0 !important;
                        margin: 0 !important;
                      }
                      
                      .custom-tree .ant-tree-node-content-wrapper {
                        padding: 6px 8px !important;
                        border-radius: 4px !important;
                        transition: all 0.15s ease !important;
                        margin: 0 !important;
                        height: 32px !important;
                        line-height: 1.5 !important;
                        display: flex !important;
                        align-items: center !important;
                        box-sizing: border-box !important;
                      }
                      
                      .custom-tree .ant-tree-node-content-wrapper:hover {
                        background-color: ${darkMode ? '#2a2a2a' : '#f5f5f5'} !important;
                      }
                      
                      .custom-tree .ant-tree-node-content-wrapper.ant-tree-node-selected {
                        background-color: ${darkMode ? '#1f3a5f' : '#e6f7ff'} !important;
                        border: 1px solid ${darkMode ? '#4096ff' : '#91d5ff'} !important;
                        box-shadow: 0 1px 2px rgba(24, 144, 255, 0.08) !important;
                      }
                      
                      /* 展开折叠按钮样式 */
                      .custom-tree .ant-tree-switcher {
                        width: 20px !important;
                        height: 20px !important;
                        line-height: 20px !important;
                        margin-right: 6px !important;
                        border-radius: 3px !important;
                        display: flex !important;
                        align-items: center !important;
                        justify-content: center !important;
                        flex-shrink: 0 !important;
                        vertical-align: top !important;
                      }
                      
                      .custom-tree .ant-tree-switcher:hover {
                        background-color: #f0f0f0 !important;
                      }
                      
                      .custom-tree .ant-tree-switcher-icon {
                        font-size: 12px !important;
                        transform: none !important;
                        display: flex !important;
                        align-items: center !important;
                        justify-content: center !important;
                      }
                      
                      /* 隐藏图标 */
                      .custom-tree .ant-tree-iconEle {
                        display: none !important;
                      }
                      
                      /* 一级标题样式 */
                      .custom-tree .tree-title-level-0 {
                        font-size: 15px !important;
                        font-weight: 600 !important;
                        color: ${darkMode ? '#ffffff' : '#262626'} !important;
                        cursor: pointer !important;
                        display: flex !important;
                        align-items: center !important;
                        line-height: 1.5 !important;
                        height: 20px !important;
                      }
                      
                      /* 二级标题样式 */
                      .custom-tree .tree-title-level-1 {
                        font-size: 14px !important;
                        font-weight: 400 !important;
                        color: ${darkMode ? '#d9d9d9' : '#595959'} !important;
                        cursor: pointer !important;
                        display: flex !important;
                        align-items: center !important;
                        line-height: 1.5 !important;
                        height: 20px !important;
                      }
                      
                      /* 二级标题前的小圆点 */
                      .custom-tree .tree-title-level-1::before {
                        content: "•" !important;
                        margin-right: 6px !important;
                        font-size: 12px !important;
                        color: #bfbfbf !important;
                        line-height: 1 !important;
                        display: flex !important;
                        align-items: center !important;
                      }
                      
                      /* 子树缩进和连线 */
                      .custom-tree .ant-tree-child-tree {
                        margin-left: 16px !important;
                        border-left: 1px dashed #e8e8e8 !important;
                        padding-left: 8px !important;
                      }
                      
                      /* 一级节点禁用选中 */
                      .custom-tree .tree-title-level-0.tree-parent-title {
                        pointer-events: none !important;
                      }
                      
                      .custom-tree .ant-tree-treenode:has(.tree-title-level-0) .ant-tree-node-content-wrapper.ant-tree-node-selected {
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                      }
                      
                      .custom-tree .ant-tree-treenode:has(.tree-title-level-0) .ant-tree-node-content-wrapper:hover {
                        background-color: #f8f8f8 !important;
                      }
                      
                      /* 树节点间距优化 */
                      .custom-tree .ant-tree-list-holder-inner {
                        padding: 4px 0 !important;
                      }
                      
                      /* 叶子节点样式 */
                      .custom-tree .ant-tree-treenode-leaf-last .ant-tree-node-content-wrapper {
                        margin-bottom: 0 !important;
                      }
                      
                      /* 内容区域滚动条样式 */
                      .ant-card-body::-webkit-scrollbar {
                        width: 8px;
                      }
                      
                      .ant-card-body::-webkit-scrollbar-track {
                        background: #f5f5f5;
                        border-radius: 4px;
                      }
                      
                      .ant-card-body::-webkit-scrollbar-thumb {
                        background: #bfbfbf;
                        border-radius: 4px;
                      }
                      
                      .ant-card-body::-webkit-scrollbar-thumb:hover {
                        background: #999;
                      }
                    `}
                  </style>
              <Tree
                    expandedKeys={expandedKeys}
                    onExpand={handleTreeExpand}
                onSelect={handleTreeSelect}
                treeData={catalogueTree}
                    className="custom-tree"
                    selectable={true}
                    titleRender={(nodeData) => (
                      <span 
                        className={`tree-title-level-${nodeData.level} ${nodeData.isParent ? 'tree-parent-title' : ''}`}
                      >
                        {nodeData.title}
                      </span>
                    )}
                style={{ fontSize: '14px' }}
              />
                </>
            ) : (
              <div style={{ textAlign: 'center', padding: '20px 0' }}>
                <Spin spinning={treeLoading}>
                  <Text type="secondary">暂无目录数据</Text>
                </Spin>
              </div>
            )}
            </div>
          </div>
        </Sider>

        {/* 右侧内容区域 */}
        <Content style={{ 
          padding: '24px',
          background: darkMode ? '#1f1f1f' : '#fff',
          display: 'flex',
          flexDirection: 'column',
          height: '100%',
          overflow: 'hidden'
        }}>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
            style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}
          >
            <Card 
              title={
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <FileTextOutlined style={{ marginRight: 8 }} />
                  {selectedTitle || '选择左侧目录查看内容'}
                </div>
              }
              style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}
              bodyStyle={{ 
                flex: 1, 
                overflow: 'auto', 
                padding: '24px',
                height: 0,
                scrollbarWidth: 'thin',
                scrollbarColor: '#bfbfbf #f5f5f5'
              }}
            >
                            {selectedContent ? (
                <div 
                  style={markdownStyles.container}
                  className="markdown-content"
                >
                  <style>
                    {`
                      .markdown-content code:not(pre code) {
                        display: inline !important;
                        padding: 0 3px !important;
                        margin: 0 !important;
                        background: #f8f8f8 !important;
                        color: #e83e8c !important;
                        border-radius: 3px !important;
                        font-size: 0.9em !important;
                        line-height: inherit !important;
                        white-space: nowrap !important;
                        vertical-align: baseline !important;
                        border: none !important;
                        box-sizing: border-box !important;
                      }
                    `}
                  </style>
                  <ReactMarkdown
                    remarkPlugins={[remarkGfm]}
                    rehypePlugins={[rehypeHighlight]}
                    components={{
                      // 测试函数，打印所有代码块
                      code: ({node, inline, className = '', children, ...props}) => {
                        console.log('Code block detected:', { inline, className, children });
                        
                        // 只处理内联代码，让pre组件处理代码块
                        if (inline) {
                          return (
                            <code 
                              style={{
                                background: '#f8f8f8',
                                padding: '0 3px',
                                margin: '0',
                                borderRadius: '3px',
                                fontSize: '0.9em',
                                color: '#e83e8c',
                                fontFamily: 'SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
                                display: 'inline',
                                lineHeight: 'inherit',
                                whiteSpace: 'nowrap',
                                verticalAlign: 'baseline',
                                border: 'none',
                                boxSizing: 'border-box'
                              }}
                              className={className}
                              {...props}
                            >
                              {children}
                            </code>
                          );
                        }
                        
                        // 非内联代码由pre组件处理
                        return (
                          <code 
                            style={{
                              background: 'transparent',
                              padding: '0',
                              margin: '0',
                              borderRadius: '0',
                              fontSize: 'inherit',
                              color: 'inherit',
                              fontFamily: 'SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
                              display: 'block',
                              lineHeight: '1.45',
                              whiteSpace: 'pre',
                              verticalAlign: 'baseline',
                              border: 'none',
                              boxSizing: 'border-box'
                            }}
                            className={className}
                            {...props}
                          >
                            {children}
                          </code>
                        );
                      },
                      // 自定义渲染组件
                      h1: ({children}) => (
                        <Typography.Title 
                          level={2} 
                          style={markdownStyles.heading}
                        >
                          {children}
                        </Typography.Title>
                      ),
                      h2: ({children}) => (
                        <Typography.Title 
                          level={3} 
                          style={markdownStyles.heading}
                        >
                          {children}
                        </Typography.Title>
                      ),
                      h3: ({children}) => (
                        <Typography.Title 
                          level={4} 
                          style={markdownStyles.heading}
                        >
                          {children}
                        </Typography.Title>
                      ),
                      h4: ({children}) => (
                        <Typography.Title 
                          level={5}
                          style={markdownStyles.heading}
                        >
                          {children}
                        </Typography.Title>
                      ),
                      p: ({children}) => (
                        <Typography.Paragraph 
                          style={{ 
                            marginBottom: '16px',
                            lineHeight: '1.8',
                            wordBreak: 'break-word'
                          }}
                        >
                          {children}
                        </Typography.Paragraph>
                      ),
                      blockquote: ({children}) => (
                        <div style={markdownStyles.blockquote}>
                          {children}
                        </div>
                                              ),
                      pre: ({children}) => {
                        // 安全地检查子元素是否是 Mermaid 图表
                        try {
                          // 验证 children 是否存在
                          if (!children) {
                            return <pre style={markdownStyles.codeBlock}>{children}</pre>;
                          }

                          // 尝试提取文本内容来检测是否是Mermaid
                          let textContent = '';
                          
                          // 递归提取所有文本内容
                          const extractText = (node) => {
                            if (typeof node === 'string') {
                              return node;
                            } else if (node && node.props && node.props.children) {
                              if (typeof node.props.children === 'string') {
                                return node.props.children;
                              } else if (Array.isArray(node.props.children)) {
                                return node.props.children.map(extractText).join('');
                              }
                            }
                            return '';
                          };
                          
                          if (Array.isArray(children)) {
                            textContent = children.map(extractText).join('');
                              } else {
                            textContent = extractText(children);
                              }
                              
                          textContent = textContent.trim();
                          
                          // 检查是否包含Mermaid语法 - 更严格的匹配
                          const mermaidPatterns = [
                            /^graph\s+(TD|TB|BT|RL|LR)/i,
                            /^flowchart\s+(TD|TB|BT|RL|LR)/i,
                            /^sequenceDiagram[\s\n]/i,
                            /^classDiagram[\s\n]/i,
                            /^stateDiagram(-v2)?[\s\n]/i,
                            /^erDiagram[\s\n]/i,
                            /^gantt[\s\n]/i,
                            /^journey[\s\n]/i,
                            /^pie\s+title/i,
                            /^gitgraph[\s\n]/i
                          ];
                          
                          // 额外验证：必须包含基本的Mermaid语法元素
                          const hasValidMermaidSyntax = (content) => {
                            // 检查是否有箭头、连接符等Mermaid特征
                            const mermaidFeatures = [
                              /-->/,     // 箭头
                              /---/,     // 连线
                              /\[\s*.*\s*\]/,  // 方括号节点
                              /\(\s*.*\s*\)/,  // 圆括号节点
                              /participant\s+/i,  // 序列图参与者
                              /class\s+\w+/i,     // 类图
                              /state\s+\w+/i,     // 状态图
                              /\|\w+\|/          // 实体关系图
                            ];
                            
                            return mermaidFeatures.some(pattern => pattern.test(content));
                          };
                          
                          const isMermaid = mermaidPatterns.some(pattern => pattern.test(textContent)) 
                                           && hasValidMermaidSyntax(textContent)
                                           && textContent.length > 10; // 最小长度检查
                          
                          if (isMermaid && textContent) {
                            // 生成唯一ID
                            const uniqueId = `mermaid-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
                                
                            return (
                              <MermaidChart 
                                chart={textContent} 
                                  id={uniqueId}
                                  key={uniqueId}
                              />
                            );
                          }
                        } catch (e) {
                          // 如果处理过程中出现任何错误，回退到普通的 pre 渲染
                          console.warn('Pre component Mermaid processing error:', e);
                        }
                        
                        // 默认情况：渲染普通的代码块
                        return (
                          <pre style={markdownStyles.codeBlock}>
                            {children}
                          </pre>
                        );
                      },
                      table: ({children}) => (
                        <div style={{ overflow: 'auto', margin: '16px 0' }}>
                          <table style={markdownStyles.table}>
                            {children}
                          </table>
                        </div>
                      ),
                      th: ({children}) => (
                        <th style={markdownStyles.th}>
                          {children}
                        </th>
                      ),
                      td: ({children}) => (
                        <td style={markdownStyles.td}>
                          {children}
                        </td>
                      ),
                      ul: ({children}) => (
                        <ul style={markdownStyles.list}>
                          {children}
                        </ul>
                      ),
                      ol: ({children}) => (
                        <ol style={markdownStyles.list}>
                          {children}
                        </ol>
                      ),
                      li: ({children}) => (
                        <li style={markdownStyles.listItem}>
                          {children}
                        </li>
                      ),
                      a: ({children, href}) => (
                        <a 
                          href={href} 
                          target="_blank" 
                          rel="noopener noreferrer"
                          style={{ color: '#1890ff', textDecoration: 'none' }}
                          onMouseEnter={(e) => e.target.style.textDecoration = 'underline'}
                          onMouseLeave={(e) => e.target.style.textDecoration = 'none'}
                        >
                          {children}
                        </a>
                      ),
                      strong: ({children}) => (
                        <strong style={{ fontWeight: 600, color: '#262626' }}>
                          {children}
                        </strong>
                      ),
                      em: ({children}) => (
                        <em style={{ fontStyle: 'italic', color: '#595959' }}>
                          {children}
                        </em>
                      ),
                      hr: () => (
                        <hr style={{ 
                          margin: '24px 0', 
                          border: 'none', 
                          borderTop: '1px solid #f0f0f0' 
                        }} />
                      )
                    }}
                  >
                    {selectedContent}
                  </ReactMarkdown>
                </div>
              ) : (
                <div style={{ 
                  textAlign: 'center', 
                  padding: '60px 20px',
                  color: 'rgba(0, 0, 0, 0.45)'
                }}>
                  <FileTextOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                  <Paragraph>
                    请从左侧目录中选择一个文档来查看详细内容
                  </Paragraph>
                </div>
              )}
            </Card>
          </motion.div>
        </Content>
      </Layout>
    </div>
  );
};

export default RepoDetail; 