const app = getApp()

Page({
  data: {
    userInfo: null,
    showEditModal: false,
    editNickname: '',
    editAvatarUrl: ''
  },

  onLoad() {},

  onShow() {
    this.loadUserInfo()
  },

  loadUserInfo() {
    const that = this
    app.request({
      url: '/user/info',
      success(data) {
        that.setData({ userInfo: data })
        app.globalData.userInfo = data
        wx.setStorageSync('userInfo', data)
      }
    })
  },

  showEdit() {
    this.setData({
      showEditModal: true,
      editNickname: this.data.userInfo.nickname,
      editAvatarUrl: this.data.userInfo.avatarUrl
    })
  },

  hideEdit() {
    this.setData({ showEditModal: false })
  },

  onNicknameInput(e) {
    this.setData({ editNickname: e.detail.value })
  },

  saveProfile() {
    const that = this
    app.request({
      url: '/user/update',
      method: 'PUT',
      data: {
        nickname: this.data.editNickname,
        avatarUrl: this.data.editAvatarUrl
      },
      success(data) {
        wx.showToast({ title: '更新成功~', icon: 'success' })
        that.setData({ showEditModal: false, userInfo: data })
        app.globalData.userInfo = data
        wx.setStorageSync('userInfo', data)
      }
    })
  },

  // 退出登录
  logout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success(res) {
        if (res.confirm) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          app.globalData.token = null
          app.globalData.userInfo = null
          wx.reLaunch({ url: '/pages/home/home' })
        }
      }
    })
  },

  // 查看操作记录
  goToLogs() {
    wx.navigateTo({ url: '/pages/logs/logs' })
  }
})
