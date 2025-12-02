// Disable specific no-console ESLint rules for this logger utility
/* eslint-disable no-console */
type LogType = 'log' | 'warn' | 'error';

const createLogger = (level: LogType, message: string, data?: unknown) => {
  const formattedMessage = `[${level.toUpperCase()}]: ${message}`;
  if (data !== undefined) {
    console[level](formattedMessage, data);
  } else {
    console[level](formattedMessage);
  }
};

const error = (message: string, data?: unknown) => {
  createLogger('error', message, data);
};

const info = (message: string, data?: unknown) => {
  createLogger('log', message, data);
};

const warn = (message: string, data?: unknown) => {
  createLogger('warn', message, data);
};

const logger = { info, warn, error };

export default logger;
