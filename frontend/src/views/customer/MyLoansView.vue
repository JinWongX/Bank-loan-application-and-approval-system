<template>
  <div class="my-loans-view">
    <div class="page-header">
      <h2>我的贷款</h2>
      <el-button @click="goHome">返回首页</el-button>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="applicationList"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="applicationNo" label="申请单号" min-width="160" />
        <el-table-column label="产品名称" min-width="160">
          <template #default="{ row }">
            {{ row.product?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="申请金额" min-width="120">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column label="期限" min-width="100">
          <template #default="{ row }">
            {{ row.termMonths }} 个月
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="140">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="viewDetail(row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && applicationList.length === 0" description="暂无贷款申请记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyApplications } from '../../api/application.js'

const router = useRouter()
const loading = ref(false)
const applicationList = ref([])

const STATUS_MAP = {
  SUBMITTED: '已提交',
  CREDIT_EVALUATED: '信用评估完成',
  DUE_DILIGENCE: '尽调中',
  DUE_DILIGENCE_COMPLETED: '尽调完成',
  PENDING_APPROVAL: '审批中',
  APPROVED: '审批通过',
  REJECTED: '已驳回',
  CONTRACT_GENERATED: '合同已生成',
  SIGNED: '已签署',
  DISBURSED: '已放款',
}

function formatAmount(amount) {
  return '¥' + Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function statusText(status) {
  return STATUS_MAP[status] || status
}

function statusType(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'info'
    case 'CREDIT_EVALUATED':
    case 'DUE_DILIGENCE':
    case 'DUE_DILIGENCE_COMPLETED':
    case 'PENDING_APPROVAL':
      return 'warning'
    case 'APPROVED':
    case 'CONTRACT_GENERATED':
    case 'SIGNED':
    case 'DISBURSED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

function goHome() {
  router.push('/customer/home')
}

function viewDetail(id) {
  router.push(`/customer/loans/${id}`)
}

function handleRowClick(row) {
  viewDetail(row.id)
}

async function loadApplications() {
  loading.value = true
  try {
    const res = await getMyApplications()
    if (res.data.code === 200) {
      applicationList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '获取申请列表失败')
    }
  } catch (error) {
    ElMessage.error('获取申请列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadApplications)
</script>

<style scoped>
.my-loans-view {
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
</style>
