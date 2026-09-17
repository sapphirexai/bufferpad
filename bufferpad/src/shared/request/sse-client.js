export default class SseClient {
  constructor(url, onMessage, onError, onOpen) {
    this.url = url
    this.onMessage = onMessage
    this.onError = onError
    this.onOpen = onOpen
    this.eventSource = null
    this.open()
  }

  open() {
    if (!('EventSource' in window)) {
      throw new Error('浏览器不支持EventSource对象')
    }

    this.close()
    this.eventSource = new EventSource(this.url, { withCredentials: true })

    this.eventSource.onopen = event => {
      if (typeof this.onOpen === 'function') this.onOpen(event)
    }

    this.eventSource.onmessage = event => {
      if (!event.data || typeof event.data !== 'string') return
      try {
        const data = JSON.parse(event.data)
        if (typeof this.onMessage === 'function') this.onMessage(data)
      } catch (error) {
        if (typeof this.onError === 'function') this.onError(error)
      }
    }

    this.eventSource.onerror = error => {
      if (typeof this.onError === 'function') this.onError(error)
      window.dispatchEvent(new CustomEvent('bufferpad:verify-session'))
    }
  }

  close() {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
  }
}
