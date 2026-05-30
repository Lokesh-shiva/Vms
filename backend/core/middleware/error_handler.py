from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
import traceback


class ErrorHandlerMiddleware(BaseHTTPMiddleware):
    """
    Global Error Handler Middleware.

    Responsibilities:
    - Catches unhandled exceptions from all routes.
    - Formats error responses consistently using the standard format.
    - Logs errors for monitoring.
    """

    async def dispatch(self, request: Request, call_next):
        try:
            response = await call_next(request)
            return response
        except Exception:
            traceback.print_exc()
            return JSONResponse(
                status_code=500,
                content={
                    "success": False,
                    "data": None,
                    "message": "Internal server error.",
                },
            )
