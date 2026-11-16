<template>
  <div class="apply-view">
    <div class="page-header">
      <h2>贷款申请</h2>
      <el-button @click="goHome">返回首页</el-button>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <el-col
        v-for="product in productList"
        :key="product.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="8"
        class="product-col"
      >
        <el-card shadow="hover" class="product-card" @click="openApply(product)">
          <div class="product-header">
            <h3>{{ product.name }}</h3>
            <el-tag type="success" size="small">{{ product.type }}</el-tag>
          </div>
          <div class="product-info">
            <p>
              <span class="label">额度范围：</span>
              {{ formatAmount(product.minAmount) }} - {{ formatAmount(product.maxAmount) }}
            </p>
            <p>
              <span class="label">年化利率：</span>
              {{ (product.interestRate * 100).toFixed(2) }}%
            </p>
            <p>
              <span class="label">期限范围：</span>
              {{ product.minTerm }} - {{ product.maxTerm }} 个月
            </p>
            <p>
              <span class="label">还款方式：</span>
              {{ product.repaymentMethod }}
            </p>
          </div>
          <div class="product-action">
            <el-button type="primary" size="small" @click.stop="openApply(product)">
              立即申请
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && productList.length === 0" description="暂无可申请的贷款产品" />

    <el-dialog v-model="formVisible" title="填写贷款申请" width="720px" :close-on-click-modal="false">
      <div v-if="selectedProduct" class="selected-product">
        <p><span class="label">申请产品：</span>{{ selectedProduct.name }}</p>
        <p>
          <span class="label">额度范围：</span>
          {{ formatAmount(selectedProduct.minAmount) }} - {{ formatAmount(selectedProduct.maxAmount) }}
        </p>
        <p>
          <span class="label">期限范围：</span>
          {{ selectedProduct.minTerm }} - {{ selectedProduct.maxTerm }} 个月
        </p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="apply-form"
      >
        <el-form-item label="申请金额" prop="amount">
          <div class="amount-row">
            <el-slider
              v-model="form.amount"
              :min="amountMin"
              :max="amountMax"
              :step="1000"
              style="flex: 1; margin-right: 16px;"
            />
            <el-input-number
              v-model="form.amount"
              :min="amountMin"
              :max="amountMax"
              :step="1000"
              :controls="false"
              style="width: 140px;"
            />
            <span class="amount-unit">元</span>
          </div>
        </el-form-item>

        <el-form-item label="贷款期限" prop="termMonths">
          <el-select v-model="form.termMonths" placeholder="请选择期限" style="width: 200px;">
            <el-option
              v-for="term in termOptions"
              :key="term"
              :label="`${term} 个月`"
              :value="term"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="贷款用途" prop="purpose">
          <el-input
            v-model="form.purpose"
            type="textarea"
            :rows="3"
            placeholder="请描述贷款用途"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="还款来源" prop="repaymentSource">
          <el-input
            v-model="form.repaymentSource"
            type="textarea"
            :rows="3"
            placeholder="请描述还款来源"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="身份证正面" prop="idCardFront">
          <el-upload
            ref="idCardFrontRef"
            :http-request="(options) => customUpload(options, 'ID_CARD_FRONT')"
            :on-remove="(file) => handleRemove(file, 'ID_CARD_FRONT')"
            :file-list="fileList.ID_CARD_FRONT"
            accept=".jpg,.jpeg,.png,.pdf"
            list-type="picture-card"
            :limit="1"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="身份证反面" prop="idCardBack">
          <el-upload
            ref="idCardBackRef"
            :http-request="(options) => customUpload(options, 'ID_CARD_BACK')"
            :on-remove="(file) => handleRemove(file, 'ID_CARD_BACK')"
            :file-list="fileList.ID_CARD_BACK"
            accept=".jpg,.jpeg,.png,.pdf"
            list-type="picture-card"
            :limit="1"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="收入证明" prop="incomeProof">
          <el-upload
            ref="incomeProofRef"
            :http-request="(options) => customUpload(options, 'INCOME_PROOF')"
            :on-remove="(file) => handleRemove(file, 'INCOME_PROOF')"
            :file-list="fileList.INCOME_PROOF"
            accept=".jpg,.jpeg,.png,.pdf"
            list-type="picture-card"
            :limit="3"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="银行流水" prop="bankStatement">
          <el-upload
            ref="bankStatementRef"
            :http-request="(options) => customUpload(options, 'BANK_STATEMENT')"
            :on-remove="(file) => handleRemove(file, 'BANK_STATEMENT')"
            :file-list="fileList.BANK_STATEMENT"
            accept=".jpg,.jpeg,.png,.pdf"
            list-type="picture-card"
            :limit="3"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getProductList } from '../../api/product.js'
import { submitApplication } from '../../api/application.js'
import { uploadFiles } from '../../api/upload.js'

const router = useRouter()
const loading = ref(false)
const productList = ref([])
const formVisible = ref(false)
const selectedProduct = ref(null)
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  productId: null,
  amount: 0,
  termMonths: null,
  purpose: '',
  repaymentSource: '',
})

