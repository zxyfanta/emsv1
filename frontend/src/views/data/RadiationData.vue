<template>
  <div class="radiation-data">
    <el-card>
      <template #header>
        <span>辐射监测数据</span>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="设备编码">
          <el-select v-model="searchForm.deviceCode" placeholder="请选择" clearable>
            <el-option
              v-for="device in radiationDevices"
              :key="device.deviceCode"
              :label="device.deviceName"
              :value="device.deviceCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据图表 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="24">
          <el-card>
            <template #header>
              <span>CPM 趋势图</span>
            </template>
            <div ref="chartRef" style="width: 100%; height: 400px"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="deviceCode" label="设备编码" width="120" />
        <el-table-column prop="cpm" label="CPM值" width="100" />
        <el-table-column prop="batvolt" label="电池电压(mV)" width="120" />
        <el-table-column prop="time" label="数据时间" width="180" />
        <el-table-column prop="recordTime" label="接收时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.recordTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 数据详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="辐射数据详情"
      width="600px"
    >
      <el-descriptions :column="1" border v-if="selectedData">
        <el-descriptions-item label="设备编码">
          {{ selectedData.deviceCode }}
        </el-descriptions-item>
        <el-descriptions-item label="CPM值">
          {{ selectedData.cpm }}
        </el-descriptions-item>
        <el-descriptions-item label="电池电压">
          {{ selectedData.batvolt }} mV
        </el-descriptions-item>
        <el-descriptions-item label="数据时间">
          {{ selectedData.time }}
        </el-descriptions-item>
        <el-descriptions-item label="接收时间">
          {{ formatDate(selectedData.recordTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="原始数据" v-if="selectedData.rawData">
          <pre class="raw-data">{{ selectedData.rawData }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getRadiationDataList } from '@/api/data'
import { getDeviceList } from '@/api/device'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import * as echarts from 'echarts'

const loading = ref(false)
const tableData = ref([])
const radiationDevices = ref([])
const dateRange = ref([])
const detailDialogVisible = ref(false)
const selectedData = ref(null)
const chartRef = ref(null)
let chartInstance = null

const searchForm = reactive({
  deviceCode: '',
  startTime: '',
  endTime: ''
})

const pagination = reactive({
  page: 0,
  size: 20,
  total: 0
})

const loadDevices = async () => {
  try {
    const res = await getDeviceList({ page: 0, size: 1000 })
    if (res.status === 200) {
      radiationDevices.value = res.data.content.filter(
        d => d.deviceType === 'RADIATION_MONITOR'
      )
    }
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    }
    const res = await getRadiationDataList(params)
    if (res.status === 200) {
      tableData.value = res.data.content
      pagination.total = res.data.totalElements
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }

  // 数据加载完成后更新图表
  await nextTick()
  updateChart()
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option = {
    title: {
      text: '辐射数据 CPM 趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const data = params[0]
        return `${data.axisValue}<br/>CPM: ${data.value}`
      }
    },
    xAxis: {
      type: 'category',
      data: [],
      axisLabel: {
        rotate: 45,
        formatter: (value) => {
          return dayjs(value).format('MM-DD HH:mm')
        }
      }
    },
    yAxis: {
      type: 'value',
      name: 'CPM',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [{
      name: 'CPM',
      type: 'line',
      data: [],
      smooth: true,
      lineStyle: {
        width: 2,
        color: '#409EFF'
      },
      itemStyle: {
        color: '#409EFF'
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      }
    }],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    }
  }

  chartInstance.setOption(option)
}

// 更新图表数据
const updateChart = () => {
  if (!chartInstance || !tableData.value.length) return

  // 按时间排序
  const sortedData = [...tableData.value].sort((a, b) => {
    return new Date(a.recordTime) - new Date(b.recordTime)
  })

  const times = sortedData.map(item => item.recordTime)
  const cpmValues = sortedData.map(item => item.cpm)

  chartInstance.setOption({
    xAxis: {
      data: times
    },
    series: [{
      data: cpmValues
    }]
  })
}

const handleSearch = () => {
  if (dateRange.value && dateRange.value.length === 2) {
    searchForm.startTime = dateRange.value[0]
    searchForm.endTime = dateRange.value[1]
  } else {
    searchForm.startTime = ''
    searchForm.endTime = ''
  }
  pagination.page = 0
  loadData()
}

const handleReset = () => {
  searchForm.deviceCode = ''
  searchForm.startTime = ''
  searchForm.endTime = ''
  dateRange.value = []
  handleSearch()
}

const handleViewDetail = (row) => {
  selectedData.value = row
  detailDialogVisible.value = true
}

const parseJsonData = (rawData) => {
  try {
    return JSON.parse(rawData)
  } catch {
    return null
  }
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(async () => {
  await loadDevices()
  await loadData()

  // 初始化图表
  await nextTick()
  initChart()
})

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

.raw-data {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
}
</style>
