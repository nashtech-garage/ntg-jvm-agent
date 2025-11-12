import { NextResponse } from 'next/server';
import { TokenInfo } from '../models/token';
import { Constants } from './constants';

// Decodes the payload of a JWT token from base64 and parses it as JSON.
export function decodeToken(token: string) {
  try {
    return JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
  } catch (error) {
    console.error('Failed to decode token:', error);
    return null;
  }
}

export async function getRefreshToken(refreshToken: string): Promise<TokenInfo | null> {
  try {
    const tokenUrl = `${process.env.NEXT_PUBLIC_AUTH_SERVER}/oauth2/token`;
    const clientId = process.env.CLIENT_ID;
    const clientSecret = process.env.CLIENT_SECRET;
    const basic = Buffer.from(`${clientId}:${clientSecret}`).toString('base64');

    const body = new URLSearchParams();
    body.set('grant_type', 'refresh_token');
    body.set('refresh_token', refreshToken);

    const res = await fetch(tokenUrl, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${basic}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: body.toString(),
    });

    if (!res.ok) {
      return null;
    }

    return await res.json();
  } catch (error) {
    console.error('Token refresh failed:', error);
    return null;
  }
}

export function setTokenIntoCookie(tokenInfo: TokenInfo, res: NextResponse) {
  if (tokenInfo.access_token) {
    res.cookies.set('access_token', tokenInfo.access_token, {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: tokenInfo.expires_in ?? 3600,
    });
  }

  // Keep refresh token on server-side using setting httpOnly cookie
  if (tokenInfo.refresh_token) {
    res.cookies.set('refresh_token', tokenInfo.refresh_token, {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: Constants.THIRTY_DAYS_IN_SECONDS,
    });
  }

  return res;
}

export const getFileExtension = (file: File) => {
  const fileName = file.name;
  const extension = fileName.substring(fileName.lastIndexOf('.'));
  return extension;
};

export const allowedExtensionsRegex = /(\.txt|\.md|\.pdf)$/i;

export const checkFileType = (file: File, regExp: RegExp = allowedExtensionsRegex): boolean => {
  const extension = getFileExtension(file);
  if (!extension) {
    return false;
  }

  const allowed = regExp.test(extension);
  return allowed;
};

const PDFExtension = '.pdf';

/**
 * PDF format's magic numbers, in base 10.
 * @see https://en.wikipedia.org/wiki/List_of_file_signatures#cite_ref-36
 */
const PDFMagicNumbers = [37, 80, 68, 70, 45];

export const isPDFFile = (file: File): boolean => {
  const extension = getFileExtension(file);
  return extension === PDFExtension;
};

/**
 * Check if a file is a PDF file by checking file extension and PDF's magic numbers.
 * @param file the input file
 * @returns true if file is a PDF file, false otherwise
 */
export const isValidPDFFile = async (file: File) => {
  console.log('check fpd file');

  if (isPDFFile(file)) {
    const arrayBuffer = await file.slice(0, 5).arrayBuffer(); // get first 5 bytes
    const bytes = new Uint8Array(arrayBuffer);
    const isPDFHeader = bytes.every((b, i) => PDFMagicNumbers[i] === b);
    return isPDFHeader;
  }
  return false;
};
