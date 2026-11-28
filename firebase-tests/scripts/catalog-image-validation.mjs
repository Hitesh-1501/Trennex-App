import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';

const MAX_BYTES = 10 * 1024 * 1024;
const MAX_DIMENSION = 8192;
const MAX_PIXELS = 24_000_000;
const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

function dimensionsOk(width, height) {
  return Number.isInteger(width) && Number.isInteger(height)
    && width > 0 && height > 0
    && width <= MAX_DIMENSION && height <= MAX_DIMENSION
    && width * height <= MAX_PIXELS;
}

function jpegDimensions(bytes) {
  if (bytes.length < 12 || bytes[0] !== 0xff || bytes[1] !== 0xd8
      || bytes[bytes.length - 2] !== 0xff || bytes[bytes.length - 1] !== 0xd9) {
    throw new Error('Invalid or truncated JPEG.');
  }
  let offset = 2;
  while (offset + 4 <= bytes.length - 2) {
    if (bytes[offset] !== 0xff) throw new Error('Invalid JPEG marker.');
    while (bytes[offset] === 0xff) offset += 1;
    const marker = bytes[offset++];
    if (marker === 0xda) break; // Start of compressed scan data.
    if (marker === 0xd8 || marker === 0xd9 || (marker >= 0xd0 && marker <= 0xd7)) continue;
    if (offset + 2 > bytes.length) throw new Error('Truncated JPEG segment.');
    const length = bytes.readUInt16BE(offset);
    if (length < 2 || offset + length > bytes.length - 2) throw new Error('Invalid JPEG segment.');
    if ([0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7,
      0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf].includes(marker)) {
      if (length < 7) throw new Error('Invalid JPEG frame.');
      return { width: bytes.readUInt16BE(offset + 5), height: bytes.readUInt16BE(offset + 3) };
    }
    offset += length;
  }
  throw new Error('JPEG dimensions are unavailable.');
}

export function inspectCatalogImage(bytes, fileName) {
  const extension = path.extname(fileName).toLowerCase();
  if (!['.jpg', '.jpeg', '.png'].includes(extension)) throw new Error('Unsupported catalog image type.');
  if (!Buffer.isBuffer(bytes) || bytes.length === 0 || bytes.length > MAX_BYTES) {
    throw new Error('Catalog image size is invalid.');
  }
  let dimensions;
  let contentType;
  if (extension === '.png') {
    if (bytes.length < 45 || !bytes.subarray(0, 8).equals(PNG_SIGNATURE)
        || bytes.readUInt32BE(8) !== 13 || bytes.toString('ascii', 12, 16) !== 'IHDR'
        || bytes.toString('ascii', bytes.length - 8, bytes.length - 4) !== 'IEND') {
      throw new Error('Invalid or truncated PNG.');
    }
    dimensions = { width: bytes.readUInt32BE(16), height: bytes.readUInt32BE(20) };
    contentType = 'image/png';
  } else {
    dimensions = jpegDimensions(bytes);
    contentType = 'image/jpeg';
  }
  if (!dimensionsOk(dimensions.width, dimensions.height)) {
    throw new Error('Catalog image dimensions exceed the approved limit.');
  }
  return { ...dimensions, contentType, byteLength: bytes.length };
}

export async function validateCatalogAsset(root, assetName) {
  const absoluteRoot = path.resolve(root);
  const filePath = path.resolve(absoluteRoot, assetName);
  if (!filePath.startsWith(`${absoluteRoot}${path.sep}`)) {
    throw new Error('Catalog asset must stay inside its approved directory.');
  }
  const info = await stat(filePath);
  if (!info.isFile() || info.size === 0 || info.size > MAX_BYTES) {
    throw new Error('Catalog image size is invalid.');
  }
  return { filePath, ...inspectCatalogImage(await readFile(filePath), assetName) };
}
