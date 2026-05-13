const app = getApp()

Page({
  data: {
    inviteInfo: null,
    code: '',
    loading: true,
    error: ''
  },

  onLoad(options) {
    if (options.code) {
      this.setData({ code: options.code })
      this.loadInviteInfo(options.code)
    } else {
      this.setData({ loading: false })
    }
  },

  onCodeInput(e) {
    this.setData({ code: e.detail.value })
  },

  queryInvite() {
    if (!this.data.code.trim()) {
      wx.showToast({ title: '请输入邀请码~', icon: 'none' })
      return
    }
    this.loadInviteInfo(this.data.code.trim())
  },

  loadInviteInfo(code) {
    const that = this
    app.request({
      url: '/family/invite/info/' + code,
      noAuth: true,
      success(data) {
        that.setData({ inviteInfo: data, loading: false })
      },
      fail(err) {
        that.setData({ loading: false, error: err.message || '邀请码无效' })
      }
    })
  },

  acceptInvite() {
    const that = this
    if (!app.globalData.token) {
      app.login(function() {
        that.doAccept()
      })
    } else {
      this.doAccept()
    }
  },

  doAccept() {
    app.request({
      url: '/family/invite/accept',
      method: 'POST',
      data: { code: this.data.code },
      success() {
        wx.showToast({ title: '加入成功~ 🎉', icon: 'success' })
        // 刷新用户信息
        app.request({
          url: '/user/info',
          success(user) {
            app.globalData.userInfo = user
            wx.setStorageSync('userInfo', user)
          }
        })
        setTimeout(() => {
          wx.switchTab({ url: '/pages/home/home' })
        }, 1500)
      }
    })
  }
})
