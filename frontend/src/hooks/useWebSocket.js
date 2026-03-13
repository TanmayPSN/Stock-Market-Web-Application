import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useWebSocket() {
  const [prices,    setPrices]    = useState({})
  // prices = { "TCS": 3512.50, "INFY": 1448.00, ... }
  // Updated every 5 seconds from the backend simulator.

  const [connected, setConnected] = useState(false)
  const [flashMap,  setFlashMap]  = useState({})
  // flashMap = { "TCS": "green", "INFY": "red" }
  // Drives the price flash animation on stock cards.

  const clientRef = useRef(null)

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () =>
        new SockJS('http://localhost:8080/ws'),
      // SockJS connects to the endpoint registered in WebSocketConfig.

      onConnect: () => {
        setConnected(true)

        client.subscribe('/topic/prices', (message) => {
          // Receives: { "ticker": "TCS", "price": 3512.50 }
          const { ticker, price } = JSON.parse(message.body)

          setPrices(prev => {
            const prevPrice = prev[ticker]
            const direction = prevPrice
              ? price > prevPrice ? 'green' : 'red'
              : null

            // Flash the card for 600ms then clear
            if (direction) {
              setFlashMap(f => ({ ...f, [ticker]: direction }))
              setTimeout(() =>
                setFlashMap(f => ({ ...f, [ticker]: null })), 600)
            }

            return { ...prev, [ticker]: price }
          })
        })
      },

      onDisconnect: () => setConnected(false),

      onStompError: (frame) => {
        console.error('WebSocket error:', frame)
        setConnected(false)
      },

      reconnectDelay: 5000
      // Auto-reconnect after 5 seconds if connection drops.
    })

    client.activate()
    clientRef.current = client

    return () => {
      // Cleanup on component unmount.
      client.deactivate()
    }
  }, [])

  return { prices, connected, flashMap }
}