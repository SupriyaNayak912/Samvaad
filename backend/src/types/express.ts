import { Request } from 'express';

export interface CustomRequest extends Request {
  user?: {
    uid: string;
    email: string;
    id: number;
  };
}
