export const findByText = (wrap: any, selector: string, text: string) => {
  return wrap
    .findAll(selector)
    .filter((n: any) => n.text().match(text))
    .at(0)
}
export const buildFetchList = (items: Record<string, any>) => {
  return (_: any, c: any) => {
    const keys = Object.keys(items)
    const foundKey = keys.find((d) => c?.body?.includes(d)) || ''

    return Promise.resolve({ json: () => Promise.resolve({ data: items[foundKey] ?? {} }), ok: true }) as any
  }
}
