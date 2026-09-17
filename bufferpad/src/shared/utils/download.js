export function getFilenameFromDisposition(disposition, fallback) {
  if (!disposition) return fallback || 'download'
  const match = disposition.match(/filename\*?=(?:UTF-8'')?("?)([^";]+)\1/i)
  if (!match) return fallback || 'download'
  try {
    return decodeURIComponent(match[2])
  } catch (error) {
    return match[2]
  }
}

export function downloadBlob(data, filename) {
  const blob = data instanceof Blob ? data : new Blob([data])
  const href = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = href
  link.download = filename || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(href)
}

