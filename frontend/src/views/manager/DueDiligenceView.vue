<template>
  <div class="due-diligence-view">
    <div class="page-header">
      <h2>尽职调查</h2>
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
        <el-descriptions-item label="评估建议" :span="2">
          {{ application.creditEvaluation.evaluationSuggestion }}
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

    <el-card class="form-card">
      <template #header>
        <span>尽调报告</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="经营情况" prop="businessSituation">
          <el-input
            v-model="form.businessSituation"
            type="textarea"
            :rows="4"
            placeholder="请输入客户经营情况"
          />
        </el-form-item>

        <el-form-item label="资产负债" prop="assetsLiabilities">
          <el-input
            v-model="form.assetsLiabilities"
            type="textarea"
            :rows="4"
            placeholder="请输入客户资产负债情况"
          />
        </el-form-item>

        <el-form-item label="还款能力评估" prop="repaymentAbility">
          <el-input
            v-model="form.repaymentAbility"
            type="textarea"
            :rows="4"
            placeholder="请输入还款能力评估"
          />
        </el-form-item>

        <el-form-item label="尽调结论" prop="conclusion">
          <el-select v-model="form.conclusion" placeholder="请选择尽调结论" style="width: 240px">
            <el-option label="通过" value="PASS" />
            <el-option label="建议拒绝" value="SUGGEST_REJECT" />
            <el-option label="退回补充" value="SUPPLEMENT" />
          </el-select>
        </el-form-item>

        <el-form-item label="附件上传">
          <el-upload
            :http-request="handleUpload"
            :show-file-list="true"
            :limit="1"
            accept=".jpg,.jpeg,.png,.pdf"
          >
            <el-button type="primary">上传附件</el-button>
            <template #tip>
              <div class="upload-tip">支持 jpg、png、pdf 格式，单个文件不超过 5MB</div>
            </template>
          </el-upload>
          <div v-if="form.attachmentPath" class="attachment-path">
            已上传：{{ form.attachmentPath }}
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交尽调报告
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
import { ElMessage } from 'element-plus'
import { getApplicationFullDetail } from '../../api/application.js'
import { submitDueDiligence } from '../../api/dueDiligence.js'
import { uploadFiles } from '../../api/upload.js'
import { downloadFile } from '../../api/file.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const application = ref({
  product: {},
  materials: [],
  creditEvaluation: null,
})

const formRef = ref(null)
const form = reactive({
  businessSituation: '',
  assetsLiabilities: '',
  repaymentAbility: '',
  conclusion: '',
  attachmentPath: '',
})

const rules = {
  businessSituation: [{ required: true, message: '请输入经营情况', trigger: 'blur' }],
  assetsLiabilities: [{ required: true, message: '请输入资产负债情况', trigger: 'blur' }],
  repaymentAbility: [{ required: true, message: '请输入还款能力评估', trigger: 'blur' }],
  conclusion: [{ required: true, message: '请选择尽调结论', trigger: 'change' }],
}

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
  router.push('/manager/pending')
}

async function handleUpload({ file }) {
  try {
    const rawFile = file.raw || file
    const res = await uploadFiles([rawFile])
    if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
      form.attachmentPath = res.data.data[0]
      ElMessage.success('附件上传成功')
    } else {
      ElMessage.error(res.data.msg || '附件上传失败')
    }
  } catch (error) {
    ElMessage.error('附件上传失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await submitDueDiligence(route.params.id, form)
    if (res.data.code === 200) {
      ElMessage.success('尽调报告提交成功')
      router.push('/manager/pending')
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
    const res = await getApplicationFullDetail(route.params.id)
    if (res.data.code === 200) {
      application.value = res.data.data || { product: {}, materials: [], creditEvaluation: null }
      if (application.value.dueDiligence) {
        const dd = application.value.dueDiligence
        form.businessSituation = dd.businessSituation || ''
        form.assetsLiabilities = dd.assetsLiabilities || ''
        form.repaymentAbility = dd.repaymentAbility || ''
        form.conclusion = dd.conclusion || ''
        form.attachmentPath = dd.attachmentPath || ''
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
.due-diligence-view {
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
.material-card,
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
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.attachment-path {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}
</style>
