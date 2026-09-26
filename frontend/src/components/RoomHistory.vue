<script setup lang="ts">
import type { Room } from '@/domain/types'

defineProps<{
  room: Room | null
}>()
</script>

<template>
  <div
    class="landscape:border-ui landscape:shadow-bottom flex flex-1 flex-col overflow-hidden bg-white opacity-50 transition-all duration-400 landscape:mt-4 landscape:w-full landscape:flex-auto landscape:rounded-xl"
  >
    <div class="flex-none">
      <div class="text-center text-base">Historique</div>
      <div class="flex-auto overflow-y-auto landscape:rounded-b-xl"></div>
    </div>
  </div>
</template>
