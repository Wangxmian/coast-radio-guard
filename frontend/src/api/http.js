import axios from 'axios'

export const backendClient = axios.create({
  baseURL: '/backend-api',
  timeout: 10000
})

export const aiClient = axios.create({
  baseURL: '/ai-api',
  timeout: 10000
})
