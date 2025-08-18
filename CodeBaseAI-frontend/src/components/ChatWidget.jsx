import React, {useState, useRef, useEffect} from 'react';
import {
    Button,
    Input,
    Space,
    Typography,
    Avatar,
    Spin,
    message
} from 'antd';
import {
    MessageOutlined,
    SendOutlined,
    CloseOutlined,
    RobotOutlined,
    UserOutlined
} from '@ant-design/icons';
import {motion, AnimatePresence} from 'framer-motion';
import {TaskApi} from '../api/task';
import {useParams} from 'react-router-dom';

const {TextArea} = Input;
const {Text} = Typography;


const ChatWidget = ({darkMode = false}) => {
    const {taskId} = useParams();
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([
        {
            id: 1,
            type: 'bot',
            content: '你好！我是CodeBase AI助手，有什么可以帮助你的吗？',
            timestamp: new Date()
        }
    ]);
    const [inputValue, setInputValue] = useState('');
    const [loading, setLoading] = useState(false);
    const messagesEndRef = useRef(null);
    const inputRef = useRef(null);

    // 自动滚动到底部
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({behavior: 'smooth'});
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    // 焦点管理
    useEffect(() => {
        if (isOpen) {
            setTimeout(() => {
                inputRef.current?.focus();
            }, 300);
        }
    }, [isOpen]);

    // 发送消息
    const sendMessage = async () => {
        if (!inputValue.trim() || loading) return;

        const userMessage = {
            id: Date.now(),
            type: 'user',
            content: inputValue.trim(),
            timestamp: new Date()
        };

        setMessages(prev => [...prev, userMessage]);
        setInputValue('');
        setLoading(true);

        try {
            // 调用ChatController的callChat接口
            const response = await TaskApi.chat(userMessage.content, taskId)
            console.log(response)
            const botMessage = {
                id: Date.now() + 1,
                type: 'bot',
                content: response.data.response || '抱歉，我现在无法回答您的问题。',
                timestamp: new Date()
            };

            setMessages(prev => [...prev, botMessage]);
        } catch (error) {
            console.error('Chat API error:', error);
            const errorMessage = {
                id: Date.now() + 1,
                type: 'bot',
                content: '抱歉，服务出现了一些问题，请稍后再试。',
                timestamp: new Date()
            };
            setMessages(prev => [...prev, errorMessage]);
            message.error('发送消息失败，请检查网络连接');
        } finally {
            setLoading(false);
        }
    };

    // 处理键盘事件
    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    };

    // 格式化时间
    const formatTime = (date) => {
        return date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
    };

    // 悬浮球样式
    const floatingButtonStyle = {
        position: 'fixed',
        right: '24px',
        bottom: '24px',
        width: '56px',
        height: '56px',
        borderRadius: '50%',
        backgroundColor: '#1890ff',
        border: 'none',
        boxShadow: '0 8px 32px rgba(24, 144, 255, 0.3), 0 4px 16px rgba(24, 144, 255, 0.2)',
        zIndex: 1000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
        transition: 'all 0.3s ease',
        backdropFilter: 'blur(10px)',
        background: 'linear-gradient(135deg, rgba(24, 144, 255, 0.9), rgba(64, 169, 255, 0.8))'
    };

    // 悬浮聊天窗样式
    const floatingChatStyle = {
        position: 'fixed',
        right: '24px',
        bottom: '90px',
        width: '380px',
        height: '500px',
        borderRadius: '16px',
        zIndex: 1001,
        border: darkMode ? '1px solid rgba(255, 255, 255, 0.1)' : '1px solid rgba(0, 0, 0, 0.08)',
        boxShadow: darkMode
            ? '0 24px 48px rgba(0, 0, 0, 0.4), 0 12px 24px rgba(0, 0, 0, 0.3)'
            : '0 24px 48px rgba(0, 0, 0, 0.12), 0 12px 24px rgba(0, 0, 0, 0.08)',
        backdropFilter: 'blur(20px) saturate(180%)',
        background: darkMode
            ? 'rgba(30, 30, 30, 0.85)'
            : 'rgba(255, 255, 255, 0.85)',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
    };

    // 消息气泡样式
    const getMessageStyle = (type) => ({
        maxWidth: '260px',
        minWidth: '60px',
        margin: type === 'user' ? '0 0 0 auto' : '0 auto 0 0',
        padding: '10px 14px',
        borderRadius: '18px',
        background: type === 'user'
            ? 'linear-gradient(135deg, rgba(24, 144, 255, 0.9), rgba(64, 169, 255, 0.8))'
            : darkMode
                ? 'rgba(255, 255, 255, 0.1)'
                : 'rgba(0, 0, 0, 0.04)',
        color: type === 'user'
            ? '#fff'
            : (darkMode ? '#fff' : '#262626'),
        wordBreak: 'break-word',
        wordWrap: 'break-word',
        whiteSpace: 'pre-wrap',
        lineHeight: '1.5',
        boxSizing: 'border-box',
        boxShadow: type === 'user'
            ? '0 4px 12px rgba(24, 144, 255, 0.2)'
            : darkMode
                ? '0 2px 8px rgba(0, 0, 0, 0.3)'
                : '0 2px 8px rgba(0, 0, 0, 0.06)'
    });

    return (
        <>
            {/* 悬浮球按钮 */}
            <motion.div
                style={floatingButtonStyle}
                whileHover={{
                    scale: 1.1,
                    boxShadow: '0 12px 40px rgba(24, 144, 255, 0.4), 0 6px 20px rgba(24, 144, 255, 0.3)'
                }}
                whileTap={{scale: 0.95}}
                onClick={() => setIsOpen(true)}
                title="AI助手"
            >
                <MessageOutlined style={{fontSize: '24px', color: '#fff'}}/>
            </motion.div>

            {/* 悬浮聊天窗 */}
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        initial={{opacity: 0, scale: 0.8, y: 20}}
                        animate={{opacity: 1, scale: 1, y: 0}}
                        exit={{opacity: 0, scale: 0.8, y: 20}}
                        transition={{
                            type: "spring",
                            stiffness: 300,
                            damping: 25,
                            duration: 0.3
                        }}
                        style={floatingChatStyle}
                    >
                        {/* 头部 */}
                        <div style={{
                            padding: '16px 20px',
                            borderBottom: darkMode
                                ? '1px solid rgba(255, 255, 255, 0.1)'
                                : '1px solid rgba(0, 0, 0, 0.08)',
                            background: darkMode
                                ? 'rgba(40, 40, 40, 0.9)'
                                : 'rgba(255, 255, 255, 0.9)',
                            backdropFilter: 'blur(20px)',
                            borderRadius: '16px 16px 0 0',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between'
                        }}>
                            <div style={{display: 'flex', alignItems: 'center'}}>
                                <RobotOutlined style={{
                                    marginRight: '8px',
                                    color: '#1890ff',
                                    fontSize: '18px'
                                }}/>
                                <Text strong style={{
                                    color: darkMode ? '#fff' : '#262626',
                                    fontSize: '16px'
                                }}>CodeBase AI助手</Text>
                            </div>
                            <motion.div
                                whileHover={{scale: 1.1, backgroundColor: 'rgba(255, 77, 79, 0.1)'}}
                                whileTap={{scale: 0.9}}
                                onClick={() => setIsOpen(false)}
                                style={{
                                    width: '32px',
                                    height: '32px',
                                    borderRadius: '8px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    cursor: 'pointer',
                                    transition: 'all 0.2s ease'
                                }}
                            >
                                <CloseOutlined style={{
                                    color: darkMode ? '#bfbfbf' : '#8c8c8c',
                                    fontSize: '14px'
                                }}/>
                            </motion.div>
                        </div>

                        {/* 消息列表区域 */}
                        <div style={{
                            flex: 1,
                            padding: '16px 20px',
                            overflowY: 'auto',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '12px',
                            background: 'transparent'
                        }}>
                            <AnimatePresence>
                                {messages.map((msg) => (
                                    <motion.div
                                        key={msg.id}
                                        initial={{opacity: 0, y: 20}}
                                        animate={{opacity: 1, y: 0}}
                                        exit={{opacity: 0, y: -20}}
                                        transition={{duration: 0.3}}
                                        style={{
                                            display: 'flex',
                                            flexDirection: msg.type === 'user' ? 'row-reverse' : 'row',
                                            alignItems: 'flex-start',
                                            gap: '8px'
                                        }}
                                    >
                                        {/* 头像 */}
                                        <Avatar
                                            size="small"
                                            icon={msg.type === 'user' ? <UserOutlined/> : <RobotOutlined/>}
                                            style={{
                                                backgroundColor: msg.type === 'user'
                                                    ? 'linear-gradient(135deg, #1890ff, #40a9ff)'
                                                    : 'linear-gradient(135deg, #52c41a, #73d13d)',
                                                border: '2px solid rgba(255, 255, 255, 0.2)',
                                                backdropFilter: 'blur(10px)',
                                                flexShrink: 0
                                            }}
                                        />

                                        {/* 消息内容 */}
                                        <div style={{
                                            display: 'flex',
                                            flexDirection: 'column',
                                            flex: 1,
                                            minWidth: 0
                                        }}>
                                            <motion.div
                                                whileHover={{scale: 1.02}}
                                                style={{
                                                    ...getMessageStyle(msg.type),
                                                    backdropFilter: 'blur(10px)',
                                                    border: msg.type === 'user'
                                                        ? '1px solid rgba(255, 255, 255, 0.2)'
                                                        : darkMode
                                                            ? '1px solid rgba(255, 255, 255, 0.1)'
                                                            : '1px solid rgba(0, 0, 0, 0.05)'
                                                }}
                                            >
                                                {msg.content}
                                            </motion.div>
                                            <Text
                                                type="secondary"
                                                style={{
                                                    fontSize: '11px',
                                                    marginTop: '4px',
                                                    textAlign: msg.type === 'user' ? 'right' : 'left',
                                                    color: darkMode ? '#8c8c8c' : '#999'
                                                }}
                                            >
                                                {formatTime(msg.timestamp)}
                                            </Text>
                                        </div>
                                    </motion.div>
                                ))}
                            </AnimatePresence>

                            {/* Loading状态 */}
                            {loading && (
                                <motion.div
                                    initial={{opacity: 0, y: 20}}
                                    animate={{opacity: 1, y: 0}}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'flex-start',
                                        gap: '8px'
                                    }}
                                >
                                    <Avatar
                                        size="small"
                                        icon={<RobotOutlined/>}
                                        style={{
                                            background: 'linear-gradient(135deg, #52c41a, #73d13d)',
                                            border: '2px solid rgba(255, 255, 255, 0.2)',
                                            backdropFilter: 'blur(10px)'
                                        }}
                                    />
                                    <div style={{
                                        ...getMessageStyle('bot'),
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '8px',
                                        backdropFilter: 'blur(10px)',
                                        border: darkMode
                                            ? '1px solid rgba(255, 255, 255, 0.1)'
                                            : '1px solid rgba(0, 0, 0, 0.05)'
                                    }}>
                                        <Spin size="small"/>
                                        <Text style={{color: darkMode ? '#fff' : '#262626'}}>正在思考...</Text>
                                    </div>
                                </motion.div>
                            )}

                            <div ref={messagesEndRef}/>
                        </div>

                        {/* 输入区域 */}
                        <div style={{
                            padding: '16px 20px',
                            borderTop: darkMode
                                ? '1px solid rgba(255, 255, 255, 0.1)'
                                : '1px solid rgba(0, 0, 0, 0.08)',
                            background: darkMode
                                ? 'rgba(40, 40, 40, 0.9)'
                                : 'rgba(255, 255, 255, 0.9)',
                            backdropFilter: 'blur(20px)',
                            borderRadius: '0 0 16px 16px'
                        }}>
                            <Space.Compact style={{width: '100%'}}>
                                <TextArea
                                    ref={inputRef}
                                    value={inputValue}
                                    onChange={(e) => setInputValue(e.target.value)}
                                    onKeyPress={handleKeyPress}
                                    placeholder="输入您的问题..."
                                    autoSize={{minRows: 1, maxRows: 3}}
                                    disabled={loading}
                                    style={{
                                        resize: 'none',
                                        borderRadius: '12px',
                                        backgroundColor: darkMode
                                            ? 'rgba(255, 255, 255, 0.05)'
                                            : 'rgba(0, 0, 0, 0.02)',
                                        border: darkMode
                                            ? '1px solid rgba(255, 255, 255, 0.1)'
                                            : '1px solid rgba(0, 0, 0, 0.08)',
                                        backdropFilter: 'blur(10px)'
                                    }}
                                />
                                <Button
                                    type="primary"
                                    icon={<SendOutlined/>}
                                    onClick={sendMessage}
                                    loading={loading}
                                    disabled={!inputValue.trim()}
                                    style={{
                                        height: 'auto',
                                        borderRadius: '12px',
                                        marginLeft: '8px',
                                        background: 'linear-gradient(135deg, #1890ff, #40a9ff)',
                                        border: 'none',
                                        boxShadow: '0 4px 12px rgba(24, 144, 255, 0.3)'
                                    }}
                                />
                            </Space.Compact>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
};

export default ChatWidget;