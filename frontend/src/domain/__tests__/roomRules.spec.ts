import { describe, expect, it } from 'vitest'
import {
  canClickWord,
  didTeamWin,
  hasNoRole,
  isHost,
  isOperativeTurn,
  isPlayerTurn,
  isSpyTurn,
  isWinningStatus,
  playersByTeamRole,
} from '@/domain/roomRules'
import type { Player, Room } from '@/domain/types'

const baseRoom = (overrides: Partial<Room> = {}): Room => ({
  id: 1,
  players: [
    { name: 'host', playerTeam: 'RED', playerRole: 'SPYMASTER' },
    { name: 'agent', playerTeam: 'RED', playerRole: 'OPERATIVE' },
  ],
  words: [],
  clues: [],
  teamTurn: 'RED',
  roleTurn: 'OPERATIVE',
  status: 'IN_PROGRESS',
  redRemainingWords: 9,
  blueRemainingWords: 8,
  isBlackCardSelected: false,
  createdAt: '',
  updatedAt: '',
  ...overrides,
})

describe('roomRules', () => {
  it('detects host as first player', () => {
    const room = baseRoom()
    expect(isHost(room, 'host')).toBe(true)
    expect(isHost(room, 'agent')).toBe(false)
  })

  it('detects operative turn', () => {
    const room = baseRoom()
    const agent = room.players[1]
    expect(isOperativeTurn(room, agent)).toBe(true)
    expect(isSpyTurn(room, agent)).toBe(false)
    expect(canClickWord(room, agent)).toBe(true)
  })

  it('detects spy turn', () => {
    const room = baseRoom({ roleTurn: 'SPYMASTER' })
    const spy = room.players[0]
    expect(isSpyTurn(room, spy)).toBe(true)
    expect(isPlayerTurn(room, spy)).toBe(true)
  })

  it('filters players by team and role', () => {
    const room = baseRoom()
    expect(playersByTeamRole(room, 'RED', 'OPERATIVE')).toHaveLength(1)
    expect(playersByTeamRole(room, 'BLUE', 'OPERATIVE')).toHaveLength(0)
  })

  it('detects winning status and team win', () => {
    const room = baseRoom({ status: 'RED_TEAM_WINS' })
    expect(isWinningStatus(room.status)).toBe(true)
    expect(didTeamWin(room, 'RED')).toBe(true)
    expect(didTeamWin(room, 'BLUE')).toBe(false)
  })

  it('detects players without role', () => {
    const room = baseRoom({
      players: [{ name: 'newbie', playerTeam: 'NONE', playerRole: 'NONE' } as Player],
    })
    expect(hasNoRole(room, 'newbie')).toBe(true)
  })
})
