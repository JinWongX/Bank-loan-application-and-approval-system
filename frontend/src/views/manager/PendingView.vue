<template>
  <div class="pending-view">
    <div class="page-header">
      <h2>待尽调申请</h2>
      <el-button @click="goHome">返回首页</el-button>
    </div>

    <el-card v-loading="loading">
      <el-table :data="pendingList" stripe style="width: 100%">
        <el-table-column prop="applicationNo" label="申请单号" min-width="160" />
        <el-table-column prop="customerName" label="客户姓名" min-width="120" />
        <el-table-column prop="product.name" label="贷款产品" min-width="160" />
        <el-table-column prop="amount" label="申请金额" min-width="140">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="creditGrade" label="信用等级" min-width="100">
          <template #default="{ row }">
            <el-tag :type="gradeType(row.creditGrade)" size="small">
              {{ row.creditGrade || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="goDueDiligence(row.id)">
              尽调
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && pendingList.length === 0" description="暂无待尽调申请" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPendingApplications } from '../../api/dueDiligence.js'

const router = useRouter()
const loading = ref(false)
const pendingList = ref([])

function formatAmount(amount) {
  if (amount === undefined || amount === null) return '-'
  return '¥' + Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function gradeType(grade) {
  switch (grade) {
    case 'A':
      return 'success'
    case 'B':
      return 'primary'
    case 'C':
      return 'warning'
    case 'D':
      return 'danger'
    default:
      return 'info'
  }
}

function goHome() {
  router.push('/manager/home')
}

function goDueDiligence(id) {
  router.push(`/manager/due-diligence/${id}`)
}

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingApplications()
    if (res.data.code === 200) {
      pendingList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '获取待尽调列表失败')
    }
  } catch (error) {
    ElMessage.error('获取待尽调列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadPending)
</script>

<style scoped>
.pending-view {
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
