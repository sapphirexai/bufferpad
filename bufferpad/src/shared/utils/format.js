export function formatDateValue(value) {
  if (!value) return ''
  return new Date(value).toLocaleString()
}

export function tableDateFormatter(row, column, cellValue) {
  return formatDateValue(cellValue)
}

export function centerCellStyle() {
  return 'text-align:center'
}

export function formatScannerPosition(scannerSeq, scannerPosition) {
  if (scannerPosition) return scannerPosition

  const legacyNames = {
    1: '上',
    2: '下',
    3: '间层1',
    4: '间层2'
  }
  if (legacyNames[scannerSeq]) return legacyNames[scannerSeq]
  return scannerSeq ? '位置ID ' + scannerSeq : '-'
}

export function scannerPositionClass(scannerSeq, scannerPosition) {
  if (scannerPosition) {
    if (scannerPosition.indexOf('上') > -1) return 'success'
    if (scannerPosition.indexOf('下') > -1) return 'error'
    return 'warning'
  }

  if (Number(scannerSeq) === 1) return 'success'
  if (Number(scannerSeq) === 2) return 'error'
  return scannerSeq ? 'warning' : ''
}

