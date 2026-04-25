import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api/product'

export const useProductStore = defineStore('product', () => {
  // 产品列表
  const products = ref([])
  const featuredProducts = ref([])
  const categories = ref([])
  const currentProduct = ref(null)
  const isLoading = ref(false)

  // 加载产品列表
  const loadProducts = async (params = {}) => {
    try {
      isLoading.value = true
      const response = await productApi.getProducts(params)
      products.value = response
    } catch (error) {
      console.error('加载产品列表失败:', error)
      // 如果API未准备好，使用模拟数据
      products.value = [
        {
          id: 1,
          name: '智能手表 Pro Max 2026',
          description: '最新款智能手表，支持健康监测、GPS定位、移动支付等功能',
          price: 3299,
          originalPrice: 3999,
          image: 'https://via.placeholder.com/300x300/409EFF/fff?text=Product1',
          images: [
            'https://via.placeholder.com/600x600/409EFF/fff?text=Product1-1',
            'https://via.placeholder.com/600x600/409EFF/fff?text=Product1-2',
            'https://via.placeholder.com/600x600/409EFF/fff?text=Product1-3'
          ],
          category: '电子产品',
          stock: 150,
          sales: 1200,
          rating: 4.8,
          reviews: 356,
          specifications: [
            { name: '屏幕尺寸', value: '1.9英寸' },
            { name: '分辨率', value: '454x454像素' },
            { name: '电池续航', value: '18小时' },
            { name: '防水等级', value: 'IP68' },
            { name: '操作系统', value: 'WatchOS 10' }
          ],
          promotions: [
            { id: 1, text: '满3000减300', type: 'danger' }
          ]
        },
        {
          id: 2,
          name: '无线耳机 Pro',
          description: '降噪无线耳机，高保真音质，30小时续航',
          price: 899,
          originalPrice: 999,
          image: 'https://via.placeholder.com/300x300/67C23A/fff?text=Product2',
          images: [
            'https://via.placeholder.com/600x600/67C23A/fff?text=Product2-1',
            'https://via.placeholder.com/600x600/67C23A/fff?text=Product2-2'
          ],
          category: '电子产品',
          stock: 200,
          sales: 850,
          rating: 4.6,
          reviews: 210,
          specifications: [
            { name: '驱动单元', value: '40mm' },
            { name: '频响范围', value: '20Hz-20kHz' },
            { name: '蓝牙版本', value: '5.3' },
            { name: '续航时间', value: '30小时' },
            { name: '充电时间', value: '2小时' }
          ],
          promotions: []
        },
        {
          id: 3,
          name: '笔记本电脑',
          description: '高性能笔记本电脑，适合办公和游戏',
          price: 5999,
          originalPrice: 6999,
          image: 'https://via.placeholder.com/300x300/E6A23C/fff?text=Product3',
          images: [
            'https://via.placeholder.com/600x600/E6A23C/fff?text=Product3-1',
            'https://via.placeholder.com/600x600/E6A23C/fff?text=Product3-2',
            'https://via.placeholder.com/600x600/E6A23C/fff?text=Product3-3'
          ],
          category: '电脑办公',
          stock: 0,
          sales: 450,
          rating: 4.7,
          reviews: 89,
          specifications: [
            { name: '处理器', value: 'Intel Core i7' },
            { name: '内存', value: '16GB' },
            { name: '存储', value: '512GB SSD' },
            { name: '显卡', value: 'NVIDIA RTX 4060' },
            { name: '屏幕', value: '15.6英寸 2K' }
          ],
          promotions: [
            { id: 2, text: '赠送原装鼠标', type: 'success' }
          ]
        }
      ]
    } finally {
      isLoading.value = false
    }
  }

  // 加载推荐产品
  const loadFeaturedProducts = async () => {
    try {
      const response = await productApi.getFeaturedProducts()
      featuredProducts.value = response
    } catch (error) {
      console.error('加载推荐产品失败:', error)
      // 如果API未准备好，使用模拟数据
      featuredProducts.value = [
        {
          id: 4,
          name: '运动手环',
          description: '健康运动手环，支持多种运动模式',
          price: 299,
          originalPrice: 399,
          image: 'https://via.placeholder.com/300x300/E6A23C/fff?text=Product4',
          category: '运动健康',
          stock: 300,
          sales: 1200,
          rating: 4.5
        },
        {
          id: 5,
          name: '蓝牙音箱',
          description: '便携式蓝牙音箱，360度环绕音效',
          price: 499,
          originalPrice: 599,
          image: 'https://via.placeholder.com/300x300/F56C6C/fff?text=Product5',
          category: '影音娱乐',
          stock: 150,
          sales: 800,
          rating: 4.7
        },
        {
          id: 6,
          name: '智能摄像头',
          description: '家用智能摄像头，支持AI人形检测',
          price: 399,
          originalPrice: 499,
          image: 'https://via.placeholder.com/300x300/909399/fff?text=Product6',
          category: '智能家居',
          stock: 200,
          sales: 600,
          rating: 4.4
        }
      ]
    }
  }

  // 加载产品分类
  const loadCategories = async () => {
    try {
      const response = await productApi.getCategories()
      categories.value = response
    } catch (error) {
      console.error('加载分类失败:', error)
      // 如果API未准备好，使用模拟数据
      categories.value = [
        { id: 1, name: '电子产品', count: 45 },
        { id: 2, name: '电脑办公', count: 32 },
        { id: 3, name: '家居生活', count: 28 },
        { id: 4, name: '运动户外', count: 23 },
        { id: 5, name: '美妆护肤', count: 19 }
      ]
    }
  }

  // 获取单个产品详情
  const getProductById = async (id) => {
    try {
      isLoading.value = true
      const response = await productApi.getProductById(id)
      currentProduct.value = response
      return currentProduct.value
    } catch (error) {
      console.error('获取产品详情失败:', error)
      // 如果API未准备好，从现有数据中查找或模拟
      const product = products.value.find(p => p.id === parseInt(id))
      if (product) {
        currentProduct.value = product
      } else {
        // 如果找不到，模拟一个
        currentProduct.value = {
          id: parseInt(id),
          name: `产品 ${id}`,
          description: '这是一个产品描述',
          price: 999,
          originalPrice: 1299,
          image: 'https://via.placeholder.com/300x300/909399/fff?text=Product',
          images: [
            'https://via.placeholder.com/600x600/909399/fff?text=Product-1',
            'https://via.placeholder.com/600x600/909399/fff?text=Product-2'
          ],
          category: '电子产品',
          stock: 100,
          sales: 500,
          rating: 4.5,
          reviews: 120,
          specifications: [
            { name: '品牌', value: '示例品牌' },
            { name: '型号', value: '示例型号' }
          ],
          promotions: []
        }
      }
      return currentProduct.value
    } finally {
      isLoading.value = false
    }
  }

  // 搜索产品
  const searchProducts = async (keyword) => {
    try {
      isLoading.value = true
      const response = await productApi.searchProducts(keyword)
      return response
    } catch (error) {
      console.error('搜索产品失败:', error)
      // 如果API未准备好，使用本地过滤
      return products.value.filter(product =>
        product.name.toLowerCase().includes(keyword.toLowerCase()) ||
        product.description.toLowerCase().includes(keyword.toLowerCase())
      )
    } finally {
      isLoading.value = false
    }
  }

  return {
    products,
    featuredProducts,
    categories,
    currentProduct,
    isLoading,
    loadProducts,
    loadFeaturedProducts,
    loadCategories,
    getProductById,
    searchProducts
  }
})