export const Constants = {
  THIRTY_DAYS_IN_SECONDS: 60 * 60 * 24 * 30,
  QUESTION_TYPE: 'QUESTION',
  ANSWER_TYPE: 'ANSWER',
  FAILED_TO_ASK_QUESTION: 'Failed to ask question',
  FAILED_TO_DELETE_CONVERSATION_MSG: 'Failed to delete conversation',
  FAILED_TO_FETCH_CONVERSATIONS_MSG: 'Failed to fetch conversations',
  FAILED_TO_FETCH_USER_INFO_MSG: 'Failed to fetch user info',
  DELETE_CONVERSATION_SUCCESS_MSG: 'Deleted conversation successfully',
  FAILED_TO_FETCH_AGENTS_MSG: 'Failed to fetch agents',
};

export enum LoginParams {
  ERROR = 'error',
}

export enum LoginErrors {
  ACCESS_DENIED = 'access_denied',
  ACCESS_DENIED_NEXTAUTH = 'AccessDenied',
  OAUTH_SIGNIN_ERROR = 'OAuthSignin',
  SESSION_EXPIRED = 'SessionExpired',
  REFRESH_TOKEN_MISSING = 'RefreshTokenMissing',
  REFRESH_TOKEN_ERROR = 'RefreshAccessTokenError',
}
