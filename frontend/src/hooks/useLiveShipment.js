import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = import.meta.env.VITE_WS_URL || `${window.location.origin}/ws`;

/**
 * Subscribes to one or more STOMP topics for the life of the component.
 * Pass a stable handler; it is kept in a ref so re-renders do not reconnect the socket.
 */
export default function useStompSubscription(topics, onMessage, enabled = true) {
  const handlerRef = useRef(onMessage);
  handlerRef.current = onMessage;

  const key = Array.isArray(topics) ? topics.join('|') : topics;

  useEffect(() => {
    if (!enabled || !key) return undefined;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 4000,
      onConnect: () => {
        key.split('|').forEach((topic) => {
          client.subscribe(topic, (frame) => {
            try {
              handlerRef.current(JSON.parse(frame.body), topic);
            } catch {
              handlerRef.current(frame.body, topic);
            }
          });
        });
      },
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, [key, enabled]);
}
