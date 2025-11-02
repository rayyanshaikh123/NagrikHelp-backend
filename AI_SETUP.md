## AI Validation Setup (OCR + Gemini Text)

Phase 4 introduces AI-based image validation & automatic issue classification. The current pipeline performs OCR locally (Tesseract) and then sends the extracted text context to Gemini (text model) for classification.

### Environment Variables

Add to your backend runtime environment (e.g. `application.properties` overrides or export):

```
GEMINI_API_KEY=your_key_here
# Optional override (defaults to gemini-pro text endpoint)
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent

# OCR (if not set via properties)
TESSERACT_DATAPATH=/path/to/parent/of/tessdata
TESSERACT_LANG=eng
```

`GEMINI_API_URL` is optional; default is set in code. You may instead set `gemini.api.key` / `tesseract.datapath` / `tesseract.lang` in `application.properties`.

### Install Tesseract
macOS (Homebrew):
```
brew install tesseract
```
Check installation location:
```
brew info tesseract
```
Set `tesseract.datapath` to the directory that contains the `tessdata` folder (NOT the `tessdata` folder itself). Example:
`/opt/homebrew/Cellar/tesseract/5.4.1/share` (version may differ).

### Flow
1. Frontend converts uploaded image to Base64 (data URL) and strips the prefix.
2. Frontend calls `POST /api/ai/validate` with `{ imageBase64 }`.
3. Backend runs OCR (Tess4J) to extract any text found in the image (license plates, signage, labels, etc.).
4. Gemini text model is prompted with a strict JSON instruction + the OCR text (if any) to classify the civic issue.
4. Response structure:
```
{
  "isValid": true,
  "suggestedCategory": "POTHOLE",
  "confidence": 0.92,
  "message": "Detected road surface damage likely a pothole.",
  "provider": "gemini"
}
```
5. Frontend pre-fills category but user can override.
6. On create issue, frontend sends `aiValidation` object along with issue fields; backend stores it in embedded `aiValidation` sub-document in `Issue`.

### Data Model
`Issue.aiValidation` contains: valid, suggestedCategory, confidence, message, provider, evaluatedAt, rawLabel (raw label derived from Gemini JSON `label`).

### Notes / Future Improvements
* Replace heuristic JSON scraping with schema-validated parsing if Gemini adds structured mode.
* Add rate limiting & caching for identical images (hash-based) to reduce cost.
* Consider persisting OCR text separately for analytics / search.
* Move large images to object storage (S3, GCS) and store reference instead of raw base64.
* Add health endpoint to verify OCR datapath + API key at startup.
