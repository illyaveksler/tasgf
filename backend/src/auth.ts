import type { Request, Response, NextFunction, RequestHandler } from "express";
import { OAuth2Client } from "google-auth-library";

const CLIENT_ID = "556640136929-dh9v14r9g92nshgdalvf5odegulaj126.apps.googleusercontent.com";
const client = new OAuth2Client(CLIENT_ID);

/**
 * asyncWrapper wraps asynchronous middleware functions to catch errors.
 */
export function asyncWrapper(fn: RequestHandler): RequestHandler {
  return (req: Request, res: Response, next: NextFunction) =>
    Promise.resolve(fn(req, res, next)).catch(next);
}

/**
 * authenticateToken verifies the Google ID token sent in the Authorization header.
 */
export const authenticateToken = asyncWrapper(async (req: Request, res: Response, next: NextFunction): Promise<void> => {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith("Bearer ")) {
    res.status(401).json({ error: "Missing or invalid authorization header" });
    return;
  }

  const token = authHeader.split(" ")[1];
  try {
    const ticket = await client.verifyIdToken({
      idToken: token,
      audience: CLIENT_ID,
    });
    req.user = ticket.getPayload();
    next();
  } catch (error) {
    console.error("Token verification failed:", error);
    res.status(403).json({ error: "Invalid token" });
  }
});
