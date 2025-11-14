// Decodes the payload of a JWT token from base64 and parses it as JSON.
export function decodeToken(token: string) {
  try {
    return JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
  } catch (error) {
    console.error('Failed to decode token:', error);
    return null;
  }
}

const isServer = typeof window === 'undefined';
export const AUTH_SERVER_URL = isServer
  ? process.env.AUTH_SERVER_INTERNAL_URL
  : process.env.NEXT_PUBLIC_AUTH_SERVER;

export const ORCHESTRATOR_URL = isServer
  ? process.env.ORCHESTRATOR_INTERNAL_URL
  : process.env.NEXT_PUBLIC_ORCHESTRATOR;

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
  if (isPDFFile(file)) {
    const arrayBuffer = await file.slice(0, 5).arrayBuffer(); // get first 5 bytes
    const bytes = new Uint8Array(arrayBuffer);
    const isPDFHeader = bytes.every((b, i) => PDFMagicNumbers[i] === b);
    return isPDFHeader;
  }
  return false;
};
