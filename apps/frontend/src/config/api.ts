export const API_BASE_URL = process.env.NUXT_API_BASE_URL || 'http://localhost:8088/api/v1'

export const API_ROUTES = {
  USER_INFO: `${API_BASE_URL}/user/me`,
  LOGIN: `${API_BASE_URL}/auth/authenticate`,
  REGISTER: `${API_BASE_URL}/auth/register`,
  OAUTH_BASE: `${API_BASE_URL}/oauth2/authorization`,
  ACTIVATE_ACCOUNT: `${API_BASE_URL}/auth/activate-account`,
}