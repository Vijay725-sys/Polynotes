/**
 * Serverless Function Entrypoint (/api/proxy)
 * Delegates all incoming requests from the Android app and WebView client to
 * the PolyNotes backend proxy handler using server-side environment variables.
 */
const { handleProxyRequest } = require("../backend/proxy-server");

module.exports = handleProxyRequest;
