<script setup lang="ts">
import type { Room, Player } from '@/domain/types'
import { roomService } from '@/application/roomService'
import { hasNoRole, playersByTeamRole } from '@/domain/roomRules'
import { computed } from 'vue'
import { useWebsocketStore } from '@/stores/websocket'

const props = defineProps<{
  room: Room
  user: Player | null
}>()

const websocketStore = useWebsocketStore()

const blueAgents = computed((): Player[] => playersByTeamRole(props.room, 'BLUE', 'OPERATIVE'))

const blueSpymaster = computed((): Player[] => playersByTeamRole(props.room, 'BLUE', 'SPYMASTER'))

const noRolePlayer = computed((): boolean => hasNoRole(props.room, props.user?.name))

const joinRole = async (role: 'OPERATIVE' | 'SPYMASTER'): Promise<void> => {
  if (props.user?.playerTeam === 'RED' || !props.user) return
  try {
    await roomService.selectRole(
      props.room.id,
      props.user.name,
      role,
      'BLUE',
      websocketStore.handleUserActivity,
    )
  } catch (error) {
    console.error(error)
  }
}

const getCharacter = (): string => {
  let number = 0
  const random = Math.floor(Math.random() * 8) + 1
  number = (random / 8) * 100
  return `background-position-y: ${number}%;`
}
</script>

<template>
  <div
    class="landscape:border-ui bg-blue-team-bg landscape:shadow-bottom flex-1 overflow-y-auto landscape:flex-none landscape:rounded-xl"
  >
    <div class="box-border w-full p-2">
      <section class="relative h-12 landscape:h-36">
        <span
          class="score absolute top-6 left-[20px] w-12 text-center text-white landscape:top-14"
          >{{ room.blueRemainingWords || '-' }}</span
        >
        <div
          class="card-background absolute top-0 right-0 z-10 portrait:hidden"
          style="background-position-y: 0%"
        >
          <div class="card-character absolute bottom-0 left-1/2" :style="getCharacter()"></div>
        </div>
      </section>
      <section>
        <span class="text-blue-light relative mt-1 w-full text-base">Agents</span>
        <div v-if="blueAgents.length" class="flex flex-wrap items-start justify-start">
          <div
            v-for="(user, i) in blueAgents"
            :key="i"
            class="user-wrapper mr-1 mb-1 inline-block truncate rounded border border-white/40 px-1 py-1 leading-none font-bold text-white"
          >
            {{ user.name }}
          </div>
        </div>
        <div v-else class="pl-2 text-white">–</div>
        <button
          v-if="noRolePlayer && (user?.playerTeam === 'RED' || user?.playerTeam === 'NONE')"
          class="button shadow-bottom text-base"
          @click="joinRole('OPERATIVE')"
        >
          Rejoindre en tant qu'agent
        </button>
      </section>
      <section>
        <span class="text-blue-light relative mt-1 w-full text-base">Espions</span>
        <div v-if="blueSpymaster.length" class="flex flex-wrap items-start justify-start">
          <div
            v-for="(user, i) in blueSpymaster"
            :key="i"
            class="user-wrapper mr-1 mb-1 inline-block truncate rounded border border-white/40 px-1 py-1 leading-none font-bold text-white"
          >
            {{ user.name }}
          </div>
        </div>
        <div v-else class="pl-2 text-white">–</div>
        <button
          v-if="
            !blueSpymaster.length && (user?.playerTeam === 'BLUE' || user?.playerTeam === 'NONE')
          "
          class="button shadow-bottom text-base"
          @click="joinRole('SPYMASTER')"
        >
          Rejoindre en tant qu'espion
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.score {
  font-size: 50px;
  font-weight: bold;
  transform: translateY(-50%);
  z-index: 10;
  text-shadow: rgba(0, 0, 0, 0.8) 0px 0px 0.8rem;
  font-feature-settings: 'tnum';
  font-variant-numeric: tabular-nums;
}

.user-wrapper {
  scale: 1;
  transform: translate3d(0, 0, 0);
  transform-origin: 50% 50% 0px;
}

.card-background {
  width: 208.32px;
  height: 134.4px;
  background-image: url('/images/backs.png');
  background-size: 100%;
  background-repeat: no-repeat;
}

.card-character {
  z-index: 100;
  width: 134px;
  height: 120px;
  transform: translateX(-50%);
  background-image: url('/images/blue.png');
  background-size: 100%;
  background-repeat: no-repeat;
}
</style>