const fileList = reactive({
  ID_CARD_FRONT: [],
  ID_CARD_BACK: [],
  INCOME_PROOF: [],
  BANK_STATEMENT: [],
})

const uploadedMaterials = reactive({
  ID_CARD_FRONT: [],
  ID_CARD_BACK: [],
  INCOME_PROOF: [],
  BANK_STATEMENT: [],
})

const amountMin = computed(() => {
  return selectedProduct.value ? Number(selectedProduct.value.minAmount) : 0
})

const amountMax = computed(() => {
  return selectedProduct.value ? Number(selectedProduct.value.maxAmount) : 1000000
})

const termOptions = computed(() => {
  if (!selectedProduct.value) return []
  const options = []
  for (let i = selectedProduct.value.minTerm; i <= selectedProduct.value.maxTerm; i++) {
    options.push(i)
  }
  return options
})

function createUploadValidator(type, message) {
  return (rule, value, callback) => {
    if (!uploadedMaterials[type] || uploadedMaterials[type].length === 0) {
      callback(new Error(message))
    } else {
      callback()
    }
  }
}

const rules = {
  amount: [{ required: true, message: '请输入申请金额', trigger: 'change' }],
  termMonths: [{ required: true, message: '请选择贷款期限', trigger: 'change' }],
  purpose: [{ required: true, message: '请输入贷款用途', trigger: 'blur' }],
  repaymentSource: [{ required: true, message: '请输入还款来源', trigger: 'blur' }],
  idCardFront: [{ required: true, validator: createUploadValidator('ID_CARD_FRONT', '请上传身份证正面'), trigger: 'change' }],
  idCardBack: [{ required: true, validator: createUploadValidator('ID_CARD_BACK', '请上传身份证反面'), trigger: 'change' }],
  incomeProof: [{ required: true, validator: createUploadValidator('INCOME_PROOF', '请上传收入证明'), trigger: 'change' }],
  bankStatement: [{ required: true, validator: createUploadValidator('BANK_STATEMENT', '请上传银行流水'), trigger: 'change' }],
}

function formatAmount(amount) {
  return '¥' + Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function goHome() {
  router.push('/customer/home')
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductList()
    if (res.data.code === 200) {
      productList.value = (res.data.data || []).filter(item => item.status === 'ACTIVE')
    } else {
      ElMessage.error(res.data.msg || '获取产品列表失败')
    }
  } catch (error) {
    ElMessage.error('获取产品列表失败')
  } finally {
    loading.value = false
  }
}

function openApply(product) {
  selectedProduct.value = product
  form.productId = product.id
  form.amount = Number(product.minAmount)
  form.termMonths = product.minTerm
  form.purpose = ''
  form.repaymentSource = ''
  resetMaterials()
  formVisible.value = true
}

function resetMaterials() {
  for (const key of Object.keys(fileList)) {
    fileList[key] = []
    uploadedMaterials[key] = []
  }
}

async function customUpload(options, type) {
  try {
    const res = await uploadFiles([options.file])
    if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
      const url = res.data.data[0]
      uploadedMaterials[type].push({
        fileName: options.file.name,
        filePath: url,
        materialType: type,
      })
      options.onSuccess(url)
    } else {
      options.onError(new Error(res.data.msg || '上传失败'))
      ElMessage.error(res.data.msg || '上传失败')
    }
  } catch (error) {
    options.onError(error)
    ElMessage.error('文件上传失败')
  }
}

function handleRemove(file, type) {
  const index = uploadedMaterials[type].findIndex(item => item.filePath === file.url || item.filePath === file.response)
  if (index > -1) {
    uploadedMaterials[type].splice(index, 1)
  }
}

function buildMaterials() {
  const list = []
  for (const key of Object.keys(uploadedMaterials)) {
    list.push(...uploadedMaterials[key])
  }
  return list
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const materials = buildMaterials()
    const payload = {
      productId: form.productId,
      amount: form.amount,
      termMonths: form.termMonths,
      purpose: form.purpose,
      repaymentSource: form.repaymentSource,
      newMaterials: materials,
    }
    const res = await submitApplication(payload)
    if (res.data.code === 200) {
      ElMessage.success('申请提交成功')
      formVisible.value = false
      router.push('/customer/loans')
    } else {
      ElMessage.error(res.data.msg || '申请提交失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '申请提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadProducts)
</script>

<style scoped>
.apply-view {
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
.product-col {
  margin-bottom: 20px;
}
.product-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.product-card:hover {
  transform: translateY(-4px);
}
.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.product-header h3 {
  margin: 0;
  font-size: 18px;
}
.product-info p {
  margin: 8px 0;
  color: #606266;
}
.product-action {
  margin-top: 16px;
  text-align: right;
}
.label {
  color: #909399;
}
.selected-product {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}
.selected-product p {
  margin: 8px 0;
}
.amount-row {
  display: flex;
  align-items: center;
}
.amount-unit {
  margin-left: 8px;
  color: #606266;
}
</style>
