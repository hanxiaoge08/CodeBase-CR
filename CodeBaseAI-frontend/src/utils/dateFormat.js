import dayjs from 'dayjs';

// 格式化日期时间
export const formatDateTime = (dateTime, format = 'YYYY-MM-DD HH:mm:ss') => {
  if (!dateTime) return '-';
  return dayjs(dateTime).format(format);
};

// 获取任务状态对应的颜色
export const getStatusColor = (status) => {
  switch (status) {
    case '进行中':
      return 'processing';
    case '已完成':
      return 'success';
    case '处理失败':
      return 'error';
    default:
      return 'default';
  }
};

export default {
  formatDateTime,
  getStatusColor,
}; 