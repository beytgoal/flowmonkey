import type { VercelRequest, VercelResponse } from '@vercel/node';

/**
 * Fast WebGL/SVG Keyframe Preview Endpoint.
 */
export default async function handler(req: VercelRequest, res: VercelResponse) {
  const { compositionId = 'KineticTypography', frame = 30 } = req.query;

  return res.status(200).json({
    status: 'OK',
    compositionId,
    previewFrame: Number(frame),
    previewThumbnail: `https://storage.googleapis.com/flowmonkey-vfx-cloud/previews/${compositionId}_f${frame}.jpg`,
    timestamp: new Date().toISOString()
  });
}
