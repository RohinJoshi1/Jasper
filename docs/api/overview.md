<!-- api/overview.md -->
# API Overview

This API allows you to process data streams efficiently with built-in error handling.

## Authentication

All API requests require an API key in the header:

```javascript
{
  "Authorization": "Bearer YOUR_API_KEY"
}
```

## Error Handling

The API uses standard HTTP response codes:

| Code | Description |
|------|-------------|
| 200  | Success |
| 400  | Bad Request |
| 401  | Unauthorized |
| 403  | Forbidden |
| 404  | Not Found |
| 500  | Server Error |

<!-- api/endpoints.md -->
# API Endpoints

## Process Stream

/**
 * @xml
 * <endpoint>
 *   <path>/api/v1/process</path>
 *   <method>POST</method>
 *   <description>
 *     Process a data stream with optional configuration.
 *   </description>
 *   <parameters>
 *     <param name="stream" type="Stream" required="true">
 *       The input data stream
 *     </param>
 *     <param name="config" type="object" required="false">
 *       Processing configuration options
 *     </param>
 *   </parameters>
 *   <responses>
 *     <response code="200">Successfully processed stream</response>
 *     <response code="400">Invalid stream format</response>
 *     <response code="401">Invalid API key</response>
 *   </responses>
 * </endpoint>
 */