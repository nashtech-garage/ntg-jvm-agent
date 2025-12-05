/**
 * Generate a random default avatar using placeholder service
 * Uses UI Avatars or similar service to generate initials-based avatars
 */
export function getDefaultAgentAvatar(agentName: string): string {
  // Using UI Avatars service which generates nice avatars from initials
  const initials = agentName
    .split(' ')
    .map((word) => word.charAt(0))
    .join('')
    .toUpperCase()
    .slice(0, 2);

  // Generate a random background color
  const colors = ['FF6B6B', '4ECDC4', '45B7D1', 'FFA07A', '98D8C8', 'F7DC6F', 'BB8FCE'];
  const randomColor = colors[Math.floor(Math.random() * colors.length)];

  // Using UI Avatars service
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(initials)}&background=${randomColor}&color=fff&bold=true&size=200`;
}

