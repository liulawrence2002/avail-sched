import { useState, useEffect, useCallback } from 'react';
import { getEvent, joinParticipant, getAvailability } from '../api.js';

/**
 * useLocalStorage — syncs state with localStorage.
 */
export function useLocalStorage(key, initialValue) {
  const [stored, setStored] = useState(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch {
      return initialValue;
    }
  });

  const setValue = useCallback(
    (value) => {
      try {
        const valueToStore = value instanceof Function ? value(stored) : value;
        setStored(valueToStore);
        window.localStorage.setItem(key, JSON.stringify(valueToStore));
      } catch {
        // silently ignore storage errors
      }
    },
    [key, stored]
  );

  const removeValue = useCallback(() => {
    try {
      window.localStorage.removeItem(key);
      setStored(initialValue);
    } catch {
      // silently ignore
    }
  }, [key, initialValue]);

  return [stored, setValue, removeValue];
}

/**
 * useApi — generic API call hook with loading/error states.
 */
export function useApi(apiFn) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const execute = useCallback(
    async (...args) => {
      setLoading(true);
      setError(null);
      try {
        const result = await apiFn(...args);
        if (!result.ok) {
          setError(result.error);
          setData(null);
          return result;
        }
        setData(result.data);
        return result;
      } catch (err) {
        setError(err);
        setData(null);
        return { ok: false, error: err };
      } finally {
        setLoading(false);
      }
    },
    [apiFn]
  );

  const reset = useCallback(() => {
    setData(null);
    setError(null);
    setLoading(false);
  }, []);

  return { data, loading, error, execute, reset, setData };
}

/**
 * useParticipantToken — manages participant token in localStorage per event.
 * Key format: goblin_participant_{publicId}
 */
export function useParticipantToken(publicId) {
  const key = publicId ? `goblin_participant_${publicId}` : null;
  const [token, setToken, removeToken] = useLocalStorage(key, null);

  const storeToken = useCallback(
    (newToken) => {
      if (key && newToken) setToken(newToken);
    },
    [key, setToken]
  );

  return { token, storeToken, removeToken };
}

/**
 * useEvent — fetches event data and manages participant token.
 */
export function useEvent(publicId) {
  const { token, storeToken } = useParticipantToken(publicId);
  const eventApi = useApi(getEvent);
  const joinApi = useApi(joinParticipant);
  const availabilityApi = useApi(getAvailability);

  const { data: event, loading: eventLoading, error: eventError, execute: fetchEvent } = eventApi;

  const loadAvailability = useCallback(
    async (participantToken) => {
      if (!publicId || !participantToken) return null;
      return availabilityApi.execute(publicId, participantToken);
    },
    [publicId, availabilityApi]
  );

  const join = useCallback(
    async (participantData) => {
      if (!publicId) return null;
      const result = await joinApi.execute(publicId, participantData);
      if (result.ok && result.data?.participantToken) {
        storeToken(result.data.participantToken);
      }
      return result;
    },
    [publicId, joinApi, storeToken]
  );

  useEffect(() => {
    if (publicId) {
      fetchEvent(publicId);
    }
  }, [publicId, fetchEvent]);

  return {
    event,
    eventLoading,
    eventError,
    token,
    storeToken,
    join,
    joinLoading: joinApi.loading,
    joinError: joinApi.error,
    loadAvailability,
    availabilityLoading: availabilityApi.loading,
    availabilityError: availabilityApi.error,
  };
}
