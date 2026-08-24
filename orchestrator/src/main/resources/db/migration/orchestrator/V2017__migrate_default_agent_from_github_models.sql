UPDATE agent
SET base_url = 'https://api.openai.com/v1',
    chat_completions_path = '/chat/completions',
    model = 'gpt-4o-mini',
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE name = 'Default agent'
  AND base_url = 'https://models.github.ai/inference'
  AND api_key = 'Dummy Github API key';
