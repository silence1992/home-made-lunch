const app = getApp()

Page({
  data: {
    recipes: [],
    tags: [],
    activeTab: 'recipe', // recipe / tag
    showRecipeModal: false,
    showTagModal: false,
    editingRecipe: null,
    editingTag: null,
    recipeName: '',
    recipeDesc: '',
    selectedTagIds: [],
    tagName: '',
    loading: true
  },

  onLoad() {},

  onShow() {
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData()
    wx.stopPullDownRefresh()
  },

  loadData() {
    this.loadRecipes()
    this.loadTags()
  },

  loadRecipes() {
    const that = this
    app.request({
      url: '/recipe/list',
      success(data) {
        that.setData({ recipes: data || [], loading: false })
      }
    })
  },

  loadTags() {
    const that = this
    app.request({
      url: '/recipe/tags',
      success(data) {
        that.setData({ tags: data || [] })
      }
    })
  },

  switchTab(e) {
    this.setData({ activeTab: e.currentTarget.dataset.tab })
  },

  // ===== 菜谱管理 =====
  showAddRecipe() {
    this.setData({
      showRecipeModal: true,
      editingRecipe: null,
      recipeName: '',
      recipeDesc: '',
      selectedTagIds: []
    })
  },

  showEditRecipe(e) {
    const recipe = e.currentTarget.dataset.recipe
    const tagIds = (recipe.tags || []).map(t => t.id)
    this.setData({
      showRecipeModal: true,
      editingRecipe: recipe,
      recipeName: recipe.name,
      recipeDesc: recipe.description || '',
      selectedTagIds: tagIds
    })
  },

  hideRecipeModal() {
    this.setData({ showRecipeModal: false })
  },

  onRecipeNameInput(e) {
    this.setData({ recipeName: e.detail.value })
  },

  onRecipeDescInput(e) {
    this.setData({ recipeDesc: e.detail.value })
  },

  toggleTag(e) {
    const tagId = e.currentTarget.dataset.tagid
    let ids = this.data.selectedTagIds.slice()
    const idx = ids.indexOf(tagId)
    if (idx >= 0) {
      ids.splice(idx, 1)
    } else {
      ids.push(tagId)
    }
    this.setData({ selectedTagIds: ids })
  },

  saveRecipe() {
    const that = this
    if (!this.data.recipeName.trim()) {
      wx.showToast({ title: '请输入菜谱名称~', icon: 'none' })
      return
    }

    if (this.data.editingRecipe) {
      // 更新
      app.request({
        url: '/recipe/' + this.data.editingRecipe.id,
        method: 'PUT',
        data: {
          name: this.data.recipeName,
          description: this.data.recipeDesc,
          tagIds: this.data.selectedTagIds
        },
        success() {
          wx.showToast({ title: '更新成功~', icon: 'success' })
          that.setData({ showRecipeModal: false })
          that.loadRecipes()
        }
      })
    } else {
      // 创建
      app.request({
        url: '/recipe/create',
        method: 'POST',
        data: {
          name: this.data.recipeName,
          description: this.data.recipeDesc,
          tagIds: this.data.selectedTagIds
        },
        success() {
          wx.showToast({ title: '创建成功~', icon: 'success' })
          that.setData({ showRecipeModal: false })
          that.loadRecipes()
        }
      })
    }
  },

  deleteRecipe(e) {
    const recipe = e.currentTarget.dataset.recipe
    const that = this
    wx.showModal({
      title: '确认删除',
      content: `确定要删除菜谱「${recipe.name}」吗？`,
      confirmColor: '#ff6b6b',
      success(res) {
        if (res.confirm) {
          app.request({
            url: '/recipe/' + recipe.id,
            method: 'DELETE',
            success() {
              wx.showToast({ title: '已删除~', icon: 'success' })
              that.loadRecipes()
            }
          })
        }
      }
    })
  },

  // ===== 标签管理 =====
  showAddTag() {
    this.setData({ showTagModal: true, editingTag: null, tagName: '' })
  },

  showEditTag(e) {
    const tag = e.currentTarget.dataset.tag
    this.setData({ showTagModal: true, editingTag: tag, tagName: tag.name })
  },

  hideTagModal() {
    this.setData({ showTagModal: false })
  },

  onTagNameInput(e) {
    this.setData({ tagName: e.detail.value })
  },

  saveTag() {
    const that = this
    if (!this.data.tagName.trim()) {
      wx.showToast({ title: '请输入标签名称~', icon: 'none' })
      return
    }

    if (this.data.editingTag) {
      app.request({
        url: '/recipe/tag/' + this.data.editingTag.id,
        method: 'PUT',
        data: { name: this.data.tagName },
        success() {
          wx.showToast({ title: '更新成功~', icon: 'success' })
          that.setData({ showTagModal: false })
          that.loadTags()
          that.loadRecipes()
        }
      })
    } else {
      app.request({
        url: '/recipe/tag/create',
        method: 'POST',
        data: { name: this.data.tagName },
        success() {
          wx.showToast({ title: '创建成功~', icon: 'success' })
          that.setData({ showTagModal: false })
          that.loadTags()
        }
      })
    }
  },

  deleteTag(e) {
    const tag = e.currentTarget.dataset.tag
    const that = this
    wx.showModal({
      title: '确认删除',
      content: `确定要删除标签「${tag.name}」吗？`,
      confirmColor: '#ff6b6b',
      success(res) {
        if (res.confirm) {
          app.request({
            url: '/recipe/tag/' + tag.id,
            method: 'DELETE',
            success() {
              wx.showToast({ title: '已删除~', icon: 'success' })
              that.loadTags()
              that.loadRecipes()
            }
          })
        }
      }
    })
  }
})
