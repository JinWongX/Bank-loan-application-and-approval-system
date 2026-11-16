import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user.js'

const ROLE_HOME_MAP = {
  customer: '/customer/home',
  manager: '/manager/home',
  approver_level1: '/approver/home',
  approver_level2: '/approver/home',
  approver_committee: '/approver/home',
  disbursement_officer: '/disburser/home',
  admin: '/admin/home',
}

const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
  },
  {
    path: '/customer/home',
    name: 'CustomerHome',
    component: () => import('../views/customer/HomeView.vue'),
    meta: { requiresAuth: true, role: 'customer' },
  },
  {
    path: '/customer/apply',
    name: 'CustomerApply',
    component: () => import('../views/customer/ApplyView.vue'),
    meta: { requiresAuth: true, role: 'customer' },
  },
  {
    path: '/customer/loans',
    name: 'CustomerLoans',
    component: () => import('../views/customer/MyLoansView.vue'),
    meta: { requiresAuth: true, role: 'customer' },
  },
  {
    path: '/customer/loans/:id',
    name: 'CustomerLoanDetail',
    component: () => import('../views/customer/ApplicationDetailView.vue'),
    meta: { requiresAuth: true, role: 'customer' },
  },
  {
    path: '/customer/contracts',
    name: 'CustomerContracts',
    component: () => import('../views/customer/ContractsView.vue'),
    meta: { requiresAuth: true, role: 'customer' },
  },
  {
    path: '/customer/contracts/:id/sign',
    name: 'CustomerContractSign',
    component: () => import('../views/customer/ContractSignView.vue'),
    meta: { requiresAuth: true, role: 'customer' },
  },
  {
    path: '/customer/repayment',
    name: 'CustomerRepayment',
    component: () => import('../views/customer/RepaymentView.vue'),
    meta: { requiresAuth: true, role: 'customer' },
  },
  {
    path: '/manager/home',
    name: 'ManagerHome',
    component: () => import('../views/manager/HomeView.vue'),
    meta: { requiresAuth: true, role: 'manager' },
  },
  {
    path: '/manager/pending',
    name: 'ManagerPending',
    component: () => import('../views/manager/PendingView.vue'),
    meta: { requiresAuth: true, role: 'manager' },
  },
  {
    path: '/manager/due-diligence/:id',
    name: 'ManagerDueDiligence',
    component: () => import('../views/manager/DueDiligenceView.vue'),
    meta: { requiresAuth: true, role: 'manager' },
  },
  {
    path: '/approver/home',
    name: 'ApproverHome',
    component: () => import('../views/approver/HomeView.vue'),
    meta: { requiresAuth: true, role: 'approver' },
  },
  {
    path: '/approver/pending',
    name: 'ApproverPending',
    component: () => import('../views/approver/PendingView.vue'),
    meta: { requiresAuth: true, role: 'approver' },
  },
  {
    path: '/approver/approval/:id',
    name: 'ApproverApprovalDetail',
    component: () => import('../views/approver/ApprovalDetailView.vue'),
    meta: { requiresAuth: true, role: 'approver' },
  },
  {
    path: '/disburser/home',
    name: 'DisburserHome',
    component: () => import('../views/disburser/HomeView.vue'),
    meta: { requiresAuth: true, role: 'disburser' },
  },
  {
    path: '/disburser/pending',
    name: 'DisburserPending',
    component: () => import('../views/disburser/PendingView.vue'),
    meta: { requiresAuth: true, role: 'disburser' },
  },
  {
    path: '/disburser/disbursement/:id',
    name: 'DisburserDisbursement',
    component: () => import('../views/disburser/DisbursementView.vue'),
    meta: { requiresAuth: true, role: 'disburser' },
  },
  {
    path: '/admin/home',
    name: 'AdminHome',
    component: () => import('../views/admin/HomeView.vue'),
    meta: { requiresAuth: true, role: 'admin' },
  },
  {
    path: '/admin/products',
    name: 'AdminProducts',
    component: () => import('../views/admin/ProductList.vue'),
    meta: { requiresAuth: true, role: 'admin' },
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('../views/admin/DashboardView.vue'),
    meta: { requiresAuth: true, role: 'admin' },
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../views/admin/UserList.vue'),
    meta: { requiresAuth: true, role: 'admin' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function normalizeRole(role) {
  if (role === 'approver_level1' || role === 'approver_level2' || role === 'approver_committee') return 'approver'
  if (role === 'disbursement_officer') return 'disburser'
  return role
}

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth
  const requiredRole = to.meta.role

  if (to.path === '/login') {
    if (userStore.isLoggedIn) {
      return next(userStore.getHomePath())
    }
    return next()
  }

  if (requiresAuth && !userStore.isLoggedIn) {
    return next('/login')
  }

  if (requiredRole && userStore.userInfo) {
    const userRole = normalizeRole(userStore.userInfo.role)
    if (userRole !== requiredRole) {
      const homePath = ROLE_HOME_MAP[userStore.userInfo.role] || '/login'
      return next(homePath)
    }
  }

  next()
})

export default router
