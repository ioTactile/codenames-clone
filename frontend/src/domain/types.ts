export type RoomStatus = 'NEW' | 'PENDING' | 'IN_PROGRESS' | 'RED_TEAM_WINS' | 'BLUE_TEAM_WINS'
export type Team = 'NONE' | 'BLUE' | 'RED'
export type Role = 'NONE' | 'SPYMASTER' | 'OPERATIVE' | 'SPECTATOR'
export type WordState = 'NOT_SELECTED' | 'SELECTED' | 'CLICKED'
export type WordColor = 'BLUE' | 'RED' | 'BLACK' | 'WHITE'

export type Room = {
  id: number
  players: Player[]
  words: Word[]
  clues: Clue[]
  teamTurn: 'BLUE' | 'RED'
  roleTurn: 'SPYMASTER' | 'OPERATIVE'
  status: RoomStatus
  redRemainingWords: number
  blueRemainingWords: number
  isBlackCardSelected: boolean
  createdAt: string
  updatedAt: string
}

export type Player = {
  name: string
  playerTeam: Team
  playerRole: Role
}

export type Word = {
  wordName: string
  selectedBy: string[]
  wordState: WordState
  wordColor: WordColor
}

export type Clue = {
  clueName: string
  attempts: number
  remaining: number
  spyName: string
}

export type User = {
  roomId: number
  username: string
}

export type RoomAction =
  | { action: 'join'; username: string }
  | { action: 'leave'; username: string }
  | { action: 'start' }
  | { action: 'shuffle-players' }
  | { action: 'reset-players' }
  | { action: 'change-host'; username: string }
  | { action: 'select-team'; username: string; team: Team }
  | { action: 'select-role'; username: string; role: Role; team: Team }
  | { action: 'change-username'; username: string; newUsername: string }
  | { action: 'manual-team-turn'; username: string }
  | { action: 'select-word'; username: string; wordname: string }
  | { action: 'click-word'; username: string; wordname: string }
  | {
      action: 'add-clue'
      username: string
      clue: Clue
    }
  | { action: 'replay'; usernames: string[] }
