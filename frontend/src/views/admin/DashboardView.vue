<template>
  <div class="dashboard">
    <div class="page-header">
      <h2>统计看板</h2>
      <el-button @click="goHome">返回首页</el-button>
    </div>

    <el-row :gutter="20" class="metrics">
      <el-col :span="6">
        <el-card>
          <div class="metric-title">待办申请数</div>
          <div class="metric-value">{{ dashboard.pendingApplicationCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">本月申请量</div>
          <div class="metric-value">{{ dashboard.monthApplicationCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">本月放款金额</div>
          <div class="metric-value">{{ formatMoney(dashboard.monthDisbursementAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">平均审批时长（小时）</div>
          <div class="metric-value">{{ dashboard.averageApprovalDurationHours }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts">
      <el-col :span="8">
        <el-card>
          <div ref="productChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div ref="managerChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div ref="timeChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="export-card">
      <div class="export-bar">
        <span>导出报表：</span>
        <el-select v-model="exportType" style="width: 120px; margin-right: 10px;">
          <el-option label="Excel" value="excel" />
          <el-option label="CSV" value="csv" />
        </el-select>
        <el-select v-model="exportDimension" style="width: 140px; margin-right: 10px;">
          <el-option label="按产品" value="product" />
          <el-option label="按客户经理" value="manager" />
          <el-option label="按时间段" value="time" />
        </el-select>
        <template v-if="exportDimension === 'time'">
          <el-date-picker
            v-model="exportDateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="margin-right: 10px;"
          />
          <el-select v-model="exportGroupBy" style="width: 120px; margin-right: 10px;">
            <el-option label="按日" value="DAY" />
            <el-option label="按周" value="WEEK" />
            <el-option label="按月" value="MONTH" />
            <el-option label="按年" value="YEAR" />
          </el-select>
        </template>
        <el-button type="primary" @click="handleExport">导出</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="按产品明细" name="product">
          <el-table :data="productData" style="width: 100%">
            <el-table-column prop="productName" label="产品名称" />
            <el-table-column prop="applicationCount" label="申请量" />
            <el-table-column prop="approvedCount" label="通过量" />
            <el-table-column prop="disbursementAmount" label="放款金额" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="按客户经理明细" name="manager">
          <el-table :data="managerData" style="width: 100%">
            <el-table-column prop="managerName" label="客户经理" />
            <el-table-column prop="applicationCount" label="申请量" />
            <el-table-column prop="approvalRate" label="通过率(%)" />
            <el-table-column prop="disbursementAmount" label="放款金额" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="按时间段明细" name="time">
          <el-table :data="timeData" style="width: 100%">
            <el-table-column prop="period" label="时间段" />
            <el-table-column prop="applicationCount" label="申请量" />
            <el-table-column prop="approvedCount" label="通过量" />
            <el-table-column prop="approvalRate" label="通过率(%)" />
            <el-table-column prop="disbursementAmount" label="放款金额" />
            <el-table-column prop="rejectedCount" label="驳回量" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import echarts from '../../utils/echarts.js'
import {
  getDashboard,
  getByProduct,
  getByManager,
  getByTime,
  exportStatistics,
} from '../../api/statistic.js'
import { ElMessage } from 'element-plus'

const dashboard = reactive({
  pendingApplicationCount: 0,
  monthApplicationCount: 0,
  monthDisbursementAmount: 0,
  averageApprovalDurationHours: 0,
})

const productData = ref([])
const managerData = ref([])
const timeData = ref([])

const productChartRef = ref(null)
const managerChartRef = ref(null)
const timeChartRef = ref(null)

let productChart = null
let managerChart = null
let timeChart = null

const router = useRouter()

const activeTab = ref('product')
const exportType = ref('excel')
const exportDimension = ref('product')
const exportDateRange = ref([])
const exportGroupBy = ref('DAY')

async function loadDashboard() {
  try {
    const res = await getDashboard()
    if (res.data.code === 200) {
      Object.assign(dashboard, res.data.data)
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadChartsData() {
  try {
    const [productRes, managerRes, timeRes] = await Promise.all([
      getByProduct(),
      getByManager(),
      getByTime({ groupBy: 'DAY' }),
    ])
    if (productRes.data.code === 200) {
      productData.value = productRes.data.data || []
    }
    if (managerRes.data.code === 200) {
      managerData.value = managerRes.data.data || []
    }
    if (timeRes.data.code === 200) {
      timeData.value = timeRes.data.data || []
    }
    initCharts()
  } catch (e) {
    console.error(e)
  }
}

function initCharts() {
  if (productChartRef.value) {
    productChart = echarts.init(productChartRef.value)
    productChart.setOption({
      title: { text: '按产品统计', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: productData.value.map((item) => item.productName),
        axisLabel: { rotate: 30 },
      },
      yAxis: { type: 'value' },
      series: [
        {
          data: productData.value.map((item) => item.applicationCount),
          type: 'bar',
          name: '申请量',
        },
      ],
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    })
  }

  if (managerChartRef.value) {
    managerChart = echarts.init(managerChartRef.value)
    managerChart.setOption({
      title: { text: '按客户经理统计', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: '60%',
          data: managerData.value.map((item) => ({
            name: item.managerName,
            value: item.applicationCount,
          })),
        },
      ],
    })
  }

  if (timeChartRef.value) {
    timeChart = echarts.init(timeChartRef.value)
    timeChart.setOption({
      title: { text: '按时间段趋势', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: timeData.value.map((item) => item.period),
      },
      yAxis: { type: 'value' },
      series: [
        {
          data: timeData.value.map((item) => item.applicationCount),
          type: 'line',
          name: '申请量',
          smooth: true,
        },
      ],
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    })
  }
}

function formatMoney(value) {
  if (value === undefined || value === null) {
    return '-'
  }
  return '¥ ' + Number(value).toLocaleString()
}

async function handleExport() {
  try {
    const params = {
      type: exportType.value,
      dimension: exportDimension.value,
      groupBy: exportGroupBy.value,
    }
    if (
      exportDimension.value === 'time' &&
      exportDateRange.value &&
      exportDateRange.value.length === 2
    ) {
      params.startDate = exportDateRange.value[0]
      params.endDate = exportDateRange.value[1]
    }
    const res = await exportStatistics(params)
    const blob = new Blob([res.data], {
      type: res.headers['content-type'] || 'application/octet-stream',
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const ext = exportType.value === 'excel' ? 'xlsx' : 'csv'
    link.download = `${exportDimension.value}_statistics.${ext}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
    console.error(e)
  }
}

function handleResize() {
  productChart && productChart.resize()
  managerChart && managerChart.resize()
  timeChart && timeChart.resize()
}

function goHome() {
  router.push('/admin/home')
}

onMounted(() => {
  loadDashboard()
  loadChartsData()
  window.addEventListener('resize', handleResize)
})

watch(activeTab, () => {
  nextTick(() => {
    handleResize()
  })
})
</script>

<style scoped>
.dashboard {
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

.metrics {
  margin-bottom: 20px;
}

.metric-title {
  color: #999;
  font-size: 14px;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.charts {
  margin-bottom: 20px;
}

.chart {
  height: 300px;
  width: 100%;
}

.export-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.export-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
</style>
