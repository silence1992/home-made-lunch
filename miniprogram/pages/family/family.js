const app = getApp()

Page({
  data: {
    familyInfo: null,
    members: [],
    isAdmin: false,
    hasFamily: false,
    familyName: '',
    showCreateModal: false,
    showInviteModal: false,
    inviteRole: 'DINER',
    inviteLink: null,
    loading: true
  },

  onLoad() {},

  onShow() {
    this.loadFamilyInfo()
  },

  onPullDownRefresh() {
    this.loadFamilyInfo()
    wx.stopPullDownRefresh()
  },

  loadFamilyInfo() {
    const that = this
    app.request({
      url: '/family/info',
      success(data) {
        if (data) {
          that.setData({
            familyInfo: data.family,
            members: data.members,
            isAdmin: data.isAdmin,
            hasFamily: true,
            loading: false
          })
        } else {
          that.setData({ hasFamily: false, loading: false })
        }
      },
      fail() {
        that.setData({ loading: false })
      }
    })
  },

  // 显示创建家庭弹窗
  showCreate() {
    this.setData({ showCreateModal: true })
  },

  hideCreate() {
    this.setData({ showCreateModal: false })
  },

  onFamilyNameInput(e) {
    this.setData({ familyName: e.detail.value })
  },

  // 创建家庭
  createFamily() {
    const that = this
    if (!this.data.familyName.trim()) {
      wx.showToast({ title: '请输入家庭名称~', icon: 'none' })
      return
    }
    app.request({
      url: '/family/create',
      method: 'POST',
      data: { name: this.data.familyName },
      success() {
        wx.showToast({ title: '家庭创建成功~', icon: 'success' })
        that.setData({ showCreateModal: false })
        // 刷新用户信息
        app.request({
          url: '/user/info',
          success(user) {
            app.globalData.userInfo = user
            wx.setStorageSync('userInfo', user)
          }
        })
        that.loadFamilyInfo()
      }
    })
  },

  // 显示邀请弹窗
  showInvite() {
    this.setData({ showInviteModal: true, inviteLink: null })
  },

  hideInvite() {
    this.setData({ showInviteModal: false })
  },

  onRoleChange(e) {
    this.setData({ inviteRole: e.detail.value })
  },

  // 生成邀请链接
  generateInvite() {
    const that = this
    app.request({
      url: '/family/invite',
      method: 'POST',
      data: { role: this.data.inviteRole },
      success(data) {
        that.setData({ inviteLink: data })
      }
    })
  },

  // 复制邀请码
  copyInviteCode() {
    if (this.data.inviteLink) {
      wx.setClipboardData({
        data: this.data.inviteLink.code,
        success() {
          wx.showToast({ title: '邀请码已复制~', icon: 'success' })
        }
      })
    }
  },

  // 踢出成员
  kickMember(e) {
    const userId = e.currentTarget.dataset.userid
    const nickname = e.currentTarget.dataset.nickname
    const that = this
    wx.showModal({
      title: '确认移出',
      content: `确定要将「${nickname}」移出家庭吗？`,
      confirmColor: '#ff6b6b',
      success(res) {
        if (res.confirm) {
          app.request({
            url: '/family/kick',
            method: 'POST',
            data: { userId: userId },
            success() {
              wx.showToast({ title: '已移出~', icon: 'success' })
              that.loadFamilyInfo()
            }
          })
        }
      }
    })
  },

  getRoleName(role) {
    const map = { 'ADMIN': '管理员', 'COOK': '厨师', 'DINER': '就餐者' }
    return map[role] || role
  }
})
