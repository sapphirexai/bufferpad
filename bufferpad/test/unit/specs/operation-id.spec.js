import { createOperationId } from '@/shared/utils/operation-id'

describe('operation id', () => {
  it('creates a non-empty id for a manual scan', () => {
    const operationId = createOperationId()
    expect(operationId).toEqual(expect.any(String))
    expect(operationId.length).toBeGreaterThan(10)
  })
})
