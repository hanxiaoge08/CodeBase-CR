import React from 'react';
import { Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';

const PageLoading = ({ tip = '加载中...' }) => {
  const antIcon = <LoadingOutlined style={{ fontSize: 24 }} spin />;

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100%',
        minHeight: '200px',
        padding: '50px 0',
      }}
    >
      <Spin indicator={antIcon} tip={tip} size="large" />
    </div>
  );
};

export default PageLoading; 