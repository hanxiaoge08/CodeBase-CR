import axios from 'axios';

const baseURL = 'http://localhost:8888/api';
const api = axios.create({
  baseURL,
  timeout: 30000,
});

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 直接返回响应数据
    return response.data;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 任务API接口
export const TaskApi = {
  // 从Git创建任务
  createFromGit: (params) => {
    return api.post('/task/create/git', params);
  },
  
  // 从Zip创建任务
  createFromZip: (formData) => {
    return api.post('/task/create/zip', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },
  
  // 分页获取任务列表
  getTasksByPage: (params) => {
    return api.post('/task/listPage', params);
  },
  
  // 获取任务详情
  getTaskDetail: (taskId) => {
    return api.get(`/task/detail?taskId=${taskId}`);
  },
  
  // 更新任务
  updateTask: (task) => {
    return api.put('/task/update', task);
  },
  
  // 删除任务
  deleteTask: (taskId) => {
    return api.get(`/task/delete?taskId=${taskId}`);
  },
  
  // 获取目录树
  getCatalogueTree: (taskId) => {
    return api.get(`/task/catalogue/tree?taskId=${taskId}`);
  }
};

export default TaskApi; 