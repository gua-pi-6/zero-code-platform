export type IdLike = string | number | undefined | null

export const toIdString = (value: IdLike): string => {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value)
}

export const hasId = (value: IdLike): boolean => {
  return toIdString(value) !== ''
}

export const sameId = (left: IdLike, right: IdLike): boolean => {
  const leftId = toIdString(left)
  const rightId = toIdString(right)
  return leftId !== '' && leftId === rightId
}
