<template>
  <div class="home">
    <h1>管理员首页</h1>
    <p>欢迎进入银行借贷系统 - 管理员工作台</p>
    <el-card class="info-card">
      <p><strong>姓名：</strong>{{ userStore.userInfo?.realName }}</p>
      <p><strong>账号：</strong>{{ userStore.userInfo?.username }}</p>
      <p><strong>角色：</strong>{{ roleText }}</p>
    </el-card>

    <h2 class="section-title">业务办理</h2>
    <el-row :gutter="20" class="entry-list">
      <el-col :span="12" :lg="8">
        <el-card class="entry-card" @click="goTo('/admin/dashboard')">
          <el-icon :size="40" color="#409EFF"><DataLine /></el-icon>
          <div class="entry-title">统计看板</div>
          <div class="entry-desc">查看业务统计报表与数据导出</div>
        </el-card>
      </el-col>
      <el-col :span="12" :lg="8">
        <el-card class="entry-card" @click="goTo('/admin/products')">
          <el-icon :size="40" color="#67C23A"><Goods /></el-icon>
          <div class="entry-title">产品配置</div>
          <div class="entry-desc">管理贷款产品与准入条件</div>
        </el-card>
      </el-col>
      <el-col :span="12" :lg="8">
        <el-card class="entry-card" @click="goTo('/admin/users')">
          <el-icon :size="40" color="#E6A23C"><User /></el-icon>
          <div class="entry-title">用户管理</div>
          <div class="entry-desc">管理系统用户与角色分配</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user.js'
import { DataLine, Goods, User } from '@element-plus/icons-vue'

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
