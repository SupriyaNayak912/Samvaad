import AWS from 'aws-sdk';
}
  }
    throw new Error('Failed to delete file from S3');
    console.error('S3 delete error:', error);
  } catch (error: any) {
    await s3.deleteObject(params).promise();
  try {

  };
    Key: fileName,
    Bucket: process.env.S3_BUCKET || 'samvaad-videos',
  const params = {
export async function deleteFileFromS3(fileName: string): Promise<void> {

}
  }
    throw new Error('Failed to upload file to S3');
    console.error('S3 upload error:', error);
  } catch (error: any) {
    return result.Location;
    const result = await s3.upload(params).promise();
  try {

  };
    ACL: 'public-read',
    ContentType: contentType,
    Body: fileBuffer,
    Key: `videos/${Date.now()}_${fileName}`,
    Bucket: process.env.S3_BUCKET || 'samvaad-videos',
  const params = {
): Promise<string> {
  contentType: string
  fileName: string,
  fileBuffer: Buffer,
export async function uploadFileToS3(

});
  region: process.env.AWS_REGION || 'us-east-1',
  secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
  accessKeyId: process.env.AWS_ACCESS_KEY_ID,
const s3 = new AWS.S3({

dotenv.config();

import dotenv from 'dotenv';
