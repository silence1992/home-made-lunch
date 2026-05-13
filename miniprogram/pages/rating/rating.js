const app = getApp()

Page({
  data: {
    date: '',
    rating: 5,
    comment: '',
    existingRating: null,
    todayMenu: [],
    loading: true
  },

  onLoad(options) {
    const today = new Date().toISOString().split('T')[0]
    this.setData({ date: options.date || today })
    this.loadData()
  },

  loadData() {
    this.loadMyRating()
    this.loadTodayMenu()
  },

  loadMyRating() {
    const that = this
    app.request({
      url: '/rating/my?date=' + this.data.date,
      success(data) {
        if (data) {
          that.setData({
            existingRating: data,
            rating: data.rating,
            comment: data.comment || '',
            loading: false
          })
        } else {
          that.setData({ loading: false })
        }
      }
    })
  },

  loadTodayMenu() {
    const that = this
    app.request({
      url: '/menu/day?date=' + this.data.date,
      success(data) {
        that.setData({ todayMenu: data || [] })
      }
    })
  },

  // 选择星级
  selectStar(e) {
    this.setData({ rating: e.currentTarget.dataset.star })
  },

  onCommentInput(e) {
    this.setData({ comment: e.detail.value })
  },

  // 提交评价
  submitRating() {
    const that = this
    app.request({
      url: '/rating/submit',
      method: 'POST',
      data: {
        date: this.data.date,
        rating: this.data.rating,
        comment: this.data.comment
      },
      success(data) {
        wx.showToast({ title: '评价成功~ 💝', icon: 'success' })
        that.setData({ existingRating: data })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      }
    })
  }
})
