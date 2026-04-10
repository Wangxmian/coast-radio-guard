import { backendClient, aiClient } from './http'

function normalizeHealth(resp) {
  if (resp && typeof resp.status === 'string') {
    return { status: resp.status, raw: resp }
  }
  if (resp && resp.data && typeof resp.data.status === 'string') {
    return { status: resp.data.status, raw: resp }
  }
  return { status: 'UNKNOWN', raw: resp }
}

export async function checkBackendHealth() {
  const { data } = await backendClient.get('/health')
  return normalizeHealth(data)
}

export async function checkAiHealth() {
  const { data } = await aiClient.get('/health')
  return normalizeHealth(data)
}
