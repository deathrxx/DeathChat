function errorHandler(err, req, res, next) {
  const status = err.status || err.statusCode || 500;
  const isProd = process.env.NODE_ENV === 'production';

  if (status >= 500) {
    console.error(`[SERVER ERROR] ${req.method} ${req.originalUrl}:`, err);
  }

  const response = {
    success: false,
    error: err.message || 'Internal Server Error'
  };

  if (err.requiresPassword) {
    response.requiresPassword = true;
  }

  if (!isProd && status >= 500 && err.stack) {
    response.stack = err.stack;
  }

  res.status(status).json(response);
}

module.exports = errorHandler;
