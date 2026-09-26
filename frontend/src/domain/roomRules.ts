import type { Player, Role, Room, Team } from './types'

export function isHost(room: Room, playerName: string | undefined | null): boolean {
  if (!playerName || room.players.length === 0) return false
  return room.players[0]?.name === playerName
}

export function findPlayer(room: Room, playerName: string | undefined | null): Player | undefined {
  if (!playerName) return undefined
  return room.players.find((player) => player.name === playerName)
}

export function isPlayerTurn(room: Room, player: Player | null | undefined): boolean {
  if (!player) return false
  return (
    player.playerTeam === room.teamTurn &&
    player.playerRole === room.roleTurn &&
    room.status === 'IN_PROGRESS'
  )
}

export function isSpyTurn(room: Room, player: Player | null | undefined): boolean {
  return (
    !!player &&
    player.playerRole === 'SPYMASTER' &&
    player.playerTeam === room.teamTurn &&
    room.roleTurn === 'SPYMASTER' &&
    room.status === 'IN_PROGRESS'
  )
}

export function isOperativeTurn(room: Room, player: Player | null | undefined): boolean {
  return (
    !!player &&
    player.playerRole === 'OPERATIVE' &&
    player.playerTeam === room.teamTurn &&
    room.roleTurn === 'OPERATIVE' &&
    room.status === 'IN_PROGRESS'
  )
}

export function canClickWord(room: Room, player: Player | null | undefined): boolean {
  return room.status === 'IN_PROGRESS' && isOperativeTurn(room, player)
}

export function playersByTeamRole(room: Room, team: Team, role: Role): Player[] {
  return room.players.filter((player) => player.playerTeam === team && player.playerRole === role)
}

export function hasNoRole(room: Room, playerName: string | undefined | null): boolean {
  if (!playerName) return false
  return room.players.some((player) => player.name === playerName && player.playerRole === 'NONE')
}

export function isWinningStatus(status: Room['status']): boolean {
  return status === 'RED_TEAM_WINS' || status === 'BLUE_TEAM_WINS'
}

export function didTeamWin(room: Room, team: Team): boolean {
  return (
    (team === 'RED' && room.status === 'RED_TEAM_WINS') ||
    (team === 'BLUE' && room.status === 'BLUE_TEAM_WINS')
  )
}

export function remainingWordsFor(room: Room, team: 'RED' | 'BLUE'): number {
  return team === 'RED' ? room.redRemainingWords : room.blueRemainingWords
}
