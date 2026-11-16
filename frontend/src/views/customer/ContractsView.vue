<template>
  <div class="contracts-view">
    <div class="page-header">
      <h2>我的合同</h2>
      <el-button @click="goHome">返回首页</el-button>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="contractList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="contractNo" label="合同编号" min-width="170" />
        <el-table-column prop="applicationNo" label="申请单号" min-width="160" />
        <el-table-column label="产品名称" min-width="150">
          <template #default="{ row }">
            {{ row.productName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="贷款金额" min-width="130">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column label="期限" min-width="100">
          <template #default="{ row }">
            {{ row.termMonths }} 个月
          </template>
        </el-table-column>
        <el-table-column label="签署状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.signStatus)" size="small">
              {{ statusText(row.signStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.signStatus === 'UNSIGNED'"
              link
              type="primary"
              size="small"
              @click="goSign(row.applicationId)"
            >
              去签署
            </el-button>
            <el-button v-else link type="success" size="small" @click="goSign(row.applicationId)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && contractList.length === 0" description="暂无合同记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyContracts } from '../../api/contract.js'

const router = useRouter()
const loading = ref(false)
const contractList = ref([])

const STATUS_MAP = {
  UNSIGNED: '待签署',
  SIGNED: '已签署',
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
    case 'UNSIGNED':
      return 'warning'
    case 'SIGNED':
      return 'success'
    default:
      return 'info'
  }
}

function goHome() {
  router.push('/customer/home')
}

function goSign(applicationId) {
  router.push(`/customer/contracts/${applicationId}/sign`)
}

async function loadContracts() {
  loading.value = true
  try {
    const res = await getMyContracts()
    if (res.data.code === 200) {
      contractList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '获取合同列表失败')
    }
  } catch (error) {
    ElMessage.error('获取合同列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadContracts)
</script>

<style scoped>
.contracts-view {
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
