'use server';

import { cookies } from 'next/headers';
import { KnowledgeInput, KnowledgeResponse } from '../models/knowledge';
import { checkFileType, isPDFFile, isValidPDFFile } from '../utils/utils';

export async function addKnowledge(formData: KnowledgeInput): Promise<KnowledgeResponse> {
  const file = formData?.knowledge?.[0];

  if (!file) {
    return { success: false, error: 'File is required' };
  }

  if (!checkFileType(file)) {
    return { success: false, error: 'File extension not supported' };
  }

  if (isPDFFile(file) && !(await isValidPDFFile(file))) {
    return { success: false, error: 'Wrong PDF file' };
  }

  const knowledgeUrl = `${process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER}/api/knowledge`;
  const cookieStore = cookies();
  const accessToken = (await cookieStore).get('access_token')?.value;
  if (!accessToken) {
    return { success: false, error: 'No access token found' };
  }

  const body = new FormData();
  body.append('file', file);
  const result = await fetch(knowledgeUrl, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: body,
  });
  if (result.ok) {
    return { success: true };
  } else {
    console.error(`${result.status} error from add knowledge action`, await result.json());
    return { success: false, error: 'Failed to import document' };
  }
}
