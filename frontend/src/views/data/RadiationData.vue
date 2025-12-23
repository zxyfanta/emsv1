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

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="deviceCode" label="设备编码" width="120" />
        <el-table-column prop="CPM" label="CPM值" width="100" />
        <el-table-column prop="Batvolt" label="电池电压(mV)" width="120" />
        <el-table-column prop="temperature" label="温度(°C)" width="100">
          <template #default="{ row }">
            {{ parseJsonData(row.rawData)?.temperature || '-' }}
          </template>
        </el-table-column>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getRadiationDataList } from '@/api/data'
import { getDeviceList } from '@/api/device'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref([])
const radiationDevices = ref([])
const dateRange = ref([])

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
  ElMessage.info('详情功能待实现')
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

onMounted(() => {
  loadDevices()
  loadData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}
</style>
