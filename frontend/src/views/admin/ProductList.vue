<template>
  <div class="product-list">
    <div class="page-header">
      <h2>产品配置</h2>
      <div>
        <el-button @click="goHome">返回首页</el-button>
        <el-button type="primary" @click="openAddDialog">新增产品</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="productList" v-loading="loading" border stripe>
        <el-table-column prop="name" label="产品名称" min-width="140" />
        <el-table-column prop="type" label="产品类型" min-width="100" />
        <el-table-column label="额度范围" min-width="160">
          <template #default="{ row }">
            {{ formatAmount(row.minAmount) }} - {{ formatAmount(row.maxAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="利率" min-width="100">
          <template #default="{ row }">
            {{ (row.interestRate * 100).toFixed(2) }}%
          </template>
        </el-table-column>
        <el-table-column label="期限范围" min-width="120">
          <template #default="{ row }">
            {{ row.minTerm }} - {{ row.maxTerm }} 个月
          </template>
        </el-table-column>
        <el-table-column prop="repaymentMethod" label="还款方式" min-width="120" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑产品' : '新增产品'"
      width="600px"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品类型" prop="type">
          <el-input v-model="form.type" placeholder="请输入产品类型" />
        </el-form-item>
        <el-form-item label="最低额度" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最高额度" prop="maxAmount">
          <el-input-number v-model="form.maxAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="利率" prop="interestRate">
          <el-input-number
            v-model="form.interestRate"
            :min="0"
            :max="1"
            :precision="4"
            style="width: 100%"
            placeholder="请输入年化利率，如 0.045"
          />
        </el-form-item>
        <el-form-item label="最短期限(月)" prop="minTerm">
          <el-input-number v-model="form.minTerm" :min="1" :step="1" step-strictly style="width: 100%" />
        </el-form-item>
        <el-form-item label="最长期限(月)" prop="maxTerm">
          <el-input-number v-model="form.maxTerm" :min="1" :step="1" step-strictly style="width: 100%" />
        </el-form-item>
        <el-form-item label="还款方式" prop="repaymentMethod">
          <el-select v-model="form.repaymentMethod" placeholder="请选择还款方式" style="width: 100%">
            <el-option label="等额本息" value="等额本息" />
            <el-option label="等额本金" value="等额本金" />
            <el-option label="先息后本" value="先息后本" />
            <el-option label="按季付息到期还本" value="按季付息到期还本" />
            <el-option label="到期一次性还本付息" value="到期一次性还本付息" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请条件" prop="eligibilityConditions">
          <el-input
            v-model="form.eligibilityConditions"
            type="textarea"
            :rows="3"
            placeholder="请输入申请条件"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductList, createProduct, updateProduct, updateProductStatus, deleteProduct } from '../../api/product.js'

const router = useRouter()
const loading = ref(false)
const productList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '',
  type: '',
  minAmount: 0,
  maxAmount: 0,
  interestRate: 0,
  minTerm: 1,
  maxTerm: 1,
  repaymentMethod: '',
  eligibilityConditions: '',
  status: 'ACTIVE',
})

const rules = {
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  type: [{ required: true, message: '请输入产品类型', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入最低额度', trigger: 'blur' }],
  maxAmount: [{ required: true, message: '请输入最高额度', trigger: 'blur' }],
  interestRate: [{ required: true, message: '请输入利率', trigger: 'blur' }],
  minTerm: [{ required: true, message: '请输入最短期限', trigger: 'blur' }],
  maxTerm: [{ required: true, message: '请输入最长期限', trigger: 'blur' }],
  repaymentMethod: [{ required: true, message: '请选择还款方式', trigger: 'change' }],
}

function formatAmount(amount) {
  return '¥' + Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function goHome() {
  router.push('/admin/home')
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductList()
    if (res.data.code === 200) {
      productList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '获取产品列表失败')
    }
  } catch (error) {
    ElMessage.error('获取产品列表失败')
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

function resetForm() {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  Object.assign(form, {
    name: '',
    type: '',
    minAmount: 0,
    maxAmount: 0,
    interestRate: 0,
    minTerm: 1,
    maxTerm: 1,
    repaymentMethod: '',
    eligibilityConditions: '',
    status: 'ACTIVE',
  })
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateProduct(currentId.value, form)
    } else {
      res = await createProduct(form)
    }
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
      dialogVisible.value = false
      loadProducts()
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  const actionText = newStatus === 'ACTIVE' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定要${actionText}产品「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const res = await updateProductStatus(row.id, newStatus)
    if (res.data.code === 200) {
      ElMessage.success(`${actionText}成功`)
      loadProducts()
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除产品「${row.name}」吗？若已存在贷款申请将无法删除。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const res = await deleteProduct(row.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      loadProducts()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '删除失败')
    }
  }
}

onMounted(loadProducts)
</script>

<style scoped>
.product-list {
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
