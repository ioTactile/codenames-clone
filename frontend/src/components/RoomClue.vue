<script setup lang="ts">
import type { Clue, Player, Room } from '@/domain/types'
import { computed, ref, watch } from 'vue'
import { roomService } from '@/application/roomService'
import { isOperativeTurn, isSpyTurn } from '@/domain/roomRules'
import Social from '@/components/SocialMedia.vue'
import { useWebsocketStore } from '@/stores/websocket'

const props = defineProps<{
  room: Room
  user: Player | null
  windowWidth: number
}>()

const websocketStore = useWebsocketStore()
const isClueModalOpen = ref<boolean>(false)
const clueNumber = ref<number>(0)
const clueName = ref<string>('')
const clueOptions = ref<number[]>([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])
const showClue = ref<boolean>(false)

const isUserSpyTurn = computed((): boolean => isSpyTurn(props.room, props.user))

const isUserOperativeTurn = computed((): boolean => isOperativeTurn(props.room, props.user))

const getLastClue = computed((): Clue => {
  const lastClue = props.room.clues[props.room.clues.length - 1]
  return lastClue
})

const getRoleTurn = (): void => {
  if (props.room.roleTurn === 'OPERATIVE') {
    showClue.value = true
  } else {
    showClue.value = false
  }
}

watch(
  () => props.room.roleTurn,
  () => {
    getRoleTurn()
  },
)

const sendClue = async (): Promise<void> => {
  if (!isUserSpyTurn.value) return
  if (!clueName.value) return
  if (!clueNumber.value) return

  try {
    await roomService.addClue(
      props.room.id,
      props.user!.name,
      {
        clueName: clueName.value,
        attempts: clueNumber.value,
        remaining: clueNumber.value + 1,
        spyName: props.user!.name,
      },
      websocketStore.handleUserActivity,
    )
  } catch (error) {
    console.error(error)
  }
}

const teamTurn = async (): Promise<void> => {
  if (!isUserOperativeTurn.value) return

  try {
    await roomService.manualTeamTurn(
      props.room.id,
      props.user!.name,
      websocketStore.handleUserActivity,
    )
  } catch (error) {
    console.error(error)
  }
}
</script>

<template>
  <template v-if="isUserSpyTurn">
    <div class="clueboard">
      <div class="flex flex-col">
        <Social v-if="windowWidth <= 500" />
        <div class="flex w-full" :class="windowWidth > 500 ? 'justify-center' : 'justify-end'">
          <div
            class="flex w-10/12 max-w-screen-md flex-wrap items-center justify-center landscape:w-full"
          >
            <div class="flex-grow">
              <input
                v-model="clueName"
                name="clue"
                autocomplete="off"
                type="text"
                tabindex="0"
                placeholder="Tapez votre indice ici"
                @input="clueName = clueName.toUpperCase()"
                class="shadow-bottom h-10 w-full rounded-xl border-none px-3 text-2xl text-black focus:outline-none"
              />
            </div>
            <div class="mx-2 flex-none">
              <div class="relative">
                <div
                  class="border-ui shadow-bottom flex h-10 w-10 cursor-pointer items-center justify-center rounded-lg bg-white text-2xl"
                  @click="isClueModalOpen = !isClueModalOpen"
                >
                  {{ clueNumber || '–' }}
                </div>
                <div v-if="isClueModalOpen" class="clue-number-modal absolute z-50">
                  <div class="border-ui shadow-bottom flex rounded-lg bg-white p-2">
                    <div v-for="number in clueOptions" :key="number">
                      <div
                        class="clue-number-option hover:bg-yellow inline-block cursor-pointer rounded-lg text-2xl leading-6"
                        @click="((clueNumber = number), (isClueModalOpen = false))"
                      >
                        {{ number }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="relative flex-initial portrait:pt-2">
              <button
                class="button color-green shadow-bottom text-base sm:text-2xl"
                @click="sendClue"
              >
                Donner un indice
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>
  <template v-if="room.roleTurn === 'OPERATIVE'">
    <div class="bottom">
      <div class="flex flex-col">
        <Social v-if="windowWidth <= 500" />
        <div class="flex w-full flex-col items-center justify-center text-xl landscape:text-3xl">
          <Transition name="slide-fade">
            <div v-show="getLastClue" class="flex items-center justify-center">
              <span
                :class="{
                  'ring-blue-light': props.room.teamTurn === 'BLUE',
                  'ring-red-light': props.room.teamTurn === 'RED',
                }"
                class="ml-1 rounded-lg bg-white px-3 py-1 font-bold uppercase ring-4 select-text landscape:ml-2 landscape:px-4 landscape:py-2 landscape:ring-8"
              >
                {{ getLastClue?.clueName }}
              </span>
              <span
                :class="{
                  'ring-blue-light': props.room.teamTurn === 'BLUE',
                  'ring-red-light': props.room.teamTurn === 'RED',
                }"
                class="ml-1 rounded-lg bg-white px-3 py-1 font-bold uppercase ring-4 select-text landscape:ml-2 landscape:px-4 landscape:py-2 landscape:ring-8"
              >
                {{ getLastClue?.attempts }}
              </span>
            </div>
          </Transition>
          <div v-if="isUserOperativeTurn" class="mt-2 landscape:mt-3">
            <button class="button shadow-button text-base" @click="teamTurn">
              Fini de deviner
            </button>
          </div>
        </div>
      </div>
    </div>
  </template>
</template>

<style scoped>
.clueboard {
  position: absolute;
  top: 860px;
  z-index: 70;
  width: 100%;
}

@media screen and (max-width: 500px) {
  .clueboard {
    top: 430px;
    height: 90px;
    box-sizing: border-box;
  }
}

.bottom {
  z-index: 70;
  position: absolute;
  width: 100%;
  top: 860px;
  transition: top 1s;
}

@media screen and (max-width: 500px) {
  .bottom {
    top: 430px;
    height: 90px;
    box-sizing: border-box;
  }
}

.social {
  position: absolute;
  bottom: 0;
  right: 0;
  z-index: 70;
}

.clue-number-modal {
  inset: 0px auto auto 0px;
  transform: translateX(-55%) translateY(-4rem) translateZ(0px);
}

@media screen and (max-width: 500px) {
  .clue-number-modal {
    transform: translateX(-90%) translateY(-4rem) translateZ(0px);
  }
}

.clue-number-option {
  padding: 0.3rem 0.5rem 0.25rem;
  margin: 0.1rem 0.2rem;
  border: 1px solid gray;
  box-shadow: rgba(0, 0, 0, 0.5) 0.1rem 0.1rem 1px 0px;
}

.slide-fade-enter-active {
  animation: appearing-in 1s;
}

.slide-fade-enter-from {
  top: 460px;
}

@keyframes appearing-in {
  0% {
    transform: scale(1.5);
  }
  100% {
    transform: scale(1);
  }
}
</style>
