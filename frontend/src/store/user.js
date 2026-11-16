import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '../api/auth.js'
import { getMenu as getMenuApi } from '../api/menu.js'

const HOME_PATHS = {
  customer: '/customer/home',
  manager: '/manager/home',
  approver_level1: '/approver/home',
  approver_level2: '/approver/home',
  approver_committee: '/approver/home',
  disbursement_officer: '/disburser/home',
  admin: '/admin/home',
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const menuList = ref([])

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function setMenuList(list) {
    menuList.value = list || []
  }

  function clear() {
    token.value = ''
    userInfo.value = null
    menuList.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function login(form) {
    const res = await loginApi(form)
    const data = res.data
    if (data.code !== 200) {
      throw new Error(data.msg || '登录失败')
    }
    setToken(data.data.token)
    setUserInfo(data.data.userInfo)
    return data.data
  }

  function logout() {
    clear()
  }

  async function getMenu() {
    const res = await getMenuApi()
    const data = res.data
    if (data.code === 200) {
      setMenuList(data.data)
    }
    return data.data || []
  }

  function getHomePath() {
    const role = userInfo.value?.role
    return HOME_PATHS[role] || '/login'
  }

  return {
    token,
    userInfo,
    menuList,
    isLoggedIn,
    setToken,
    setUserInfo,
    setMenuList,
    clear,
    login,
    logout,
    getMenu,
    getHomePath,
  }
})
