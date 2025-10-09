# Admin Portal Authentication

This admin portal implements OAuth 2.0 authentication with role-based access control.

## Authentication Flow

1. **Login**: Users are redirected to the authorization server for authentication
2. **Authorization**: Authorization server validates credentials and checks for admin role
3. **Token Exchange**: Admin UI exchanges authorization code for access/refresh tokens
4. **Access Control**: Only users with `ROLE_ADMIN` or `admin` role can access the portal

## Setup

### Authorization Server

The authorization server is configured with:

- **Client ID**: `demo-client`
- **Client Secret**: `demo-secret`
- **Redirect URIs**:
  - `http://localhost:3000/auth/callback` (Chat UI)
  - `http://localhost:3001/auth/callback` (Admin UI)
- **Scopes**: `openid`, `profile`, `chatbot.read`, `chatbot.write`, `admin.read`, `admin.write`

### Default Users

- **Admin User**:
  - Username: `admin`
  - Password: `password`
  - Roles: `ROLE_ADMIN`
- **Regular User**:
  - Username: `testuser`
  - Password: `password`
  - Roles: `ROLE_USER`

## Running the Applications

1. **Start Authorization Server** (Port 9000):

   ```bash
   cd authorization-server
   ./mvnw spring-boot:run
   ```

2. **Start Admin UI** (Port 3001):

   ```bash
   cd admin-ui
   npm run dev -- --port 3001
   ```

3. **Access Admin Portal**:
   - Navigate to `http://localhost:3001`
   - Login with admin credentials
   - Only admin users can access the portal

## Features

- **Secure Authentication**: OAuth 2.0 with PKCE
- **Role-Based Access**: Only admin roles allowed
- **Token Management**: Automatic token refresh
- **Session Management**: Secure HTTP-only cookies
- **Protection**: Middleware-based route protection

## API Endpoints

### Admin UI API Routes

- `POST /api/auth/exchange` - Exchange authorization code for tokens
- `GET /api/auth/me` - Get current user info and validate session
- `POST /api/auth/logout` - Logout and clear session

### Authorization Server Endpoints

- `GET /oauth2/authorize` - Authorization endpoint
- `POST /oauth2/token` - Token endpoint
- `GET /userinfo` - User information endpoint
- `GET /.well-known/openid-configuration` - OpenID Connect discovery

## Security Features

- HTTP-only cookies for token storage
- CSRF protection
- Secure session management
- Role validation on every request
- Automatic token refresh
- Protected routes with middleware
