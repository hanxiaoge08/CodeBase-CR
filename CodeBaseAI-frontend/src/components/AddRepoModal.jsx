import React, { useState } from 'react';
import { 
  Modal, 
  Form, 
  Input, 
  Button, 
  Radio, 
  Upload, 
  message, 
  Space, 
  Divider 
} from 'antd';
import { 
  UploadOutlined, 
  GithubOutlined,
  LinkOutlined
} from '@ant-design/icons';
import { TaskApi } from '../api/task';
import { motion } from 'framer-motion';

const AddRepoModal = ({ visible, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [sourceType, setSourceType] = useState('git');
  const [fileList, setFileList] = useState([]);

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      
      let response;
      
      if (sourceType === 'git') {
        // 从Git创建
        response = await TaskApi.createFromGit({
          ...values,
          sourceType: 'git'
        });
      } else {
        // 从Zip创建
        if (fileList.length === 0) {
          message.error('请上传ZIP文件');
          setLoading(false);
          return;
        }
        
        const formData = new FormData();
        formData.append('file', fileList[0].originFileObj);
        formData.append('projectName', values.projectName);
        formData.append('userName', values.userName);
        formData.append('sourceType', 'zip');
        
        response = await TaskApi.createFromZip(formData);
      }
      
      if (response.code === 200) {
        message.success('添加仓库成功');
        form.resetFields();
        setFileList([]);
        onSuccess && onSuccess(response.data);
        onCancel();
      } else {
        message.error(response.msg || '添加仓库失败');
      }
    } catch (error) {
      console.error('添加仓库失败:', error);
      if (error.errorFields) {
        // 表单验证错误
        message.error('请填写完整信息');
      } else {
        message.error('添加仓库失败');
      }
    } finally {
      setLoading(false);
    }
  };

  // 处理文件上传改变
  const handleFileChange = ({ fileList }) => {
    setFileList(fileList);
  };

  // 处理源类型改变
  const handleSourceTypeChange = (e) => {
    setSourceType(e.target.value);
    // 切换类型时重置部分表单字段
    form.setFieldsValue({
      projectUrl: undefined,
      branch: undefined,
      userName: '',
      password: undefined,
    });
    
    // 清除文件列表
    if (e.target.value === 'git') {
      setFileList([]);
    }
  };

  // 文件上传前校验
  const beforeUpload = (file) => {
    const isZip = file.type === 'application/zip' || 
                 file.type === 'application/x-zip-compressed' ||
                 file.name.endsWith('.zip');
    if (!isZip) {
      message.error('只能上传ZIP文件');
    }
    const isLt50M = file.size / 1024 / 1024 < 50;
    if (!isLt50M) {
      message.error('文件必须小于50MB');
    }
    return isZip && isLt50M;
  };

  // 上传文件的属性
  const uploadProps = {
    onRemove: () => {
      setFileList([]);
    },
    beforeUpload: beforeUpload,
    onChange: handleFileChange,
    fileList,
    maxCount: 1,
    customRequest: ({ onSuccess }) => {
      setTimeout(() => {
        onSuccess("ok");
      }, 0);
    }
  };

  // 关闭时重置表单
  const handleCancel = () => {
    form.resetFields();
    setFileList([]);
    setSourceType('git');
    onCancel();
  };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <GithubOutlined style={{ fontSize: 20 }} /> 
          <span>添加新仓库</span>
        </div>
      }
      open={visible}
      onCancel={handleCancel}
      footer={null}
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{ sourceType: 'git' }}
      >
        <Form.Item
          name="projectName"
          label="项目名称"
          rules={[{ required: true, message: '请输入项目名称' }]}
        >
          <Input placeholder="请输入项目名称" prefix={<GithubOutlined />} />
        </Form.Item>
        
        <Form.Item
          name="sourceType"
          label="源类型"
        >
          <Radio.Group onChange={handleSourceTypeChange} value={sourceType}>
            <Radio value="git">Git仓库</Radio>
            <Radio value="zip">ZIP文件</Radio>
          </Radio.Group>
        </Form.Item>
        
        <Divider style={{ margin: '12px 0' }} />
        
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ 
            opacity: sourceType === 'git' ? 1 : 0,
            height: sourceType === 'git' ? 'auto' : 0
          }}
          transition={{ duration: 0.3 }}
          style={{ overflow: 'hidden' }}
        >
          {sourceType === 'git' && (
            <>
              <Form.Item
                name="projectUrl"
                label="项目URL"
                rules={[
                  { required: true, message: '请输入项目URL' },
                  { type: 'url', message: '请输入有效的URL地址' }
                ]}
              >
                <Input placeholder="请输入项目URL，例如: https://github.com/username/repo" prefix={<LinkOutlined />} />
              </Form.Item>

              <Form.Item
                name="branch"
                label="分支"
              >
                <Input placeholder="请输入分支名称，默认为master" />
              </Form.Item>
              
              <Form.Item
                name="userName"
                label="用户名"
              >
                <Input placeholder="请输入Git仓库用户名（如需要）" />
              </Form.Item>
              
              <Form.Item
                name="password"
                label="密码"
              >
                <Input.Password placeholder="请输入Git仓库密码（如需要）" />
              </Form.Item>
            </>
          )}
        </motion.div>
        
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ 
            opacity: sourceType === 'zip' ? 1 : 0,
            height: sourceType === 'zip' ? 'auto' : 0
          }}
          transition={{ duration: 0.3 }}
          style={{ overflow: 'hidden' }}
        >
          {sourceType === 'zip' && (
            <>
              <Form.Item
                name="userName"
                label="用户名"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input placeholder="请输入用户名" />
              </Form.Item>

              <Form.Item
                label="ZIP文件"
                required
              >
                <Upload {...uploadProps} accept=".zip">
                  <Button icon={<UploadOutlined />}>选择ZIP文件</Button>
                </Upload>
              </Form.Item>
            </>
          )}
        </motion.div>
        
        <Divider style={{ margin: '12px 0' }} />
        
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={handleCancel}>
              取消
            </Button>
            <Button 
              type="primary" 
              onClick={handleSubmit} 
              loading={loading}
            >
              添加仓库
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AddRepoModal; 