<template>
  <div class="home">
    <h1>审批人首页</h1>
    <p>欢迎进入银行借贷系统 - 审批人工作台</p>
    <el-card class="info-card">
      <p><strong>姓名：</strong>{{ userStore.userInfo?.realName }}</p>
      <p><strong>账号：</strong>{{ userStore.userInfo?.username }}</p>
      <p><strong>角色：</strong>{{ roleText }}</p>
      <el-button type="danger" @click="logout">退出登录</el-button>
    </el-card>

    <h2 class="section-title">业务办理</h2>
    <el-row :gutter="20" class="entry-list">
      <el-col :span="24" :lg="8">
        <el-card class="entry-card" @click="goTo('/approver/pending')">
          <el-icon :size="40" color="#409EFF"><Timer /></el-icon>
          <div class="entry-title">待审批申请</div>
          <div class="entry-desc">查看并处理当前层级的待审批申请</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user.js'
import { Timer } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

function logout() {
  userStore.logout()
  router.push('/login')
}

const roleText = computed(() => {
  const map = {
    customer: '客户',
    manager: '客户经理',
    approver_level1: '一级审批人',
    approver_level2: '二级审批人',
    approver_committee: '审批委员会',
    disbursement_officer: '放款员',
    admin: '管理员',
  }
  return map[userStore.userInfo?.role] || userStore.userInfo?.role
})

function goTo(path) {
  router.push(path)
}
</script>

<style scoped>
.home {
  padding: 20px;
}
.info-card {
  margin-top: 20px;
  max-width: 400px;
}
.section-title {
  margin-top: 30px;
  margin-bottom: 16px;
  font-size: 18px;
}
.entry-list {
  margin-top: 10px;
}
.entry-card {
  margin-bottom: 20px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.entry-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}
.entry-title {
  margin-top: 12px;
  font-size: 16px;
  font-weight: bold;
}
.entry-desc {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}
</style>
