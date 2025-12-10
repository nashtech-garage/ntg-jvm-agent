export const normalizeRoles = (roles: unknown): string[] => {
  if (Array.isArray(roles)) {
    return roles.map((role) => String(role));
  }
  if (roles) {
    return [String(roles)];
  }
  return [];
};
