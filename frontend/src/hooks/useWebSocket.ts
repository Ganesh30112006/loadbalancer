import { useEffect, useRef, useState, useCallback } from 'react';
import type { WsMessage, WsMetricsData, DashboardData } from '../types';

const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8082/ws/metrics';

interface UseWebSocketOptions {
  onMetrics?: (data: WsMetricsData) => void;
  onDashboardUpdate?: (data: DashboardData) => void;
  onError?: (error: string) => void;
  reconnectInterval?: number;
}

export function useMetricsWebSocket(options: UseWebSocketOptions = {}) {
  const { onMetrics, onDashboardUpdate, onError, reconnectInterval = 5000 } = options;
  const wsRef = useRef<WebSocket | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [subscribedServices, setSubscribedServices] = useState<Set<string>>(new Set());
  const [isDashboardSubscribed, setIsDashboardSubscribed] = useState(false);
  const reconnectTimeoutRef = useRef<number | null>(null);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    const ws = new WebSocket(WS_URL);

    ws.onopen = () => {
      console.log('WebSocket connected');
      setIsConnected(true);
      
      // Resubscribe to previously subscribed services
      subscribedServices.forEach(serviceId => {
        ws.send(JSON.stringify({ action: 'subscribe', serviceId }));
      });
      
      // Resubscribe to dashboard if was subscribed
      if (isDashboardSubscribed) {
        ws.send(JSON.stringify({ action: 'subscribe_dashboard' }));
      }
    };

    ws.onclose = () => {
      console.log('WebSocket disconnected');
      setIsConnected(false);
      
      // Attempt reconnect
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      reconnectTimeoutRef.current = window.setTimeout(connect, reconnectInterval);
    };

    ws.onerror = (event) => {
      console.error('WebSocket error:', event);
      onError?.('WebSocket connection error');
    };

    ws.onmessage = (event) => {
      try {
        const message: WsMessage = JSON.parse(event.data);
        
        switch (message.type) {
          case 'metrics':
            onMetrics?.(message.data as WsMetricsData);
            break;
          case 'dashboard_update':
            onDashboardUpdate?.(message.data as DashboardData);
            break;
          case 'error':
            onError?.((message.data as { message: string }).message);
            break;
          case 'connected':
          case 'subscribed':
          case 'unsubscribed':
          case 'dashboard_subscribed':
          case 'dashboard_unsubscribed':
            // Info messages, can be logged if needed
            break;
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    wsRef.current = ws;
  }, [onMetrics, onError, reconnectInterval, subscribedServices]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
    wsRef.current?.close();
    wsRef.current = null;
    setIsConnected(false);
  }, []);

  const subscribe = useCallback((serviceId: string) => {
    setSubscribedServices(prev => new Set(prev).add(serviceId));
    
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ action: 'subscribe', serviceId }));
    }
  }, []);

  const unsubscribe = useCallback((serviceId: string) => {
    setSubscribedServices(prev => {
      const next = new Set(prev);
      next.delete(serviceId);
      return next;
    });
    
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ action: 'unsubscribe', serviceId }));
    }
  }, []);

  const refresh = useCallback((serviceId: string) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ action: 'refresh', serviceId }));
    }
  }, []);

  const subscribeDashboard = useCallback(() => {
    setIsDashboardSubscribed(true);
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ action: 'subscribe_dashboard' }));
    }
  }, []);

  const unsubscribeDashboard = useCallback(() => {
    setIsDashboardSubscribed(false);
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ action: 'unsubscribe_dashboard' }));
    }
  }, []);

  const refreshDashboard = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ action: 'refresh_dashboard' }));
    }
  }, []);

  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    isConnected,
    subscribe,
    unsubscribe,
    refresh,
    subscribeDashboard,
    unsubscribeDashboard,
    refreshDashboard,
    disconnect,
    reconnect: connect,
  };
}
