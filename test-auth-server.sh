#!/bin/bash

echo "=== Testing OAuth2 Authorization Server ==="
echo ""

# Server configuration
AUTH_SERVER="http://localhost:9000"
CLIENT_ID="mcp-client"
CLIENT_SECRET="mcp-secret"

echo "1. Testing OAuth2 Discovery Endpoint..."
curl -s "$AUTH_SERVER/.well-known/oauth-authorization-server" | jq . || echo "Failed to get discovery info"
echo ""

echo "2. Testing JWKS Endpoint..."
curl -s "$AUTH_SERVER/oauth2/jwks" | jq . || echo "Failed to get JWKS"
echo ""

echo "3. Testing Client Credentials Grant..."
TOKEN_RESPONSE=$(curl -s -X POST "$AUTH_SERVER/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "$CLIENT_ID:$CLIENT_SECRET" \
  -d "grant_type=client_credentials")

echo "$TOKEN_RESPONSE" | jq .
ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
echo ""

if [ "$ACCESS_TOKEN" != "null" ] && [ "$ACCESS_TOKEN" != "" ]; then
    echo "4. Testing Token Introspection..."
    curl -s -X POST "$AUTH_SERVER/oauth2/introspect" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -u "$CLIENT_ID:$CLIENT_SECRET" \
      -d "token=$ACCESS_TOKEN" | jq .
    echo ""

    echo "5. Testing Token Revocation..."
    curl -s -X POST "$AUTH_SERVER/oauth2/revoke" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -u "$CLIENT_ID:$CLIENT_SECRET" \
      -d "token=$ACCESS_TOKEN"
    echo "Token revoked (no response expected)"
else
    echo "Failed to get access token, skipping introspection and revocation tests"
fi

echo ""
echo "=== Authorization Server Testing Complete ==="
echo ""
echo "For Authorization Code flow, open this URL in your browser:"
echo "$AUTH_SERVER/oauth2/authorize?response_type=code&client_id=$CLIENT_ID&redirect_uri=http://localhost:8080/authorize/oauth2/code/authserver&scope=openid"
