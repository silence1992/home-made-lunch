const app = getApp()

Page({
  data: {
    greeting: null,
    loading: true,
    userInfo: null
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    if (app.globalData.token) {
      this.loadGreeting()
    }
  },

  onPullDownRefresh() {
    this.loadGreeting()
    wx.stopPullDownRefresh()
  },

  checkLogin() {
    if (app.globalData.token) {
      this.loadGreeting()
    } else {
      app.login((user) => {
        this.setData({ userInfo: user })
        this.loadGreeting()
      })
    }
  },

  loadGreeting() {
    const that = this
    app.request({
      url: '/home/greeting',
      success(data) {
        that.setData({ greeting: data, loading: false })
      },
      fail() {
        that.setData({ loading: false })
      }
    })
  },

  // 跳转到食谱管理
  goToMenu() {
    wx.navigateTo({ url: '/pages/menu/menu' })
  },

  // 跳转到评价
  goToRating() {
    wx.navigateTo({ url: '/pages/rating/rating' })
  },

  // 跳转到创建家庭
  goToFamily() {
    wx.switchTab({ url: '/pages/family/family' })
  },

  // 跳转到操作记录
  goToLogs() {
    wx.navigateTo({ url: '/pages/logs/logs' })
  }
})
