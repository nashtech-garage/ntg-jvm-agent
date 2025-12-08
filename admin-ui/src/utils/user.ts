const ADMIN_ROLE = ['ADMIN'];

export const hasAdminRole = (roles: string[] = []): boolean => {
  return roles.some((role) => ADMIN_ROLE.includes(role.toUpperCase()));
};

export const normalizeRoles = (roles: unknown): string[] => {
  if (Array.isArray(roles)) {
    return roles.map((role) => String(role));
  }
  if (roles) {
    return [String(roles)];
  }
  return [];
};
