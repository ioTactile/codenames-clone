<script setup lang="ts">
import type { Player, Room } from '@/domain/types'
import { roomService } from '@/application/roomService'
import { ref } from 'vue'
import { useWebsocketStore } from '@/stores/websocket'

const props = defineProps<{
  id: number
  users: Player[]
  isHost: boolean
  roomStatus: Room['status']
}>()

const websocketStore = useWebsocketStore()
const isPlayerMenuOpen = ref<number | null>(null)

const togglePlayerMenu = (playerIndex: number): void => {
  isPlayerMenuOpen.value = isPlayerMenuOpen.value === playerIndex ? null : playerIndex
}

const copyLink = (): void => {
  const link = document.getElementById('link') as HTMLInputElement
  link.select()
  document.execCommand('copy')
}

const shufflePlayers = async (): Promise<void> => {
  if (!props.isHost) return
  try {
    await roomService.shufflePlayers(props.id, websocketStore.handleUserActivity)
  } catch (error) {
    console.error(error)
  }
}

const resetPlayers = async (): Promise<void> => {
  if (!props.isHost) return
  try {
    await roomService.resetPlayers(props.id, websocketStore.handleUserActivity)
  } catch (error) {
    console.error(error)
  }
}

const changeHost = async (name: string): Promise<void> => {
  if (!props.isHost) return
  try {
    await roomService.changeHost(props.id, name, websocketStore.handleUserActivity)
  } catch (error) {
    console.error(error)
  }
}

const kickPlayer = async (name: string): Promise<void> => {
  if (!props.isHost) return
  try {
    await roomService.leave(props.id, name, websocketStore.handleUserActivity)
  } catch (error) {
    console.error(error)
  }
}
</script>

<template>
  <div class="menu-wrapper">
    <div class="border-ui shadow-bottom rounded-xl bg-white">
      <div class="flex flex-col items-center p-4">
        <h3 class="mb-2 text-center font-bold text-blue-700">
          Invitez des joueurs en leur envoyant ce lien :
        </h3>
        <input
          id="link"
          type="text"
          :value="`http://localhost:8080/room/${props.id}`"
          class="shadow-inset mb-1 w-[80%] rounded-xl border p-2 text-center text-base text-black"
        />
        <button @click="copyLink" class="button shadow-bottom text-base">
          Copier le lien dans le presse-papier.
        </button>
      </div>
      <hr class="border-gray-300" />
      <div class="bg-gray-200 px-4 pt-4 pb-2">
        <h3 class="mb-2 text-center font-bold">Joueurs dans ce salon</h3>
        <div class="flex flex-wrap items-start justify-start">
          <div class="relative" v-for="(player, i) in users" :key="i">
            <button
              class="m-1 inline-flex cursor-default items-center justify-start rounded border-2 border-white bg-white px-1.5 py-0.5 font-bold text-black italic outline-none hover:outline-none active:outline-none"
              @click="togglePlayerMenu(i)"
              :class="{
                'hover:bg-yellow hover:cursor-pointer': isHost && i !== 0
              }"
            >
              <span class="bg-green-online mr-1 h-2 w-2 rounded-full"></span>
              <span>{{ player.name }}</span>
            </button>
            <div v-if="isPlayerMenuOpen === i && i !== 0 && isHost" class="player-details">
              <div class="border-ui shadow-bottom rounded-xl bg-white px-2 py-4">
                <h3 class="mb-2 text-center text-base font-bold">{{ player.name }}</h3>
                <div class="flex flex-wrap justify-center">
                  <div class="m-0.5">
                    <button class="button shadow-bottom text-base" @click="changeHost(player.name)">
                      Faire devenir hôte
                    </button>
                  </div>
                  <div class="m-0.5">
                    <button class="button shadow-bottom text-base" @click="kickPlayer(player.name)">
                      Expulser le joueur
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template v-if="isHost && roomStatus === 'PENDING'">
        <hr class="border-gray-300" />
        <div class="rounded-br-xl rounded-bl-xl bg-gray-200 p-2 text-center">
          <div class="m-2 inline-block">
            <button @click="shufflePlayers" class="button">Répartir les équipes au hasard</button>
          </div>
          <div class="m-2 inline-block">
            <button @click="resetPlayers" class="button">Réinitialiser les équipes</button>
          </div>
        </div>
      </template>
      <div v-else class="rounded-br-xl rounded-bl-xl bg-gray-200 p-2 text-center"></div>
    </div>
  </div>
</template>

<style scoped>
.menu-wrapper {
  z-index: 9999;
  position: absolute;
  width: 380px;
  inset: 0px auto auto 0px;
  transform: translateX(-1px) translateY(45px) translateZ(0px);
}

.player-details {
  z-index: 9999;
  position: absolute;
  width: 350px;
  inset: 0px auto auto 0px;
  transform: translateX(-90px) translateY(40px) translateZ(0px);
}
</style>
