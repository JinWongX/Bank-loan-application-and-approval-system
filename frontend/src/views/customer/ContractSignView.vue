<template>
  <div class="contract-sign-view">
    <div class="page-header">
      <h2>合同签署</h2>
      <el-button @click="goBack">返回</el-button>
    </div>

    <el-card v-loading="loading" class="contract-card">
      <template #header>
        <div class="card-header">
          <span>合同信息</span>
          <el-tag :type="statusType(contract.signStatus)" size="small">
            {{ statusText(contract.signStatus) }}
          </el-tag>
        </div>
      </template>

      <div class="contract-meta">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="合同编号">{{ contract.contractNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请单号">{{ contract.applicationNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="借款人">{{ contract.customerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ contract.idCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ contract.productName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="贷款金额">{{ formatAmount(contract.amount) }}</el-descriptions-item>
          <el-descriptions-item label="贷款期限">{{ contract.termMonths }} 个月</el-descriptions-item>
          <el-descriptions-item label="年利率">{{ formatRate(contract.interestRate) }}</el-descriptions-item>
          <el-descriptions-item label="还款方式">{{ contract.repaymentMethod || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="contract-content">
        <h3>合同文本</h3>
        <pre>{{ contract.contractContent || '暂无合同内容' }}</pre>
      </div>

      <div v-if="contract.signStatus === 'UNSIGNED'" class="sign-action">
        <el-checkbox v-model="agreed" label="我已阅读并同意以上合同内容" size="large" />
        <el-button
          type="primary"
          size="large"
          :disabled="!agreed || signing"
          :loading="signing"
          @click="handleSign"
        >
          电子签名
        </el-button>
      </div>

      <div v-else class="sign-action signed-tip">
        <el-alert title="合同已签署" type="success" :closable="false" show-icon>
          <template #default>
            签署时间：{{ formatTime(contract.signTime) }}
          </template>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getContract, signContract } from '../../api/contract.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const signing = ref(false)
const agreed = ref(false)
const contract = ref({})

const STATUS_MAP = {
  UNSIGNED: '待签署',
  SIGNED: '已签署',
}

function formatAmount(amount) {
  if (amount === undefined || amount === null) return '-'
  return '¥' + Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRate(rate) {
  if (rate === undefined || rate === null) return '-'
  return (Number(rate) * 100).toFixed(2) + '%'
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

function goBack() {
  router.push('/customer/contracts')
}

async function loadContract() {
  loading.value = true
  try {
    const res = await getContract(route.params.id)
    if (res.data.code === 200) {
      contract.value = res.data.data || {}
    } else {
      ElMessage.error(res.data.msg || '获取合同失败')
    }
  } catch (error) {
    ElMessage.error('获取合同失败')
  } finally {
    loading.value = false
  }
}

async function handleSign() {
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意合同内容')
    return
  }
  try {
    await ElMessageBox.confirm('确认进行电子签名？签名后合同立即生效。', '提示', {
      confirmButtonText: '确认签署',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  signing.value = true
  try {
    const res = await signContract(contract.value.id)
    if (res.data.code === 200) {
      ElMessage.success('签署成功')
      contract.value = res.data.data || contract.value
    } else {
      ElMessage.error(res.data.msg || '签署失败')
    }
  } catch (error) {
    ElMessage.error('签署失败')
  } finally {
    signing.value = false
  }
}

onMounted(loadContract)
</script>

<style scoped>
.contract-sign-view {
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
.contract-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.contract-meta {
  margin-bottom: 20px;
}
.contract-content h3 {
  margin-top: 0;
}
.contract-content pre {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
}
.sign-action {
  margin-top: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
}
.signed-tip {
  display: block;
}
</style>
