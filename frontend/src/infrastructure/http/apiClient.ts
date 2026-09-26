export async function apiFetchData<T = unknown>(
  url: string,
  method: string,
  bodyData?: Record<string, unknown>,
): Promise<T> {
  const fullUrl = import.meta.env.DEV
    ? import.meta.env.VITE_API_URL_DEV + url
    : import.meta.env.VITE_API_URL_PROD + url

  const headers = new Headers({
    'Content-Type': 'application/json',
  })

  const options: RequestInit = {
    method,
    headers,
    body: bodyData ? JSON.stringify(bodyData) : undefined,
  }

  const response = await fetch(fullUrl, options)

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  if (response.headers.get('Content-Type')?.includes('application/json')) {
    return (await response.json()) as T
  }

  return (await response.text()) as T
}
