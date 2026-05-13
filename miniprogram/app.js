/**
 * 家庭便当 - 工作日午餐沟通小程序
 */
App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://localhost:8080/api'  // 开发环境
  },

  onLaunch() {
    // 检查本地存储的登录信息
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
    }
  },

  /**
   * 登录
   */
  login(callback) {
    const that = this
    wx.login({
      success(res) {
        if (res.code) {
          that.request({
            url: '/user/login',
            method: 'POST',
            data: { code: res.code },
            noAuth: true,
            success(result) {
              that.globalData.token = String(result.id)
              that.globalData.userInfo = result
              wx.setStorageSync('token', String(result.id))
              wx.setStorageSync('userInfo', result)
              callback && callback(result)
            }
          })
        }
      }
    })
  },

  /**
   * 统一请求封装
   */
  request(options) {
    const that = this
    const header = {
      'Content-Type': 'application/json'
    }
    if (!options.noAuth && this.globalData.token) {
      header['X-Token'] = this.globalData.token
    }

    wx.request({
      url: this.globalData.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: header,
      success(res) {
        if (res.data.code === 200) {
          options.success && options.success(res.data.data)
        } else if (res.data.code === 401) {
          // 需要重新登录
          that.login(function() {
            // 重试请求
            that.request(options)
          })
        } else {
          wx.showToast({
            title: res.data.message || '操作失败',
            icon: 'none',
            duration: 2000
          })
          options.fail && options.fail(res.data)
        }
      },
      fail(err) {
        wx.showToast({
          title: '网络异常，请稍后再试~',
          icon: 'none'
        })
        options.fail && options.fail(err)
      },
      complete() {
        options.complete && options.complete()
      }
    })
  }
})
