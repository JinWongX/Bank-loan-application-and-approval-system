<template>
  <div class="app">
    <NavBar />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from './store/user.js'
import NavBar from './components/NavBar.vue'

const userStore = useUserStore()

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.getMenu()
  }
})
</script>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  width: 100%;
  min-width: 0;
  overflow: auto;
  background-color: #f5f7fa;
}
</style>
