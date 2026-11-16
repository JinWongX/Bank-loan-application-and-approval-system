<template>
  <div class="repayment-view">
    <div class="page-header">
      <h2>还款计划</h2>
      <el-button @click="goHome">返回首页</el-button>
    </div>

    <el-card v-loading="loading">
      <el-empty v-if="!loading && loanList.length === 0" description="暂无已放款的贷款" />

      <el-table
        v-if="loanList.length > 0"
        :data="loanList"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="applicationNo" label="申请单号" min-width="170" />
        <el-table-column prop="product.name" label="贷款产品" min-width="150" />
        <el-table-column prop="amount" label="贷款金额" min-width="140">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="termMonths" label="期限" min-width="100">
          <template #default="{ row }">
            {{ row.termMonths }} 个月
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag type="success" size="small">已放款</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="viewSchedule(row.id)">
              查看还款计划
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="selectedLoan || scheduleList.length > 0" class="schedule-card">
      <template #header>
        <span>还款计划明细（{{ selectedLoan?.applicationNo || '' }}）</span>
      </template>

      <el-table :data="scheduleList" stripe style="width: 100%">
        <el-table-column prop="period" label="期数" min-width="80" />
        <el-table-column prop="dueDate" label="还款日" min-width="120" />
        <el-table-column prop="principal" label="应还本金" min-width="140">
          <template #default="{ row }">
            {{ formatAmount(row.principal) }}
          </template>
        </el-table-column>
        <el-table-column prop="interest" label="应还利息" min-width="140">
          <template #default="{ row }">
            {{ formatAmount(row.interest) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="应还总额" min-width="140">
          <template #default="{ row }">
            {{ formatAmount(row.totalAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="remainingPrincipal" label="剩余本金" min-width="140">
          <template #default="{ row }">
            {{ formatAmount(row.remainingPrincipal) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PAID' ? 'success' : 'info'" size="small">
              {{ row.status === 'PAID' ? '已还款' : '待还款' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyApplications } from '../../api/application.js'
import { getRepaymentSchedules } from '../../api/repaymentSchedule.js'

const router = useRouter()
const loading = ref(false)
const loanList = ref([])
const selectedLoan = ref(null)
const scheduleList = ref([])

function formatAmount(amount) {
  if (amount === undefined || amount === null) return '-'
  return '¥' + Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function goHome() {
  router.push('/customer/home')
}

function handleRowClick(row) {
  viewSchedule(row.id)
}

async function viewSchedule(applicationId) {
  selectedLoan.value = loanList.value.find((item) => item.id === Number(applicationId)) || null
  try {
    const res = await getRepaymentSchedules(applicationId)
    if (res.data.code === 200) {
      scheduleList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '获取还款计划失败')
    }
  } catch (error) {
    ElMessage.error('获取还款计划失败')
  }
}

async function loadLoans() {
  loading.value = true
  try {
    const res = await getMyApplications()
    if (res.data.code === 200) {
      const list = res.data.data || []
      loanList.value = list.filter((item) => item.status === 'DISBURSED')
      if (loanList.value.length > 0) {
        await viewSchedule(loanList.value[0].id)
      }
    } else {
      ElMessage.error(res.data.msg || '获取贷款列表失败')
    }
  } catch (error) {
    ElMessage.error('获取贷款列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadLoans)
</script>

<style scoped>
.repayment-view {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
}
.schedule-card {
  margin-top: 20px;
}
</style>
