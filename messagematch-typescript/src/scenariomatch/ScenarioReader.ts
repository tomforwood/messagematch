import * as fs from 'fs/promises';

interface FileResult<T> {
    data: T;
    crc32: string;
    checksum: number;
}

/**
 * Reads a file, computes its CRC32 checksum, and deserializes it as JSON
 * @param filePath - Path to the file to read
 * @returns Object containing the parsed JSON data and CRC32 checksum
 */
async function readFileWithCRC32<T = any>(filePath: string): Promise<FileResult<T>> {
    // Read the file as a buffer
    const buffer = await fs.readFile(filePath);

    // Compute CRC32 checksum
    const crc32 = computeCRC32(buffer);

    // Convert buffer to string and parse as JSON
    const text = buffer.toString('utf-8');
    const data = JSON.parse(text) as T;

    return {
        data,
        crc32: crc32.toString(16).padStart(8, '0'), // Hex string representation
        checksum: crc32 // Numeric value
    };
}

/**
 * Computes CRC32 checksum for a buffer
 * @param buffer - Buffer to compute checksum for
 * @returns CRC32 checksum as a number
 */
function computeCRC32(buffer: Buffer): number {
    const crcTable = generateCRC32Table();
    let crc = 0xFFFFFFFF;

    for (let i = 0; i < buffer.length; i++) {
        const byte = buffer[i];
        const index = (crc ^ byte) & 0xFF;
        crc = (crc >>> 8) ^ crcTable[index];
    }

    return (crc ^ 0xFFFFFFFF) >>> 0;
}

/**
 * Generates the CRC32 lookup table
 * @returns Array of CRC32 table values
 */
function generateCRC32Table(): number[] {
    const table: number[] = [];

    for (let i = 0; i < 256; i++) {
        let crc = i;
        for (let j = 0; j < 8; j++) {
            crc = (crc & 1) ? (0xEDB88320 ^ (crc >>> 1)) : (crc >>> 1);
        }
        table[i] = crc >>> 0;
    }

    return table;
}



export { readFileWithCRC32, computeCRC32, FileResult };