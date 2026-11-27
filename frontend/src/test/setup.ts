import '@testing-library/jest-dom'

if (typeof window !== 'undefined' && typeof window.localStorage?.clear !== 'function') {
  const stores = new WeakMap<Storage, Map<string, string>>()

  const getStore = (target: Storage): Map<string, string> => {
    let store = stores.get(target)
    if (!store) {
      store = new Map<string, string>()
      stores.set(target, store)
    }
    return store
  }

  Object.defineProperties(Storage.prototype, {
    length: {
      configurable: true,
      get(this: Storage) {
        return getStore(this).size
      }
    },
    key: {
      configurable: true,
      writable: true,
      value(this: Storage, index: number) {
        return Array.from(getStore(this).keys())[index] ?? null
      }
    },
    getItem: {
      configurable: true,
      writable: true,
      value(this: Storage, key: string) {
        return getStore(this).get(String(key)) ?? null
      }
    },
    setItem: {
      configurable: true,
      writable: true,
      value(this: Storage, key: string, value: string) {
        getStore(this).set(String(key), String(value))
      }
    },
    removeItem: {
      configurable: true,
      writable: true,
      value(this: Storage, key: string) {
        getStore(this).delete(String(key))
      }
    },
    clear: {
      configurable: true,
      writable: true,
      value(this: Storage) {
        getStore(this).clear()
      }
    }
  })

  const storageInstance = Object.create(Storage.prototype) as Storage
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    writable: true,
    value: storageInstance
  })
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    writable: true,
    value: storageInstance
  })
}
