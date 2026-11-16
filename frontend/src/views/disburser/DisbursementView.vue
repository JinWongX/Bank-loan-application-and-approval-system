<template>
  <div class="disbursement-view">
    <div class="page-header">
      <h2>放款登记</h2>
      <el-button @click="goBack">返回</el-button>
    </div>

    <el-card v-loading="loading" class="info-card">
      <template #header>
        <div class="card-header">
          <span>合同与申请信息</span>
          <el-tag :type="alreadyDisbursed ? 'success' : 'warning'" size="small">
            {{ alreadyDisbursed ? '已放款' : '已签署' }}
          </el-tag>
        </div>
      </template>

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
    </el-card>

    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <span>填写放款信息</span>
          <el-tag v-if="alreadyDisbursed" type="success" size="small">已放款</el-tag>
        </div>
      </template>

      <el-alert
        v-if="alreadyDisbursed"
        title="该申请已完成放款"
        type="success"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />

      <el-form v-if="!alreadyDisbursed" ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="放款时间" prop="disbursementTime">
          <el-date-picker
            v-model="form.disbursementTime"
            type="datetime"
            placeholder="选择放款时间"
            style="width: 100%"
            value-format="YYYY-MM-DD[T]HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="放款金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收款账号" prop="accountNo">
          <el-input v-model="form.accountNo" placeholder="请输入收款账号" />
        </el-form-item>
        <el-form-item label="开户行" prop="bankName">
          <el-input v-model="form.bankName" placeholder="请输入开户行" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            确认放款
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="放款时间">{{ formatTime(disbursementInfo.disbursementTime) }}</el-descriptions-item>
        <el-descriptions-item label="放款金额">{{ formatAmount(disbursementInfo.amount) }}</el-descriptions-item>
        <el-descriptions-item label="收款账号">{{ disbursementInfo.accountNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开户行">{{ disbursementInfo.bankName || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="scheduleList.length > 0" class="schedule-card">
      <template #header>
        <span>生成还款计划</span>
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
            <el-tag type="info" size="small">{{ row.status === 'PENDING' ? '待还款' : row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getContract } from '../../api/contract.js'
import { disburse, getDisbursement } from '../../api/disbursement.js'
import { getRepaymentSchedules } from '../../api/repaymentSchedule.js'

const route = useRoute()
const router = useRouter()
const applicationId = route.params.id

const loading = ref(false)
const submitting = ref(false)
const contract = ref({})
const scheduleList = ref([])
const alreadyDisbursed = ref(false)
const disbursementInfo = ref({})

const formRef = ref(null)
const form = ref({
  disbursementTime: '',
  amount: 0,
  accountNo: '',
  bankName: '',
})

const rules = {
  disbursementTime: [{ required: true, message: '请选择放款时间', trigger: 'change' }],
  amount: [{ required: true, message: '请输入放款金额', trigger: 'blur' }],
  accountNo: [{ required: true, message: '请输入收款账号', trigger: 'blur' }],
  bankName: [{ required: true, message: '请输入开户行', trigger: 'blur' }],
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

function goBack() {
  router.push('/disburser/pending')
}

async function loadContract() {
  loading.value = true
  try {
    const res = await getContract(applicationId)
    if (res.data.code === 200) {
      contract.value = res.data.data || {}
      form.value.amount = Number(contract.value.amount) || 0
    } else {
      ElMessage.error(res.data.msg || '获取合同信息失败')
    }
  } catch (error) {
    ElMessage.error('获取合同信息失败')
  }
}

async function loadDisbursementStatus() {
  try {
    const res = await getDisbursement(applicationId)
    if (res.data.code === 200 && res.data.data) {
      alreadyDisbursed.value = true
      disbursementInfo.value = res.data.data
      await loadSchedule()
    }
  } catch (error) {
    // 未放款时接口可能返回空或异常，忽略
  } finally {
    loading.value = false
  }
}

async function loadSchedule() {
  try {
    const res = await getRepaymentSchedules(applicationId)
    if (res.data.code === 200) {
      scheduleList.value = res.data.data || []
    }
  } catch (error) {
    ElMessage.error('获取还款计划失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = { ...form.value }
    const res = await disburse(applicationId, payload)
    if (res.data.code === 200) {
      ElMessage.success('放款成功，已生成还款计划')
      alreadyDisbursed.value = true
      disbursementInfo.value = res.data.data || {}
      await loadSchedule()
    } else {
      ElMessage.error(res.data.msg || '放款失败')
    }
  } catch (error) {
    const msg = error?.response?.data?.msg || error?.message || '放款失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadContract()
  await loadDisbursementStatus()
})
</script>

<style scoped>
.disbursement-view {
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
.info-card,
.form-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
