<template>
  <div class="home">
    <h1>客户首页</h1>
    <p>欢迎进入银行借贷系统 - 客户工作台</p>
    <el-card class="info-card">
      <p><strong>姓名：</strong>{{ userStore.userInfo?.realName }}</p>
      <p><strong>账号：</strong>{{ userStore.userInfo?.username }}</p>
      <p><strong>角色：</strong>{{ roleText }}</p>
      <el-button type="danger" @click="logout">退出登录</el-button>
    </el-card>

    <h2 class="section-title">业务办理</h2>
    <el-row :gutter="20" class="entry-list">
      <el-col :span="12" :lg="6">
        <el-card class="entry-card" @click="goTo('/customer/apply')">
          <el-icon :size="40" color="#409EFF"><Edit /></el-icon>
          <div class="entry-title">贷款申请</div>
          <div class="entry-desc">在线提交贷款申请并上传材料</div>
        </el-card>
      </el-col>
      <el-col :span="12" :lg="6">
        <el-card class="entry-card" @click="goTo('/customer/loans')">
          <el-icon :size="40" color="#67C23A"><Document /></el-icon>
          <div class="entry-title">我的贷款</div>
          <div class="entry-desc">查看申请进度与审批详情</div>
        </el-card>
      </el-col>
      <el-col :span="12" :lg="6">
        <el-card class="entry-card" @click="goTo('/customer/contracts')">
          <el-icon :size="40" color="#E6A23C"><DocumentChecked /></el-icon>
          <div class="entry-title">我的合同</div>
          <div class="entry-desc">查看与签署电子合同</div>
        </el-card>
      </el-col>
      <el-col :span="12" :lg="6">
        <el-card class="entry-card" @click="goTo('/customer/repayment')">
          <el-icon :size="40" color="#F56C6C"><Calendar /></el-icon>
          <div class="entry-title">还款计划</div>
          <div class="entry-desc">查看还款计划与明细</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user.js'
import { Edit, Document, DocumentChecked, Calendar } from '@element-plus/icons-vue'

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
