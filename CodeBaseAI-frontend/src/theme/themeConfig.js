// Ant Design主题配置
const themeConfig = {
  token: {
    colorPrimary: '#1677ff',
    borderRadius: 6,
    wireframe: false,
    colorInfo: '#1677ff',
  },
  components: {
    Card: {
      boxShadow: '0 1px 2px -2px rgba(0, 0, 0, 0.16), 0 3px 6px 0 rgba(0, 0, 0, 0.12), 0 5px 12px 4px rgba(0, 0, 0, 0.09)',
      borderRadiusLG: 8,
    },
    Button: {
      borderRadius: 4,
      controlHeight: 36,
    },
    Table: {
      borderRadius: 8,
      fontSize: 14,
    },
    Input: {
      borderRadius: 4,
    },
    Select: {
      borderRadius: 4,
    },
    Tag: {
      borderRadiusSM: 4,
    },
  },
};

export default themeConfig; 