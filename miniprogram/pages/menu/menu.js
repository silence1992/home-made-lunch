const app = getApp()

Page({
  data: {
    weeklyMenu: [],
    recipes: [],
    loading: true,
    showRecipePicker: false,
    selectedDate: '',
    selectedDateLabel: '',
    currentWeekOffset: 0
  },

  onLoad() {
    this.loadWeeklyMenu()
    this.loadRecipes()
  },

  onPullDownRefresh() {
    this.loadWeeklyMenu()
    wx.stopPullDownRefresh()
  },

  loadWeeklyMenu() {
    const that = this
    let url = '/menu/weekly'
    if (this.data.currentWeekOffset !== 0) {
      const date = new Date()
      date.setDate(date.getDate() + this.data.currentWeekOffset * 7)
      const dateStr = date.toISOString().split('T')[0]
      url += '?date=' + dateStr
    }
    app.request({
      url: url,
      success(data) {
        that.setData({ weeklyMenu: data || [], loading: false })
      }
    })
  },

  loadRecipes() {
    const that = this
    app.request({
      url: '/recipe/list',
      success(data) {
        that.setData({ recipes: data || [] })
      }
    })
  },

  prevWeek() {
    this.setData({ currentWeekOffset: this.data.currentWeekOffset - 1 })
    this.loadWeeklyMenu()
  },

  nextWeek() {
    this.setData({ currentWeekOffset: this.data.currentWeekOffset + 1 })
    this.loadWeeklyMenu()
  },

  thisWeek() {
    this.setData({ currentWeekOffset: 0 })
    this.loadWeeklyMenu()
  },

  // 显示菜谱选择器
  showPicker(e) {
    const date = e.currentTarget.dataset.date
    const label = e.currentTarget.dataset.label
    this.setData({
      showRecipePicker: true,
      selectedDate: date,
      selectedDateLabel: label
    })
  },

  hidePicker() {
    this.setData({ showRecipePicker: false })
  },

  // 添加菜谱到某天
  addRecipe(e) {
    const recipeId = e.currentTarget.dataset.recipeid
    const that = this
    app.request({
      url: '/menu/add',
      method: 'POST',
      data: {
        date: this.data.selectedDate,
        recipeId: recipeId
      },
      success() {
        wx.showToast({ title: '添加成功~', icon: 'success' })
        that.setData({ showRecipePicker: false })
        that.loadWeeklyMenu()
      }
    })
  },

  // 移除某天的菜谱
  removeRecipe(e) {
    const date = e.currentTarget.dataset.date
    const recipeId = e.currentTarget.dataset.recipeid
    const recipeName = e.currentTarget.dataset.recipename
    const that = this
    wx.showModal({
      title: '确认移除',
      content: `确定要从当天移除「${recipeName}」吗？`,
      confirmColor: '#ff6b6b',
      success(res) {
        if (res.confirm) {
          app.request({
            url: '/menu/remove',
            method: 'POST',
            data: { date: date, recipeId: recipeId },
            success() {
              wx.showToast({ title: '已移除~', icon: 'success' })
              that.loadWeeklyMenu()
            }
          })
        }
      }
    })
  },

  // 查看某天操作记录
  viewLogs(e) {
    const date = e.currentTarget.dataset.date
    wx.navigateTo({ url: '/pages/logs/logs?date=' + date })
  }
})
