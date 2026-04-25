import api from './index'

export const productApi = {
  // 获取产品列表 - 对应后端 GET /products?page=1&size=10&categoryId=xxx&keyword=xxx
  getProducts(params = {}) {
    // 后端分页参数: page, size, categoryId, keyword
    const queryParams = {
      page: params.page || 1,
      size: params.size || 10
    }
    if (params.categoryId) queryParams.categoryId = params.categoryId
    if (params.keyword) queryParams.keyword = params.keyword
    return api.get('/products', { params: queryParams })
  },

  // 获取产品详情 - 对应后端 GET /products/{id}
  getProductById(id) {
    return api.get(`/products/${id}`)
  },

  // 搜索产品 - 对应后端 GET /products/search?keyword=xxx
  searchProducts(keyword, params = {}) {
    // 后端参数名是 keyword，不是 q
    return api.get('/products/search', {
      params: { keyword, ...params }
    })
  },

  // 获取推荐产品 - 对应后端 GET /products/featured
  getFeaturedProducts() {
    return api.get('/products/featured')
  }
}