import { type NextAuthOptions } from 'next-auth';
import { PUBLIC_CONFIG, SERVER_CONFIG } from '@/constants/site-config';
import { decodeToken, refreshAccessToken } from '@/utils/server-utils';
import logger from '@/utils/logger';
import { normalizeRoles } from '@/utils/user';
import { exchangeAuthorizationCode } from '@/services/auth';
import { API_PATH, PAGE_PATH } from '@/constants/url';

const authServerURL = {
  authorize: `${SERVER_CONFIG.AUTH_SERVER}/oauth2/authorize`,
  userInfo: `${SERVER_CONFIG.AUTH_SERVER}/userinfo`,
  token: `${SERVER_CONFIG.AUTH_SERVER}/oauth2/token`,
};

/* IMPORTANT: Need to add allowed list in redirectURI in Auth Server
  Template: <app_host>/api/auth/callback/<provider_id>
  Sample: http://localhost:30001/api/auth/callback/auth-server
*/
export const authOptions: NextAuthOptions = {
  providers: [
    {
      id: 'auth-server',
      name: 'Authorization Server',
      type: 'oauth',
      clientId: SERVER_CONFIG.CLIENT_ID,
      clientSecret: SERVER_CONFIG.CLIENT_SECRET,
      authorization: {
        url: authServerURL.authorize,
        params: {
          scope: PUBLIC_CONFIG.SCOPE,
          response_type: 'code',
        },
      },
      token: {
        /* Authorization server does NOT support the standard OAuth token exchange that NextAuth expects
          TODO: remove it once BE supports the standard exchange.
         */
        async request({ provider, params }) {
          const redirectUri =
            provider.callbackUrl ??
            `${SERVER_CONFIG.NEXTAUTH_URL}${API_PATH.AUTH_CALLBACK(provider.id)}`;

          const tokens = await exchangeAuthorizationCode({
            requestTokenUri: authServerURL.token,
            code: String(params?.code ?? ''),
            redirectUri,
            codeVerifier: params?.code_verifier ? String(params.code_verifier) : undefined,
          });
          return { tokens };
        },
      },
      // fetch userInfo from Auth Server
      userinfo: authServerURL.userInfo,
      // Runs once during the OAuth callback to shape the user object from userinfo/token claims
      // Return `user` object which is seen in NextAuth callbacks/session
      profile: (profile, tokens) => {
        const currentProfile = profile as Record<string, unknown>;
        const decoded = decodeToken(tokens?.id_token ?? tokens.access_token ?? '');
        const roles = normalizeRoles(currentProfile.roles ?? decoded?.roles ?? []);

        return {
          id: currentProfile.sub ?? decoded?.sub ?? null,
          name:
            currentProfile.name ??
            currentProfile.preferred_username ??
            decoded?.preferred_username ??
            null,
          email: currentProfile.email ?? decoded?.email ?? null,
          roles,
        };
      },
    },
  ],
  pages: {
    signIn: PAGE_PATH.LOGIN,
    error: PAGE_PATH.LOGIN,
  },
  session: {
    strategy: 'jwt',
    // 7 days for the session-token cookie
    maxAge: 60 * 60 * 24 * 7,
    // refresh the JWT every 1 hour
    updateAge: 60 * 60,
  },
  callbacks: {
    async signIn({ user, account }) {
      logger.info(`User ${user.id} is signing in with provider ${account?.provider}`);
      return true;
    },
    // Runs on sign-in and before getSession/useSession/getServerSession/getToken to update JWT (including refresh)
    async jwt({ token, account, user }) {
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.expiresAt = account.expires_at ? account.expires_at * 1000 : undefined;
        token.roles =
          user.roles ?? decodeToken(account.id_token ?? account.access_token ?? '')?.roles ?? [];
      }

      const tokenIsExpired = token.expiresAt ? Date.now() > token.expiresAt - 60_000 : false;

      if (tokenIsExpired) {
        return refreshAccessToken(token);
      }

      return token;
    },
    // Fields added here are returned in the session from getSession()/useSession()/getServerSession()
    async session({ session, token }) {
      session.accessToken = token.accessToken as string | undefined;
      session.refreshToken = token.refreshToken as string | undefined;
      session.error = token.error as string | undefined;
      session.user = {
        ...session.user,
        id: token.sub || '',
        roles: (token.roles as string[]) ?? [],
      };
      return session;
    },
  },
};
