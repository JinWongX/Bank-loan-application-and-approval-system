<template>
  <el-header class="navbar" v-if="userStore.isLoggedIn">
    <div class="navbar-brand">银行借贷系统</div>
    <el-menu
      :default-active="activePath"
      class="navbar-menu"
      mode="horizontal"
      router
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      :ellipsis="true"
    >
      <el-menu-item
        v-for="menu in userStore.menuList"
        :key="menu.name"
        :index="menu.path"
      >
        {{ menu.title }}
      </el-menu-item>
    </el-menu>
    <div class="navbar-user">
      <span class="user-name">{{ userStore.userInfo?.realName }}</span>
      <el-button type="danger" size="small" @click="logout">退出</el-button>
    </div>
  </el-header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user.js'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const activePath = computed(() => route.path)

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
  background-color: #304156;
  padding: 0 20px;
  height: 60px;
  box-sizing: border-box;
}

.navbar-brand {
  flex-shrink: 0;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  margin-right: 20px;
}

.navbar-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
}

.navbar-user {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #bfcbd9;
  white-space: nowrap;
  margin-left: 20px;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 10px;
  }

  .navbar-brand {
    font-size: 16px;
    margin-right: 10px;
  }

  .navbar-user {
    margin-left: 10px;
    gap: 8px;
  }

  .user-name {
    display: none;
  }
}
</style>
