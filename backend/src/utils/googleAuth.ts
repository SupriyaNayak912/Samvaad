import axios, { AxiosError } from 'axios';

const GOOGLE_OAUTH_URL = 'https://oauth2.googleapis.com/token';

export async function verifyGoogleToken(idToken: string): Promise<any> {
  try {
    const response = await axios.get(
      `https://www.googleapis.com/oauth2/v1/tokeninfo?id_token=${idToken}`
    );
    return response.data;
  } catch (error) {
    throw new Error('Invalid Google token');
  }
}

export async function getGoogleProfile(accessToken: string): Promise<any> {
  try {
    const response = await axios.get(
      'https://www.googleapis.com/oauth2/v2/userinfo',
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );
    return response.data;
  } catch (error) {
    throw new Error('Failed to fetch Google profile');
  }
}
