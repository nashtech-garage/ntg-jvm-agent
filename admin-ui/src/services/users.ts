import { API_PATH } from '@/constants/url';
import { User } from '@/models/user';

interface UpdateUserPayload {
  username: string;
  name: string;
  email: string;
}

export async function updateUser(id: string, payload: UpdateUserPayload): Promise<User> {
  const res = await fetch(API_PATH.USER_BY_ID(id), {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const errData = await res.json().catch(() => ({}));
    throw new Error(errData.message || errData.error || 'Failed to update user');
  }

  return res.json();
}
