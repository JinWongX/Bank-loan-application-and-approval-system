<template>
  <div class="approval-detail-view">
    <div class="page-header">
      <h2>审批详情</h2>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <el-card v-loading="loading" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>申请详情</span>
          <el-tag :type="statusType(application.status)" size="small">
            {{ statusText(application.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请单号">{{ application.applicationNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ application.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户电话">{{ application.customerPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ application.idCard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="贷款产品">{{ application.product?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ formatAmount(application.amount) }}</el-descriptions-item>
        <el-descriptions-item label="贷款期限">{{ application.termMonths }} 个月</el-descriptions-item>
        <el-descriptions-item label="年化利率">{{ formatRate(application.product?.interestRate) }}</el-descriptions-item>
        <el-descriptions-item label="还款方式">{{ application.product?.repaymentMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatTime(application.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="贷款用途" :span="2">{{ application.purpose || '-' }}</el-descriptions-item>
        <el-descriptions-item label="还款来源" :span="2">{{ application.repaymentSource || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="application.creditEvaluation" class="credit-card">
      <template #header>
        <div class="card-header">
          <span>信用评估结果</span>
          <el-tag :type="gradeType(application.creditEvaluation.grade)" size="small">
            {{ application.creditEvaluation.grade }} 级
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="信用评分">
          <span class="credit-score">{{ application.creditEvaluation.score }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="信用等级">
          <el-tag :type="gradeType(application.creditEvaluation.grade)" size="large">
            {{ application.creditEvaluation.grade }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="评估时间">
          {{ formatTime(application.creditEvaluation.evaluateTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="评估建议" :span="3">
          {{ application.creditEvaluation.evaluationSuggestion }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="application.dueDiligence" class="due-diligence-card">
      <template #header>
        <span>尽调报告</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="经营情况">{{ application.dueDiligence.businessSituation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资产负债">{{ application.dueDiligence.assetsLiabilities || '-' }}</el-descriptions-item>
        <el-descriptions-item label="还款能力">{{ application.dueDiligence.repaymentAbility || '-' }}</el-descriptions-item>
        <el-descriptions-item label="尽调结论">
          <el-tag :type="conclusionType(application.dueDiligence.conclusion)" size="small">
            {{ conclusionText(application.dueDiligence.conclusion) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(application.dueDiligence.submitTime) }}</el-descriptions-item>
        <el-descriptions-item label="附件">
          <el-button v-if="application.dueDiligence.attachmentPath" link type="primary" size="small" @click="openFile(application.dueDiligence.attachmentPath)">
            查看附件
          </el-button>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
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

    <el-card v-if="application.approvalRecords && application.approvalRecords.length > 0" class="history-card">
      <template #header>
        <span>审批历史</span>
      </template>

      <el-timeline>
        <el-timeline-item
          v-for="record in application.approvalRecords"
          :key="record.id"
          :type="actionType(record.action)"
          :timestamp="formatTime(record.approvalTime)"
        >
          <p><strong>{{ record.approverName || '-' }}</strong> - {{ actionText(record.action) }}</p>
          <p v-if="record.comment" class="comment">意见：{{ record.comment }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card class="form-card">
      <template #header>
        <span>审批意见</span>
      </template>

      <el-form :model="form" label-width="100px">
        <el-form-item label="审批意见">
          <el-input
            v-model="form.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入审批意见"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="success" :loading="submitting" @click="handleSubmit('APPROVE')">
            通过
          </el-button>
          <el-button type="danger" :loading="submitting" @click="handleSubmit('REJECT')">
            驳回
          </el-button>
          <el-button type="warning" :loading="submitting" @click="handleSubmit('SUPPLEMENT')">
            退回补充
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApprovalDetail, submitApproval } from '../../api/approval.js'
import { downloadFile } from '../../api/file.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const application = ref({
  product: {},
  materials: [],
  creditEvaluation: null,
  dueDiligence: null,
  approvalRecords: [],
})

const form = reactive({
  comment: '',
})

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

const ACTION_MAP = {
  APPROVE: '通过',
  REJECT: '驳回',
  SUPPLEMENT: '退回补充',
}

const MATERIAL_TYPE_MAP = {
  ID_CARD_FRONT: '身份证正面',
  ID_CARD_BACK: '身份证反面',
  INCOME_PROOF: '收入证明',
  BANK_STATEMENT: '银行流水',
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

function conclusionText(conclusion) {
  switch (conclusion) {
    case 'PASS':
      return '通过'
    case 'SUGGEST_REJECT':
      return '建议拒绝'
    case 'SUPPLEMENT':
      return '退回补充'
    default:
      return conclusion || '-'
  }
}

function conclusionType(conclusion) {
  switch (conclusion) {
    case 'PASS':
      return 'success'
    case 'SUGGEST_REJECT':
      return 'danger'
    case 'SUPPLEMENT':
      return 'warning'
    default:
      return 'info'
  }
}

function materialTypeText(type) {
  return MATERIAL_TYPE_MAP[type] || type
}

function actionText(action) {
  return ACTION_MAP[action] || action
}

function actionType(action) {
  switch (action) {
    case 'APPROVE':
      return 'success'
    case 'REJECT':
      return 'danger'
    case 'SUPPLEMENT':
      return 'warning'
    default:
      return 'info'
  }
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

function goBack() {
  router.push('/approver/pending')
}

async function handleSubmit(action) {
  const actionName = actionText(action)
  try {
    await ElMessageBox.confirm(`确认要${actionName}该申请吗？`, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: action === 'APPROVE' ? 'success' : action === 'REJECT' ? 'danger' : 'warning',
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    const res = await submitApproval(route.params.id, {
      action,
      comment: form.comment,
    })
    if (res.data.code === 200) {
      ElMessage.success(`${actionName}成功`)
      router.push('/approver/pending')
    } else {
      ElMessage.error(res.data.msg || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getApprovalDetail(route.params.id)
    if (res.data.code === 200) {
      application.value = res.data.data || { product: {}, materials: [], creditEvaluation: null, dueDiligence: null, approvalRecords: [] }
    } else {
      ElMessage.error(res.data.msg || '获取审批详情失败')
    }
  } catch (error) {
    ElMessage.error('获取审批详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.approval-detail-view {
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
.detail-card,
.credit-card,
.due-diligence-card,
.material-card,
.history-card,
.form-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.credit-score {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}
.comment {
  color: #606266;
  font-size: 13px;
  margin-top: 4px;
}
</style>
