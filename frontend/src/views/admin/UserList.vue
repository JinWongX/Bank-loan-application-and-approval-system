<template>
  <div class="user-list">
    <div class="page-header">
      <h2>用户管理</h2>
      <div>
        <el-button @click="goHome">返回首页</el-button>
        <el-button type="primary" @click="openAddDialog">新增用户</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="userList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            {{ roleText(row.role) }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="idCard" label="身份证号" min-width="160" />
        <el-table-column prop="age" label="年龄" min-width="70" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="openResetDialog(row)">重置密码</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleList" :key="role.code" :label="role.name" :value="role.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="1" :max="120" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月收入" prop="monthlyIncome">
          <el-input-number v-model="form.monthlyIncome" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="征信状态" prop="creditStatus">
          <el-select v-model="form.creditStatus" placeholder="请选择征信状态" style="width: 100%">
            <el-option label="优秀" value="GOOD" />
            <el-option label="良好" value="GOOD" />
            <el-option label="一般" value="FAIR" />
            <el-option label="较差" value="POOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="月负债" prop="monthlyDebt">
          <el-input-number v-model="form.monthlyDebt" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialogVisible" title="重置密码" width="400px">
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="100px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="submitReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, updateUserStatus, resetUserPassword, getRoleList } from '../../api/user.js'

const router = useRouter()
const loading = ref(false)
const userList = ref([])
const roleList = ref([])
const dialogVisible = ref(false)
const resetDialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const submitting = ref(false)
const resetting = ref(false)
const formRef = ref(null)
const resetFormRef = ref(null)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: '',
  phone: '',
  idCard: '',
  age: 30,
  monthlyIncome: 0,
  creditStatus: 'GOOD',
  monthlyDebt: 0,
  status: 1,
})

const resetForm = reactive({
  newPassword: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const resetRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
}

const roleMap = {
  customer: '客户',
  manager: '客户经理',
  approver_level1: '一级审批人',
  approver_level2: '二级审批人',
  approver_committee: '审批委员会',
  disbursement_officer: '放款员',
  admin: '管理员',
}

function roleText(role) {
  return roleMap[role] || role
}

function goHome() {
  router.push('/admin/home')
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList()
    if (res.data.code === 200) {
      userList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '获取用户列表失败')
    }
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await getRoleList()
    if (res.data.code === 200) {
      roleList.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载角色列表失败', error)
  }
}

function openAddDialog() {
  isEdit.value = false
  currentId.value = null
  resetFormModel()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

function openResetDialog(row) {
  currentId.value = row.id
  resetForm.newPassword = ''
  resetDialogVisible.value = true
}

function resetFormModel() {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    role: '',
    phone: '',
    idCard: '',
    age: 30,
    monthlyIncome: 0,
    creditStatus: 'GOOD',
    monthlyDebt: 0,
    status: 1,
  })
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      const { username, password, ...updateData } = form
      res = await updateUser(currentId.value, updateData)
    } else {
      res = await createUser(form)
    }
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
      dialogVisible.value = false
      loadUsers()
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function submitReset() {
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid) return

  resetting.value = true
  try {
    const res = await resetUserPassword(currentId.value, resetForm.newPassword)
    if (res.data.code === 200) {
      ElMessage.success('密码重置成功')
      resetDialogVisible.value = false
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  } finally {
    resetting.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${actionText}用户「${row.realName || row.username}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const res = await updateUserStatus(row.id, newStatus)
    if (res.data.code === 200) {
      ElMessage.success(`${actionText}成功`)
      loadUsers()
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
    await ElMessageBox.confirm(`确定要删除用户「${row.realName || row.username}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const res = await deleteUser(row.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      loadUsers()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '删除失败')
    }
  }
}

onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>

<style scoped>
.user-list {
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
