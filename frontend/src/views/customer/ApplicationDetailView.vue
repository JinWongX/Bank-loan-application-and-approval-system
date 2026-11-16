<template>
  <div class="application-detail-view">
    <div class="page-header">
      <h2>申请详情</h2>
      <el-button @click="goBack">返回</el-button>
    </div>

    <el-card v-loading="loading" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-tag :type="statusType(application.status)" size="small">
            {{ statusText(application.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请单号">{{ application.applicationNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="贷款产品">{{ application.product?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ formatAmount(application.amount) }}</el-descriptions-item>
        <el-descriptions-item label="贷款期限">{{ application.termMonths }} 个月</el-descriptions-item>
        <el-descriptions-item label="年化利率">{{ formatRate(application.product?.interestRate) }}</el-descriptions-item>
        <el-descriptions-item label="还款方式">{{ application.product?.repaymentMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatTime(application.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(application.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="贷款用途" :span="2">{{ application.purpose || '-' }}</el-descriptions-item>
        <el-descriptions-item label="还款来源" :span="2">{{ application.repaymentSource || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="creditEvaluation" class="credit-card">
      <template #header>
        <div class="card-header">
          <span>信用评估结果</span>
          <el-tag :type="gradeType(creditEvaluation.grade)" size="small">
            {{ creditEvaluation.grade }} 级
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="信用评分">
          <span class="credit-score">{{ creditEvaluation.score }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="信用等级">
          <el-tag :type="gradeType(creditEvaluation.grade)" size="large">
            {{ creditEvaluation.grade }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="评估时间">
          {{ formatTime(creditEvaluation.evaluateTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="评估建议" :span="2">
          {{ creditEvaluation.evaluationSuggestion }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card v-else class="credit-card">
      <template #header>
        <span>信用评估结果</span>
      </template>
      <el-empty description="暂无信用评估结果" />
    </el-card>

    <el-card class="material-card">
      <template #header>
        <span>已上传材料</span>
      </template>

      <div v-if="application.materials && application.materials.length > 0">
        <el-table :data="application.materials" stripe style="width: 100%">
          <el-table-column prop="materialType" label="材料类型" min-width="160">
            <template #default="{ row }">
              {{ materialTypeText(row.materialType) }}
            </template>
          </el-table-column>
          <el-table-column prop="fileName" label="文件名称" min-width="200" />
          <el-table-column prop="uploadTime" label="上传时间" min-width="160">
            <template #default="{ row }">
              {{ formatTime(row.uploadTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openFile(row.filePath)">
                查看文件
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else description="暂无上传材料" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApplicationDetail } from '../../api/application.js'
import { getCreditEvaluation } from '../../api/creditEvaluation.js'
import { downloadFile } from '../../api/file.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const application = ref({
  product: {},
  materials: [],
})
const creditEvaluation = ref(null)

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

const MATERIAL_TYPE_MAP = {
  ID_CARD_FRONT: '身份证正面',
  ID_CARD_BACK: '身份证反面',
  INCOME_PROOF: '收入证明',
  BANK_STATEMENT: '银行流水',
}

function formatAmount(amount) {
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

function materialTypeText(type) {
  return MATERIAL_TYPE_MAP[type] || type
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

async function loadCreditEvaluation(applicationId) {
  try {
    const res = await getCreditEvaluation(applicationId)
    if (res.data.code === 200) {
      creditEvaluation.value = res.data.data
    }
  } catch (error) {
    // 信用评估结果非关键信息，失败时不阻断页面展示
    console.error('获取信用评估结果失败', error)
  }
}

function goBack() {
  router.push('/customer/loans')
}

async function openFile(url) {
  if (!url) return
  try {
    const res = await downloadFile(url)
    const blob = new Blob([res.data])
    const blobUrl = URL.createObjectURL(blob)
    window.open(blobUrl, '_blank')
  } catch (error) {
    ElMessage.error('文件下载失败')
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getApplicationDetail(route.params.id)
    if (res.data.code === 200) {
      application.value = res.data.data || { product: {}, materials: [] }
      if (application.value.id) {
        await loadCreditEvaluation(application.value.id)
      }
    } else {
      ElMessage.error(res.data.msg || '获取申请详情失败')
    }
  } catch (error) {
    ElMessage.error('获取申请详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.application-detail-view {
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
.detail-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.material-card {
  margin-bottom: 20px;
}
.credit-card {
  margin-bottom: 20px;
}
.credit-score {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}
</style>
