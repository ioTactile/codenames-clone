import type { Clue, Room, RoomAction, Role, Team } from '@/domain/types'
import * as roomHttp from '@/infrastructure/http/roomHttp'

export type RoomActivityHook = () => void

async function runAction(
  roomId: number,
  payload: RoomAction,
  onActivity?: RoomActivityHook
): Promise<void> {
  await roomHttp.sendRoomAction(roomId, payload)
  onActivity?.()
}

export const roomService = {
  createRoom: (username: string) => roomHttp.createRoom(username),
  getRoom: (roomId: number) => roomHttp.getRoom(roomId),

  join: (roomId: number, username: string, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'join', username }, onActivity),

  leave: (roomId: number, username: string, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'leave', username }, onActivity),

  start: (roomId: number, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'start' }, onActivity),

  shufflePlayers: (roomId: number, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'shuffle-players' }, onActivity),

  resetPlayers: (roomId: number, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'reset-players' }, onActivity),

  changeHost: (roomId: number, username: string, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'change-host', username }, onActivity),

  selectTeam: (roomId: number, username: string, team: Team, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'select-team', username, team }, onActivity),

  selectRole: (
    roomId: number,
    username: string,
    role: Role,
    team: Team,
    onActivity?: RoomActivityHook
  ) => runAction(roomId, { action: 'select-role', username, role, team }, onActivity),

  changeUsername: (
    roomId: number,
    username: string,
    newUsername: string,
    onActivity?: RoomActivityHook
  ) => runAction(roomId, { action: 'change-username', username, newUsername }, onActivity),

  manualTeamTurn: (roomId: number, username: string, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'manual-team-turn', username }, onActivity),

  selectWord: (roomId: number, username: string, wordname: string, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'select-word', username, wordname }, onActivity),

  clickWord: (roomId: number, username: string, wordname: string, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'click-word', username, wordname }, onActivity),

  addClue: (roomId: number, username: string, clue: Clue, onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'add-clue', username, clue }, onActivity),

  replay: (roomId: number, usernames: string[], onActivity?: RoomActivityHook) =>
    runAction(roomId, { action: 'replay', usernames }, onActivity)
}

export type { Room }
