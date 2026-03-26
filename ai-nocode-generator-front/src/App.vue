<template>
  <div id="app">
    <BasicLayout />
  </div>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

import { healthCheck } from '@/api/healthController.ts'

healthCheck().then((res) => {
  console.log(res)
})
</script>

<style scoped></style>
