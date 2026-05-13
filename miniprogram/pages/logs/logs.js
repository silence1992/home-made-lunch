const app = getApp()

Page({
  data: {
    logs: [],
    date: '',
    loading: true
  },

  onLoad(options) {
    if (options.date) {
      this.setData({ date: options.date })
      this.loadLogsByDate(options.date)
    } else {
      this.loadRecentLogs()
    }
  },

  loadLogsByDate(date) {
    const that = this
    app.request({
      url: '/menu/logs?date=' + date,
      success(data) {
        that.setData({ logs: data || [], loading: false })
      },
      fail() {
        that.setData({ loading: false })
      }
    })
  },

  loadRecentLogs() {
    const that = this
    app.request({
      url: '/home/logs?limit=50',
      success(data) {
        that.setData({ logs: data || [], loading: false })
      },
      fail() {
        that.setData({ loading: false })
      }
    })
  },

  getModuleName(module) {
    const map = {
      'USER': '用户', 'FAMILY': '家庭', 'RECIPE': '菜谱',
      'TAG': '标签', 'MENU': '食谱', 'RATING': '评价'
    }
    return map[module] || module
  },

  getActionName(action) {
    const map = {
      'CREATE': '创建', 'UPDATE': '更新', 'DELETE': '删除',
      'JOIN': '加入', 'LEAVE': '离开', 'KICK': '移出',
      'INVITE': '邀请', 'ADD': '添加', 'REMOVE': '移除'
    }
    return map[action] || action
  }
})
