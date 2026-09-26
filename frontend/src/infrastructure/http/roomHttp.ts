import type { Room, RoomAction } from '@/domain/types'
import { apiFetchData } from './apiClient'

export async function createRoom(username: string): Promise<Room> {
  return apiFetchData<Room>('room/create', 'POST', { username })
}

export async function getRoom(roomId: number): Promise<Room> {
  return apiFetchData<Room>(`room/${roomId}`, 'GET')
}

export async function sendRoomAction(roomId: number, payload: RoomAction): Promise<void> {
  await apiFetchData(`room/${roomId}`, 'PUT', payload as unknown as Record<string, unknown>)
}
